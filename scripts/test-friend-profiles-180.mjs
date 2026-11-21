import fs from 'node:fs';
import assert from 'node:assert/strict';
import { initializeTestEnvironment, assertFails, assertSucceeds }
  from '@firebase/rules-unit-testing';

const rules=fs.readFileSync('firebase/database.rules.json','utf8');
const env=await initializeTestEnvironment({
  projectId:'demo-wordquest-friends',
  database:{host:'127.0.0.1',port:9001,rules}
});
try {
  await env.withSecurityRulesDisabled(async ctx=>{
    await ctx.database().ref('friends').set({
      alice:{bob:true, charlie:true},
      bob:{alice:true},
      charlie:{} // only one side: not a mutual friendship
    });
  });
  const alice=env.authenticatedContext('alice').database();
  const bob=env.authenticatedContext('bob').database();
  const charlie=env.authenticatedContext('charlie').database();
  const visitor=env.authenticatedContext('visitor').database();
  const anon=env.unauthenticatedContext().database();
  const base='friendProfiles/alice';
  const profile={avatar:'tera',points:120,completed:7,lastActiveAt:Date.now()};
  await assertSucceeds(alice.ref(base).set(profile));
  const mine=await assertSucceeds(alice.ref(base).get());
  assert.equal(mine.val().avatar,'tera');
  await assertSucceeds(bob.ref(base).get());
  await assertFails(charlie.ref(base).get());
  await assertFails(visitor.ref(base).get());
  await assertFails(anon.ref(base).get());
  await assertFails(bob.ref(base).set(profile));
  await assertFails(alice.ref(base).update({avatar:'fake-avatar'}));
  await assertFails(alice.ref(base).update({points:-1}));
  await assertFails(alice.ref(base).update({completed:1000001}));
  await assertFails(alice.ref(base).update({email:'private@example.invalid'}));
  await env.withSecurityRulesDisabled(async ctx=>{
    await ctx.database().ref('friends/bob/alice').remove();
  });
  await assertFails(bob.ref(base).get());
  await assertSucceeds(alice.ref(base).remove());
  console.log('PASS: self-write and self-read, mutual-friend-only reads, revoked-friend denial, field limits, privacy and deletion');
} finally {
  await env.cleanup();
}
