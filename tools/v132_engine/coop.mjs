// Trusted cooperative crossword reducer. Answer keys and seed stay on the server.
// Caller authenticates actor, supplies trusted time and commits via one transaction.
import {createHash} from 'node:crypto';
const modes=['alternating','free','double','timed','perfect'];
const safe=x=>typeof x==='string'&&/^[A-Za-z0-9_-]{1,100}$/.test(x)&&!['constructor','prototype','__proto__'].includes(x);
const fail=m=>{throw Error(m)};
const clone=x=>JSON.parse(JSON.stringify(x));
const other=(s,p)=>s.players.find(x=>x!==p);
const done=(s,reason)=>{s.phase='finished';s.result={kind:reason,completed:reason==='completed',solved:Object.keys(s.solved).length,total:s.words.length,mistakes:s.mistakes};};
export function createCoop({players,words,mode,first,now,hints=0,seconds=0,maxMistakes=3,seed}){
 if(!modes.includes(mode)||!Array.isArray(players)||players.length!==2||new Set(players).size!==2||!players.every(safe)||!players.includes(first)||
 !Array.isArray(words)||words.length<1||words.length>70||!Number.isSafeInteger(now)||now<0||
 !Number.isInteger(hints)||hints<0||hints>20||!Number.isInteger(seconds)||seconds<0||seconds>3600||
 (mode==='timed'&&seconds===0)||!Number.isInteger(maxMistakes)||maxMistakes<1||maxMistakes>20||typeof seed!=='string'||!seed)fail('Invalid coop config');
 const cells={};
 const normalized=words.map(w=>{
  if(!w||typeof w.answer!=='string'||!/^[A-Za-z]{2,35}$/.test(w.answer)||typeof w.clue!=='string'||!w.clue.trim()||
   !Array.isArray(w.cells)||w.cells.length!==w.answer.length||!w.cells.every(c=>typeof c==='string'&&/^[0-9]{1,3},[0-9]{1,3}$/.test(c))||new Set(w.cells).size!==w.cells.length)fail('Invalid word geometry');
  const answer=w.answer.toUpperCase();
  w.cells.forEach((c,i)=>{if(Object.hasOwn(cells,c)&&cells[c]!==answer[i])fail('Conflicting crossing');cells[c]=answer[i];});
  return {clue:w.clue,answer,cells:[...w.cells]};
 });
 return {players:[...players],words:normalized,mode,turn:first,active:0,phase:'playing',revision:0,now,deadline:mode==='timed'?now+seconds*1000:null,
 maxMistakes,mistakes:0,hints,spent:0,seed,solved:{},known:{},claims:{},confirmed:[],passes:[],stats:Object.fromEntries(players.map(p=>[p,{correct:0,wrong:0,pass:0,hints:0}])),receipts:{},result:null};
}
function next(s,i){
 s.confirmed=[];s.passes=[];
 const all=s.words.map((_,j)=>j).filter(j=>!Object.hasOwn(s.solved,j));
 if(!all.length){done(s,'completed');s.active=null;return;}
 s.active=all.find(j=>j>i)??all[0];
}
function solve(s,i,actors){
 s.solved[i]={by:actors};s.words[i].cells.forEach((c,j)=>{s.known[c]=s.words[i].answer[j];});
 next(s,i);
}
export function applyCoop(state,cmd,now){
 if(!cmd||!safe(cmd.id)||!Number.isSafeInteger(now)||now<state.now)fail('Invalid command');
 if(Object.hasOwn(state.receipts,cmd.id))return state;
 if(state.phase!=='playing'||cmd.revision!==state.revision)fail('Stale or inactive room');
 const s=clone(state);
 function commit(){s.revision++;s.receipts[cmd.id]=s.revision;s.now=now;return s;}
 if(s.deadline!==null&&now>=s.deadline){done(s,'time-expired');return commit();}
 if(cmd.type==='tick')return state;
 const p=cmd.actor;
 if(!s.players.includes(p))fail('Not participant');
 if(cmd.type==='leave'){done(s,'incomplete');return commit();}
 if(s.mode==='alternating'&&p!==s.turn)fail('Not your turn');
 if(!Number.isInteger(cmd.word)||cmd.word<0||cmd.word>=s.words.length||Object.hasOwn(s.solved,cmd.word))fail('Invalid word');
 if(['alternating','double'].includes(s.mode)&&cmd.word!==s.active)fail('Not active word');
 const i=cmd.word,w=s.words[i];
 if(cmd.type==='hint'){
  if(s.spent>=s.hints)fail('No hints left');
  const candidates=w.cells.filter(c=>!Object.hasOwn(s.known,c));
  if(!candidates.length)fail('No missing letters');
  const rank=c=>createHash('sha256').update(JSON.stringify([s.seed,i,s.spent,c])).digest('hex');
  candidates.sort((a,b)=>rank(a).localeCompare(rank(b)));
  const cell=candidates[0];s.known[cell]=w.answer[w.cells.indexOf(cell)];s.claims[cell]=p;s.spent++;s.stats[p].hints++;
 }else if(cmd.type==='pass'){
  if(s.mode==='double'&&s.confirmed.includes(p))fail('Already confirmed');
  if(s.mode==='double'){
   if(s.passes.includes(p))fail('Already passed');
   s.passes.push(p);if(s.passes.length===2){next(s,i);}
  }else next(s,i);
  s.stats[p].pass++;if(s.mode==='alternating')s.turn=other(s,p);
 }else if(cmd.type==='submit'){
  if(typeof cmd.answer!=='string'||!cmd.answer.trim())fail('Empty answer');
  if(s.mode==='double'&&(s.confirmed.includes(p)||s.passes.includes(p)))fail('Already acted');
  if(cmd.answer.trim().toUpperCase()===w.answer){
   s.stats[p].correct++;
   if(s.mode==='double'){
    s.confirmed.push(p);
    if(s.confirmed.length===2)solve(s,i,[...s.confirmed]);
   }else solve(s,i,[p]);
   if(s.mode==='alternating')s.turn=other(s,p);
  }else{
   s.stats[p].wrong++;s.mistakes++;
   if(s.mode==='perfect'&&s.mistakes>=s.maxMistakes)done(s,'mistake-limit');
   else if(s.mode==='alternating')s.turn=other(s,p);
  }
 }else fail('Unknown command');
 return commit();
}
export function coopView(s){
 const {words,seed,receipts,...rest}=s;
 return {...clone(rest),words:words.map(({answer,...w})=>({...w,length:answer.length}))};
}
