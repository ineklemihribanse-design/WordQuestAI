import {readFile,writeFile,stat} from 'node:fs/promises';
import {createHash} from 'node:crypto';
import {createVocabularyBank,loadVocabulary,vocabularyReport,vocabularyCatalog,vocabularyRejectionReason,crosswordEligible} from './vocabulary_bank.mjs';
import assert from 'node:assert/strict';
import {expandLearning} from './expand_learning.mjs';
const input=process.argv[2],out=process.argv[3];
if(!input||!out)throw Error('usage: real_vocabulary_smoke.mjs input output-dir');
const bytes=await readFile(input),raw=JSON.parse(bytes),expanded=await expandLearning(raw),rows=loadVocabulary(expanded),build=createVocabularyBank(expanded,{curated:true});
const report=vocabularyReport(rows);
for(const levels of [['A1'],['A1','A2','B1'],['B2','C1','C2']]){
 const b=await build({kind:'duel',mode:'classic',levels,count:20},'wq150-audit');
 assert.equal(b.length,20);assert.ok(levels.every(l=>b.some(w=>w.level===l)));
 const q=await build({kind:'quiz',mode:'classic',levels,count:20},'wq150-audit');
 assert.equal(q.length,20);assert.ok(q.every(x=>x.options.length===5));
}
assert.ok((await stat(out)).isDirectory(),'Audit output must be a directory prepared by CI');
assert.ok(expanded.length>raw.length+1000,'Vocabulary expansion unexpectedly small');
await writeFile(out+'/arena-vocabulary-v1.json',bytes);
await writeFile(out+'/report.json',JSON.stringify({...report,sha256:createHash('sha256').update(bytes).digest('hex'),tested:'3 connected boards and 3 quizzes'},null,2)+'\n');
await writeFile(out+'/catalog.json',JSON.stringify(vocabularyCatalog(rows),null,2)+'\n');
await writeFile(out+'/arena-learning-expanded.json',JSON.stringify(expanded));
const distinct=rows=>new Set(rows.map(w=>String(w.answer).trim().toUpperCase())).size;
// Explain every record omitted by the existing loader, without changing eligibility.
const rejectionReasons={invalidLevel:0,emptyAnswerOrMeaning:0,unsupportedAnswer:0,duplicateCanonicalRecord:0};
let eligible=0;
for(const w of expanded){
 const reason=vocabularyRejectionReason(w);
 if(reason!==null){rejectionReasons[reason]++;continue;}
 eligible++;
}
rejectionReasons.duplicateCanonicalRecord=eligible-rows.length;
assert.ok(rejectionReasons.duplicateCanonicalRecord>=0);
assert.equal(Object.values(rejectionReasons).reduce((a,b)=>a+b,0),expanded.length-rows.length);
console.log('VOCABULARY_REJECTION_AUDIT',JSON.stringify(rejectionReasons));
const expansion={base:raw.length,expanded:expanded.length,net:expanded.length-raw.length,validatedRecords:rows.length,uniqueEnglishHeadwords:distinct(rows),quizOnlyRecords:rows.filter(w=>w.quizOnly===true).length,crosswordEligibleRecords:rows.filter(crosswordEligible).length,uniqueOriginalHeadwords:distinct(loadVocabulary(raw)),byLevel:report.levels,yds:report.yds,rejectionReasons};
await writeFile(out+'/expanded-report.json',JSON.stringify(expansion,null,2)+'\n');
console.log('LEARNING EXPANSION',JSON.stringify(expansion));
console.log('PASS real shipped vocabulary',JSON.stringify(report));
