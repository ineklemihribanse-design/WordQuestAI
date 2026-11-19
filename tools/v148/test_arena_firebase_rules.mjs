import fs from 'node:fs';
import assert from 'node:assert/strict';
import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';

const env = await initializeTestEnvironment({
  projectId: 'demo-wordquest-arena-v2',
  database: {host: '127.0.0.1', port: 9000, rules: fs.readFileSync('source/database.rules.json','utf8')}
});
const A = env.authenticatedContext('arena-a', {firebase: {sign_in_provider: 'google.com'}}).database();
const B = env.authenticatedContext('arena-b', {firebase: {sign_in_provider: 'google.com'}}).database();
const intruder = env.authenticatedContext('arena-x', {firebase: {sign_in_provider: 'google.com'}}).database();
const id = 'ABCDE2345';
const path = 'premiumRooms/'+id;
const ref = (db, sub = '') => db.ref(path + (sub ? '/'+sub : ''));

async function seed(update = {}) {
  await env.withSecurityRulesDisabled(async ctx => {
    await ctx.database().ref(path).set({
      hostUid:'arena-a',guestUid:'arena-b',hostName:'A',guestName:'B',
      rulesVersion:2, configRevision:1, hostAvatar:'robot',guestAvatar:'fox',
      phase:'playing',mode:'duel',boardId:'p1',level:'A1',topic:'All',
      size:20,seconds:30,quizSeed:77,createdAt:Date.now(),
      quotaDay:0,quotaSlot:'0',
      turn:{uid:'arena-a',until:0},
      ...update
    });
  });
}

try {
  await seed({phase:'lobby',guestAvatar:null,ready:{}});
  await assertSucceeds(ref(A, 'ready/arena-a').set(1));
  await assertSucceeds(ref(B, 'ready/arena-b').set(1));
  await assertFails(ref(intruder,'ready/arena-a').set(1));
  await assertFails(ref(A,'ready/arena-b').set(1));
  await assertSucceeds(ref(B,'guestAvatar').set('owl'));
  await assertFails(ref(A,'guestAvatar').set('robot'));
  await assertFails(ref(B,'hostAvatar').set('fox'));
  await assertFails(ref(B,'ready/arena-a').set(1));

  // Settings are changeable by the host even after a guest joins.
  // Old ready acknowledgements no longer match the new config revision.
  await assertSucceeds(ref(A).update({
    level:'A2', seconds:45, size:30, configRevision:2
  }));
  assert.equal((await ref(A,'configRevision').get()).val(),2);
  assert.equal((await ref(A,'ready/arena-a').get()).val(),1);
  await assertFails(ref(B,'level').set('C2'));
  await assertFails(ref(intruder,'topic').set('All'));
  await assertFails(ref(A,'ready/arena-a').set(1));
  await assertSucceeds(ref(A,'ready/arena-a').set(2));
  await assertSucceeds(ref(B,'ready/arena-b').set(2));
  await assertFails(ref(A,'configRevision').set(2));
  await assertSucceeds(ref(A,'phase').set('playing'));
  await seed({phase:'lobby',configRevision:3,
    ready:{'arena-a':2,'arena-b':2}});
  await assertFails(ref(A,'phase').set('playing'));
  await assertSucceeds(ref(A,'ready/arena-a').set(3));
  await assertFails(ref(A,'phase').set('playing'));
  await assertSucceeds(ref(B,'ready/arena-b').set(3));
  await assertSucceeds(ref(A,'phase').set('playing'));

  await seed();
  await assertSucceeds(ref(A, 'hintUsed/arena-a/0').set(true));
  await assertFails(ref(A, 'hintUsed/arena-a/0').set(false));
  await assertFails(ref(B, 'hintUsed/arena-b/0').set(true));
  await assertFails(ref(A, 'hintUsed/arena-a/1').set('notBoolean'));
  await assertFails(ref(intruder, 'hintUsed/arena-x/0').set(true));
  await assertFails(ref(A, 'replay/0/arena-a').set('pass'));

  await seed({unknown:{'0':{'arena-a':true,'arena-b':true}}});
  await assertFails(ref(A, 'solved/0').set('reveal'));
  await assertSucceeds(ref(A, 'replay/0/arena-a').set('pass'));
  await assertFails(ref(A, 'solved/0').set('reveal'));
  await env.withSecurityRulesDisabled(async ctx => {
    await ctx.database().ref(path+'/turn').set({uid:'arena-b',until:0});
  });
  await assertSucceeds(ref(B, 'replay/0/arena-b').set('wrong'));
  await assertSucceeds(ref(A, 'solved/0').set('reveal'));
  assert.equal((await ref(A,'solved/0').get()).val(),'reveal');

  await seed();
  await assertFails(ref(intruder, 'solved/0').set('reveal'));
  console.log('Arena v2 RTDB: revision-ready, host settings edit, avatar, hint, replay and deferred reveal checks passed.');
} finally {
  await env.cleanup();
}
