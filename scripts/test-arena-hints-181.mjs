import assert from 'node:assert/strict';
import fs from 'node:fs';
import {initializeTestEnvironment,assertSucceeds,assertFails}
  from '@firebase/rules-unit-testing';

const rules=fs.readFileSync('firebase/database.rules.json','utf8');
const env=await initializeTestEnvironment({
 projectId:'demo-wordquest-hints-181',
 database:{host:'127.0.0.1',port:9002,rules}
});
const path='premiumRooms/ABC-HINT';
try{
  await env.withSecurityRulesDisabled(async ctx=>{
    await ctx.database().ref(path).set({
      hostUid:'h',guestUid:'g',hostName:'Host',guestName:'Guest',
      phase:'playing',mode:'duel',level:'A1',topic:'travel',
      size:20,seconds:30,boardId:'test',quizSeed:0,
      turn:{uid:'h',until:0},createdAt:Date.now(),
      quotaDay:1,quotaSlot:'1'
    });
  });
  const h=env.authenticatedContext('h').database().ref(path);
  const g=env.authenticatedContext('g').database().ref(path);
  const x=env.authenticatedContext('outsider').database().ref(path);
  const n=env.unauthenticatedContext().database().ref(path);
  await assertSucceeds(h.child('hintUsed/h/2').set(true));
  await assertFails(h.child('hintUsed/h/2').set(true)); // single-use
  await assertFails(g.child('hintUsed/h/3').set(true)); // cannot spend rival's hint
  await assertFails(g.child('hintUsed/g/3').set(true)); // not guest's turn
  await assertFails(x.child('hintUsed/outsider/3').set(true));
  await assertFails(n.child('hintUsed/h/3').set(true));
  await assertFails(h.child('hintUsed/h/20').set(true));
  await assertFails(h.child('hintUsed/h/3').set(false));
  await assertSucceeds(h.child('hintUsed/h/3').set(true));
  await assertSucceeds(h.child('hintUsed/h/4').set(true));
  await env.withSecurityRulesDisabled(async ctx=>{
    await ctx.database().ref(path+'/turn').set({uid:'g',until:0});
  });
  await assertSucceeds(g.child('hintUsed/g/1').set(true));
  await assertFails(h.child('hintUsed/h/6').set(true));
  await env.withSecurityRulesDisabled(async ctx=>{
    await ctx.database().ref(path+'/phase').set('finished');
  });
  await assertFails(g.child('hintUsed/g/2').set(true));
  console.log('PASS: real hint writes, one per word, owner-only, active turn, valid indices and phase.');
}finally{
  await env.cleanup();
}
