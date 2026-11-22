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
 // A participant must not submit a thumbnail for a different avatar ID.
 await assertFails(host.child('hostMascotPreview').set(good(theirs)));
 await assertFails(guest.child('guestMascotPreview').set(good(mine)));
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
 // Once playing, only the owning participant may change their selected maskot.
 await env.withSecurityRulesDisabled(async ctx=>{
  await ctx.database().ref(path+'/phase').set('playing');
 });
 const alternate='custom_'+'3'.repeat(32);
 await assertFails(guest.child('hostAvatar').set(alternate));
 await assertFails(host.child('guestAvatar').set(alternate));
 await assertSucceeds(host.child('hostAvatar').set(alternate));
 await assertFails(host.child('hostMascotPreview').set(good(mine)));
 await assertSucceeds(host.child('hostMascotPreview').set(good(alternate)));
 await assertFails(stranger.child('hostAvatar').set(mine));
 await assertSucceeds(host.child('hostAvatar').set(mine));
 await assertSucceeds(host.child('hostMascotPreview').set(good(mine)));
 // Revoking consent deletes only own payload; the rival remains intact.
 await assertSucceeds(host.child('hostMascotPreview').remove());
 assert.equal((await assertSucceeds(guest.child('hostMascotPreview').get())).val(),null);
 assert.equal((await assertSucceeds(host.child('guestMascotPreview').get())).val(),good(theirs));
 await env.withSecurityRulesDisabled(async ctx=>{
  await ctx.database().ref(path+'/phase').set('finished');
 });
 // No renewed preview upload after the match closes; deletion stays allowed.
 await assertFails(host.child('hostMascotPreview').set(good(mine)));
 // Result screens can still read the opponent's image while the room exists.
 assert.equal((await assertSucceeds(host.child('guestMascotPreview').get())).val(),good(theirs));
 await assertSucceeds(guest.child('guestMascotPreview').remove());
 // Repeat cross-player preview access for every backend Arena match mode.
 for (const mode of ['duel','team','quiz']) {
  const roomPath='premiumRooms/MASCOT183_'+mode;
  await env.withSecurityRulesDisabled(async ctx=>{
   await ctx.database().ref(roomPath).set({
    hostUid:'host',guestUid:'guest',hostAvatar:mine,guestAvatar:theirs,
    phase:'playing',mode,createdAt:Date.now(),size:mode==='quiz'?10:20,
    hostName:'Host',guestName:'Guest'
   });
  });
  const h=env.authenticatedContext('host').database().ref(roomPath);
  const g=env.authenticatedContext('guest').database().ref(roomPath);
  const x=env.authenticatedContext('stranger').database().ref(roomPath);
  await assertSucceeds(h.child('hostMascotPreview').set(good(mine)));
  await assertSucceeds(g.child('guestMascotPreview').set(good(theirs)));
  assert.equal((await assertSucceeds(g.child('hostMascotPreview').get())).val(),good(mine));
  assert.equal((await assertSucceeds(h.child('guestMascotPreview').get())).val(),good(theirs));
  await assertFails(x.child('hostMascotPreview').get());
 }
 console.log('PASS: all three Arena backend modes, participant isolation, avatar-ID binding, revocation and result-room visibility');
}finally{await env.cleanup();}
