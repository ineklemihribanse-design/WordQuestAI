// Transport-independent HTTP handler. The hosting runtime supplies Firebase Admin
// auth/database instances. No Admin credentials are ever embedded in Android.
// Only verified participants can read their view or submit a command.
import {matchView,transactMatch,matchCommand,validateSettings} from './match_service.mjs';
import {createLobby,lobbyView,transactLobby,transactStart} from './lobby.mjs';
import {createHmac,randomBytes} from 'node:crypto';
import {profileMembers,validatePortrait,readProfiles} from './profiles.mjs';
const matchId=x=>typeof x==='string'&&/^[A-Za-z0-9_-]{6,64}$/.test(x)&&!['constructor','__proto__','prototype'].includes(x);
const allowed=new Set(['submit','answer','pass','skip','choose','hint','eliminate','half','time','reaction','finish-offer','finish-accept','finish-reject','finish-withdraw']);
const reply=(res,status,body)=>{res.setHeader('Cache-Control','no-store');res.setHeader('Content-Type','application/json; charset=utf-8');res.statusCode=status;res.end(JSON.stringify(body));};
const problem=(res,status,code)=>reply(res,status,{ok:false,code});
async function readBody(req,max=8192){
 let bytes=0,parts=[];
 for await(const chunk of req){const b=Buffer.isBuffer(chunk)?chunk:Buffer.from(chunk);bytes+=b.length;if(bytes>max)throw Error('body-too-large');parts.push(b);}
 const data=JSON.parse(Buffer.concat(parts).toString('utf8'));
 if(!data||typeof data!=='object'||Array.isArray(data))throw Error('invalid-body');return data;
}
function command(data){
 if(typeof data.id!=='string'||! /^[A-Za-z0-9_-]{1,100}$/.test(data.id)||!Number.isInteger(data.revision)||data.revision<0||!allowed.has(data.type))throw Error('invalid-command');
 // Never trust actor, time, points, answer keys, or raw state supplied by caller.
 if(Object.keys(data).some(k=>!['id','revision','type','word','index','item','answer','choice','symbol','offerId'].includes(k)))throw Error('unexpected-field');
 const out={id:data.id,revision:data.revision,type:data.type};
 for(const key of ['word','index','item','choice'])if(data[key]!==undefined){
  if(!Number.isInteger(data[key])||data[key]<0||data[key]>69)throw Error('invalid-index');out[key]=data[key];
 }
 if(data.answer!==undefined){
  if(typeof data.answer!=='string'||data.answer.length>80)throw Error('invalid-answer');
  out.answer=data.answer;
 }
 if(data.type==='reaction'){
  if(typeof data.symbol!=='string'||data.symbol.length>8)throw Error('invalid-reaction');out.symbol=data.symbol;
 }else if(data.symbol!==undefined)throw Error('unexpected-symbol');
 if(['finish-accept','finish-reject','finish-withdraw'].includes(data.type)){
  if(!requestId(data.offerId))throw Error('invalid-offer');out.offerId=data.offerId;
 }else if(data.offerId!==undefined)throw Error('unexpected-offer');
 return out;
}
const requestId=x=>typeof x==='string'&&/^[A-Za-z0-9_-]{1,100}$/.test(x)&&!['__proto__','constructor','prototype'].includes(x);
const settingsKeys=['kind','mode','format','scale','levels','topic','wordType','count','seconds','hints','retries','skipLimit','limit','maxMistakes'];
function checkedSettings(data){
 if(!data||typeof data!=='object'||Array.isArray(data)||Object.keys(data).some(k=>!settingsKeys.includes(k)))throw Error('invalid-settings');
 return validateSettings(data);
}
function lobbyCommand(data){
 if(!requestId(data.id)||(data.type!=='join'||data.revision!==undefined)&&(!Number.isSafeInteger(data.revision)||data.revision<0))throw Error('invalid-command');
 const base={id:data.id,revision:data.revision,type:data.type};
 const fields={join:[],leave:[],ready:['ready','configRevision'],settings:['settings'],start:[]};
 if(!Object.hasOwn(fields,data.type)||Object.keys(data).some(k=>!['id','revision','type',...fields[data.type]].includes(k)))throw Error('invalid-command');
 if(data.type==='ready'){
  if(typeof data.ready!=='boolean'||!Number.isSafeInteger(data.configRevision)||data.configRevision<0)throw Error('invalid-ready');
  return {...base,ready:data.ready,configRevision:data.configRevision};
 }
 if(data.type==='settings')return {...base,settings:checkedSettings(data.settings)};
 return base;
}
// An optional bankBuilder enables lobby creation; it must be trusted and provide
// a validated, server-generated crossword or curated five-choice question pool.
export function createArenaHandler({auth,database,clock=Date.now,log=()=>{},bankBuilder=null,roomSecret=null}){
 if(!auth||typeof auth.verifyIdToken!=='function'||!database||typeof database.ref!=='function')throw Error('Missing trusted dependencies');
 return async function handle(req,res){
  const path=new URL(req.url,'http://localhost').pathname;
  const match=/^\/v150\/matches\/([A-Za-z0-9_-]+)(\/commands)?$/.exec(path);
  const portrait=/^\/v150\/profiles\/([A-Za-z0-9_-]+)$/.exec(path);
  const create=path==='/v150/lobbies';
  const lobby=/^\/v150\/lobbies\/([A-Za-z0-9_-]+)(\/commands)?$/.exec(path);
  if(!create&&!(portrait&&matchId(portrait[1]))&&!(lobby&&matchId(lobby[1]))&&!(match&&matchId(match[1])))return problem(res,404,'not-found');
  const write=create||Boolean((lobby??match)?.[2]);
  if(portrait?!['GET','POST'].includes(req.method):req.method!==(write?'POST':'GET'))return problem(res,405,'method-not-allowed');
  const bearer=/^Bearer ([^\s]+)$/.exec(req.headers.authorization??'');
  if(!bearer)return problem(res,401,'unauthenticated');
  let identity;
  try{identity=await auth.verifyIdToken(bearer[1],true);}catch{return problem(res,401,'unauthenticated');}
  if(typeof identity?.uid!=='string'||!identity.uid)return problem(res,401,'unauthenticated');
  const uid=identity.uid;
  if(portrait){
   const room=portrait[1];
   try{
    const state=(await database.ref('arenaV150Private/'+room).get()).val();
    const members=profileMembers(state);
    if(!members.includes(uid))return problem(res,404,'not-found');
    if(req.method==='POST'){
     let value;
     try{value=validatePortrait(await readBody(req,190000));}
     catch(e){return problem(res,e.message==='body-too-large'?413:400,'invalid-portrait');}
     await database.ref('arenaV150Profiles/'+room+'/'+uid).set(value);
     return reply(res,200,{ok:true});
    }
    const result=await readProfiles(database,room,members);
    const since=new URL(req.url,'http://localhost').searchParams.get('version');
    return reply(res,200,since===result.version?{ok:true,version:result.version}:{ok:true,...result});
   }catch(e){log('arena-profile-request-failed',String(e?.name??'Error'));return problem(res,409,'refresh-required');}
  }
  if(create||lobby){
   if(typeof bankBuilder!=='function'||typeof roomSecret!=='string'||roomSecret.length<32)return problem(res,503,'lobby-unavailable');
   let data;
   if(write){try{data=await readBody(req);}catch(e){return problem(res,e.message==='body-too-large'?413:400,'invalid-command');}}
   try{
    if(create){
     if(!requestId(data.id)||Object.keys(data).some(k=>!['id','settings'].includes(k)))return problem(res,400,'invalid-command');
     let settings;
     try{settings=checkedSettings(data.settings);}catch{return problem(res,400,'invalid-settings');}
     const id=createHmac('sha256',roomSecret).update(JSON.stringify([uid,data.id])).digest('hex').slice(0,20);
     const ref=database.ref('arenaV150Private/'+id),now=clock();
     const tx=await ref.transaction(state=>state===null?createLobby({host:uid,settings,now}):undefined,undefined,false);
     const state=tx.snapshot.val();
     if(state.phase==='lobby'&&state.host===uid)return reply(res,200,{ok:true,id,view:lobbyView(state,uid),committed:tx.committed});
     if(state.lobby?.host===uid)return reply(res,200,{ok:true,id,view:matchView(state,uid),committed:false});
     return problem(res,409,'room-unavailable');
    }
    const id=lobby[1],ref=database.ref('arenaV150Private/'+id),snap=await ref.get(),state=snap.val();
    if(!state||state.phase==='closed')return problem(res,404,'not-found');
    if(!write){
     try{return reply(res,200,{ok:true,id,view:state.phase==='lobby'?lobbyView(state,uid):matchView(state,uid)});}
     catch{return problem(res,404,'not-found');}
    }
    let cmd;
    try{cmd=lobbyCommand(data);}catch{return problem(res,400,'invalid-command');}
    if(cmd.type==='start'){
     if(state.lobby?.host===uid&&state.lobby.startedFromRevision===cmd.revision)
      return reply(res,200,{ok:true,id,view:matchView(state,uid),committed:false});
     if(state.phase!=='lobby'||state.host!==uid)return problem(res,404,'not-found');
     const view=lobbyView(state,uid);
     if(!view.canStart||cmd.revision!==state.revision)return problem(res,409,'lobby-not-ready');
     const seed=randomBytes(32).toString('hex');
     const bank=await bankBuilder(state.settings,seed);
     const now=clock(),epochs=Object.fromEntries(state.members.map(p=>[p.uid,1]));
     const tx=await transactStart(ref,uid,{revision:cmd.revision,bank,seed,epochs,now});
     return reply(res,200,{ok:true,id,view:matchView(tx.match,uid),committed:tx.committed});
    }
    if(state.phase!=='lobby')return problem(res,409,'match-already-started');
    if(cmd.type!=='join'&&!state.members.some(p=>p.uid===uid))return problem(res,404,'not-found');
    const tx=await transactLobby(ref,uid,cmd,clock());
    return reply(res,200,{ok:true,id,...tx});
   }catch(e){log('arena-lobby-request-failed',String(e?.name??'Error'));return problem(res,409,'refresh-required');}
  }
  const ref=database.ref('arenaV150Private/'+match[1]);
  let cmd;
  if(write){try{cmd=command(await readBody(req));}catch(e){return problem(res,e.message==='body-too-large'?413:400,'invalid-command');}}
  try{
   const snap=await ref.get(),state=snap.val();
   if(!state)return problem(res,404,'not-found');
   try{matchView(state,uid);}catch{return problem(res,404,'not-found');}
   if(!write)return reply(res,200,{ok:true,view:matchView(state,uid,clock())});
   // Fixed server arrival timestamp reused across Firebase transaction retries.
   const now=clock();
   try{matchCommand(state,uid,cmd,now);}catch{return problem(res,409,'command-rejected');}
   const result=await transactMatch(ref,uid,cmd,now);
   return reply(res,200,{ok:true,committed:result.committed,view:result.view});
  }catch(e){log('arena-request-failed',String(e?.name??'Error'));return problem(res,409,'refresh-required');}
 };
}
