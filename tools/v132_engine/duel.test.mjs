import test from 'node:test';
import assert from 'node:assert/strict';
import {createDuel,applyDuel,publicDuel,wordPoints} from './duel.mjs';
const words=[{answer:'APPLE',level:'A1',clue:'elma'},{answer:'HOUSE',level:'A2',clue:'ev'},{answer:'RIVER',level:'B1',clue:'nehir'},{answer:'GREEN',level:'A1',clue:'yeşil'},{answer:'CHAIR',level:'A1',clue:'sandalye'},{answer:'WATER',level:'A1',clue:'su'}];
const base=(mode,opts={})=>createDuel({players:['a','b'],words,first:'a',now:1000,mode,...opts});
let seq=0;
const cmd=(s,type,actor,extra={},now=s.now+1)=>applyDuel(s,{id:'cmd'+(++seq),revision:s.revision,type,actor,word:s.active,...extra},now);
const correct=(s,actor)=>cmd(s,'submit',actor,{answer:s.words[s.active].answer});
test('classic changes turns after correct answer and retains existing score formula',()=>{
 let s=base('classic');s=correct(s,'a');assert.equal(s.turn,'b');assert.equal(s.stats.a.score,14);
 s=cmd(s,'submit','b',{answer:'WRONG'});assert.equal(s.turn,'a');assert.equal(s.stats.b.score,-3);assert.equal(s.active,1);
});
test('streak retains turn on correct then transfers on pass',()=>{
 let s=base('streak');s=correct(s,'a');s=correct(s,'a');assert.equal(s.turn,'a');assert.equal(s.streak,2);
 s=cmd(s,'pass','a');assert.equal(s.turn,'b');assert.equal(s.active,2);assert.equal(s.stats.a.pass,1);
});
test('limited streak obeys 2, 3 and 5 thresholds',()=>{
 for(const limit of [2,3,5]){let s=base('limited',{limit});
 for(let i=0;i<limit;i++){assert.equal(s.turn,'a');s=correct(s,'a');}
 assert.equal(s.turn,'b');assert.equal(s.streak,0);}
});
test('speed first correct wins; stale simultaneous submission cannot score twice',()=>{
 let s=base('speed');const old=s;
 s=correct(s,'b');assert.equal(s.resolved[0].by,'b');
 assert.throws(()=>applyDuel(s,{id:'late',revision:old.revision,type:'submit',actor:'a',word:0,answer:'APPLE'},s.now+1));
 assert.equal(s.stats.a.score,0);assert.equal(s.stats.b.score,14);
});
test('speed wrong penalty is personal; both wrong defer without a second award',()=>{
 let s=base('speed');s=cmd(s,'submit','a',{answer:'WRONG'});assert.equal(s.active,0);
 s=cmd(s,'submit','b',{answer:'WRONG'});assert.deepEqual(s.deferred,[0]);assert.equal(s.active,1);
 assert.equal(s.stats.a.score,-3);assert.equal(s.stats.b.score,-3);
});
test('strategy chooses available word and awards its level and length score',()=>{
 let s=base('strategy');assert.equal(s.active,null);assert.equal(s.deadline,null);
 assert.throws(()=>cmd(s,'choose','b',{word:2}));
 s=cmd(s,'choose','a',{word:2});assert.equal(s.active,2);s=correct(s,'a');
 assert.equal(s.stats.a.score,wordPoints(words[2]));assert.equal(s.turn,'b');assert.equal(s.active,null);
 assert.throws(()=>cmd(s,'choose','b',{word:2}));
});
test('both pass then second round reveals answer without points',()=>{
 let s=base('classic',{words:words.slice(0,1)});
 s=cmd(s,'pass','a');s=cmd(s,'pass','b');assert.equal(s.round,2);
 s=cmd(s,'pass','a');s=cmd(s,'pass','b');assert.equal(s.phase,'finished');
 assert.deepEqual(s.resolved[0],{kind:'revealed',answer:'APPLE',points:0});assert.equal(s.result.winner,null);
});
test('skip differs from pass, bounded and deferred',()=>{
 let s=base('classic',{skipLimit:1});s=cmd(s,'skip','a');assert.equal(s.active,1);
 assert.deepEqual(s.deferred,[0]);assert.equal(s.stats.a.skip,1);assert.equal(s.stats.a.pass,0);
 s=correct(s,'b');assert.throws(()=>cmd(s,'skip','a'));
});
test('timeout at boundary wins; incomplete answer does not score',()=>{
 let s=base('classic');assert.throws(()=>cmd(s,'submit','a',{answer:''}));
 s=cmd(s,'submit','a',{answer:'APPLE'},s.deadline);assert.equal(s.stats.a.timeout,1);
 assert.equal(s.stats.a.score,0);assert.equal(s.turn,'b');assert.equal(s.active,0);
});
test('retries are bounded and never remove penalties',()=>{
 let s=base('classic',{retries:1});s=cmd(s,'submit','a',{answer:'WRONG'});assert.equal(s.turn,'a');
 s=cmd(s,'submit','a',{answer:'WRONG'});assert.equal(s.turn,'b');assert.equal(s.stats.a.score,-6);
});
test('command receipts are idempotent; private answer keys never exposed',()=>{
 let s=base('classic');const c={id:'unique',revision:0,type:'submit',actor:'a',word:0,answer:'APPLE'};
 const next=applyDuel(s,c,1001);assert.equal(applyDuel(next,c,1002),next);
 const view=publicDuel(next);assert.equal(view.words[0].answer,undefined);assert.equal(view.words[0].length,5);
 assert.equal(view.words[1].answer,undefined);assert.equal(view.receipts,undefined);assert.equal(s.stats.a.score,0);
});

test('personal hint claims are unique, preserve crossings and hide from rival',()=>{
 const w=[{answer:'CAT',level:'A1',clue:'kedi',cells:['0,0','0,1','0,2']},{answer:'CUP',level:'A1',clue:'fincan',cells:['0,0','1,0','2,0']}];
 let s=base('streak',{words:w,hints:3,seed:'private'});
 for(let i=0;i<3;i++)s=cmd(s,'hint','a');
 assert.equal(Object.keys(publicDuel(s,'a').myHints[0]).length,3);
 assert.deepEqual(publicDuel(s,'b').myHints[0],{});
 assert.equal(publicDuel(s,'b').seed,undefined);assert.equal(publicDuel(s,'b').claims,undefined);
 assert.throws(()=>cmd(s,'hint','a'));
 s=correct(s,'a');assert.equal(publicDuel(s,'b').myHints[1][0],'C');
});
