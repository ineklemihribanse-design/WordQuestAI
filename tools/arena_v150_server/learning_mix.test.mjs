import test from 'node:test';
import assert from 'node:assert/strict';
import {authoredWords,authoredQuestions} from './learning_content.mjs';
import {academicWords,collocationQuestions} from './learning_academic.mjs';
import {readingQuestions} from './learning_reading.mjs';
import {appendLearningWords,diversifyQuiz,allLearningQuestions} from './learning_mix.mjs';
import {createVocabularyBank} from './vocabulary_bank.mjs';
import {LEVELS} from '../v132_engine/engine.mjs';
test('curated corpus contains unique, complete five-option questions',()=>{
 assert.equal(new Set(authoredQuestions.map(q=>q.id)).size,authoredQuestions.length);
 assert.equal(new Set(authoredQuestions.map(q=>q.prompt.toLowerCase())).size,authoredQuestions.length);
 assert.ok(authoredQuestions.length>=50);
 assert.ok(authoredQuestions.every(q=>q.options.length===5&&new Set(q.options.map(o=>o.toLowerCase())).size===5&&q.explanation.length>=20&&q.correct===0));
 assert.ok(LEVELS.every(l=>authoredQuestions.some(q=>q.level===l)));
 assert.ok(new Set(authoredQuestions.map(q=>q.topic)).size>=6);
 assert.ok(authoredWords.length>=180);
});
test('expansion is append-only and idempotent',()=>{
 const base=[{level:'B1',answer:'ACQUIRE',meaning:'old',yds:'easy'}];
 const out=appendLearningWords(base);
 assert.equal(out[0],base[0]);assert.equal(out.length,appendLearningWords(out).length);
});
test('curated question generation preserves filters, levels and count',async()=>{
 const sample=LEVELS.flatMap(level=>['APPLE','PLANE','LEMON','MELON','PEACH','GRAPE','PEAR','PAPER','PENCIL','CHAIR','TABLE','WATER','RIVER','GREEN','BREAD','CLOUD','FLOWER','GARDEN','WINDOW','HOUSE'].map((answer,i)=>({answer,meaning:level+' clue '+i,level,category:'food'})));
 const build=createVocabularyBank(appendLearningWords(sample),{curated:true});
 const q=await build({kind:'quiz',mode:'classic',levels:['B1','B2'],count:10},'mix-a');
 assert.equal(q.length,10);assert.ok(q.some(x=>x.id.startsWith('wq150_')));assert.ok(q.some(x=>!x.id.startsWith('wq150_')));
 assert.ok(['B1','B2'].every(l=>q.some(x=>x.level===l)));
 assert.ok(q.every(x=>x.options.length===5&&x.correct>=0&&x.correct<5));
 const filtered=await build({kind:'quiz',mode:'classic',levels:['A1'],count:5,topic:'food'},'mix-b');
 assert.equal(filtered.length,5);
 assert.ok(filtered.every(x=>x.topic==='food'&&x.level==='A1'&&sample.some(w=>w.level==='A1'&&w.category==='food'&&w.answer===x.options[x.correct].toUpperCase())));
 // A curated vocabulary item is allowed when its answer belongs to the exact selected filter.
 const yds=await build({kind:'quiz',mode:'classic',scale:'yds',levels:['easy','medium','hard'],count:15},'mix-c');
 assert.equal(yds.length,15);assert.ok(['easy','medium','hard'].every(l=>yds.some(x=>x.yds===l)));
});

test('larger mixed pool has unique questions and five options',()=>{assert.ok(allLearningQuestions.length>=600);assert.equal(new Set(allLearningQuestions.map(q=>q.id)).size,allLearningQuestions.length);assert.equal(new Set(allLearningQuestions.map(q=>q.prompt.toLowerCase())).size,allLearningQuestions.length);assert.ok(allLearningQuestions.every(q=>q.options.length===5&&new Set(q.options.map(o=>o.toLowerCase())).size===5));});

