import fs from 'node:fs';
import assert from 'node:assert/strict';
import { initializeTestEnvironment, assertFails, assertSucceeds }
  from '@firebase/rules-unit-testing';

const rules = fs.readFileSync('firebase/database.rules.v185.json','utf8');
const env = await initializeTestEnvironment({
  projectId:'demo-wordquest-friends',
  database:{host:'127.0.0.1',port:9001,rules}
});
try {
  await env.withSecurityRulesDisabled(async ctx=>{
    await ctx.database().ref('friends').set({
      alice:{bob:true,charlie:true},
      bob:{alice:true},
      charlie:{}
    });
  });
  const alice=env.authenticatedContext('alice').database();
  const bob=env.authenticatedContext('bob').database();
  const stranger=env.authenticatedContext('visitor').database();
  const oneSided=env.authenticatedContext('charlie').database();
  const anon=env.unauthenticatedContext().database();
  const ref='friendProfiles/alice';
  const base={avatar:'piko',points:100,completed:2,lastActiveAt:Date.now()};
  const avatarId='custom_'+'a'.repeat(32);
  const preview=avatarId+':'+'A'.repeat(160);
  await assertSucceeds(alice.ref(ref).set({...base,avatarPreview:preview}));
  const own=await assertSucceeds(alice.ref(ref).get());
  assert.equal(own.child('avatarPreview').val(),preview);
  const friend=await assertSucceeds(bob.ref(ref).get());
  assert.equal(friend.child('avatarPreview').val(),preview);
  await assertFails(oneSided.ref(ref).get());
  await assertFails(stranger.ref(ref).get());
  await assertFails(anon.ref(ref).get());
  await assertFails(bob.ref(ref).update({avatarPreview:preview}));
  await assertFails(alice.ref(ref).update({avatarPreview:'not-a-valid-preview'}));
  await assertFails(alice.ref(ref).update({avatarPreview:avatarId+':'+'A'.repeat(18100)}));
  await assertFails(alice.ref(ref).update({email:'private@example.invalid'}));
  await assertSucceeds(alice.ref(ref).set(base));
  assert.equal((await bob.ref(ref).get()).child('avatarPreview').exists(),false);
  await env.withSecurityRulesDisabled(async ctx=>{
    await ctx.database().ref('friends/bob/alice').remove();
  });
  await assertFails(bob.ref(ref).get());
  await assertSucceeds(alice.ref(ref).remove());
  console.log('PASS 1.0.85: optional bounded preview, mutual-friend read, self-write only, revoke, remove');
} finally { await env.cleanup(); }
