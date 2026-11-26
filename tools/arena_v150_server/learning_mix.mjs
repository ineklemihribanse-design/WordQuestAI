import {createHash} from 'node:crypto';
import {authoredWords,authoredQuestions} from './learning_content.mjs';
import {extraQuestions,grammarVariants} from './learning_more.mjs';
import {academicWords,collocationQuestions} from './learning_academic.mjs';
import {readingQuestions} from './learning_reading.mjs';
import {coherenceQuestions} from './learning_coherence.mjs';
import {wordFormWords,wordFormQuestions} from './learning_wordforms.mjs';
import {lexicalRelationWords,lexicalRelationQuestions} from './learning_relations.mjs';
import {domainWords,domainQuestions} from './learning_domains.mjs';
import {contextQuestions} from './learning_context_batch.mjs';
import {deepReadingQuestions} from './learning_deep_reading.mjs';
import {foundationQuestions} from './learning_foundation.mjs';
import {advancedQuestions} from './learning_advanced.mjs';
import {logicQuestions} from './learning_logic.mjs';
import {researchQuestions} from './learning_research.mjs';
import {cohesionExpansion} from './learning_cohesion_expansion.mjs';
import {lexicalFastQuestions} from './learning_lexical_fast.mjs';
export const allLearningQuestions=[...authoredQuestions,...extraQuestions,...grammarVariants,...collocationQuestions,...readingQuestions,...coherenceQuestions,...wordFormQuestions,...lexicalRelationQuestions,...domainQuestions,...contextQuestions,...deepReadingQuestions,...foundationQuestions,...advancedQuestions,...logicQuestions,...researchQuestions,...cohesionExpansion,...lexicalFastQuestions];
const hash=(...v)=>createHash('sha256').update(JSON.stringify(v)).digest('hex');
// Keep every existing record and only append distinct new headwords. An
// explicit YDS sense may coexist with a previously untagged CEFR entry.
export function appendLearningWords(input,licensed=[]){
 const seen=new Set(input.map(w=>[w.level,String(w.answer).toUpperCase(),w.yds??''].join('|')));
 const out=[...input];
 for(const w of [...authoredWords,...academicWords,...wordFormWords,...lexicalRelationWords,...domainWords,...licensed]){
  const key=[w.level,w.answer,w.yds??''].join('|');
  if(!seen.has(key)){seen.add(key);out.push(w);}
 }
 return out;
}
// Replace a bounded share of translation items only when an unfiltered match
// has an eligible curated question at the same selected difficulty. Never
// alter requested count, selected levels, word-type filters or topic filters.
export function diversifyQuiz(items,cfg,seed,records=[]){
 const filtered=cfg.topic!==null||cfg.wordType!==null;
 const used=new Set(),result=[];
 const key=q=>cfg.scale==='yds'?q.yds:q.level;
 const eligible=allLearningQuestions.filter(q=>cfg.levels.includes(key(q))).flatMap(q=>{
  if(!filtered)return [{...q,topicId:null,wordType:null}];
  // A filtered vocabulary question must have a verified matching target word.
  if(q.topic!=='Vocabulary'&&!q.family?.startsWith('wordform_'))return [];
  const answer=q.options[q.correct].toUpperCase();
  const record=records.find(w=>w.answer.toUpperCase()===answer&&key(w)===key(q)&&
    (cfg.topic===null||w.topic===cfg.topic)&&(cfg.wordType===null||w.wordType===cfg.wordType));
  return record?[{...q,topicId:record.topic,wordType:record.wordType}]:[];
 });
 const byLevel=new Map();
 for(const q of eligible){
  const k=key(q);if(!byLevel.has(k))byLevel.set(k,[]);byLevel.get(k).push(q);
 }
 for(const [level,pool] of byLevel){
  byLevel.set(level,pool.map(q=>({q,rank:hash(seed,q.id)})).sort((a,b)=>a.rank.localeCompare(b.rank)).map(x=>x.q));
 }
 // Prefer different question families before reusing the same family.
 const typeCounts=new Map(),usedPatterns=new Set();
 const type=q=>q.family?.startsWith('cloze_')?'Cloze':q.family?.startsWith('wordform_')?'WordForm':(q.id.startsWith('wq150_ant_')||q.id.startsWith('wq150_speedlex_')&&q.id.endsWith('_ant'))?'Antonym':q.id.startsWith('wq150_cohesion_fill_')?'ParagraphCompletion':q.id.startsWith('wq150_cohesion_odd_')?'IrrelevantSentence':q.topic;
 for(const item of items){
  const k=key(item),pool=byLevel.get(k)??[];
  const available=pool.filter(q=>!used.has(q.id)&&(!q.family||!usedPatterns.has(q.family)));
  const q=available.reduce((best,candidate)=>!best||(typeCounts.get(type(candidate))??0)<(typeCounts.get(type(best))??0)?candidate:best,null);
  if(q&&used.size<Math.ceil(items.length*0.65)){
   used.add(q.id);typeCounts.set(type(q),(typeCounts.get(type(q))??0)+1);if(q.family)usedPatterns.add(q.family);
   const options=[...q.options].sort((a,b)=>hash(seed,q.id,a).localeCompare(hash(seed,q.id,b)));
   result.push({id:q.id,level:q.level,yds:q.yds,topic:q.topicId,wordType:q.wordType,prompt:q.prompt,options,correct:options.indexOf(q.options[q.correct]),letterCount:Math.max(2,Math.min(24,q.options[q.correct].length))});
  }else result.push(item);
 }
 return result;
}