test('academic expansion has explicit YDS metadata and complete five-choice questions',()=>{assert.ok(academicWords.length>=300);assert.equal(new Set(academicWords.map(w=>w.level+'|'+w.answer)).size,academicWords.length);assert.ok(academicWords.every(w=>/^[A-Z]{2,22}$/.test(w.answer)&&w.meaning.trim()&&['easy','medium','hard'].includes(w.yds)));assert.ok(collocationQuestions.length>=60);assert.ok(collocationQuestions.every(q=>q.options.length===5&&new Set(q.options.map(o=>o.toLowerCase())).size===5&&q.explanation.length>=20));});

test('reading bank contains distinct passages with paired comprehension items and basic-tier questions',()=>{const reading=readingQuestions.filter(q=>q.topic==='Paragraph');assert.equal(reading.length,40);assert.equal(new Set(reading.map(q=>q.family)).size,20);assert.equal(readingQuestions.filter(q=>q.level==='A1'||q.level==='A2').length,32);assert.ok(readingQuestions.every(q=>q.options.length===5&&new Set(q.options.map(x=>x.toLowerCase())).size===5&&q.explanation.length>=20));});

import {coherenceQuestions} from './learning_coherence.mjs';
test('cloze sets are distinct and all have one answer with five options',()=>{const cloze=coherenceQuestions.filter(q=>q.family?.startsWith('cloze_'));assert.equal(cloze.length,36);assert.equal(new Set(cloze.map(q=>q.family)).size,12);assert.ok(cloze.every(q=>q.prompt.split('____').length===2));assert.ok(coherenceQuestions.every(q=>q.options.length===5&&new Set(q.options).size===5&&q.explanation.length>=20));});

import {wordFormWords,wordFormQuestions} from './learning_wordforms.mjs';
test('word formation practice uses distinct families and five forms',()=>{assert.equal(wordFormQuestions.length,140);assert.equal(new Set(wordFormQuestions.map(q=>q.family)).size,35);assert.ok(wordFormQuestions.every(q=>q.options.length===5&&new Set(q.options).size===5&&q.prompt.split('____').length===2&&q.correct===0));assert.ok(wordFormWords.every(w=>/^[A-Z]{2,22}$/.test(w.answer)&&['İsimler','Fiiller','Sıfatlar','Zarflar'].includes(w.wordType)));});

test('unfiltered mixed match balances question families, including word forms and cloze',()=>{
 const cfg={scale:'cefr',levels:['B2'],topic:null,wordType:null};
 const items=Array.from({length:30},(_,i)=>({id:'legacy'+i,level:'B2',yds:'medium'}));
 const picked=diversifyQuiz(items,cfg,'balance-2026');
 const curated=picked.filter(q=>q.id.startsWith('wq150_'));
 assert.equal(picked.length,30);assert.equal(curated.length,20);
 const byId=new Map(allLearningQuestions.map(q=>[q.id,q]));
 assert.ok(curated.some(q=>byId.get(q.id)?.family?.startsWith('wordform_')));assert.ok(curated.some(q=>byId.get(q.id)?.family?.startsWith('cloze_')));
 const families=curated.map(q=>byId.get(q.id).family).filter(Boolean);assert.equal(new Set(families).size,families.length);
 const types=new Set(curated.map(q=>{const x=byId.get(q.id);return x.family?.startsWith('cloze_')?'Cloze':x.family?.startsWith('wordform_')?'WordForm':x.topic;}));assert.ok(types.size>=7);
});

import {lexicalRelationWords,lexicalRelationQuestions} from './learning_relations.mjs';
test('lexical relations and phrasal verbs are complete and grouped',()=>{const relations=lexicalRelationQuestions.filter(q=>q.topic==='Synonym');assert.equal(relations.length,128);assert.equal(new Set(relations.map(q=>q.family)).size,64);assert.equal(lexicalRelationQuestions.filter(q=>q.topic==='Phrasals').length,32);assert.ok(lexicalRelationQuestions.every(q=>q.options.length===5&&new Set(q.options).size===5&&q.explanation.length>=20));assert.ok(lexicalRelationWords.every(w=>/^[A-Z]{2,22}$/.test(w.answer)&&w.meaning));});

