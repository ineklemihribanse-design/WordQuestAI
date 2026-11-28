import test from 'node:test';
import assert from 'node:assert/strict';
import {createMatch,matchView,matchCommand} from './match_service.mjs';
import {presenceEvent,sweepPresence} from './presence.mjs';
const words=['APPLE','HOUSE','RIVER','CHAIR','WATER','GREEN','TABLE','MOUSE','BREAD','PLANT'];
const bank=words.map((answer,i)=>({id:'w'+i,answer,clue:'clue '+i,level:'A1',cells:Array.from({length:5},(_,j)=>i+','+j)}));
const base=(kind='duel')=>createMatch({settings:{kind,mode:'classic',format:'crossword',levels:['A1'],count:10,seconds:30},bank,players:['a','b'],teams:[{id:'red',members:['a','b']},{id:'blue',members:['c','d']}],epochs:{a:1,b:1,c:1,d:1},seed:'private',now:1000});
const pulse=(s,p,now,session=p.repeat(20))=>presenceEvent(s,p,{type:'heartbeat',session},now);
test('heartbeat does not change gameplay revision or reveal session identity',()=>{
 let s=pulse(base(),'a',1001);assert.equal(s.revision,0);assert.equal(s.presence.a.until,13001);
 assert.equal(matchView(s,'b').presence,undefined);assert.equal(matchView(s,'b').connection.players[0].epoch,undefined);
 assert.throws(()=>pulse(s,'outsider',1002));
});
test('loss pauses clock and reconnect within grace restores remaining time',()=>{
 let s=pulse(base(),'a',2000);s=pulse(s,'b',10000);s=sweepPresence(s,14000);
 assert.equal(matchView(s,'b').phase,'paused');assert.equal(s.pauseAt,14000);
 assert.throws(()=>matchCommand(s,'b',{id:'x',revision:s.revision,type:'pass',word:0},14001));
 s=pulse(s,'a',18000,'new-session-123456789');assert.equal(matchView(s,'b').phase,'playing');assert.equal(s.game.deadline,35000);
});
test('first and second grace deadlines, third loss forfeits',()=>{
 let s=pulse(base(),'a',1001);s=pulse(s,'b',12000);s=sweepPresence(s,13001);assert.equal(s.connection.players[0].until,73001);
 s=pulse(s,'a',14000);s=pulse(s,'b',25000);s=sweepPresence(s,26000);assert.equal(s.connection.players[0].until,56000);
 s=pulse(s,'a',27000);s=pulse(s,'b',36000);s=sweepPresence(s,39000);
 assert.equal(s.result.kind,'forfeit');assert.equal(s.result.winner,'b');
});
test('scheduler alone expires both missing players; no client needed',()=>{
 const s=sweepPresence(base(),73000);assert.equal(matchView(s,'a').phase,'finished');assert.equal(s.result.kind,'no-contest');
});
test('explicit departure requires current session and ends duel immediately',()=>{
 let s=pulse(base(),'a',1001);
 assert.throws(()=>presenceEvent(s,'a',{type:'leave',session:'wrong-session-123456'},1002));
 s=presenceEvent(s,'a',{type:'leave',session:'a'.repeat(20)},1002);
 assert.equal(s.result.kind,'forfeit');assert.equal(s.result.winner,'b');
});
test('another session cannot replace connected player',()=>{
 const s=pulse(base(),'a',1001);
 assert.throws(()=>pulse(s,'a',1002,'another-session-123456'));
});

test('neutral disconnect exit requires current authenticated session and real pause',()=>{
 let s=pulse(base(),'a',1001);s=pulse(s,'b',12000);
 assert.throws(()=>presenceEvent(s,'b',{type:'cancel-disconnected',session:'b'.repeat(20)},12001));
 s=sweepPresence(s,13001);
 assert.equal(matchView(s,'b').phase,'paused');
 assert.throws(()=>presenceEvent(s,'b',{type:'cancel-disconnected',session:'wrong-session-123456'},13002));
 s=presenceEvent(s,'b',{type:'cancel-disconnected',session:'b'.repeat(20)},13002);
 assert.equal(matchView(s,'a').phase,'finished');assert.equal(matchView(s,'b').result.kind,'no-contest');
 assert.equal(matchView(s,'b').result.winner,undefined);
});
