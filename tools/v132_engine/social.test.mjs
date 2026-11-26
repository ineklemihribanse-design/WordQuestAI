import test from 'node:test';
import assert from 'node:assert/strict';
import {createMatch,matchCommand,matchTick,matchView} from './match_service.mjs';
const bank=['APPLE','HOUSE','RIVER','CHAIR','WATER','GREEN','TABLE','MOUSE','BREAD','PLANT'].map((answer,i)=>({id:'w'+i,answer,clue:'clue '+i,level:'A1',cells:Array.from({length:5},(_,j)=>i+','+j)}));
const base=(kind='duel')=>createMatch({settings:{kind,mode:'classic',format:'crossword',levels:['A1'],count:10,seconds:0},bank,players:['a','b'],teams:[{id:'red',members:['a','b']},{id:'blue',members:['c','d']}],epochs:{a:1,b:1,c:1,d:1},seed:'private',now:1000});
let n=0;
const run=(s,uid,type,extra={},now=s.now+1)=>matchCommand(s,uid,{id:'social'+(++n),revision:s.revision,type,...extra},now);
test('reactions do not change gameplay revision, are rate limited and expire',()=>{
 let s=run(base(),'a','reaction',{symbol:'👏'});
 assert.equal(s.revision,0);assert.equal(matchView(s,'b').social.reactions.a.symbol,'👏');
 assert.throws(()=>run(s,'a','reaction',{symbol:'🔥'}));assert.throws(()=>run(s,'b','reaction',{symbol:'BAD'}));
 s=run(s,'b','reaction',{symbol:'👍'},2501);assert.equal(matchView(s,'b').social.reactions.a,undefined);
});
test('reject or withdraw notifies participants; cooldown applies',()=>{
 let s=run(base(),'a','finish-offer');const id=s.social.offer.id;
 assert.throws(()=>run(s,'b','finish-withdraw',{offerId:id}));
 s=run(s,'b','finish-reject',{offerId:id});assert.equal(matchView(s,'a').social.notice.kind,'rejected');
 assert.equal(matchView(s,'a').phase,'playing');assert.throws(()=>run(s,'a','finish-offer'));
 s=run(s,'b','finish-offer',{},62000);s=run(s,'b','finish-withdraw',{offerId:s.social.offer.id});
 assert.equal(matchView(s,'a').social.notice.kind,'withdrawn');
});
test('two player agreement finishes without host-only privilege',()=>{
 let s=run(base(),'b','finish-offer');s=run(s,'a','finish-accept',{offerId:s.social.offer.id});
 assert.equal(matchView(s,'a').phase,'finished');assert.equal(s.result.kind,'mutual-finish');
});
test('four player agreement requires all four explicit votes',()=>{
 let s=run(base('team'),'a','finish-offer');const offerId=s.social.offer.id;
 s=run(s,'b','finish-accept',{offerId});s=run(s,'c','finish-accept',{offerId});
 assert.equal(matchView(s,'a').phase,'playing');
 assert.throws(()=>run(s,'b','finish-accept',{offerId}));
 s=run(s,'d','finish-accept',{offerId});assert.equal(matchView(s,'a').phase,'finished');
});
test('server tick expires offer and prevents delayed acceptance',()=>{
 let s=run(base(),'a','finish-offer');const id=s.social.offer.id;
 s=matchTick(s,31001);assert.equal(s.social.offer.status,'expired');
 assert.throws(()=>run(s,'b','finish-accept',{offerId:id}));assert.equal(matchView(s,'a').phase,'playing');
});