import {domainWords,domainQuestions} from './learning_domains.mjs';
test('academic domains contain complete contextual items and explicit level metadata',()=>{assert.equal(domainWords.length,320);assert.equal(domainQuestions.length,71);assert.equal(new Set(domainWords.map(w=>w.level+'|'+w.answer)).size,domainWords.length);assert.equal(new Set(domainWords.map(w=>w.category)).size,6);assert.ok(domainWords.every(w=>/^[A-Z]{2,22}$/.test(w.answer)&&w.meaning.trim()&&w.wordType==='İsimler'));assert.ok(domainQuestions.every(q=>q.options.length===5&&new Set(q.options).size===5&&q.correct===0&&q.explanation.length>=20));assert.ok(domainQuestions.some(q=>q.level==='C2'));});

import {contextQuestions} from './learning_context_batch.mjs';
test('context question batch has unique stems, five options and paired concept families',()=>{assert.equal(contextQuestions.length,208);assert.equal(new Set(contextQuestions.map(q=>q.prompt.toLowerCase())).size,contextQuestions.length);assert.ok(contextQuestions.every(q=>q.options.length===5&&new Set(q.options.map(o=>o.toLowerCase())).size===5&&q.prompt.split('____').length===2&&q.explanation.length>=20));const families=new Map();for(const q of contextQuestions)families.set(q.family,(families.get(q.family)??0)+1);assert.ok([...families.values()].every(n=>n<=2));assert.ok(new Set(contextQuestions.map(q=>q.topic)).size>=4);});

import {deepReadingQuestions} from './learning_deep_reading.mjs';
test('extended comprehension bank has distinct stems and five unique options',()=>{assert.equal(deepReadingQuestions.length,201);assert.ok(deepReadingQuestions.every(q=>q.options.length===5&&new Set(q.options.map(o=>o.toLowerCase())).size===5&&q.correct===0&&q.explanation.length>=20&&q.yds));const cloze=deepReadingQuestions.filter(q=>q.family?.startsWith('cloze_deep_'));assert.equal(cloze.length,100);assert.ok(cloze.every(q=>q.prompt.split('____').length===2));const reading=deepReadingQuestions.filter(q=>q.family?.startsWith('reading_deep_'));assert.equal(reading.length,60);assert.ok(reading.every(q=>q.prompt.split('\n\n')[0].split(/\s+/).length>=40));});

import {foundationQuestions} from './learning_foundation.mjs';
test('foundation questions add A1 A2 B1 contextual practice',()=>{assert.equal(foundationQuestions.length,206);assert.ok(['A1','A2','B1'].every(l=>foundationQuestions.filter(q=>q.level===l).length>=40));assert.ok(foundationQuestions.every(q=>q.options.length===5&&new Set(q.options.map(o=>o.toLowerCase())).size===5&&q.prompt.split('____').length===2));});

import {advancedQuestions} from './learning_advanced.mjs';
test('advanced contextual batch has distinct stems and five options',()=>{assert.equal(advancedQuestions.length,231);assert.equal(new Set(advancedQuestions.map(q=>q.prompt.toLowerCase())).size,advancedQuestions.length);assert.ok(advancedQuestions.every(q=>q.options.length===5&&new Set(q.options.map(o=>o.toLowerCase())).size===5&&q.prompt.split('____').length===2&&q.explanation.length>=20&&q.yds));assert.ok(['B2','C1','C2'].every(l=>advancedQuestions.some(q=>q.level===l)));assert.ok(allLearningQuestions.length>=2000);});

