import test from 'node:test';
import assert from 'node:assert/strict';
import {createLobby,applyLobby,lobbyView,startLobby,createLobbyRecord,transactLobby,transactStart} from './lobby.mjs';
import {matchView} from './match_service.mjs';
const cfg=(kind='duel')=>({kind,mode:'classic',format:'crossword',levels:['A1'],count:10});
const base=(kind='duel')=>createLobby({host:'a',settings:cfg(kind),now:1000});
let n=0;
const run=(s,uid,type,extra={})=>applyLobby(s,uid,{id:'l'+(++n),revision:s.revision,type,...extra},s.now+1);
const ready=(s,uid)=>run(s,uid,'ready',{configRevision:s.configRevision,ready:true});
const words=['APPLE','HOUSE','RIVER','CHAIR','WATER','GREEN','TABLE','MOUSE','BREAD','PLANT'];
const bank=words.map((answer,i)=>({id:'w'+i,answer,clue:'clue '+i,level:'A1',cells:Array.from({length:5},(_,j)=>i+','+j)}));
const start=(s,uid='a')=>startLobby(s,uid,{revision:s.revision,bank,seed:'private',epochs:{a:1,b:1,c:1,d:1},now:s.now+1});
test('two player lobby requires full roster and all ready',()=>{
 let s=base();assert.throws(()=>start(s));s=run(s,'b','join');assert.throws(()=>run(s,'c','join'));
 s=ready(s,'a');assert.throws(()=>start(s));s=ready(s,'b');assert.equal(lobbyView(s,'a').canStart,true);
 assert.throws(()=>start(s,'b'));const m=start(s);assert.equal(matchView(m,'b').phase,'playing');assert.equal(m.game.players.length,2);
});
test('settings are host-only and clear all readiness with a new config revision',()=>{
 let s=run(base(),'b','join');s=ready(s,'a');s=ready(s,'b');
 assert.throws(()=>run(s,'b','settings',{settings:cfg('quiz')}));
 s=run(s,'a','settings',{settings:{...cfg(),levels:['A1','A2']}});
 assert.equal(s.configRevision,1);assert.ok(s.members.every(x=>!x.ready));
 assert.throws(()=>run(s,'a','ready',{configRevision:0,ready:true}));
 assert.throws(()=>start(s));
});
test('2v2 waits for four and assigns both teams in arrival order',()=>{
 let s=base('team');for(const p of ['b','c','d'])s=run(s,p,'join');
 assert.throws(()=>run(s,'e','join'));assert.throws(()=>run(s,'a','settings',{settings:cfg()}));
 for(const p of ['a','b','c','d'])s=ready(s,p);
 const m=start(s);assert.deepEqual(m.game.teams,[{id:'red',members:['a','c']},{id:'blue',members:['b','d']}]);
 assert.equal(matchView(m,'d').phase,'playing');
});
test('join and leave reset readiness; host leave closes lobby',()=>{
 let s=ready(base(),'a');s=run(s,'b','join');assert.equal(s.members[0].ready,false);
 s=ready(s,'a');s=run(s,'b','leave');assert.equal(s.members.length,1);assert.equal(s.members[0].ready,false);
 s=run(s,'a','leave');assert.equal(s.phase,'closed');assert.throws(()=>run(s,'c','join'));
});
test('stale commands and unauthorized read fail; duplicate commands are idempotent',()=>{
 let s=base();const c={id:'join1',revision:0,type:'join'};s=applyLobby(s,'b',c,1001);
 assert.equal(applyLobby(s,'b',c,1002),s);assert.throws(()=>lobbyView(s,'c'));
 assert.throws(()=>applyLobby(s,'a',{id:'stale',revision:0,type:'ready',configRevision:0,ready:true},1002));
});
test('transaction start preserves exact revision and cannot create second game',async()=>{
 let value=null;
 const ref={transaction:async f=>{const next=f(value);if(next!==undefined)value=next;return {committed:next!==undefined,snapshot:{val:()=>value}};}};
 await createLobbyRecord(ref,{host:'a',settings:cfg(),now:1000});
 await assert.rejects(()=>createLobbyRecord(ref,{host:'b',settings:cfg(),now:1000}));
 await transactLobby(ref,'b',{id:'j',revision:0,type:'join'},1001);
 await transactLobby(ref,'a',{id:'r1',revision:1,type:'ready',configRevision:0,ready:true},1002);
 await transactLobby(ref,'b',{id:'r2',revision:2,type:'ready',configRevision:0,ready:true},1003);
 const opt={revision:3,bank,seed:'private',epochs:{a:1,b:1},now:1004};
 assert.equal((await transactStart(ref,'a',opt)).committed,true);
 assert.equal((await transactStart(ref,'a',opt)).committed,false);
 assert.equal(value.lobby.startedFromRevision,3);
});
