import test from 'node:test';import assert from 'node:assert/strict';
import {normalizeSettings,mixedPool,quiz,crossword,LEVELS} from './engine.mjs';
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

// The synthetic words share letters and are varied enough for valid crossings.
test('connected mixed crossword has exact count, valid crossings and deterministic positions',()=>{
 const words=['APPLE','PEAR','ORANGE','BANANA','MELON','GRAPE','PLUM','CHERRY','LEMON','LIME','MANGO','PAPAYA','APRICOT','PEACH','GUAVA','KIWI','OLIVE','FIG','BERRY','COCONUT','CARROT','POTATO','ONION','GARLIC','CELERY','SPINACH','PARSLEY','PEPPER','RADISH','CABBAGE','PUMPKIN','SQUASH','BEAN','CORN','LEEK','LETTUCE','TURNIP','GINGER','TOMATO','CUCUMBER','BROCCOLI','AVOCADO'];
 const rs=words.map((answer,i)=>({answer,meaning:'anlam '+i,level:['A1','A2','B1'][i%3],category:'food'}));
 const cfg={...s,mode:'team',size:20,topic:'food'};
 const b=crossword(rs,cfg,42);assert.equal(b.words.length,20);assert.deepEqual(b,crossword([...rs].reverse(),cfg,42));
 const cells=new Map(),links=b.words.map(()=>new Set());
 b.words.forEach(([answer,meaning,level,r,c,d],i)=>{
   assert.ok(cfg.levels.includes(level));assert.ok(r>=0&&c>=0);
   [...answer].forEach((ch,j)=>{
      const k=[r+(d==='D'?j:0),c+(d==='A'?j:0)].join(',');
      if(cells.has(k)){const old=cells.get(k);assert.equal(old.ch,ch);assert.notEqual(old.d,d);links[i].add(old.i);links[old.i].add(i)}
      else cells.set(k,{ch,d,i});
   })
 });
 const reached=new Set([0]),queue=[0];while(queue.length){for(const j of links[queue.shift()])if(!reached.has(j)){reached.add(j);queue.push(j)}}assert.equal(reached.size,20);
 for(const l of cfg.levels)assert.ok(b.words.some(w=>w[2]===l));
});

test("reject too few questions for selected levels",()=>assert.throws(()=>mixedPool(records,{...s,levels:LEVELS,size:5},12),/Seviye|seviye/));
