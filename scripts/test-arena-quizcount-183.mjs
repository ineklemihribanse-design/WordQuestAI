import fs from 'node:fs';
import assert from 'node:assert/strict';
import {initializeTestEnvironment,assertFails,assertSucceeds} from '@firebase/rules-unit-testing';

const env=await initializeTestEnvironment({
 projectId:'demo-wordquest-quizcount-183',
 database:{host:'127.0.0.1',port:9004,rules:fs.readFileSync('firebase/database.rules.json','utf8')}
});
const path='premiumRooms/COUNTQUIZ';
try{
 await env.withSecurityRulesDisabled(async ctx=>{
  await ctx.database().ref(path).set({
    hostUid:'host',hostName:'Host',guestUid:'guest',guestName:'Guest',
    mode:'quiz',level:'A1',topic:'General',size:12,seconds:30,
    boardId:'',quizSeed:200,rulesVersion:2,configRevision:1,
    ready:{host:1,guest:1},phase:'lobby',createdAt:Date.now(),
    quotaDay:20000,quotaSlot:'0',turn:{uid:'host',until:0}
  });
 });
 const host=env.authenticatedContext('host').database();
 const guest=env.authenticatedContext('guest').database();
 const outsider=env.authenticatedContext('outsider').database();
 const anonymous=env.unauthenticatedContext().database();
 const room=host.ref(path);
 // Merely selecting a value locally must not bypass the atomic settings revision.
 await assertFails(room.child('size').set(5));
 await assertFails(guest.ref(path).update({size:5,configRevision:2}));
 await assertFails(outsider.ref(path).update({size:5,configRevision:2}));
 await assertFails(anonymous.ref(path).update({size:5,configRevision:2}));
 await assertFails(room.update({size:7,configRevision:2}));
 await assertFails(room.update({size:5,configRevision:7}));
 await assertFails(room.update({size:30,configRevision:2}));
 // Every permitted multiple-choice question count must save successfully.
 let rev=1;
 for(const count of [5,10,12,15,20]){
   await assertSucceeds(room.update({size:count,quizSeed:count+1000,configRevision:++rev}));
   const snap=await assertSucceeds(room.get());
   assert.equal(snap.child('size').val(),count);
   assert.equal(snap.child('configRevision').val(),rev);
   await assertFails(room.child('phase').set('playing')); // old ready acknowledgements
 }
 await assertFails(room.update({size:20,configRevision:rev})); // unchanged revision
 await assertFails(room.update({mode:'duel',size:5,boardId:'board123',configRevision:rev+1}));
 await assertSucceeds(room.update({mode:'duel',size:20,boardId:'board123',configRevision:++rev}));
 await assertFails(room.update({mode:'quiz',size:30,configRevision:rev+1}));
 await assertSucceeds(room.update({mode:'quiz',size:5,boardId:'',configRevision:++rev}));
 // Two participants must acknowledge revised configuration again.
 await assertSucceeds(host.ref(path+'/ready/host').set(rev));
 await assertFails(room.child('phase').set('playing'));
 await assertSucceeds(guest.ref(path+'/ready/guest').set(rev));
 await assertSucceeds(room.child('phase').set('playing'));
 await assertFails(room.update({size:10,configRevision:rev+1})); // locked during match
 console.log('PASS: five-choice 5/10/12/15/20, host-only settings, atomic revision, invalid counts rejected, and ready re-confirmation.');
}finally{await env.cleanup();}
