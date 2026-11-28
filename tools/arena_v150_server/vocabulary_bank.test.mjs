import test from 'node:test';
import assert from 'node:assert/strict';
import {createVocabularyBank,loadVocabulary,vocabularyReport,vocabularyCatalog,vocabularyRejectionReason,crosswordEligible} from './vocabulary_bank.mjs';
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

test('catalog reports real filters and safe stable identifiers without answers',async()=>{
 const input=records.map(w=>({...w,category:'Günlük yaşam'}));
 const c=vocabularyCatalog(loadVocabulary(input));
 assert.equal(c.topics.length,1);assert.equal(c.topics[0].name,'Günlük yaşam');assert.match(c.topics[0].id,/^f_[a-f0-9]{32}$/);
 assert.equal(c.topics[0].counts.cefr.A1.crossword,20);assert.equal(c.all.yds.hard.quiz,0);
 assert.ok(!JSON.stringify(c).includes('APPLE'));
 const q=await createVocabularyBank(input)({kind:'quiz',mode:'classic',levels:['A1'],count:5,topic:c.topics[0].id},'private');assert.ok(q.every(x=>x.topic===c.topics[0].id));
});


test('audit and loader agree on quiz-only phrases and invalid inputs',()=>{
 const phrase={level:'B2',answer:'RUSH HOUR',meaning:'yoğun trafik saati',quizOnly:true};
 const markedWord={level:'B2',answer:'SINCE',meaning:'-den beri',quizOnly:true};
 const entries=[phrase,markedWord,{...phrase,quizOnly:false},{...phrase,answer:'A'.repeat(81)},{...phrase,meaning:''},{...phrase,level:'Z9'},phrase];
 assert.deepEqual(entries.map(vocabularyRejectionReason),[null,null,'unsupportedAnswer','unsupportedAnswer','emptyAnswerOrMeaning','invalidLevel',null]);
 const loaded=loadVocabulary([...records,...entries]);
 assert.equal(loaded.length,records.length+2);
 assert.ok(loaded.filter(w=>w.quizOnly).every(w=>!crosswordEligible(w)));
 assert.equal(loaded.filter(w=>w.answer==='RUSH HOUR').length,1);
});
