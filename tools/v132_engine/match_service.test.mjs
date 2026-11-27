import test from 'node:test';
import assert from 'node:assert/strict';
import {validateSettings,selectBank,createMatch,matchCommand,matchTransport,matchTick,matchView,transactMatch} from './match_service.mjs';
const words=['APPLE','HOUSE','RIVER','CHAIR','WATER','GREEN','TABLE','MOUSE','BREAD','PLANT'];
const bank=words.map((answer,i)=>({id:'w'+i,answer,clue:'clue '+i,prompt:'question '+i,level:i%2?'A2':'A1',yds:i%2?'medium':'easy',topic:'daily',wordType:'noun',options:['a','b','c','d','e'],correct:0,letterCount:5,cells:Array.from({length:5},(_,j)=>i+','+j)}));
const cfg=(kind='duel',mode='classic',extra={})=>({kind,mode,levels:['A1','A2'],count:10,seconds:30,...extra});
const base=(settings=cfg())=>createMatch({settings,bank,players:['a','b'],teams:[{id:'red',members:['a','b']},{id:'blue',members:['c','d']}],epochs:{a:1,b:1,c:1,d:1},seed:'trusted-private-seed',now:1000});
test('multi-level settings normalized, filters exclusive, limits validated',()=>{
 assert.deepEqual(validateSettings(cfg('duel','limited',{levels:['A2','A1','A2']})).levels,['A1','A2']);
 assert.throws(()=>validateSettings(cfg('duel','classic',{topic:'daily',wordType:'noun'})));
 assert.throws(()=>validateSettings(cfg('duel','classic',{count:71})));
 assert.throws(()=>validateSettings(cfg('coop','timed',{seconds:0})));
});
test('bank really applies both selected levels and filter',()=>{
 const s=validateSettings(cfg('quiz','classic',{count:5,topic:'daily'}));const picked=selectBank(bank,s,'seed');
 assert.equal(picked.length,5);assert.deepEqual(new Set(picked.map(q=>q.level)),new Set(['A1','A2']));
 assert.throws(()=>selectBank(bank,{...s,topic:'other'},'seed'));
});
test('YDS selection does not silently use CEFR filters',()=>{
 const s=validateSettings(cfg('quiz','classic',{count:5,scale:'yds',levels:['medium']}));
 assert.ok(selectBank(bank,s,'seed').every(q=>q.yds==='medium'));
});
test('all four engines use shared match settings',()=>{
 for(const [kind,mode,format] of [['duel','classic'],['quiz','simultaneous'],['coop','free'],['team','speed','quiz']]){
  const s=base(cfg(kind,mode,{format}));assert.equal(s.kind,kind);assert.equal(s.game.mode,mode);assert.equal(s.settings.count,10);
 }
});
test('authenticated actor cannot impersonate rival or replay another revision',()=>{
 const s=base(),p=s.game.turn,c={id:'1',type:'submit',word:s.game.active,answer:s.game.words[s.game.active].answer,revision:0};
 assert.throws(()=>matchCommand(s,'outsider',c,1001));
 assert.throws(()=>matchCommand(s,p,{...c,actor:p==='a'?'b':'a'},1001));
 const next=matchCommand(s,p,c,1001);assert.equal(next.revision,1);
 assert.equal(matchCommand(next,p,c,1002),next);
 assert.throws(()=>matchCommand(next,next.game.turn,{...c,id:'2'},1002));
});
test('pause blocks commands and resumes exact remaining time',()=>{
 let s=base();const p=s.game.turn;
 s=matchTransport(s,{type:'lost',id:p,epoch:1},6000);assert.equal(matchView(s,p).phase,'paused');
 assert.throws(()=>matchCommand(s,p,{id:'bad',type:'pass',word:0,revision:s.revision},6001));
 s=matchTransport(s,{type:'restored',id:p,epoch:2},26000);
 assert.equal(s.game.deadline,51000);assert.equal(matchView(s,p).phase,'playing');
});
test('personal quiz timers both shift after reconnect without equalizing them',()=>{
 let s=base(cfg('quiz','simultaneous',{hints:2}));s=matchCommand(s,'a',{id:'hint',type:'time',index:0,revision:0},1001);
 s=matchTransport(s,{type:'lost',id:'a',epoch:1},6000);
 s=matchTransport(s,{type:'restored',id:'a',epoch:2},16000);
 assert.equal(s.game.current.a.deadline,51000);assert.equal(s.game.current.b.deadline,41000);
 assert.equal(matchView(s,'b').game.mine.deadline,41000);
});
test('trusted timeout and forfeit result require no disconnected client',()=>{
 let s=base();s=matchTransport(s,{type:'lost',id:'a',epoch:1},2000);
 s=matchTick(s,62000);assert.equal(matchView(s,'b').phase,'finished');assert.deepEqual(s.result,{kind:'forfeit',winner:'b'});
});
test('removed team member cannot play, teammate can remain',()=>{
 let s=base(cfg('team','classic',{format:'crossword'}));s=matchTransport(s,{type:'leave',id:'a',epoch:1},2000);
 assert.equal(s.game.removed.includes('a'),true);assert.equal(s.connection.phase,'playing');
 assert.throws(()=>matchCommand(s,'a',{id:'x',type:'pass',item:0,revision:s.revision},2001));
});
test('private views omit answer bank, seed and session epochs',()=>{
 const s=base(cfg('quiz','simultaneous',{hints:2}));
 const next=matchCommand(s,'a',{id:'hint',type:'half',index:0,revision:0},1001);
 const b=matchView(next,'b');assert.equal(b.game.question.correct,undefined);assert.deepEqual(b.game.mine.removed,[]);
 assert.equal(b.connection.players[0].epoch,undefined);assert.equal(b.game.seed,undefined);
 assert.throws(()=>matchView(next,'outsider'));
});
test('admin transaction retries do not duplicate scores',async()=>{
 let value=base();const p=value.game.turn;
 const ref={transaction:async f=>{f(value);const next=f(value);if(next!==undefined)value=next;return {committed:next!==undefined,snapshot:{val:()=>value}};}};
 const c={id:'unique',type:'submit',word:0,answer:value.game.words[0].answer,revision:0};
 assert.equal((await transactMatch(ref,p,c,1001)).committed,true);
 assert.equal((await transactMatch(ref,p,c,1002)).committed,false);
 assert.equal(value.game.stats[p].correct,1);
});

