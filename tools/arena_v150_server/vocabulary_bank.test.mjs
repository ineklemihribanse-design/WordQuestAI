import test from 'node:test';
import assert from 'node:assert/strict';
import {createVocabularyBank,loadVocabulary,vocabularyReport} from './vocabulary_bank.mjs';
import {LEVELS,normalizeSettings} from '../v132_engine/engine.mjs';
const answers=['APPLE','PLANE','LEMON','MELON','PEACH','GRAPE','PEAR','PAPER','PENCIL','CHAIR','TABLE','WATER','RIVER','GREEN','BREAD','CLOUD','FLOWER','GARDEN','WINDOW','HOUSE'];
const records=LEVELS.flatMap((level,i)=>answers.map((answer,j)=>({level,answer,meaning:level+' clue '+j,category:'food',kind:'noun',...(i===0?{yds:'easy'}:{})})));
test('legacy generator constraints remain unchanged; server counts opt in',()=>{
 const s={mode:'team',levels:['A1'],size:10,seconds:30,hints:3,retries:0,topic:'all'};
 assert.throws(()=>normalizeSettings(s));assert.equal(normalizeSettings(s,true).size,10);
});
test('real record normalization and metadata reporting',()=>{
 const rows=loadVocabulary(records),report=vocabularyReport(rows);
 assert.equal(report.count,120);assert.equal(report.levels.C2,20);assert.equal(report.yds.easy,20);
});
test('existing generator produces exact selected quiz counts and options',async()=>{
 const build=createVocabularyBank(records);
 const q=await build({kind:'quiz',mode:'classic',levels:['A1'],count:7,topic:'food'},'private');
 assert.equal(q.length,7);assert.ok(q.every(x=>x.level==='A1'&&x.options.length===5&&x.correct>=0&&x.topic==='food'));
});
test('connected board uses only source entries and exact count',async()=>{
 const build=createVocabularyBank(records);
 const b=await build({kind:'duel',mode:'classic',levels:['A1'],count:10,wordType:'noun'},'private');
 assert.equal(b.length,10);assert.ok(b.every(w=>answers.includes(w.answer)&&w.cells.length===w.answer.length));
});
test('YDS requires explicit metadata, does not invent level mapping',async()=>{
 const build=createVocabularyBank(records);
 const q=await build({kind:'quiz',mode:'classic',scale:'yds',levels:['easy'],count:5},'private');
 assert.ok(q.every(x=>x.yds==='easy'));
 await assert.rejects(()=>build({kind:'quiz',mode:'classic',scale:'yds',levels:['hard'],count:5},'private'));
});
