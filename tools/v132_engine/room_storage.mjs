// Firebase RTDB omits null values and empty objects/arrays. Keep the whole
// trusted state as a versioned JSON string, with a separate searchable flag.
// The private path is Admin-only. Never store player-supplied state.
const fail=m=>{throw Error(m)};
const active=s=>s.phase==='lobby'||Boolean(s.game&&s.result==null&&s.game.phase!=='finished'&&s.connection?.phase!=='finished');
export function encodeRoom(state){
 if(!state||state.schema!==150)fail('Invalid room schema');
 const payload=JSON.stringify(state);
 if(Buffer.byteLength(payload,'utf8')>2_000_000)fail('Room exceeds storage limit');
 return {codec:1,active:active(state),payload};
}
export function decodeRoom(raw){
 if(raw===null)return null;
 if(!raw||raw.codec!==1||typeof raw.payload!=='string'||Buffer.byteLength(raw.payload,'utf8')>2_000_000)fail('Invalid stored room');
 const state=JSON.parse(raw.payload);
 if(!state||state.schema!==150||active(state)!==raw.active)fail('Invalid room envelope');
 return state;
}
const wrapSnapshot=snap=>({val:()=>decodeRoom(snap.val())});
export function roomRef(raw){
 return {
  get:async()=>wrapSnapshot(await raw.get()),
  transaction:async(fn,complete,local)=>{
   // Warm the Admin SDK cache so an existing room is not treated as absent.
   await raw.get();
   const tx=await raw.transaction(value=>{
    const next=fn(decodeRoom(value));
    return next===undefined?undefined:next===null?null:encodeRoom(next);
   },undefined,local??false);
   const result={committed:tx.committed,snapshot:wrapSnapshot(tx.snapshot)};
   if(complete)complete(null,result.committed,result.snapshot);
   return result;
  }
 };
}
export function arenaDatabase(db){
 return {ref(path){
  const ref=db.ref(path);
  return /^arenaV150Private\/[A-Za-z0-9_-]{6,64}$/.test(path)?roomRef(ref):ref;
 }};
}
// Only active rooms are scanned. Runtime schedules this even without clients.
export async function activeRoomIds(db){
 const snap=await db.ref('arenaV150Private').orderByChild('active').equalTo(true).get();
 const ids=[];
 snap.forEach(child=>{if(/^[A-Za-z0-9_-]{6,64}$/.test(child.key))ids.push(child.key);});
 return ids;
}