test('team players are authorized and actual team commands work through common service',()=>{
 let s=base(cfg('team','classic',{format:'crossword',hints:2}));
 const first=s.game.teams.find(t=>t.id===s.game.turn).members[0];
 const next=matchCommand(s,first,{id:'team-hint',type:'hint',item:0,revision:0},1001);
 assert.equal(matchView(next,first).game.hintsLeft,1);
 const rival=next.game.teams.find(t=>!t.members.includes(first)).members[0];
 assert.deepEqual(matchView(next,rival).game.myHints,{});
 s=matchCommand(next,first,{id:'team-answer',type:'submit',item:0,answer:next.game.items[0].answer,revision:1},1002);
 assert.equal(s.game.playerStats[first].correct,1);
});
test('simultaneous quiz accepts both original-revision answers to same question',()=>{
 let s=base(cfg('quiz','simultaneous'));
 const choice=s.game.questions[0].correct;
 s=matchCommand(s,'a',{id:'same',type:'answer',index:0,choice,revision:0},1001);
 s=matchCommand(s,'b',{id:'same',type:'answer',index:0,choice,revision:0},1002);
 assert.equal(s.game.stats.a.correct,1);assert.equal(s.game.stats.b.correct,1);assert.equal(s.game.index,1);
});
test('duel hints use shared service but do not leak to opponent',()=>{
 let s=base(cfg('duel','classic',{hints:2}));const p=s.game.turn,other=p==='a'?'b':'a';
 s=matchCommand(s,p,{id:'hint',type:'hint',word:0,revision:0},1001);
 assert.equal(matchView(s,p).game.hintsLeft,1);assert.equal(Object.keys(matchView(s,p).game.myHints[0]).length,1);
 assert.deepEqual(matchView(s,other).game.myHints[0],{});assert.equal(matchView(s,other).game.claims,undefined);
});

