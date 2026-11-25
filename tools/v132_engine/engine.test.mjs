import test from 'node:test';import assert from 'node:assert/strict';
import {normalizeSettings,mixedPool,quiz,LEVELS} from './engine.mjs';
const records=LEVELS.flatMap(l=>Array.from({length:20},(_,i)=>({level:l,answer:l.toLowerCase()+'word'+i,meaning:l+' anlam '+i,topic:'all'})));
const s={mode:'quiz',levels:['B1','A1','A2'],size:15,seconds:30,hints:3,retries:1,topic:'all'};
test('all selected levels actually present, balanced and deterministic across devices',()=>{
 const p=mixedPool(records,s,123);
 assert.equal(p.length,15);assert.equal(new Set(p.map(x=>x.answer)).size,15);
 for(const l of s.levels)assert.equal(p.filter(x=>x.level===l).length,5);
 assert.deepEqual(p,mixedPool([...records].reverse(),s,123));
 assert.notDeepEqual(p,mixedPool(records,s,124));
});
test('five options and correct answer in selected pool',()=>{
 const qs=quiz(records,s,432);assert.equal(qs.length,15);
 for(const q of qs){assert.equal(new Set(q.options).size,5);assert.equal(q.options[q.correct],q.answer);assert.ok(s.levels.includes(q.level))}
});
test('does not silently reduce selected levels or accept invalid settings',()=>{
 assert.throws(()=>mixedPool(records.filter(w=>w.level!=='B1'),s,2),/B1/);
 assert.throws(()=>normalizeSettings({...s,levels:[]}));
 assert.throws(()=>normalizeSettings({...s,levels:['A9']}));
 assert.throws(()=>normalizeSettings({...s,hints:99}));
 assert.throws(()=>mixedPool(records.slice(0,3),{...s,levels:['A1']},2));
});
test('crossword pool uses all selected levels but is not yet a generated board',()=>{
 const p=mixedPool(records,{...s,mode:'team',size:30},8);
 assert.equal(p.length,30);for(const l of s.levels)assert.equal(p.filter(x=>x.level===l).length,10);
});
