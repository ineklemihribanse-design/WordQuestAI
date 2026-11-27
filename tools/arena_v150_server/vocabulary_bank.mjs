// Reuse the shipped Arena vocabulary and its existing connected-board generator.
// No client can supply vocabulary or select a seed. Missing metadata fails closed.
import {createHash} from 'node:crypto';
import {crossword,quiz,quizSpellingKey,LEVELS} from '../v132_engine/engine.mjs';
import {validateSettings,selectBank} from '../v132_engine/match_service.mjs';
import {diversifyQuiz} from './learning_mix.mjs';
const fail=m=>{throw Error(m)};
const canonical=x=>typeof x==='string'?x.trim().normalize('NFC'):'';
const code=(...v)=>createHash('sha256').update(JSON.stringify(v)).digest('hex');
// The board retains its original alphabet and length limits. Expanded lexical
// entries with spaces, punctuation, or unsupported board categories are quiz-only.
export const crosswordEligible=w=>w.quizOnly!==true&&/^[A-Za-z]{2,24}$/.test(w.answer);
const quizAnswerEligible=s=>s.length<=80&&/^[A-Za-z]+(?:[ '-][A-Za-z]+)*$/.test(s);
// Resolve CEFR/YDS overlap after applying mode and metadata filters, not by
// deleting either sense from the dictionary. CEFR prefers its untagged entry.
export function preferCefrRecords(rows){
 const preferred=new Map();
 for(const w of rows){
  const k=w.level+'|'+w.answer.toUpperCase(),old=preferred.get(k);
  if(!old||(old.yds!=null&&w.yds==null))preferred.set(k,w);
 }
 return [...preferred.values()];
}
const ydsMap={easy:'A1',medium:'A2',hard:'B1'};
// Preserve established safe IDs; map labels containing spaces or non-ASCII
// characters to deterministic IDs accepted by the existing settings validator.
const filterId=x=>!x?null:/^[A-Za-z0-9_-]{1,100}$/.test(x)&&!['all','constructor','prototype','__proto__'].includes(x)?x:'f_'+code(x).slice(0,32);
export function loadVocabulary(input){
 if(!Array.isArray(input)||!input.length)fail('Missing shipped vocabulary');
 const rows=input.flatMap(w=>{
  if(!w||!LEVELS.includes(w.level)||!canonical(w.answer)||!canonical(w.meaning))return [];
  const answer=canonical(w.answer),meaning=canonical(w.meaning);
  const quizOnly=w.quizOnly===true;
  if(quizOnly?!quizAnswerEligible(answer):!crosswordEligible({answer}))return [];
  const topicLabel=canonical(w.category??w.topic)||null,wordTypeLabel=canonical(w.wordType??w.kind)||null;
  const topic=filterId(topicLabel),wordType=filterId(wordTypeLabel);
  const yds=Object.hasOwn(ydsMap,w.yds)?w.yds:null;
  return [{id:code(w.level,answer.toLowerCase(),meaning,topic,wordType,yds).slice(0,32),
   answer,meaning,level:w.level,topic,wordType,yds,topicLabel,wordTypeLabel,...(quizOnly?{quizOnly:true}:{})}];
 });
 if(!rows.length||LEVELS.some(l=>!rows.some(w=>w.level===l)))fail('Incomplete CEFR vocabulary');
 return [...new Map(rows.map(w=>[w.id,w])).values()];
}
export function vocabularyReport(rows){
 return {count:rows.length,levels:Object.fromEntries(LEVELS.map(l=>[l,rows.filter(w=>w.level===l).length])),
  yds:Object.fromEntries(Object.keys(ydsMap).map(l=>[l,rows.filter(w=>w.yds===l).length])),
  topicCount:new Set(rows.map(w=>w.topic).filter(Boolean)).size,
  wordTypeCount:new Set(rows.map(w=>w.wordType).filter(Boolean)).size};
}
// Only metadata and aggregate counts are exported, never private answer keys.
export function vocabularyCatalog(rows){
 const scales={cefr:LEVELS,yds:Object.keys(ydsMap)};
 const summarize=pool=>Object.fromEntries(Object.entries(scales).map(([scale,levels])=>[scale,
  Object.fromEntries(levels.map(level=>{
   const selected=pool.filter(w=>(scale==='cefr'?w.level:w.yds)===level);
   const quizRows=scale==='cefr'?preferCefrRecords(selected):selected;
   return [level,{crossword:new Set(selected.filter(crosswordEligible).map(w=>w.answer.toUpperCase())).size,
    quiz:Math.min(new Set(quizRows.map(w=>w.meaning.trim().toLowerCase())).size,
     new Set(quizRows.map(w=>quizSpellingKey(w.answer))).size)}];
  }))]));
 const groups=(key,label)=>[...new Map(rows.filter(w=>w[key]).map(w=>[w[key],w[label]])).entries()]
  .sort((a,b)=>a[1].localeCompare(b[1],'tr')).map(([id,name])=>({id,name,counts:summarize(rows.filter(w=>w[key]===id))}));
 return {schema:150,all:summarize(rows),topics:groups('topic','topicLabel'),wordTypes:groups('wordType','wordTypeLabel')};
}
export function createVocabularyBank(input,{curated=false}={}){
 const rows=loadVocabulary(input);
 return async(raw,seed)=>{
  const cfg=validateSettings(raw),isYds=cfg.scale==='yds';
  const eligible=rows.filter(w=>cfg.levels.includes(isYds?w.yds:w.level)&&
   (cfg.format==='quiz'||crosswordEligible(w))&&
   (cfg.topic===null||cfg.topic===w.topic)&&(cfg.wordType===null||cfg.wordType===w.wordType));
  const pool=isYds?eligible:preferCefrRecords(eligible);
  const levels=isYds?cfg.levels.map(x=>ydsMap[x]):cfg.levels;
  const records=pool.map(w=>({...w,level:isYds?ydsMap[w.yds]:w.level}));
  const byAnswer=new Map();
  for(const w of records){
   const key=w.level+'|'+w.answer.toUpperCase()+'|'+w.meaning;
   if(!byAnswer.has(key))byAnswer.set(key,w);
  }
  if(cfg.count<levels.length)fail('Too few questions for selected levels');
  const settings={mode:cfg.format==='quiz'?'quiz':'team',levels,size:cfg.count,seconds:cfg.seconds,hints:cfg.hints,retries:cfg.retries,topic:'all'};
  const n=Number.parseInt(code(seed).slice(0,8),16);
  const meta=(level,answer,meaning)=>{
   const w=byAnswer.get(level+'|'+answer.toUpperCase()+'|'+meaning);
   if(!w)fail('Generated word is not in trusted bank');
   const real=pool.find(x=>x.id===w.id);
   return {id:real.id,level:real.level,yds:real.yds,topic:real.topic,wordType:real.wordType};
  };
  let result;
  if(cfg.format==='quiz'){
   result=quiz(records,settings,n,true,rows).map(q=>({...meta(q.level,q.answer,q.prompt),
    prompt:q.prompt,options:q.options,correct:q.correct,letterCount:Math.max(2,Math.min(24,q.answer.replace(/[^A-Za-z]/g,'').length))}));
  }else{
   result=crossword(records,settings,n,true).words.map(([answer,meaning,level,r,c,d])=>({...meta(level,answer,meaning),
    answer,clue:meaning,cells:Array.from({length:answer.length},(_,i)=>(r+(d==='D'?i:0))+','+(c+(d==='A'?i:0)))}));
  }
  // Verify all chosen levels, filters and word counts again against the game engine.
  const selected=selectBank(result,cfg,seed);
  return curated&&cfg.format==='quiz'?selectBank(diversifyQuiz(selected,cfg,seed,pool),cfg,seed):selected;
 };
}
