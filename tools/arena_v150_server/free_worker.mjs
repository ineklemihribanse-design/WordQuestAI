import {DurableObject} from 'cloudflare:workers';
import {createHmac} from 'node:crypto';
import {createArenaHandler} from '../v132_engine/arena_gateway.mjs';
import {sweepRoom} from '../v132_engine/presence.mjs';
import {safeNickname} from '../v132_engine/profiles.mjs';
import {createVocabularyBank} from './vocabulary_bank.mjs';
import {roomDatabase,nextAlarm,handleFetch} from './free_room_adapter.mjs';
import {authenticate} from './free_auth.mjs';
import bank from './arena-vocabulary-v1.json';
const bankBuilder=createVocabularyBank(bank);
const json=(status,body)=>new Response(JSON.stringify(body),{status,headers:{'Content-Type':'application/json','Cache-Control':'no-store'}});
const roomId=x=>typeof x==='string'&&/^[A-Za-z0-9_-]{6,64}$/.test(x)&&!['constructor','prototype','__proto__'].includes(x);
const requestId=x=>typeof x==='string'&&/^[A-Za-z0-9_-]{1,100}$/.test(x)&&!['constructor','prototype','__proto__'].includes(x);
async function smallBody(req,max){
 const len=Number(req.headers.get('Content-Length')??0);if(len>max)throw Error('Body too large');
 const reader=req.body?.getReader();if(!reader)throw Error('Missing body');
 const chunks=[];let count=0;
 try{for(;;){const {done,value}=await reader.read();if(done)break;count+=value.length;if(count>max)throw Error('Body too large');chunks.push(value);}}finally{reader.releaseLock();}
 const bytes=new Uint8Array(count);let offset=0;for(const c of chunks){bytes.set(c,offset);offset+=c.length;}
 return bytes;
}
export default {async fetch(req,env){
 const url=new URL(req.url),path=url.pathname;
 if(path==='/healthz'&&req.method==='GET')return json(200,{ok:true,schema:150,hosting:'workers-free'});
 if(!env.ARENA_ROOM_SECRET||env.ARENA_ROOM_SECRET.length<32)return json(503,{ok:false,code:'not-configured'});
 const match=/^\/v150\/(?:matches|lobbies|profiles|presence)\/([A-Za-z0-9_-]+)(?:\/commands)?$/.exec(path);
 const create=path==='/v150/lobbies'&&req.method==='POST';
 if(!create&&!roomId(match?.[1]))return json(404,{ok:false,code:'not-found'});
 const bearer=/^Bearer ([^\s]+)$/.exec(req.headers.get('Authorization')??'');
 if(!bearer)return json(401,{ok:false,code:'unauthenticated'});
 let identity;
 try{identity=await authenticate(bearer[1],env);}catch{return json(401,{ok:false,code:'unauthenticated'});}
 let id=match?.[1],body;
 try{
  if(req.method==='POST')body=await smallBody(req,path.startsWith('/v150/profiles/')?190000:8192);
  if(create){
   const data=JSON.parse(new TextDecoder().decode(body));
   if(!requestId(data.id))return json(400,{ok:false,code:'invalid-command'});
   id=createHmac('sha256',env.ARENA_ROOM_SECRET).update(JSON.stringify([identity.uid,data.id])).digest('hex').slice(0,20);
   // Explicit cap: ten new rooms per UTC day per authenticated UID.
   const quota=env.ROOMS.get(env.ROOMS.idFromName('quota:'+identity.uid));
   const q=await quota.fetch('https://internal/quota',{method:'POST',body:id});
   if(!q.ok)return json(429,{ok:false,code:'room-quota'});
  }
 }catch{return json(400,{ok:false,code:'invalid-command'});}
 // Only the user's own nickname is read, using that user's limited Firebase
 // token. No service account or global database access.
 let nickname;
 if(req.method==='POST'&&path.startsWith('/v150/profiles/')&&env.FIREBASE_DATABASE_URL){
  try{
   const db=new URL(env.FIREBASE_DATABASE_URL);
   if(db.protocol==='https:'&&(/\.firebaseio\.com$|\.firebasedatabase\.app$/.test(db.hostname))){
    db.pathname='/users/'+encodeURIComponent(identity.uid)+'/nickname.json';
    db.search='';db.searchParams.set('auth',bearer[1]);
    const res=await fetch(db,{redirect:'error'});
    if(res.ok)nickname=safeNickname(await res.json());
   }
  }catch{}
 }
 const stub=env.ROOMS.get(env.ROOMS.idFromName('room:'+id));
 const h=new Headers({'x-internal-uid':identity.uid,'x-internal-room':id});
 if(nickname)h.set('x-internal-nickname',encodeURIComponent(nickname));
 return stub.fetch(new Request('https://internal'+path+url.search,{method:req.method,headers:h,body}));
}};
export class ArenaRoom extends DurableObject{
 async fetch(request){
  const url=new URL(request.url);
  if(url.pathname==='/quota'){
   const id=await request.text();if(!roomId(id))return json(400,{ok:false});
   const day=new Date().toISOString().slice(0,10);
   const allowed=await this.ctx.storage.transaction(async tx=>{
    const old=await tx.get('quota'),ids=old?.day===day?old.ids:[];
    if(ids.includes(id))return true;if(ids.length>=10)return false;
    await tx.put('quota',{day,ids:[...ids,id]});return true;
   });
   return json(allowed?200:429,{ok:allowed});
  }
  const uid=request.headers.get('x-internal-uid'),room=request.headers.get('x-internal-room');
  if(!uid||!roomId(room))return json(403,{ok:false});
  const db=roomDatabase(this.ctx.storage,room),before=(await db.ref('arenaV150Private/'+room).get()).val();
  if(before?.phase==='lobby'&&Date.now()-(before.now??0)>600000){
   await this.ctx.storage.deleteAll();return json(404,{ok:false,code:'lobby-expired'});
  }
  if(!before&&url.pathname!=='/v150/lobbies')return json(404,{ok:false,code:'not-found'});
  const handler=createArenaHandler({auth:{verifyIdToken:async()=>({uid})},database:db,bankBuilder,
   roomSecret:this.env.ARENA_ROOM_SECRET,presenceEnabled:true});
  // Protect state from concurrent requests across await boundaries.
  return this.ctx.blockConcurrencyWhile(async()=>{
   const response=await handleFetch(handler,request,{uid});
   const state=(await db.ref('arenaV150Private/'+room).get()).val();
   const nick=request.headers.get('x-internal-nickname');
   if(nick&&response.ok&&request.method==='POST'&&url.pathname.startsWith('/v150/profiles/'))
    await this.ctx.storage.put('nickname:'+uid,safeNickname(decodeURIComponent(nick)));
   if(state){
    await this.ctx.storage.put('room',room);
    const next=nextAlarm(state,Date.now());
    if(next!==null)await this.ctx.storage.setAlarm(next);
    else await this.ctx.storage.setAlarm(Date.now()+24*60*60*1000);
   }
   return response;
  });
 }
 async alarm(){
  const room=await this.ctx.storage.get('room');if(!roomId(room))return;
  const db=roomDatabase(this.ctx.storage,room),ref=db.ref('arenaV150Private/'+room);
  const before=(await ref.get()).val();
  if(!before||!before.game||before.result!==null||before.game.phase==='finished'||before.connection.phase==='finished'){
   await this.ctx.storage.deleteAll();return;
  }
  await sweepRoom(ref,Date.now);
  const next=nextAlarm((await ref.get()).val(),Date.now());
  await this.ctx.storage.setAlarm(next??Date.now()+24*60*60*1000);
 }
}
