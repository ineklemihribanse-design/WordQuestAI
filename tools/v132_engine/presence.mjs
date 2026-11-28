// Authenticated session leases. The server owns all times and epochs.
// A scheduler must call sweepPresence even if every client disappears.
import {matchTransport,matchTick,matchView} from './match_service.mjs';
export const LEASE_MS=12000;
const safe=x=>typeof x==='string'&&/^[A-Za-z0-9_-]{16,100}$/.test(x);
const clone=x=>JSON.parse(JSON.stringify(x));
const fail=m=>{throw Error(m)};
const finished=s=>s.result!==null||s.game.phase==='finished'||s.connection.phase==='finished';
export function initialPresence(players,now){
 return Object.fromEntries(players.map(id=>[id,{session:null,until:now+LEASE_MS}]));
}
export function sweepPresence(state,now){
 if(!Number.isSafeInteger(now)||now<state.now)fail('Invalid server time');
 if(finished(state))return state;
 let s=state;
 const due=state.connection.players.filter(p=>p.connected&&!p.removed&&state.presence?.[p.id]?.until<=now)
  .map(p=>({id:p.id,at:state.presence[p.id].until})).sort((a,b)=>a.at-b.at);
 for(const event of due){
  if(finished(s))break;
  const p=s.connection.players.find(p=>p.id===event.id);
  const at=Math.max(s.now,event.at);
  // Apply question expiry before a lease expiry when both are already due.
  s=matchTick(s,at);
  if(!finished(s)&&p.connected)s=matchTransport(s,{type:'lost',id:p.id,epoch:p.epoch},at);
 }
 return matchTick(s,now);
}
export function presenceEvent(state,uid,request,now){
 if(!request||!['heartbeat','leave','cancel-disconnected'].includes(request.type)||!safe(request.session)||
  !state.connection.players.some(p=>p.id===uid))fail('Invalid presence request');
 let s=sweepPresence(state,now);
 const p=s.connection.players.find(p=>p.id===uid);
 if(finished(s)||p.removed)return s;
 const lease=s.presence?.[uid]??{session:null,until:null};
 if(request.type==='leave'||request.type==='cancel-disconnected'){
  if(lease.session!==request.session)fail('Session not current');
  return matchTransport(s,{type:request.type,id:uid,epoch:p.epoch},now);
 }
 if(p.connected&&lease.session!==null&&lease.session!==request.session)fail('Another session is active');
 // A stale session cannot override a current connected session. After a
 // genuine loss, authenticated reconnect assigns a new server epoch.
 if(!p.connected)s=matchTransport(s,{type:'restored',id:uid,epoch:p.epoch+1},now);
 if(finished(s)||s.connection.players.find(p=>p.id===uid).removed)return s;
 const out=clone(s);
 out.presence={...(out.presence??{}),[uid]:{session:request.session,until:now+LEASE_MS}};
 // Heartbeat itself does not invalidate an in-flight gameplay revision.
 return out;
}
export async function transactPresence(ref,uid,request,clock){
 const tx=await ref.transaction(state=>{
  if(!state?.game)fail('Match not found');
  const next=presenceEvent(state,uid,request,Math.max(state.now,clock()));
  return next===state?undefined:next;
 },undefined,false);
 return matchView(tx.snapshot.val(),uid,clock());
}
export async function sweepRoom(ref,clock){
 return ref.transaction(state=>{
  if(!state?.game||finished(state))return;
  const next=sweepPresence(state,Math.max(state.now,clock()));
  return next===state?undefined:next;
 },undefined,false);
}
