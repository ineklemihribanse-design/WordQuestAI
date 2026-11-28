import fs from 'node:fs';import assert from 'node:assert/strict';import {crossword,quiz} from './engine.mjs';
const words=JSON.parse(fs.readFileSync(process.argv[2],'utf8'));
const s={mode:'team',levels:['A1','A2','B1'],size:20,seconds:30,hints:3,retries:1,topic:'Tüm konular'};
for(const levels of [['A1','A2','B1'],['B1','B2','C1','C2'],['A1','A2','B1','B2','C1','C2']]){
 const b=crossword(words,{...s,levels},2026);assert.equal(b.words.length,20);for(const l of levels)assert.ok(b.words.some(w=>w[2]===l),l);
 console.log('PASS real vocabulary crossword',levels.join(','),b.words.length);
 const q=quiz(words,{...s,mode:'quiz',levels,size:20},42);assert.equal(q.length,20);
}
