// Trusted runtime only. Never ship answer keys or the adjudication reducer to clients.
// Persist state and command receipt atomically in a transaction.
import {createHash} from 'node:crypto';
const modes=new Set(['classic','streak','limited','speed','strategy']);
const clone=s=>JSON.parse(JSON.stringify(s));
const normalize=s=>String(s).trim().toUpperCase();
const tier={A1:1,A2:2,B1:3,B2:4,C1:5,C2:6};
export const wordPoints=w=>2*Math.min(35,Math.max(2,w.answer.length))+4*tier[w.level];
const other=(s,id)=>s.players.find(p=>p!==id);
const fail=m=>{throw new Error(m)};
export function createDuel({players,words,mode,limit=3,first,now,seconds=30,retries=0,skipLimit=2,hints=0,seed=''}){
 if(!modes.has(mode)||!Array.isArray(players)||players.length!==2||new Set(players).size!==2||
   players.some(p=>typeof p!=='string'||!p||['__proto__','constructor','prototype'].includes(p))||
   !players.includes(first)||!Array.isArray(words)||words.length<1||words.length>70||
   words.some(w=>!w||typeof w.answer!=='string'||!/^[A-Za-z]+$/.test(w.answer)||!Object.hasOwn(tier,w.level))||
   !Number.isSafeInteger(now)||now<0||!Number.isInteger(seconds)||seconds<0||seconds>600||
   ![2,3,5].includes(limit)||!Number.isInteger(retries)||retries<0||retries>3||
   !Number.isInteger(skipLimit)||skipLimit<0||skipLimit>20||!Number.isInteger(hints)||hints<0||hints>20||typeof seed!=='string'||hints>0&&!seed)fail('Invalid duel configuration');
 const geometry={};
 const normalized=words.map(w=>{
  const answer=normalize(w.answer);
  if(w.cells!==undefined&&(!Array.isArray(w.cells)||w.cells.length!==answer.length||
   !w.cells.every(c=>typeof c==='string'&&/^[0-9]{1,3},[0-9]{1,3}$/.test(c))||new Set(w.cells).size!==w.cells.length))fail('Invalid cells');
  (w.cells??[]).forEach((c,i)=>{if(Object.hasOwn(geometry,c)&&geometry[c]!==answer[i])fail('Conflicting crossing');geometry[c]=answer[i];});
  return {answer,clue:w.clue,level:w.level,cells:w.cells};
 });
 const s={players:[...players],words:normalized,mode,limit,seconds,
   retries,skipLimit,hints,seed,claims:Object.fromEntries(players.map(p=>[p,{}])),round:1,queue:words.map((_,i)=>i),deferred:[],active:null,turn:first,
   streak:0,revision:0,now,deadline:null,phase:'playing',result:null,receipts:{},
   stats:Object.fromEntries(players.map(p=>[p,{score:0,correct:0,wrong:0,pass:0,timeout:0,skip:0,hints:0}])),
   resolved:{},attempted:[],wrongCounts:{},last:null};
 next(s,now);return s;
}
function next(s,now){
 s.attempted=[];s.wrongCounts={};
 if(!s.queue.length && s.round===1 && s.deferred.length){
   s.round=2;s.queue=s.deferred;s.deferred=[];
 }
 if(!s.queue.length){s.phase='finished';s.active=null;s.deadline=null;
   const [a,b]=s.players;s.result={winner:s.stats[a].score===s.stats[b].score?null:
     s.stats[a].score>s.stats[b].score?a:b};return;}
 // Strategy explicitly chooses from the available words. Its clock starts on choice.
 s.active=s.mode==='strategy'?null:s.queue[0];
 s.deadline=s.active===null||!s.seconds?null:now+s.seconds*1000;
}
function switchTurn(s,actor){s.turn=other(s,actor);s.streak=0;}
function removeActive(s){s.queue=s.queue.filter(i=>i!==s.active);}
function bothFailed(s,now){
 const i=s.active;removeActive(s);
 if(s.round===1)s.deferred.push(i);
 else s.resolved[i]={kind:'revealed',answer:s.words[i].answer,points:0};
 next(s,now);
}
function failAttempt(s,actor,now,kind){
 s.stats[actor][kind]++;
 if(kind==='wrong'){
   s.stats[actor].score-=3;s.wrongCounts[actor]=(s.wrongCounts[actor]||0)+1;
   if(s.wrongCounts[actor]<=s.retries)return;
 }
 if(!s.attempted.includes(actor))s.attempted.push(actor);
 if(s.mode!=='speed')switchTurn(s,actor);
 if(s.attempted.length===2)bothFailed(s,now);
 else if(s.mode!=='speed')s.deadline=s.seconds?now+s.seconds*1000:null;
}
function visiblePositions(s,actor,i){
 const w=s.words[i],fixed={};
 if(w.cells)for(const [j] of Object.entries(s.resolved)){
  const solved=s.words[Number(j)];
  if(solved.cells)w.cells.forEach((cell,pos)=>{const at=solved.cells.indexOf(cell);if(at>=0)fixed[pos]=solved.answer[at];});
 }
 for(const [j,positions] of Object.entries(s.claims[actor]??{})){
  const old=s.words[Number(j)];
  for(const pos of positions){
   if(Number(j)===i)fixed[pos]=old.answer[pos];
   else if(w.cells&&old.cells){const at=w.cells.indexOf(old.cells[pos]);if(at>=0)fixed[at]=old.answer[pos];}
  }
 }
 return fixed;
}
function hint(s,actor){
 if(s.stats[actor].hints>=s.hints)fail('No hints left');
 const i=s.active,w=s.words[i],fixed=visiblePositions(s,actor,i);
 const candidates=Array.from({length:w.answer.length},(_,j)=>j).filter(j=>!Object.hasOwn(fixed,j));
 if(!candidates.length)fail('No hidden letters');
 const rank=j=>createHash('sha256').update(JSON.stringify([s.seed,actor,i,s.stats[actor].hints,j])).digest('hex');
 candidates.sort((a,b)=>rank(a).localeCompare(rank(b)));
 s.claims[actor][i]=[...(s.claims[actor][i]??[]),candidates[0]];
 s.stats[actor].hints++;
}
function accept(s,actor,now){
 const i=s.active;const points=wordPoints(s.words[i]);s.stats[actor].score+=points;
 s.stats[actor].correct++;s.resolved[i]={kind:'solved',by:actor,points};
 removeActive(s);
 if(s.mode==='streak'){s.turn=actor;s.streak++;}
 else if(s.mode==='limited'){
   s.streak++;if(s.streak>=s.limit)switchTurn(s,actor);else s.turn=actor;
 }else if(s.mode!=='speed')switchTurn(s,actor);
 next(s,now);
}
function expire(s,now){
 if(s.deadline===null||now<s.deadline)return false;
 const at=s.deadline;
 if(s.mode==='speed'){
   for(const p of s.players)if(!s.attempted.includes(p)){
     s.attempted.push(p);s.stats[p].timeout++;
   }
   bothFailed(s,at);
 }else failAttempt(s,s.turn,at,'timeout');
 return true;
}
export function applyDuel(state,cmd,now){
 if(!cmd||typeof cmd.id!=='string'||!/^[A-Za-z0-9_-]{1,100}$/.test(cmd.id)||
   ['__proto__','constructor','prototype'].includes(cmd.id)||!Number.isSafeInteger(now)||now<state.now)
   fail('Invalid command');
 if(Object.hasOwn(state.receipts,cmd.id))return state;
 if(state.phase!=='playing')fail('Match finished or paused');
 if(!Number.isInteger(cmd.revision)||cmd.revision!==state.revision)fail('Stale revision');
 const s=clone(state);
 // Expiry wins over a submitted answer at the exact deadline. Do not accept
 // that answer against the next player/word. The client must refresh state.
 if(expire(s,now)){
   s.last={kind:'timeout',at:now};s.receipts[cmd.id]=s.revision+1;s.revision++;s.now=now;return s;
 }
 if(cmd.type==='tick')return state;
 const actor=cmd.actor;
 if(!s.players.includes(actor)||s.mode!=='speed'&&actor!==s.turn)fail('Not your turn');
 if(cmd.type==='choose'){
   if(s.mode!=='strategy'||s.active!==null||!s.queue.includes(cmd.word))fail('Invalid selection');
   s.active=cmd.word;s.deadline=s.seconds?now+s.seconds*1000:null;
 }else{
   if(s.active===null||cmd.word!==s.active||s.attempted.includes(actor))fail('Invalid or exhausted word');
   if(cmd.type==='submit'){
     if(typeof cmd.answer!=='string'||!cmd.answer.trim())fail('Empty answer');
     if(normalize(cmd.answer)===s.words[s.active].answer)accept(s,actor,now);
     else failAttempt(s,actor,now,'wrong');
   }else if(cmd.type==='hint')hint(s,actor);
   else if(cmd.type==='pass')failAttempt(s,actor,now,'pass');
   else if(cmd.type==='skip'){
     if(s.stats[actor].skip>=s.skipLimit||s.round!==1)fail('Skip unavailable');
     s.stats[actor].skip++;const i=s.active;removeActive(s);s.deferred.push(i);
     // A skip retains the current player's turn and streak.
     next(s,now);
   }else fail('Invalid command type');
 }
 s.last={kind:cmd.type,actor,word:cmd.word,at:now};
 s.receipts[cmd.id]=s.revision+1;s.revision++;s.now=now;return s;
}
export function publicDuel(s,viewer){
 const {words,receipts,seed,claims,...rest}=s;
 if(viewer!==undefined&&!s.players.includes(viewer))fail('Not participant');
 const view={...clone(rest),words:words.map(({answer,...w})=>({...w,length:answer.length}))};
 if(viewer!==undefined){
  view.myHints=Object.fromEntries(words.map((_,i)=>[i,visiblePositions(s,viewer,i)]));
  view.hintsLeft=s.hints-s.stats[viewer].hints;
 }
 // Hint use counts are private during play.
 if(s.phase!=='finished')for(const id of s.players)if(id!==viewer)delete view.stats[id].hints;
 return view;
}
