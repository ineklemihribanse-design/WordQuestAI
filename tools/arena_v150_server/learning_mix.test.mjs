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
