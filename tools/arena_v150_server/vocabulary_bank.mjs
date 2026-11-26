// Reuse the shipped Arena vocabulary and its existing connected-board generator.
// No client can supply vocabulary or select a seed. Missing metadata fails closed.
import {createHash} from 'node:crypto';
import {crossword,quiz,LEVELS} from '../v132_engine/engine.mjs';
import {validateSettings,selectBank} from '../v132_engine/match_service.mjs';
const fail=m=>{throw Error(m)};
const canonical=x=>typeof x==='string'?x.trim().normalize('NFC'):'';
const code=(...v)=>createHash('sha256').update(JSON.stringify(v)).digest('hex');
const ydsMap={easy:'A1',medium:'A2',hard:'B1'};
export function loadVocabulary(input){
 if(!Array.isArray(input)||!input.length)fail('Missing shipped vocabulary');
 const rows=input.flatMap(w=>{
  if(!w||!LEVELS.includes(w.level)||!canonical(w.answer)||!canonical(w.meaning))return [];
  const answer=canonical(w.answer),meaning=canonical(w.meaning);
  if(!/^[A-Za-z]{2,24}$/.test(answer))return [];
  const topic=canonical(w.category??w.topic)||null,wordType=canonical(w.wordType??w.kind)||null;
  const yds=Object.hasOwn(ydsMap,w.yds)?w.yds:null;
  return [{id:code(w.level,answer.toLowerCase(),meaning,topic,wordType,yds).slice(0,32),
   answer,meaning,level:w.level,topic,wordType,yds}];
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
export function createVocabularyBank(input){
 const rows=loadVocabulary(input);
 return async(raw,seed)=>{
  const cfg=validateSettings(raw),isYds=cfg.scale==='yds';
  const pool=rows.filter(w=>cfg.levels.includes(isYds?w.yds:w.level)&&
   (cfg.topic===null||cfg.topic===w.topic)&&(cfg.wordType===null||cfg.wordType===w.wordType));
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
   result=quiz(records,settings,n,true).map(q=>({...meta(q.level,q.answer,q.prompt),
    prompt:q.prompt,options:q.options,correct:q.correct,letterCount:q.answer.length}));
  }else{
   result=crossword(records,settings,n,true).words.map(([answer,meaning,level,r,c,d])=>({...meta(level,answer,meaning),
    answer,clue:meaning,cells:Array.from({length:answer.length},(_,i)=>(r+(d==='D'?i:0))+','+(c+(d==='A'?i:0)))}));
  }
  // Verify all chosen levels, filters and word counts again against the game engine.
  return selectBank(result,cfg,seed);
 };
}
