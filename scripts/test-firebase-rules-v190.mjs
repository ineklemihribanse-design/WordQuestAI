import assert from 'node:assert/strict';
import fs from 'node:fs';
import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';

const rules = fs.readFileSync('firebase/database.rules.json', 'utf8');
assert(!rules.includes('numChildren('), 'Realtime Database rules do not support numChildren');
const parsed = JSON.parse(rules).rules;
assert(parsed.friendProfiles.$uid.avatarPreview, 'The v185 approved friend mascot preview must remain');
assert(parsed.premiumRooms.$code.package6.settings, 'Arena Package 6 settings must remain');
const env = await initializeTestEnvironment({
  projectId: 'demo-wordquest-v190-rules',
  database: {host:'127.0.0.1',port:9000,rules}
});

const base = (mode, size, extras={}) => ({
  hostUid:'host', guestUid:'guest', phase:'playing',
  hostName:'Host', guestName:'Guest', createdAt:Date.now(),
  mode, level:'A1', topic:'travel', size, seconds:30,
  boardId:'board', quizSeed:1, quotaDay:1, quotaSlot:'0',
  turn:{uid:'host',until:0},
  package6:{settings:{levels:'A1',hints:2,retries:1}},
  ...extras
});
const seed = async (code, record) => env.withSecurityRulesDisabled(ctx =>
  ctx.database().ref('premiumRooms/'+code).set(record));
const host = env.authenticatedContext('host').database();
const guest = env.authenticatedContext('guest').database();
const outsider = env.authenticatedContext('outsider').database();
const anon = env.unauthenticatedContext().database();
const complete = n => Object.fromEntries(Array.from({length:n},(_,i)=>[String(i),'host']));
const answers = n => Object.fromEntries(Array.from({length:n},(_,i)=>[String(i),i%5]));

try {
  await seed('DUEL-EARLY',base('duel',20,{solved:{0:'host',19:'host'}}));
  await assertFails(host.ref('premiumRooms/DUEL-EARLY/phase').set('finished'));
  await assertFails(guest.ref('premiumRooms/DUEL-EARLY/phase').set('finished'));
  await assertFails(outsider.ref('premiumRooms/DUEL-EARLY/phase').set('finished'));
  await assertFails(anon.ref('premiumRooms/DUEL-EARLY/phase').set('finished'));
  await seed('DUEL-FULL',base('duel',20,{solved:complete(20)}));
  await assertSucceeds(host.ref('premiumRooms/DUEL-FULL/phase').set('finished'));
  await seed('TEAM-FULL',base('team',30,{solved:complete(30)}));
  await assertSucceeds(host.ref('premiumRooms/TEAM-FULL/phase').set('finished'));
  await seed('DUEL-CONSENT',base('duel',20,{package6:{
    settings:{levels:'A1',hints:2,retries:1},
    finishVotes:{host:true,guest:true}}}));
  await assertSucceeds(host.ref('premiumRooms/DUEL-CONSENT/phase').set('finished'));

  await seed('QUIZ-INCOMPLETE',base('quiz',5,{answers:{host:answers(5),guest:answers(4)}}));
  await assertFails(host.ref('premiumRooms/QUIZ-INCOMPLETE/phase').set('finished'));
  await seed('QUIZ-FULL',base('quiz',5,{answers:{host:answers(5),guest:answers(5)}}));
  await assertSucceeds(host.ref('premiumRooms/QUIZ-FULL/phase').set('finished'));

  await seed('HINT-QUOTA',base('duel',20));
  const claim = (n) => host.ref('premiumRooms/HINT-QUOTA/package6/hintClaims/host/'+n)
    .set({word:0,position:n});
  await assertSucceeds(claim(0));
  await assertSucceeds(claim(1));
  await assertFails(claim(2));
  await assertFails(claim(19));
  await assertFails(guest.ref('premiumRooms/HINT-QUOTA/package6/hintClaims/host/3')
    .set({word:0,position:3}));
  await assertFails(outsider.ref('premiumRooms/HINT-QUOTA/package6/hintClaims/outsider/0')
    .set({word:0,position:0}));
  await assertFails(claim(1)); // append-only
  await seed('HINT-ZERO',base('duel',20,{package6:{
    settings:{levels:'A1',hints:0,retries:1}}}));
  await assertFails(host.ref('premiumRooms/HINT-ZERO/package6/hintClaims/host/0')
    .set({word:0,position:0}));
  console.log('PASS: full duel/team completion, 5-question quiz, consensus exit');
  console.log('PASS: no premature finish, outsider or guest-forged phase changes');
  console.log('PASS: hint budget 0 or 2, append-only claims, owner-only restrictions');
  console.log('PASS: friend mascot preview preserved, Firebase rules compile');
} finally {
  await env.cleanup();
}
