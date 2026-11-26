import test from 'node:test';
import assert from 'node:assert/strict';
import {authoredWords,authoredQuestions} from './learning_content.mjs';
import {appendLearningWords,diversifyQuiz} from './learning_mix.mjs';
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
 assert.equal(q.length,10);assert.ok(q.some(x=>x.id.startsWith('wq150_q_')));assert.ok(q.some(x=>!x.id.startsWith('wq150_q_')));
 assert.ok(['B1','B2'].every(l=>q.some(x=>x.level===l)));
 assert.ok(q.every(x=>x.options.length===5&&x.correct>=0&&x.correct<5));
 const filtered=await build({kind:'quiz',mode:'classic',levels:['A1'],count:5,topic:'food'},'mix-b');
 assert.ok(filtered.every(x=>x.topic==='food'&&!x.id.startsWith('wq150_q_')));
 const yds=await build({kind:'quiz',mode:'classic',scale:'yds',levels:['easy','medium','hard'],count:15},'mix-c');
 assert.equal(yds.length,15);assert.ok(['easy','medium','hard'].every(l=>yds.some(x=>x.yds===l)));
});
