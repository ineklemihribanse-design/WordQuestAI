import test from 'node:test';
import assert from 'node:assert/strict';
import {createConnectionState,connectionTransition,gameplayAllowed} from './connection.mjs';
const base=(kind='duel')=>createConnectionState({kind,now:1000,matchDeadline:181000,questionDeadline:31000,
  players:kind==='team'?[{id:'a',team:'blue',epoch:1},{id:'b',team:'blue',epoch:1},
    {id:'c',team:'red',epoch:1},{id:'d',team:'red',epoch:1}]:[{id:'a',epoch:1},{id:'b',epoch:1}]});
const e=(s,type,id,epoch,at)=>connectionTransition(s,{type,id,epoch},at);
const tick=(s,at)=>connectionTransition(s,{type:'tick'},at);
test('first loss freezes both deadlines; restoration preserves exact remaining time',()=>{
  let s=e(base(),'lost','a',1,6000);
  assert.equal(s.phase,'paused');assert.deepEqual(s.frozen,{match:175000,question:25000});
  assert.equal(s.players[0].until,66000);assert.equal(gameplayAllowed(s,'b'),false);
  assert.equal(tick(s,20000),s);
  s=e(s,'restored','a',2,26000);
  assert.equal(s.phase,'playing');assert.deepEqual(s.deadlines,{match:201000,question:51000});
  assert.equal(s.players[0].drops,1);assert.equal(gameplayAllowed(s,'a'),true);
});
test('second loss grants 30 seconds; third eliminates immediately',()=>{
  let s=e(base(),'lost','a',1,2000);s=e(s,'restored','a',2,3000);
  s=e(s,'lost','a',2,4000);assert.equal(s.players[0].until,34000);
  s=e(s,'restored','a',3,5000);s=e(s,'lost','a',3,6000);
  assert.equal(s.phase,'finished');assert.deepEqual(s.result,{kind:'forfeit',winner:'b'});
  assert.equal(s.players[0].reason,'third-disconnect');
});
test('duplicate losses and stale transport events do not count',()=>{
  let s=e(base(),'lost','a',1,2000);assert.equal(e(s,'lost','a',1,3000),s);
  s=e(s,'restored','a',2,4000);assert.equal(e(s,'lost','a',1,5000),s);
  assert.equal(e(s,'restored','a',1,6000),s);assert.equal(s.players[0].drops,1);
});
test('reconnect exactly at expiry is too late; timer uses trusted time',()=>{
  let s=e(base(),'lost','a',1,2000);
  s=e(s,'restored','a',2,62000);assert.equal(s.phase,'finished');assert.equal(s.result.winner,'b');
  assert.equal(s.players[0].reason,'timeout');
});
test('both offline: neither device can move; simultaneous expiry yields no contest',()=>{
  let s=e(base(),'lost','a',1,2000);s=e(s,'lost','b',1,2000);
  assert.equal(gameplayAllowed(s,'a'),false);s=tick(s,62000);
  assert.deepEqual(s.result,{kind:'no-contest'});
});
test('cooperative leave gives incomplete rather than victory',()=>{
  const s=e(base('coop'),'leave','a',1,2000);
  assert.deepEqual(s.result,{kind:'incomplete'});assert.equal(s.players[0].drops,0);
});
test('team can resume after removing one member; full team departure forfeits',()=>{
  let s=e(base('team'),'lost','a',1,2000);s=tick(s,62000);
  assert.equal(s.phase,'playing');assert.equal(s.deadlines.question,91000);
  assert.equal(gameplayAllowed(s,'a'),false);assert.equal(gameplayAllowed(s,'b'),true);
  s=e(s,'leave','b',1,63000);assert.deepEqual(s.result,{kind:'forfeit',winner:'red'});
});
test('multiple disconnects freeze once and resume only when all remaining players return',()=>{
  let s=e(base('team'),'lost','a',1,2000);s=e(s,'lost','c',1,3000);
  s=e(s,'restored','a',2,4000);assert.equal(s.phase,'paused');
  s=e(s,'restored','c',2,5000);assert.equal(s.deadlines.question,34000);
});
test('unlimited clock stays unlimited and zero remaining is not reset',()=>{
  let s=createConnectionState({kind:'quiz',now:1,matchDeadline:null,questionDeadline:2,
    players:[{id:'a',epoch:1},{id:'b',epoch:1}]});
  s=e(s,'lost','a',1,2);s=e(s,'restored','a',2,20);
  assert.deepEqual(s.deadlines,{match:null,question:20});
});
test('invalid inputs rejected and state immutable',()=>{
  const s=base();assert.throws(()=>e(s,'lost','outsider',1,2000));
  assert.throws(()=>e(s,'lost','a',1,0));
  e(s,'lost','a',1,2000);assert.equal(s.players[0].drops,0);assert.equal(s.phase,'playing');
});