import {logicQuestions} from './learning_logic.mjs';
test('discourse practice uses five unique options and explicit families',()=>{assert.equal(logicQuestions.length,192);assert.ok(logicQuestions.every(q=>q.options.length===5&&new Set(q.options.map(o=>o.toLowerCase())).size===5&&q.correct===0&&q.explanation.length>=20&&q.family&&q.yds));assert.equal(new Set(logicQuestions.map(q=>q.prompt.toLowerCase())).size,logicQuestions.length);assert.ok(logicQuestions.filter(q=>q.topic==='Completion').every(q=>q.prompt.includes('____')));});

import {researchQuestions} from './learning_research.mjs';
test('research expansion includes distinct reading and contextual items',()=>{assert.equal(researchQuestions.length,299);assert.equal(new Set(researchQuestions.map(q=>q.prompt.toLowerCase())).size,researchQuestions.length);assert.ok(researchQuestions.every(q=>q.options.length===5&&new Set(q.options.map(o=>o.toLowerCase())).size===5&&q.correct===0&&q.explanation.length>=20&&q.family&&q.yds));assert.ok(researchQuestions.filter(q=>q.topic==='Paragraph').every(q=>q.prompt.split('\n\n')[0].split(/\s+/).length>=40));assert.ok(researchQuestions.filter(q=>q.topic==='Vocabulary').every(q=>q.prompt.split('____').length===2));});

import {cohesionExpansion} from './learning_cohesion_expansion.mjs';
test('cohesion expansion has complete items and rotating irrelevant sentence positions',()=>{assert.equal(cohesionExpansion.length,164);assert.equal(new Set(cohesionExpansion.map(q=>q.prompt.toLowerCase())).size,cohesionExpansion.length);assert.ok(cohesionExpansion.every(q=>q.options.length===5&&new Set(q.options.map(o=>o.toLowerCase())).size===5&&q.correct===0&&q.family&&q.yds&&q.explanation.length>=20));const odd=cohesionExpansion.filter(q=>q.id.includes('_odd_'));assert.equal(odd.length,28);assert.equal(new Set(odd.map(q=>q.options[0])).size,5);assert.ok(odd.every(q=>['I','II','III','IV','V'].every(n=>q.prompt.includes('('+n+')'))));assert.ok(cohesionExpansion.filter(q=>!q.id.includes('_odd_')).every(q=>q.prompt.split('____').length===2));});

import {lexicalFastQuestions} from './learning_lexical_fast.mjs';
test('bulk lexical expansion has distinct stems and bounded families',()=>{assert.equal(lexicalFastQuestions.length,368);assert.equal(new Set(lexicalFastQuestions.map(q=>q.prompt.toLowerCase())).size,lexicalFastQuestions.length);assert.ok(lexicalFastQuestions.every(q=>q.options.length===5&&new Set(q.options.map(o=>o.toLowerCase())).size===5&&q.correct===0&&q.family&&q.yds&&q.explanation.length>=20));const prior=new Set(allLearningQuestions.filter(q=>!q.id.startsWith('wq150_speedlex_')).map(q=>q.prompt.toLowerCase()));assert.ok(lexicalFastQuestions.every(q=>!prior.has(q.prompt.toLowerCase())));});

import {bulkPhraseQuestions} from './learning_bulk_phrases.mjs';
test('bulk phrase questions have five unique options and no exact prior stem duplicates',()=>{assert.equal(bulkPhraseQuestions.length,618);assert.equal(new Set(bulkPhraseQuestions.map(q=>q.prompt.toLowerCase())).size,bulkPhraseQuestions.length);assert.ok(bulkPhraseQuestions.every(q=>q.options.length===5&&new Set(q.options.map(o=>o.toLowerCase())).size===5&&q.correct===0&&q.family&&q.yds&&q.explanation.length>=20));const prior=new Set(allLearningQuestions.filter(q=>!q.id.startsWith('wq150_bulkphrase_')).map(q=>q.prompt.toLowerCase()));assert.ok(bulkPhraseQuestions.every(q=>!prior.has(q.prompt.toLowerCase())));});

