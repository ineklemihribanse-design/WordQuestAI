import {createHash} from 'node:crypto';
import {authoredWords,authoredQuestions} from './learning_content.mjs';
const hash=(...v)=>createHash('sha256').update(JSON.stringify(v)).digest('hex');
// Keep every existing record and only append distinct new headwords. An
// explicit YDS sense may coexist with a previously untagged CEFR entry.
export function appendLearningWords(input,licensed=[]){
 const seen=new Set(input.map(w=>[w.level,String(w.answer).toUpperCase(),w.yds??''].join('|')));
 const out=[...input];
 for(const w of [...authoredWords,...licensed]){
  const key=[w.level,w.answer,w.yds??''].join('|');
  if(!seen.has(key)){seen.add(key);out.push(w);}
 }
 return out;
}
// Replace a bounded share of translation items only when an unfiltered match
// has an eligible curated question at the same selected difficulty. Never
// alter requested count, selected levels, word-type filters or topic filters.
export function diversifyQuiz(items,cfg,seed){
 if(cfg.topic!==null||cfg.wordType!==null)return items;
 const used=new Set(),result=[];
 const key=q=>cfg.scale==='yds'?q.yds:q.level;
 const eligible=authoredQuestions.filter(q=>cfg.levels.includes(key(q)));
 const byLevel=new Map();
 for(const q of eligible){
  const k=key(q);if(!byLevel.has(k))byLevel.set(k,[]);byLevel.get(k).push(q);
 }
 for(const pool of byLevel.values())pool.sort((a,b)=>hash(seed,a.id).localeCompare(hash(seed,b.id)));
 // Prefer different question families before reusing the same family.
 const families=new Set();
 for(const item of items){
  const k=key(item),pool=byLevel.get(k)??[];
  const available=pool.filter(q=>!used.has(q.id));
  const q=available.find(q=>!families.has(q.topic))??available[0];
  if(q&&result.length<Math.ceil(items.length*0.65)){
   used.add(q.id);families.add(q.topic);
   const options=[...q.options].sort((a,b)=>hash(seed,q.id,a).localeCompare(hash(seed,q.id,b)));
   result.push({id:q.id,level:q.level,yds:q.yds,topic:null,wordType:null,prompt:q.prompt,options,correct:options.indexOf(q.options[q.correct]),letterCount:Math.max(2,Math.min(24,q.options[q.correct].length))});
  }else result.push(item);
 }
 return result;
}
