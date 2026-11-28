import test from 'node:test';
import assert from 'node:assert/strict';
import {createTrustedBank} from './trusted_bank.mjs';
import {validateSettings} from '../v132_engine/match_service.mjs';
const words=['APPLE','HOUSE','RIVER','CHAIR','WATER','GREEN','TABLE','MOUSE','BREAD','PLANT'];
const board=words.map((answer,i)=>({id:'w'+i,answer,clue:'clue '+i,level:'A1',cells:Array.from({length:5},(_,j)=>i+','+j)}));
const quizzes=Array.from({length:5},(_,i)=>({id:'q'+i,prompt:'Prompt '+i,options:['A','B','C','D','E'],correct:0,level:'A1'}));
const manifest={schema:150,boards:[{id:'board1',words:board}],quizzes};
test('trusted bank supplies exact configured content',async()=>{
 const load=createTrustedBank(manifest);
 const cfg=validateSettings({kind:'duel',mode:'classic',levels:['A1'],count:10});
 assert.equal((await load(cfg,'secret')).length,10);
 await assert.rejects(()=>load({...cfg,levels:['B2']},'secret'));
 const quiz=validateSettings({kind:'quiz',mode:'classic',levels:['A1'],count:5});
 assert.equal((await load(quiz,'secret')).length,5);
});
test('bad geometry and malformed options are rejected on startup',()=>{
 const bad=structuredClone(manifest);bad.boards[0].words[0].cells[1]='99,99';assert.throws(()=>createTrustedBank(bad));
 const q=structuredClone(manifest);q.quizzes[0].options=['a'];assert.throws(()=>createTrustedBank(q));
});
test('production cannot start on an empty placeholder bank',()=>{
 assert.throws(()=>createTrustedBank({schema:150,quizzes:[],boards:[]}));
});