test('terminal summary covers forfeits and early mutual finish without exposing unplayed answers',()=>{
 for(const [kind,mode,format] of [['duel','classic'],['quiz','classic'],['coop','free'],['team','classic','crossword']]){
  let s=base(cfg(kind,mode,{format}));assert.equal(matchView(s,'a').summary,undefined);
  if(kind==='team'){s=matchTransport(s,{type:'leave',id:'a',epoch:1},1001);s=matchTransport(s,{type:'leave',id:'b',epoch:1},1002);}
  else s=matchTransport(s,{type:'leave',id:'a',epoch:1},1001);
  const v=matchView(s,kind==='team'?'c':'b');assert.equal(v.phase,'finished');assert.equal(v.summary.total,10);assert.equal(v.summary.players.a.correct,0);
  assert.equal(v.summary.solved,0);assert.equal(JSON.stringify(v).includes('APPLE'),false);
  if(kind==='team')assert.equal(v.summary.teams.red.score,0);
 }
});
test('solved duel cells are public but pending answers stay private',()=>{
 let s=base();const actor=s.game.turn,i=s.game.active,answer=s.game.words[i].answer;
 s=matchCommand(s,actor,{id:'solve',revision:0,type:'submit',word:i,answer},1001);
 for(const p of ['a','b']){const v=matchView(s,p);assert.equal(v.game.resolved[i].answer,answer);assert.equal(v.game.words[i].answer,undefined);assert.equal(v.game.resolved[s.game.active],undefined);}
});

test('feedback counts are per authenticated player, not predicted from submissions',()=>{
 let s=base();const p=s.game.turn,other=p==='a'?'b':'a',i=s.game.active;
 assert.deepEqual(matchView(s,p).feedback,{correct:0,wrong:0});
 s=matchCommand(s,p,{id:'feedback-correct',revision:0,type:'submit',word:i,answer:s.game.words[i].answer},1001);
 assert.deepEqual(matchView(s,p).feedback,{correct:1,wrong:0});assert.deepEqual(matchView(s,other).feedback,{correct:0,wrong:0});
 const j=s.game.active;s=matchCommand(s,other,{id:'feedback-wrong',revision:s.revision,type:'submit',word:j,answer:'ZZZZZ'},1002);
 assert.deepEqual(matchView(s,other).feedback,{correct:0,wrong:1});
});


test('delayed team sweep resumes at grace boundary without granting extra time',()=>{
 let s=base(cfg('team','classic',{format:'crossword'}));
 s=matchTransport(s,{type:'lost',id:'a',epoch:1},2000);
 assert.equal(s.connection.phase,'paused');
 s=matchTick(s,70000);
 assert.equal(s.connection.phase,'playing');
 assert.equal(s.connection.players.find(p=>p.id==='a').removed,true);
 assert.equal(s.game.deadline,91000);
 assert.equal(s.pauseAt,null);
 assert.equal(matchTick(s,80000),s);
 const previous=s.game.revision;
 s=matchTick(s,91000);
 assert.ok(s.game.revision>previous,'Question expiry must occur at the original resumed deadline');
});

test('expired team grace and another loss in one transport event preserve both pause intervals',()=>{
 let s=base(cfg('team','classic',{format:'crossword'}));
 s=matchTransport(s,{type:'lost',id:'a',epoch:1},2000);
 s=matchTransport(s,{type:'lost',id:'c',epoch:1},70000);
 assert.equal(s.game.deadline,91000);
 assert.equal(s.connection.phase,'paused');
 assert.equal(s.pauseAt,70000);
 s=matchTransport(s,{type:'restored',id:'c',epoch:2},75000);
 assert.equal(s.game.deadline,96000);
 assert.equal(s.connection.phase,'playing');
});

test('departed voter invalidates pending team finish offer without ending match',()=>{
 let s=base(cfg('team','classic',{format:'crossword',seconds:0}));
 s=matchCommand(s,'a',{id:'offer-before-leave',type:'finish-offer',revision:s.revision},1001);
 s=matchCommand(s,'b',{id:'vote-before-leave',type:'finish-accept',offerId:s.social.offer.id,revision:s.revision},1002);
 s=matchTransport(s,{type:'leave',id:'c',epoch:1},1003);
 assert.equal(s.social.offer.status,'expired');
 assert.equal(matchView(s,'d').social.notice.kind,'expired');
 assert.equal(matchView(s,'d').phase,'playing');
 assert.throws(()=>matchCommand(s,'d',{id:'stale-vote',type:'finish-accept',offerId:s.social.offer.id,revision:s.revision},1004));
 s=matchCommand(s,'a',{id:'new-offer',type:'finish-offer',revision:s.revision},61001);
 assert.deepEqual(s.social.offer.required,['a','b','d']);
 s=matchCommand(s,'b',{id:'new-vote-b',type:'finish-accept',offerId:s.social.offer.id,revision:s.revision},61002);
 s=matchCommand(s,'d',{id:'new-vote-d',type:'finish-accept',offerId:s.social.offer.id,revision:s.revision},61003);
 assert.equal(s.result.kind,'mutual-finish');
});