import {prepositionBasicQuestions} from './learning_preposition_basic.mjs';
test('preposition and basic meaning expansion is complete and unique',()=>{assert.equal(prepositionBasicQuestions.length,389);assert.ok(prepositionBasicQuestions.every(q=>q.options.length===5&&new Set(q.options).size===5&&q.correct===0&&q.family&&q.explanation.length>=20));assert.ok(prepositionBasicQuestions.filter(q=>q.family.startsWith('prepbulk_')).every(q=>q.prompt.split('____').length===2));const prior=new Set(allLearningQuestions.filter(q=>!prepositionBasicQuestions.includes(q)).map(q=>q.prompt.toLowerCase()));assert.ok(prepositionBasicQuestions.every(q=>!prior.has(q.prompt.toLowerCase())));});
test('B2 mixed selection retains cloze and word forms across seeds',()=>{const cfg={scale:'cefr',levels:['B2'],topic:null,wordType:null};const byId=new Map(allLearningQuestions.map(q=>[q.id,q]));for(const seed of ['mix-regression-a','mix-regression-b','mix-regression-c']){const items=Array.from({length:30},(_,i)=>({id:'legacy'+i,level:'B2',yds:'medium'}));const curated=diversifyQuiz(items,cfg,seed).filter(q=>byId.has(q.id));assert.equal(curated.length,20);assert.ok(curated.some(q=>byId.get(q.id).family?.startsWith('cloze_')));assert.ok(curated.some(q=>byId.get(q.id).family?.startsWith('wordform_')));assert.ok(curated.some(q=>byId.get(q.id).family?.startsWith('prepbulk_')));}});

import {verbalNewQuestions} from './learning_verbal_new.mjs';
test('verbal expansion includes paired blanks and no exact previous stems',()=>{assert.equal(verbalNewQuestions.length,176);assert.equal(new Set(verbalNewQuestions.map(q=>q.prompt.toLowerCase())).size,verbalNewQuestions.length);assert.ok(verbalNewQuestions.every(q=>q.options.length===5&&new Set(q.options.map(o=>o.toLowerCase())).size===5&&q.correct===0&&q.family&&q.yds&&q.explanation.length>=20));assert.ok(verbalNewQuestions.filter(q=>q.id.startsWith('wq150_verbalpair_')).every(q=>q.prompt.split('____').length===3&&q.options.every(o=>o.split(' / ').length===2)));const prior=new Set(allLearningQuestions.filter(q=>!q.id.startsWith('wq150_verbalnew_')&&!q.id.startsWith('wq150_verbalpair_')).map(q=>q.prompt.toLowerCase()));assert.ok(verbalNewQuestions.every(q=>!prior.has(q.prompt.toLowerCase())));});

import {formExtension} from './learning_form_extension.mjs';
test('word form extension records have unique stems and choices',()=>{assert.equal(formExtension.length,56);assert.ok(formExtension.every(q=>q.options.length===5&&new Set(q.options).size===5&&q.correct===0&&q.prompt.split('____').length===2));const prior=new Set(allLearningQuestions.filter(q=>!q.id.startsWith('wq150_formextension_')).map(q=>q.prompt.toLowerCase()));assert.ok(formExtension.every(q=>!prior.has(q.prompt.toLowerCase())));});

import {clozeExpansion} from './learning_cloze_expansion.mjs';
test('cloze expansion has complete passage families and distinct stems',()=>{assert.equal(clozeExpansion.length,125);assert.equal(new Set(clozeExpansion.map(q=>q.prompt.toLowerCase())).size,clozeExpansion.length);assert.ok(clozeExpansion.every(q=>q.options.length===5&&new Set(q.options.map(o=>o.toLowerCase())).size===5&&q.correct===0&&q.family.startsWith('cloze_')&&q.yds&&q.explanation.length>=20&&q.prompt.split('____').length===2));const prior=new Set(allLearningQuestions.filter(q=>!q.id.startsWith('wq150_clozeexp_')).map(q=>q.prompt.toLowerCase()));assert.ok(clozeExpansion.every(q=>!prior.has(q.prompt.toLowerCase())));});
