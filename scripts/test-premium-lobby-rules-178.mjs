import fs from 'node:fs';
import assert from 'node:assert/strict';
import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';

const rules = fs.readFileSync('firebase/database.rules.json','utf8');
const env = await initializeTestEnvironment({
  projectId:'demo-wordquest',
  database:{host:'127.0.0.1',port:9000,rules}
});
try {
  const code='TESTROOM9', path='premiumRooms/'+code;
  await env.withSecurityRulesDisabled(async ctx => {
    await ctx.database().ref(path).set({
      hostUid:'owner', hostName:'Owner', guestUid:'friend', guestName:'Friend',
      phase:'lobby', mode:'quiz', rulesVersion:2, configRevision:3,
      hostAvatar:'robot', guestAvatar:'fox', level:'A1', topic:'General',
      size:10, seconds:30, boardId:'', quizSeed:317,
      turn:{uid:'owner',until:0}, createdAt:1700000000000,
      quotaDay:20000, quotaSlot:'0'
    });
  });
  const owner=env.authenticatedContext('owner').database();
  const friend=env.authenticatedContext('friend').database();
  const stranger=env.authenticatedContext('stranger').database();
  const anonymous=env.unauthenticatedContext().database();

  await assertFails(anonymous.ref(path+'/ready/owner').set(3));
  await assertFails(stranger.ref(path+'/ready/stranger').set(3));
  await assertFails(friend.ref(path+'/ready/owner').set(3));
  await assertFails(owner.ref(path+'/ready/owner').set(2));
  await assertFails(owner.ref(path+'/phase').set('playing'));
  await assertSucceeds(owner.ref(path+'/ready/owner').set(3));
  await assertFails(owner.ref(path+'/phase').set('playing'));
  await assertSucceeds(friend.ref(path+'/ready/friend').set(3));
  await assertFails(friend.ref(path+'/phase').set('playing'));
  await assertSucceeds(owner.ref(path+'/hostAvatar').set('piko'));
  await assertSucceeds(friend.ref(path+'/guestAvatar').set('astra'));
  await assertFails(owner.ref(path+'/guestAvatar').set('astra'));
  await assertFails(friend.ref(path+'/hostAvatar').set('foksi'));
  await assertFails(friend.ref(path+'/guestAvatar').set('unknown-id'));
  await assertSucceeds(friend.ref(path+'/ready/friend').remove());
  await assertFails(owner.ref(path+'/phase').set('playing'));
  await assertSucceeds(friend.ref(path+'/ready/friend').set(3));
  await assertSucceeds(owner.ref(path+'/phase').set('playing'));
  await assertFails(friend.ref(path+'/ready/friend').remove());
  console.log('PASS: both ready buttons, revision guard, role-specific 10-mascot avatar writes, host-only start, and lobby lockdown');
} finally {
  await env.cleanup();
}
