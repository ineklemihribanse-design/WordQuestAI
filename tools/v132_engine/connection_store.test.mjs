import test from 'node:test';
import assert from 'node:assert/strict';
import {initializeRoom,advanceRoom,transactConnection,initializeConnection} from './connection_store.mjs';
const room=()=>({phase:'playing',mode:'duel',hostUid:'a',guestUid:'b',turn:{uid:'a',until:31000},
  solved:{0:'b'},points:{a:4,b:9},package6:{hintClaims:{a:{0:{word:1,position:2}}}}});
const begin=()=>initializeRoom(room(),{epochs:{a:1,b:1},now:1000});
const lost={type:'lost',id:'a',epoch:1},back={type:'restored',id:'a',epoch:2};
test('transaction freezes latest room deadline and restores it, preserving all unrelated state',()=>{
  const before=begin();before.turn.until=51000;
  const paused=advanceRoom(before,lost,6000);
  assert.equal(paused.connectionV150.phase,'paused');
  assert.equal(JSON.parse(paused.connectionV150.payload).frozen.question,45000);
  const resumed=advanceRoom(paused,back,26000);
  assert.equal(resumed.turn.until,71000);
  assert.equal(resumed.turn.uid,'a');assert.deepEqual(resumed.points,before.points);
  assert.deepEqual(resumed.package6,before.package6);assert.deepEqual(resumed.solved,before.solved);
  assert.equal(before.connectionV150.phase,'playing');
});
test('forfeit outcome is preserved in server state and room finished atomically',()=>{
  const paused=advanceRoom(begin(),lost,2000);
  const done=advanceRoom(paused,{type:'tick'},62000);
  assert.equal(done.phase,'finished');assert.equal(done.turn.until,0);
  assert.deepEqual(JSON.parse(done.connectionV150.payload).result,{kind:'forfeit',winner:'b'});
  assert.equal(advanceRoom(done,back,63000),done);
});
test('adapter rejects corrupt server state and unsupported roster',()=>{
  const r=begin();r.connectionV150.revision=7;
  assert.throws(()=>advanceRoom(r,lost,2000));
  assert.throws(()=>initializeRoom({...room(),mode:'future'},{epochs:{a:1,b:1},now:1000}));
});
test('admin transaction retry and duplicate transport event are idempotent',async()=>{
  let value=room();
  const ref={transaction:async fn=>{
    fn(value);const next=fn(value);
    if(next!==undefined)value=next;
    return {committed:next!==undefined,snapshot:{val:()=>value}};
  }};
  assert.equal((await initializeConnection(ref,{epochs:{a:1,b:1},now:1000})).committed,true);
  assert.equal((await initializeConnection(ref,{epochs:{a:1,b:1},now:1000})).committed,false);
  assert.equal((await transactConnection(ref,lost,2000)).committed,true);
  assert.equal((await transactConnection(ref,lost,3000)).committed,false);
  assert.equal(JSON.parse(value.connectionV150.payload).players[0].drops,1);
});
