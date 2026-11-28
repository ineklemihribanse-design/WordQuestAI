// Trusted server only. Store secret question bank apart from public room data.
import {createHash} from 'node:crypto';
const levels={A1:1,A2:2,B1:3,B2:4,C1:5,C2:6};
const modes=['classic','streak','limited','speed','simultaneous'];
const clone=x=>JSON.parse(JSON.stringify(x));
const fail=m=>{throw Error(m)};
const safeId=x=>typeof x==='string'&&/^[A-Za-z0-9_-]{1,100}$/.test(x)&&!['constructor','prototype','__proto__'].includes(x);
const both=s=>['speed','simultaneous'].includes(s.mode);
const opponent=(s,p)=>s.players.find(x=>x!==p);
const points=q=>Math.min(35,Math.max(2,q.letterCount))*2+4*levels[q.level];
export function createQuiz({players,questions,mode,limit=3,first,now,seconds=30,hints=0,seed}){
 if(!modes.includes(mode)||!Array.isArray(players)||players.length!==2||new Set(players).size!==2||
 !players.every(safeId)||!players.includes(first)||!Array.isArray(questions)||questions.length<1||questions.length>70||
 !questions.every(q=>q&&typeof q.prompt==='string'&&q.prompt.trim()&&Array.isArray(q.options)&&q.options.length===5&&
 q.options.every(o=>typeof o==='string'&&o.trim())&&new Set(q.options.map(o=>o.trim().toLowerCase())).size===5&&
 Number.isInteger(q.correct)&&q.correct>=0&&q.correct<5&&Object.hasOwn(levels,q.level)&&Number.isInteger(q.letterCount)&&q.letterCount>0)||
 ![2,3,5].includes(limit)||!Number.isSafeInteger(now)||now<0||!Number.isInteger(seconds)||seconds<0||seconds>600||
 !Number.isInteger(hints)||hints<0||hints>20||typeof seed!=='string'||!seed)fail('Invalid quiz config');
 const s={players:[...players],questions:questions.map(q=>({prompt:q.prompt,options:[...q.options],correct:q.correct,level:q.level,letterCount:q.letterCount})),
 mode,limit,turn:first,streak:0,now,seconds,hints,seed,index:0,phase:'playing',revision:0,receipts:{},history:[],result:null,
 stats:Object.fromEntries(players.map(p=>[p,{score:0,correct:0,wrong:0,pass:0,timeout:0,hints:0}])),current:null};
 open(s,now);return s;
}
function open(s,now){
 if(s.index>=s.questions.length){
  s.phase='finished';s.current=null;const [a,b]=s.players;
  s.result={winner:s.stats[a].score===s.stats[b].score?null:s.stats[a].score>s.stats[b].score?a:b};return;
 }
 s.current=Object.fromEntries(s.players.map(p=>[p,{status:'open',choice:null,removed:[],used:[],deadline:(both(s)||p===s.turn)&&s.seconds?now+s.seconds*1000:null}]));
}
function swap(s,p){s.turn=opponent(s,p);s.streak=0;}
function finishQuestion(s,now,winner=null){
 const q=s.questions[s.index];s.history.push({index:s.index,correct:q.correct,winner,
  players:Object.fromEntries(s.players.map(p=>[p,{status:s.current[p].status,choice:s.current[p].choice,used:[...s.current[p].used]}]))});
 s.index++;open(s,now);
}
function settle(s,p,status,choice,now){
 const c=s.current[p];c.status=status;c.choice=choice;c.deadline=null;
 if(status==='correct'){s.stats[p].correct++;s.stats[p].score+=points(s.questions[s.index]);}
 else if(status==='wrong'){s.stats[p].wrong++;s.stats[p].score-=3;}
 else s.stats[p][status]++;
 if(s.mode==='speed'&&status==='correct'){
  const rival=s.current[opponent(s,p)];if(rival.status==='open'){rival.status='lost-race';rival.deadline=null;}
  finishQuestion(s,now,p);return;
 }
 if(s.mode==='simultaneous'||s.mode==='speed'){
  if(s.players.every(id=>s.current[id].status!=='open'))finishQuestion(s,now);
  return;
 }
 if(status==='correct'){
  if(s.mode==='classic')swap(s,p);
  else if(s.mode==='limited'){s.streak++;if(s.streak>=s.limit)swap(s,p);}
  else s.streak++;
  const rival=s.current[opponent(s,p)];if(rival.status==='open')rival.status='not-attempted';
  finishQuestion(s,now,p);return;
 }
 swap(s,p);
 if(s.current[s.turn].status!=='open')finishQuestion(s,now);
 else s.current[s.turn].deadline=s.seconds?now+s.seconds*1000:null;
}
function expire(s,now){
 // Process simultaneous due clocks before moving to the next question.
 const index=s.index;
 const due=s.players.filter(p=>s.current[p].status==='open'&&s.current[p].deadline!==null&&s.current[p].deadline<=now)
  .sort((a,b)=>s.current[a].deadline-s.current[b].deadline);
 for(const p of due){
  if(s.phase!=='playing'||s.index!==index)break;
  settle(s,p,'timeout',null,s.current[p].deadline);
 }
 return due.length>0;
}
export function applyQuiz(state,cmd,now){
 if(!cmd||!safeId(cmd.id)||!Number.isSafeInteger(now)||now<state.now)fail('Invalid command');
 if(Object.hasOwn(state.receipts,cmd.id))return state;
 if(state.phase!=='playing')fail('Quiz finished or paused');
 if(cmd.index!==state.index||!Number.isInteger(cmd.revision)||cmd.revision<0||cmd.revision>state.revision||
 (!both(state)&&cmd.revision!==state.revision))fail('Stale question');
 const s=clone(state);const changed=expire(s,now);
 function commit(){s.now=now;s.revision++;s.receipts[cmd.id]=s.revision;return s;}
 if(s.phase!=='playing'||s.index!==state.index)return commit();
 if(cmd.type==='tick')return changed?commit():state;
 const p=cmd.actor;
 if(!s.players.includes(p)||(!both(s)&&s.turn!==p)||s.current[p].status!=='open'){
  if(changed)return commit();fail('Not eligible');
 }
 const c=s.current[p],q=s.questions[s.index];
 if(cmd.type==='answer'){
  if(!Number.isInteger(cmd.choice)||cmd.choice<0||cmd.choice>4||c.removed.includes(cmd.choice))fail('Invalid option');
  settle(s,p,cmd.choice===q.correct?'correct':'wrong',cmd.choice,now);
 }else if(cmd.type==='pass')settle(s,p,'pass',null,now);
 else if(['eliminate','half','time'].includes(cmd.type)){
  if(s.stats[p].hints>=s.hints||c.used.includes(cmd.type))fail('Hint unavailable');
  if(cmd.type==='time'){
   if(c.deadline===null)fail('No timed question');
   c.deadline+=10000;
  }else{
   const wrong=[0,1,2,3,4].filter(i=>i!==q.correct&&!c.removed.includes(i));
   // Stable random ordering from secret seed, actor and question. Never remove the correct answer.
   const rank=i=>createHash('sha256').update(JSON.stringify([s.seed,p,s.index,i])).digest('hex');
   wrong.sort((a,b)=>rank(a).localeCompare(rank(b)));
   const count=cmd.type==='half'?Math.max(0,3-c.removed.length):1;
   if(!count||wrong.length<count)fail('No further options to remove');
   c.removed.push(...wrong.slice(0,count));c.removed.sort((a,b)=>a-b);
  }
  c.used.push(cmd.type);s.stats[p].hints++;
 }else fail('Invalid action');
 return commit();
}
export function quizView(s,viewer){
 if(!s.players.includes(viewer))fail('Not a participant');
 const q=s.questions[s.index],mine=s.current?.[viewer];
 return clone({phase:s.phase,index:s.index,total:s.questions.length,revision:s.revision,mode:s.mode,
  turn:s.turn,streak:s.streak,scores:Object.fromEntries(s.players.map(p=>[p,s.stats[p].score])),
  question:q?{prompt:q.prompt,options:q.options,level:q.level}:null,
  mine:mine?{...mine,hintsLeft:s.hints-s.stats[viewer].hints}:null,
  history:s.history,result:s.result,stats:s.phase==='finished'?s.stats:undefined});
}
