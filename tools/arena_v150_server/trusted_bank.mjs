// Trusted content manifest: curated quiz questions and prevalidated crossword
// boards. No device-supplied answer keys and no invented production questions.
import {createHash} from 'node:crypto';
import {selectBank} from '../v132_engine/match_service.mjs';
const fail=m=>{throw Error(m)};
const clone=x=>JSON.parse(JSON.stringify(x));
const rank=(seed,id)=>createHash('sha256').update(JSON.stringify([seed,id])).digest('hex');
function validBoard(board){
 if(!Array.isArray(board)||board.length<10||board.length>70)return false;
 const used=new Map();
 for(const w of board){
  if(typeof w.answer!=='string'||!/^[A-Z]{2,35}$/.test(w.answer)||typeof w.clue!=='string'||!w.clue.trim()||
   !Array.isArray(w.cells)||w.cells.length!==w.answer.length||new Set(w.cells).size!==w.cells.length)return false;
  const p=w.cells.map(c=>typeof c==='string'&&/^\d{1,3},\d{1,3}$/.test(c)?c.split(',').map(Number):null);
  if(p.some(c=>!c))return false;
  const dr=p[1][0]-p[0][0],dc=p[1][1]-p[0][1];
  if(!((dr===1&&dc===0)||(dr===0&&dc===1))||p.some((c,i)=>c[0]!==p[0][0]+i*dr||c[1]!==p[0][1]+i*dc))return false;
  for(let i=0;i<w.cells.length;i++){
   const c=w.cells[i];if(used.has(c)&&used.get(c)!==w.answer[i])return false;used.set(c,w.answer[i]);
  }
 }
 return true;
}
export function createTrustedBank(manifest){
 if(!manifest||manifest.schema!==150||!Array.isArray(manifest.quizzes)||!Array.isArray(manifest.boards))fail('Invalid trusted bank manifest');
 if(!manifest.quizzes.length||!manifest.boards.length)fail('Both quiz and crossword content required');
 if(manifest.quizzes.some(q=>!q||typeof q.prompt!=='string'||!q.prompt.trim()||!Array.isArray(q.options)||q.options.length!==5||
  q.options.some(o=>typeof o!=='string'||!o.trim())||new Set(q.options).size!==5||!Number.isInteger(q.correct)||q.correct<0||q.correct>4))fail('Invalid curated quiz');
 if(manifest.boards.some(b=>!b||typeof b.id!=='string'||!validBoard(b.words)))fail('Invalid curated board');
 const content=clone(manifest);
 return async(settings,seed)=>{
  if(settings.format==='quiz')return selectBank(content.quizzes,settings,seed);
  const candidates=content.boards.filter(b=>{
   try{return selectBank(b.words,settings,seed).length===settings.count;}catch{return false;}
  }).sort((a,b)=>rank(seed,a.id).localeCompare(rank(seed,b.id)));
  if(!candidates.length)fail('No curated board matches selected settings');
  return clone(candidates[0].words);
 };
}
