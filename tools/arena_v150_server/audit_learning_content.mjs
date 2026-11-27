import { authoredWords } from './learning_content.mjs';
import { academicWords } from './learning_academic.mjs';
import { wordFormWords } from './learning_wordforms.mjs';
import { lexicalRelationWords } from './learning_relations.mjs';
import { domainWords } from './learning_domains.mjs';
import { allLearningQuestions as rows } from './learning_mix.mjs';
import assert from 'node:assert/strict';
const norm = s => String(s ?? '').normalize('NFKC').toLowerCase().replace(/[‘’]/g, "'").replace(/[“”]/g, '"').replace(/\s+/g,' ').trim();
const group = (items, key) => { const out=new Map(); for(const q of items){const k=key(q); if(!out.has(k))out.set(k,[]);out.get(k).push(q);} return out; };
const counts = (items,key) => Object.fromEntries([...group(items,key)].map(([k,v])=>[k,v.length]));
const duplicates = (items,key) => [...group(items,key).values()].filter(v=>v.length>1).map(v=>v.map(q=>q.id));
const levels=['A1','A2','B1','B2','C1','C2'];
const issues=[];
for(const q of rows){
 const fail=reason=>issues.push({id:q.id,reason});
 if(!norm(q.id)||!norm(q.prompt))fail('missing id or prompt');
 if(!levels.includes(q.level)||![null,undefined,'easy','medium','hard'].includes(q.yds))fail('invalid level');
 if(!Array.isArray(q.options)||q.options.length!==5||q.options.some(x=>typeof x!=='string'||!norm(x))||new Set(q.options.map(norm)).size!==5)fail('invalid options');
 if(!Number.isInteger(q.correct)||q.correct<0||q.correct>=5)fail('invalid answer index');
 if(typeof q.explanation!=='string'||q.explanation.trim().length<20)fail('missing explanation');
 else if(q.explanation.startsWith('Doğru seçenek:')&&!q.explanation.startsWith('Doğru seçenek: '+q.options?.[q.correct]+'.'))fail('answer/explanation mismatch');
}
const duplicateIds=duplicates(rows,q=>q.id),duplicatePrompts=duplicates(rows,q=>norm(q.prompt));
const near=duplicates(rows,q=>norm(q.prompt).replace(/\b\d+(?:[.,:]\d+)*\b/g,'#').replace(/[.!?]+$/,''));
const report={total:rows.length,uniqueIds:new Set(rows.map(q=>q.id)).size,uniquePrompts:new Set(rows.map(q=>norm(q.prompt))).size,levels:counts(rows,q=>q.level),yds:counts(rows,q=>q.yds??'none'),positions:counts(rows,q=>q.correct),families:group(rows.filter(q=>q.family),q=>q.family).size,issues,duplicateIds,duplicatePrompts,editorialCandidates:{nearDuplicateGroups:near,largestFamilies:[...group(rows.filter(q=>q.family),q=>q.family)].sort((a,b)=>b[1].length-a[1].length).slice(0,15).map(([family,v])=>({family,count:v.length})),longBasic:rows.filter(q=>['A1','A2'].includes(q.level)&&q.prompt.length>400).map(q=>q.id)}};
console.log(JSON.stringify(report,null,2));
assert.equal(rows.length,10809,'Unexpected change in corpus size; update this baseline only for intentional content expansions');
assert.deepEqual(issues,[],'Invalid question records');
assert.deepEqual(duplicateIds,[],'Duplicate question IDs');
assert.deepEqual(duplicatePrompts,[],'Duplicate normalized prompts');
// Editorial flags are not proof of incorrect answers; they need human review.

// The authored addendum is checked separately from the licensed and legacy dictionaries.
// Same headword in different tiers or domains can be legitimate, but runtime keys must be unique.
const words=[...authoredWords,...academicWords,...wordFormWords,...lexicalRelationWords,...domainWords];
const wordKey=w=>[w.level,w.answer,w.yds??''].join('|');
const wordIssues=words.filter(w=>!(/^[A-Z]{2,24}$/.test(w.answer))||
 !levels.includes(w.level)||!['easy','medium','hard'].includes(w.yds)||
 !['Fiiller','İsimler','Sıfatlar','Zarflar'].includes(w.wordType)||
 !norm(w.category)||!norm(w.meaning)||/[\t\r\n]/.test(w.meaning));
const wordKeyCollisions=[...group(words,wordKey)].filter(([,v])=>v.length>1).map(([key])=>key);
console.log(JSON.stringify({authoredVocabulary:{total:words.length,uniqueHeadwords:new Set(words.map(w=>w.answer)).size,uniqueRuntimeKeys:new Set(words.map(wordKey)).size,invalid:wordIssues.map(w=>w.answer),duplicateKeys:wordKeyCollisions,multipleTierHeadwords:[...group(words,w=>w.answer)].filter(([,v])=>v.length>1).length}},null,2));
assert.deepEqual(wordIssues,[],'Invalid authored vocabulary records');
assert.deepEqual(wordKeyCollisions,[],'Duplicate authored vocabulary runtime keys');
assert.equal(words.length,1231,'Unexpected authored vocabulary count');
