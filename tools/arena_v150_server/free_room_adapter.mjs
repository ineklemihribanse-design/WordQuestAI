// Room-local database adapter. A room is owned by exactly one Durable Object.
// No Firebase Admin key and no public client access to game storage.
const snap=value=>({val:()=>value??null});
const safe=id=>typeof id==='string'&&/^[A-Za-z0-9_-]{1,128}$/.test(id)&&!['__proto__','constructor','prototype'].includes(id);
export function roomDatabase(storage,room){
 if(!safe(room))throw Error('Invalid room');
 const statePath='arenaV150Private/'+room,profilePath='arenaV150Profiles/'+room;
 return {ref(path){
  if(path===statePath)return {
   get:async()=>snap(await storage.get('state')),
   transaction:async(fn,complete)=>{
    const result=await storage.transaction(async tx=>{
     const current=(await tx.get('state'))??null,next=fn(current);
     if(next!==undefined){if(next===null)await tx.delete('state');else await tx.put('state',next);}
     return {committed:next!==undefined,snapshot:snap(next===undefined?current:next)};
    });
    if(complete)complete(null,result.committed,result.snapshot);
    return result;
   }
  };
  if(path===profilePath)return {get:async()=>{
   const profiles=await storage.list({prefix:'profile:'});
   return snap(Object.fromEntries([...profiles].map(([k,v])=>[k.slice(8),v])));
  }};
  if(path.startsWith(profilePath+'/')){
   const id=path.slice(profilePath.length+1);if(!safe(id))throw Error('Invalid profile ID');
   return {set:async value=>storage.put('profile:'+id,value)};
  }
  const nickname=/^users\/([A-Za-z0-9_-]{1,128})\/nickname$/.exec(path);
  if(nickname&&safe(nickname[1]))return {get:async()=>snap(await storage.get('nickname:'+nickname[1]))};
  throw Error('Path not allowed');
 }};
}
export function nextAlarm(state,now){
 if(!state)return null;
 if(state.phase==='lobby')return now+600000;
 if(!state.game||state.result!==null||state.game.phase==='finished'||state.connection.phase==='finished')return null;
 const times=[];
 for(const p of state.connection.players){
  if(p.removed)continue;
  if(p.connected)times.push(state.presence?.[p.id]?.until);
  else times.push(p.until);
 }
 if(state.connection.phase==='playing'){
  times.push(state.game.deadline);
  for(const c of Object.values(state.game.current??{}))times.push(c.deadline);
 }
 if(state.social?.offer?.status==='pending')times.push(state.social.offer.expiresAt);
 const due=times.filter(Number.isSafeInteger);
 return due.length?Math.max(now+50,Math.min(...due)):now+12000;
}
export async function handleFetch(handler,request,identity){
 // Only the Worker has access to this binding. The verified UID is passed
 // separately, never accepted from public headers or request payloads.
 const url=new URL(request.url),headers=new Headers();
 const req={method:request.method,url:url.pathname+url.search,headers:{authorization:'Bearer internal'},
  async *[Symbol.asyncIterator](){
   if(!request.body)return;
   const reader=request.body.getReader();
   try{for(;;){const {done,value}=await reader.read();if(done)break;yield value;}}finally{reader.releaseLock();}
  }};
 let statusCode=500,body='';
 const res={setHeader:(k,v)=>headers.set(k,String(v)),get statusCode(){return statusCode;},
  set statusCode(v){statusCode=v;},end(v){body=v??'';}};
 await handler(req,res,identity);
 return new Response(body,{status:statusCode,headers});
}
