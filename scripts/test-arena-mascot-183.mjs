import fs from 'node:fs';
import assert from 'node:assert/strict';
import {
 initializeTestEnvironment, assertFails, assertSucceeds
} from '@firebase/rules-unit-testing';

const env=await initializeTestEnvironment({
 projectId:'demo-wordquest-maskot-v183',
 database:{host:'127.0.0.1',port:9005,
  rules:fs.readFileSync('firebase/database.rules.v183.json','utf8')}
});
const path='premiumRooms/MASCOT183';
const mine='custom_'+'1'.repeat(32);
const theirs='custom_'+'2'.repeat(32);
const good=id=>id+':'+('A'.repeat(700));
try{
 await env.withSecurityRulesDisabled(async ctx=>{
  await ctx.database().ref(path).set({
   hostUid:'host',hostName:'Host',guestUid:'guest',guestName:'Guest',
   hostAvatar:'piko',guestAvatar:'astra',
   mode:'quiz',level:'A1',topic:'General',size:10,seconds:30,
   boardId:'',quizSeed:123,rulesVersion:2,configRevision:1,
   ready:{host:1,guest:1},phase:'lobby',createdAt:Date.now(),
   quotaDay:20000,quotaSlot:'0',turn:{uid:'host',until:0}
  });
 });
 const host=env.authenticatedContext('host').database().ref(path);
 const guest=env.authenticatedContext('guest').database().ref(path);
 const stranger=env.authenticatedContext('stranger').database().ref(path);
 const anonymous=env.unauthenticatedContext().database().ref(path);
 // Built-in avatar remains accepted and may not send a personal image.
 await assertFails(host.child('hostMascotPreview').set(good(mine)));
 await assertSucceeds(host.child('hostAvatar').set(mine));
 await assertSucceeds(guest.child('guestAvatar').set(theirs));
 await assertFails(host.child('guestMascotPreview').set(good(theirs)));
 await assertFails(guest.child('hostMascotPreview').set(good(mine)));
 await assertFails(stranger.child('hostAvatar').set(theirs));
 await assertFails(stranger.child('hostMascotPreview').set(good(mine)));
 await assertFails(anonymous.child('hostMascotPreview').set(good(mine)));
 // Both authenticated participants can see the same bounded thumbnail.
 await assertSucceeds(host.child('hostMascotPreview').set(good(mine)));
 await assertSucceeds(guest.child('guestMascotPreview').set(good(theirs)));
 assert.equal((await assertSucceeds(guest.child('hostMascotPreview').get())).val(),good(mine));
 assert.equal((await assertSucceeds(host.child('guestMascotPreview').get())).val(),good(theirs));
 await assertFails(stranger.child('hostMascotPreview').get());
 await assertFails(anonymous.child('guestMascotPreview').get());
 await assertFails(host.child('hostMascotPreview').set('INVALID'));
 await assertFails(host.child('hostMascotPreview').set(good(mine)+'X'.repeat(18051)));
 await assertFails(host.child('hostAvatar').set('custom_bad/name'));
 // Revoking consent deletes only own payload; the rival remains intact.
 await assertSucceeds(host.child('hostMascotPreview').remove());
 assert.equal((await assertSucceeds(guest.child('hostMascotPreview').get())).val(),null);
 assert.equal((await assertSucceeds(host.child('guestMascotPreview').get())).val(),good(theirs));
 await env.withSecurityRulesDisabled(async ctx=>{
  await ctx.database().ref(path+'/phase').set('finished');
 });
 // No renewed preview upload after the match closes; deletion stays allowed.
 await assertFails(host.child('hostMascotPreview').set(good(mine)));
 await assertSucceeds(guest.child('guestMascotPreview').remove());
 console.log('PASS: opt-in per-room thumbnails, host/guest isolation, bounded values, revocation and finished-room lock');
}finally{await env.cleanup();}
