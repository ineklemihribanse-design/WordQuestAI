// Firebase Admin adapter, deliberately not a client API or deployed cloud function.
// Only a trusted runtime may invoke these functions with verified session events.
// A scheduler must call tick at every pending expiry; an authenticated presence
// gateway must provide strictly increasing session epochs and trusted event time.
import {createConnectionState,connectionTransition} from './connection.mjs';
const clone=x=>JSON.parse(JSON.stringify(x));
const deadline=x=>Number.isSafeInteger(x)&&x>0?x:null;
const mode=m=>({duel:'duel',quiz:'quiz',team:'coop'})[m];
const wrap=s=>({phase:s.phase,revision:s.revision,payload:JSON.stringify(s)});
function unpack(room){
  const w=room.connectionV150;
  if(!w||typeof w.payload!=='string')throw Error('Connection protocol not initialized');
  const s=JSON.parse(w.payload);
  if(s.phase!==w.phase||s.revision!==w.revision)throw Error('Connection state integrity mismatch');
  return s;
}
export function initializeRoom(room,{epochs,now}){
  if(!room||room.phase!=='playing'||room.connectionV150||!mode(room.mode)||
    !room.hostUid||!room.guestUid||room.hostUid===room.guestUid)
    throw Error('Unsupported or unready room');
  const s=createConnectionState({kind:mode(room.mode),now,
    players:[room.hostUid,room.guestUid].map(id=>({id,epoch:epochs[id]})),
    matchDeadline:deadline(room.matchUntil),questionDeadline:deadline(room.turn?.until)});
  return {...clone(room),connectionV150:wrap(s)};
}
export function advanceRoom(room,event,now){
  if(!room||!room.connectionV150||room.phase==='finished')return room;
  if(room.phase!=='playing')throw Error('Invalid room phase');
  const state=unpack(room);
  // A completed legacy turn may have changed its deadline since the last
  // connection event. Freeze the latest authoritative value, not an old copy.
  if(state.phase==='playing'){
    state.deadlines.question=deadline(room.turn?.until);
    state.deadlines.match=deadline(room.matchUntil);
  }
  const next=connectionTransition(state,event,now);
  if(next===state)return room;
  const out={...clone(room),connectionV150:wrap(next)};
  if(state.phase==='paused'&&next.phase==='playing'){
    if(out.turn)out.turn.until=next.deadlines.question??0;
    if('matchUntil' in out)out.matchUntil=next.deadlines.match??0;
  }
  if(next.phase==='finished'){
    out.phase='finished';
    if(out.turn)out.turn.until=0;
    if('matchUntil' in out)out.matchUntil=0;
  }
  return out;
}
// ref must be an Admin SDK reference to precisely one room. The callback is
// pure and safe to retry; no external messages or scheduling inside it.
export async function transactConnection(ref,event,now){
  const result=await ref.transaction(room=>{
    if(!room||!room.connectionV150)return;
    const next=advanceRoom(room,event,now);
    return next===room?undefined:next;
  },undefined,false);
  return {committed:result.committed,room:result.snapshot.val()};
}
export async function initializeConnection(ref,options){
  const result=await ref.transaction(room=>{
    if(!room||room.connectionV150)return;
    return initializeRoom(room,options);
  },undefined,false);
  return {committed:result.committed,room:result.snapshot.val()};
}
