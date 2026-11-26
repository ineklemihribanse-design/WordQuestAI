// Trusted lobby lifecycle. All mutations run in an Admin-only room transaction.
// Room codes must be generated with cryptographic randomness by the gateway.
// A trusted bank builder, not a client, supplies the final game content.
import {validateSettings,createMatch} from './match_service.mjs';
const safe=x=>typeof x==='string'&&/^[A-Za-z0-9_-]{1,100}$/.test(x)&&!['constructor','prototype','__proto__'].includes(x);
const fail=m=>{throw Error(m)};
const clone=x=>JSON.parse(JSON.stringify(x));
const capacity=s=>s.settings.kind==='team'?4:2;
const member=(s,uid)=>s.members.find(m=>m.uid===uid);
const validTime=n=>Number.isSafeInteger(n)&&n>=0;
export function createLobby({host,settings,now}){
 if(!safe(host)||!validTime(now))fail('Invalid host or time');
 return {schema:150,phase:'lobby',host,settings:validateSettings(settings),members:[{uid:host,ready:false}],
  revision:0,configRevision:0,createdAt:now,now,receipts:{}};
}
export function lobbyView(s,uid){
 if(s?.phase!=='lobby'||!member(s,uid))fail('Not a member');
 const {receipts,...view}=s;
 return clone({...view,capacity:capacity(s),canStart:uid===s.host&&s.members.length===capacity(s)&&s.members.every(m=>m.ready)});
}
export function applyLobby(state,uid,cmd,now){
 if(!state||state.phase!=='lobby'||!safe(uid)||!cmd||!safe(cmd.id)||!validTime(now)||now<state.now)fail('Invalid lobby command');
 const receipt=uid+':'+cmd.id;
 if(Object.hasOwn(state.receipts,receipt))return state;
 if(cmd.revision!==state.revision)fail('Stale lobby revision');
 const s=clone(state),p=member(s,uid);
 if(cmd.type==='join'){
  if(p)return state;
  if(s.members.length>=capacity(s))fail('Lobby full');
  s.members.push({uid,ready:false});
  s.members.forEach(m=>m.ready=false);
 }else{
  if(!p)fail('Not a member');
  if(cmd.type==='settings'){
   if(uid!==s.host)fail('Host only');
   const cfg=validateSettings(cmd.settings);
   if(s.members.length>(cfg.kind==='team'?4:2))fail('Too many members for selected mode');
   s.settings=cfg;s.configRevision++;s.members.forEach(m=>m.ready=false);
  }else if(cmd.type==='ready'){
   if(cmd.configRevision!==s.configRevision||typeof cmd.ready!=='boolean')fail('Stale settings');
   if(p.ready===cmd.ready)return state;
   p.ready=cmd.ready;
  }else if(cmd.type==='leave'){
   if(uid===s.host){s.phase='closed';}
   else{s.members=s.members.filter(m=>m.uid!==uid);s.members.forEach(m=>m.ready=false);}
  }else fail('Invalid lobby action');
 }
 s.revision++;s.now=now;s.receipts[receipt]=s.revision;return s;
}
export function startLobby(state,uid,{revision,bank,seed,epochs,now}){
 if(!state||state.phase!=='lobby'||uid!==state.host||state.revision!==revision||
 !validTime(now)||now<state.now||state.members.length!==capacity(state)||!state.members.every(m=>m.ready))fail('Lobby not ready');
 const ids=state.members.map(m=>m.uid);
 // Stable alternating seats: host and third arrival vs second and fourth.
 const teams=state.settings.kind==='team'?[{id:'red',members:[ids[0],ids[2]]},{id:'blue',members:[ids[1],ids[3]]}]:undefined;
 const match=createMatch({settings:state.settings,bank,players:ids,teams,epochs,seed,now});
 return {...match,lobby:{host:uid,configRevision:state.configRevision,startedFromRevision:state.revision}};
}
export async function createLobbyRecord(ref,options){
 const result=await ref.transaction(s=>s===null?createLobby(options):undefined,undefined,false);
 if(!result.committed)fail('Room code collision');
 return lobbyView(result.snapshot.val(),options.host);
}
export async function transactLobby(ref,uid,cmd,now){
 const result=await ref.transaction(s=>{
  if(!s)fail('Room not found');
  const effective=cmd.type==='join'?{...cmd,revision:s.revision}:cmd;
  const next=applyLobby(s,uid,effective,now);return next===s?undefined:next;
 },undefined,false);
 const s=result.snapshot.val();
 return {committed:result.committed,view:s.phase==='closed'||cmd.type==='leave'?null:lobbyView(s,uid)};
}
export async function transactStart(ref,uid,options){
 const result=await ref.transaction(s=>{
  if(s?.schema===150&&s.lobby?.host===uid&&s.lobby.startedFromRevision===options.revision)return;
  return startLobby(s,uid,options);
 },undefined,false);
 const s=result.snapshot.val();
 return {committed:result.committed,match:s};
}
