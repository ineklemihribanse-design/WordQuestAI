import fs from 'node:fs';
import assert from 'node:assert/strict';
import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';

const rules=fs.readFileSync('database.rules.json','utf8');
const env=await initializeTestEnvironment({
 projectId:'demo-wordquest',
 database:{host:'127.0.0.1',port:9000,rules}
});
try {
 const day=Math.floor(Date.now()/86400000);
 const path='roomQuota/google-user/'+day;
 const first=env.authenticatedContext('google-user',{firebase:{sign_in_provider:'google.com'}}).database();
 const secondDevice=env.authenticatedContext('google-user',{firebase:{sign_in_provider:'google.com'}}).database();
 const another=env.authenticatedContext('guest-user',{firebase:{sign_in_provider:'google.com'}}).database();
 const anonymous=env.authenticatedContext('anon-user',{firebase:{sign_in_provider:'anonymous'}}).database();
 const code='ABCD23456';
 await assertFails(anonymous.ref('roomQuota/anon-user/'+day+'/0').set('ANON23456'));
 await assertSucceeds(first.ref(path+'/0').set(code));
 await assertSucceeds(secondDevice.ref(path+'/0').get());
 await assertFails(secondDevice.ref(path+'/0').set('ZZZZ23456'));
 await assertFails(another.ref(path).get());
 await assertSucceeds(first.ref('rooms/'+code).set({
   hostUid:'google-user',mode:'words',gameType:'fast',phase:'lobby',
   createdAt:Date.now(),utcDay:new Date().toISOString().slice(0,10),
   quotaDay:day,quotaSlot:'0'
 }));
 await assertSucceeds(another.ref('rooms/'+code+'/guestUid').set('guest-user'));
 await assertSucceeds(another.ref('rooms/'+code).get());
 await assertFails(another.ref('rooms/'+code+'/phase').set('playing'));
 await assertSucceeds(first.ref('rooms/'+code+'/phase').set('playing'));
 for(let i=1;i<10;i++)await assertSucceeds(first.ref(path+'/'+i).set('ROOM'+String(i).padStart(5,'0')));
 await assertFails(secondDevice.ref(path+'/10').set('ROOM00010'));
 await assertFails(first.ref('rooms/NONQUOTA9').set({
   hostUid:'google-user',mode:'words',phase:'lobby',createdAt:Date.now(),
   quotaDay:day,quotaSlot:'0',utcDay:new Date().toISOString().slice(0,10)
 }));
 console.log('RTDB rules: same-account 10-slot quota, Google-only quota, join, host-only phase verified');
} finally {await env.cleanup();}
