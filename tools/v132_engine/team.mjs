// Trusted 2v2 (expandable roster) game reducer. Run only in server transactions.
// Private answers, hint claims and receipts must never be stored in public room data.
import {createHash} from 'node:crypto';
const modes=['classic','streak','limited','speed','strategy'];
const tier={A1:1,A2:2,B1:3,B2:4,C1:5,C2:6};
const safe=x=>typeof x==='string'&&/^[A-Za-z0-9_-]{1,100}$/.test(x)&&!['constructor','prototype','__proto__'].includes(x);
const fail=m=>{throw Error(m)};
const clone=x=>JSON.parse(JSON.stringify(x));
const other=(s,t)=>s.teams.find(x=>x.id!==t).id;
const roster=(s,t)=>s.teams.find(x=>x.id===t);
const actorTeam=(s,p)=>s.teams.find(t=>t.members.includes(p))?.id;
const available=(s,t)=>roster(s,t).members.filter(p=>!s.removed.includes(p));
const points=q=>Math.min(35,Math.max(2,q.letterCount))*2+4*tier[q.level];
export function createTeam({teams,items,kind,mode,first,now,seconds=30,limit=3,hints=0,seed}){
 if(!['crossword','quiz'].includes(kind)||!modes.includes(mode)||!Array.isArray(teams)||teams.length!==2||
 !teams.every(t=>t&&safe(t.id)&&Array.isArray(t.members)&&t.members.length>=2&&t.members.length<=4&&t.members.every(safe))||
 new Set(teams.map(t=>t.id)).size!==2||teams[0].members.length!==teams[1].members.length||
 new Set(teams.flatMap(t=>t.members)).size!==teams.flatMap(t=>t.members).length||
 !teams.some(t=>t.id===first)||!Array.isArray(items)||items.length<1||items.length>70||
 !Number.isSafeInteger(now)||now<0||!Number.isInteger(seconds)||seconds<0||seconds>600||
 ![2,3,5].includes(limit)||!Number.isInteger(hints)||hints<0||hints>20||typeof seed!=='string'||!seed)fail('Invalid team config');
 const normalized=items.map(q=>{
  if(!q||typeof q.clue!=='string'||!q.clue.trim()||!Object.hasOwn(tier,q.level))fail('Invalid item');
  if(kind==='quiz'){
   if(!Array.isArray(q.options)||q.options.length!==5||!q.options.every(x=>typeof x==='string'&&x.trim())||
    new Set(q.options.map(x=>x.trim().toLowerCase())).size!==5||!Number.isInteger(q.correct)||q.correct<0||q.correct>4||
    !Number.isInteger(q.letterCount)||q.letterCount<1)fail('Invalid quiz item');
   return {clue:q.clue,options:[...q.options],correct:q.correct,level:q.level,letterCount:q.letterCount};
  }
  if(typeof q.answer!=='string'||!/^[A-Za-z]{2,35}$/.test(q.answer))fail('Invalid answer');
  return {clue:q.clue,answer:q.answer.toUpperCase(),level:q.level,letterCount:q.answer.length};
 });
 const s={teams:clone(teams),items:normalized,kind,mode,turn:first,limit,streak:0,seconds,hints,seed,now,revision:0,phase:'playing',
  active:null,queue:normalized.map((_,i)=>i),deferred:[],round:1,deadline:null,removed:[],attempted:[],resolved:{},receipts:{},result:null,
  teamStats:Object.fromEntries(teams.map(t=>[t.id,{score:0,correct:0,wrong:0,pass:0,timeout:0,spent:0}])),
  playerStats:Object.fromEntries(teams.flatMap(t=>t.members).map(p=>[p,{correct:0,wrong:0,pass:0,timeout:0,hints:0}])),
  rotation:Object.fromEntries(teams.map(t=>[t.id,0])),claims:Object.fromEntries(teams.map(t=>[t.id,{}]))};
 next(s,now);return s;
}
function activePlayer(s,t){const m=roster(s,t).members;for(let n=0;n<m.length;n++){const p=m[(s.rotation[t]+n)%m.length];if(!s.removed.includes(p))return p;}return null;}
function rotate(s,t){const m=roster(s,t).members;const p=activePlayer(s,t);if(p!==null)s.rotation[t]=(m.indexOf(p)+1)%m.length;}
function finish(s,kind,winner=null){s.phase='finished';s.deadline=null;s.active=null;s.result={kind,winner};}
function next(s,now){
 s.attempted=[];
 if(!s.queue.length&&s.round===1&&s.deferred.length){s.round=2;s.queue=s.deferred;s.deferred=[];}
 if(!s.queue.length){
  const [a,b]=s.teams.map(t=>t.id),x=s.teamStats[a].score,y=s.teamStats[b].score;
  finish(s,'completed',x===y?null:x>y?a:b);return;
 }
 s.active=s.mode==='strategy'?null:s.queue[0];
 s.deadline=s.active===null||!s.seconds?null:now+s.seconds*1000;
}
function swap(s,t){s.turn=other(s,t);s.streak=0;}
function clearActive(s){s.queue=s.queue.filter(i=>i!==s.active);}
function recordFailure(s,t,p,reason,now){
 s.teamStats[t][reason]++;if(p)s.playerStats[p][reason]++;
 if(reason==='wrong')s.teamStats[t].score-=3;
 s.attempted.push(t);rotate(s,t);
 if(s.mode!=='speed')swap(s,t);
 if(s.attempted.length===2){
  const i=s.active;clearActive(s);
  if(s.round===1)s.deferred.push(i);
  else s.resolved[i]={kind:'revealed',answer:s.kind==='quiz'?s.items[i].correct:s.items[i].answer,points:0};
  next(s,now);
 }else if(s.mode!=='speed')s.deadline=s.seconds?now+s.seconds*1000:null;
}
function accept(s,t,p,now){
 const i=s.active,score=points(s.items[i]);s.teamStats[t].score+=score;s.teamStats[t].correct++;s.playerStats[p].correct++;
 s.resolved[i]={kind:'solved',team:t,player:p,points:score,answer:s.kind==='quiz'?s.items[i].correct:s.items[i].answer};clearActive(s);
 rotate(s,t);
 if(s.mode==='classic'||s.mode==='strategy')swap(s,t);
 else if(s.mode==='streak'){s.turn=t;s.streak++;}
 else if(s.mode==='limited'){s.turn=t;s.streak++;if(s.streak>=s.limit)swap(s,t);}
 next(s,now);
}
function timeout(s,now){
 if(s.deadline===null||now<s.deadline)return false;
 const at=s.deadline;
 if(s.mode==='speed'){
  for(const {id:t} of s.teams)if(!s.attempted.includes(t)){
   const current=s.active;recordFailure(s,t,activePlayer(s,t),'timeout',at);if(s.active!==current)break;
  }
 }else recordFailure(s,s.turn,activePlayer(s,s.turn),'timeout',at);
 return true;
}
export function applyTeam(state,cmd,now){
 if(!cmd||!safe(cmd.id)||!Number.isSafeInteger(now)||now<state.now)fail('Invalid command');
 if(Object.hasOwn(state.receipts,cmd.id))return state;
 if(state.phase!=='playing'||cmd.revision!==state.revision)fail('Stale or inactive match');
 const s=clone(state);
 function commit(){s.revision++;s.receipts[cmd.id]=s.revision;s.now=now;return s;}
 if(timeout(s,now))return commit();
 if(cmd.type==='tick')return state;
 const p=cmd.actor,t=actorTeam(s,p);if(!t||s.removed.includes(p))fail('Not eligible');
 if(cmd.type==='leave'){
  s.removed.push(p);
  if(!available(s,t).length)finish(s,'forfeit',other(s,t));
  return commit();
 }
 if(s.mode!=='speed'&&t!==s.turn||activePlayer(s,t)!==p)fail('Not your turn');
 if(cmd.type==='choose'){
  if(s.mode!=='strategy'||s.active!==null||!s.queue.includes(cmd.item))fail('Invalid selection');
  s.active=cmd.item;s.deadline=s.seconds?now+s.seconds*1000:null;return commit();
 }
 if(s.active===null||cmd.item!==s.active||s.attempted.includes(t))fail('Invalid or exhausted item');
 const q=s.items[s.active];
 if(cmd.type==='hint'){
  if(s.teamStats[t].spent>=s.hints)fail('No team hints');
  const used=s.claims[t][s.active]??[];
  const candidates=(s.kind==='quiz'?[0,1,2,3,4].filter(i=>i!==q.correct):Array.from({length:q.answer.length},(_,i)=>i)).filter(i=>!used.includes(i));
  if(!candidates.length)fail('No more hints');
  const hash=i=>createHash('sha256').update(JSON.stringify([s.seed,t,s.active,used.length,i])).digest('hex');
  candidates.sort((a,b)=>hash(a).localeCompare(hash(b)));
  s.claims[t][s.active]=[...used,candidates[0]];s.teamStats[t].spent++;s.playerStats[p].hints++;
 }else if(cmd.type==='pass')recordFailure(s,t,p,'pass',now);
 else if(cmd.type==='submit'){
  let correct;
  if(s.kind==='quiz'){
   if(!Number.isInteger(cmd.choice)||cmd.choice<0||cmd.choice>4||(s.claims[t][s.active]??[]).includes(cmd.choice))fail('Invalid choice');
   correct=cmd.choice===q.correct;
  }else{
   if(typeof cmd.answer!=='string'||!cmd.answer.trim())fail('Empty answer');
   correct=cmd.answer.trim().toUpperCase()===q.answer;
  }
  if(correct)accept(s,t,p,now);else recordFailure(s,t,p,'wrong',now);
 }else fail('Invalid action');
 return commit();
}
export function teamView(s,p){
 const t=actorTeam(s,p);if(!t)fail('Not a participant');
 const items=s.items.map(({answer,correct,...q})=>q);
 const myHints=Object.fromEntries(Object.entries(s.claims[t]).map(([i,positions])=>[i,
  s.kind==='quiz'?positions:positions.map(pos=>({position:pos,letter:s.items[i].answer[pos]}))]));
 const scores=Object.fromEntries(s.teams.map(x=>[x.id,s.teamStats[x.id].score]));
 return clone({teams:s.teams,kind:s.kind,mode:s.mode,phase:s.phase,revision:s.revision,turn:s.turn,streak:s.streak,active:s.active,
  activePlayers:Object.fromEntries(s.teams.map(x=>[x.id,activePlayer(s,x.id)])),deadline:s.deadline,removed:s.removed,
  queue:s.queue,round:s.round,items,resolved:s.resolved,scores,myHints,hintsLeft:s.hints-s.teamStats[t].spent,
  result:s.result,stats:s.phase==='finished'?{teams:s.teamStats,players:s.playerStats}:undefined});
}
