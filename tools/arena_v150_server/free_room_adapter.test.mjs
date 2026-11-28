import test from 'node:test';
import assert from 'node:assert/strict';
import {roomDatabase,nextAlarm,retentionDeadline,handleFetch} from './free_room_adapter.mjs';
function storage(){
 const data=new Map();const tx={get:async k=>data.get(k),put:async(k,v)=>data.set(k,structuredClone(v)),delete:async k=>data.delete(k),
  list:async({prefix})=>new Map([...data].filter(([k])=>k.startsWith(prefix)))};
 return {...tx,transaction:async fn=>fn(tx)};
}
test('room transaction round-trips null and private state',async()=>{
 const db=roomDatabase(storage(),'room123'),ref=db.ref('arenaV150Private/room123');
 const x=await ref.transaction(s=>{assert.equal(s,null);return {game:{answer:'SECRET'},values:[],result:null}});
 assert.equal(x.committed,true);assert.deepEqual((await ref.get()).val(),{game:{answer:'SECRET'},values:[],result:null});
 const y=await ref.transaction(()=>undefined);assert.equal(y.committed,false);
 assert.throws(()=>db.ref('arenaV150Private/other123'));
});
test('profile storage is scoped and safe',async()=>{
 const db=roomDatabase(storage(),'room123');
 await db.ref('arenaV150Profiles/room123/user1').set({avatar:'piko'});
 assert.deepEqual((await db.ref('arenaV150Profiles/room123').get()).val(),{user1:{avatar:'piko'}});
 assert.throws(()=>db.ref('arenaV150Profiles/room123/__proto__'));
});
test('alarm picks earliest server deadline, including lost-player grace',()=>{
 const s={game:{phase:'playing',deadline:25000},result:null,connection:{phase:'playing',players:[{id:'a',connected:true},{id:'b',connected:false,until:16000}]},presence:{a:{until:18000}}};
 assert.equal(nextAlarm(s,10000),16000);
 s.result={kind:'forfeit'};assert.equal(nextAlarm(s,10000),null);
});
test('HTTP bridge keeps public actor header out',async()=>{
 const request=new Request('https://x/v150/matches/room123',{method:'POST',headers:{'x-internal-uid':'fake'},body:'{"type":"pass"}'});
 const res=await handleFetch(async(req,res)=>{assert.equal(req.headers['x-internal-uid'],undefined);let body='';for await(const b of req)body+=Buffer.from(b).toString();assert.equal(body,'{"type":"pass"}');res.statusCode=200;res.end('ok');},request,{uid:'real'});
 assert.equal(await res.text(),'ok');
});

test('lobby polling does not extend its idle deadline',()=>{
 const s={phase:'lobby',now:1000};assert.equal(nextAlarm(s,100000),601000);assert.equal(nextAlarm(s,200000),601000);
});
test('finished room retention is fixed, not extended by reads',()=>{
 const s={game:{phase:'finished'},result:{kind:'forfeit'}};
 const due=retentionDeadline(s,1000,null);assert.equal(due,86401000);assert.equal(retentionDeadline(s,100000,due),due);
});
