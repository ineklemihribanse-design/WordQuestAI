import fs from 'node:fs';
import assert from 'node:assert/strict';
import {initializeTestEnvironment,assertFails,assertSucceeds} from '@firebase/rules-unit-testing';
const env=await initializeTestEnvironment({projectId:'demo-wordquest',database:{host:'127.0.0.1',port:9000,rules:fs.readFileSync(process.env.WQ_HD_RULES,'utf8')}});
try {
 const owner='hd-owner',friend='hd-friend',stranger='hd-stranger';
 const as=id=>env.authenticatedContext(id,{firebase:{sign_in_provider:'google.com'}}).database();
 const db=as(owner), fb=as(friend), sb=as(stranger), guest=env.unauthenticatedContext().database();
 const profile='friendProfiles/'+owner, path='friendMascotHd/'+owner;
 const id='custom_'+'a'.repeat(32),preview=id+':'+'A'.repeat(300),hd=id+':'+'B'.repeat(100000);
 const data={preview,hd};
 await env.withSecurityRulesDisabled(async c=>{
   await c.database().ref('friends/'+owner+'/'+friend).set(true);
 });
 const summary={avatar:'piko',points:22,completed:3,lastActiveAt:Date.now(),avatarPreview:preview};
 await assertSucceeds(db.ref(profile).set(summary));
 await assertSucceeds(db.ref(path).set(data));
 await assertFails(fb.ref(path).get());
 await assertFails(sb.ref(path).get());
 await assertFails(guest.ref(path).get());
 await env.withSecurityRulesDisabled(async c=>{await c.database().ref('friends/'+friend+'/'+owner).set(true);});
 assert.equal((await assertSucceeds(as(friend).ref(path).get())).val().hd,hd);
 await assertFails(as(friend).ref('friendMascotHd').get());
 await assertFails(as(friend).ref(path).set(data));
 await assertFails(sb.ref(path).remove());
 await assertFails(db.ref(path).set({...data,extra:'no'}));
 await assertFails(db.ref(path).set({preview,hd:id+':'+'B'.repeat(160101)}));
 await assertFails(db.ref(path).set({preview,hd:id+':'+'!'.repeat(300)}));
 await assertFails(db.ref(path).set({preview}));
 // Changing the preview or withdrawing consent invalidates the old HD node,
 // even if the client crashed before deleting that optional node.
 await assertSucceeds(db.ref(profile+'/avatarPreview').set(id+':'+'C'.repeat(300)));
 await assertFails(as(friend).ref(path).get());
 await assertSucceeds(db.ref(profile+'/avatarPreview').set(preview));
 await assertSucceeds(as(friend).ref(path).get());
 await assertSucceeds(db.ref(profile+'/avatarPreview').remove());
 await assertFails(as(friend).ref(path).get());
 await assertSucceeds(db.ref(profile+'/avatarPreview').set(preview));
 await env.withSecurityRulesDisabled(async c=>{await c.database().ref('friends/'+friend+'/'+owner).remove();});
 await assertFails(as(friend).ref(path).get());
 await assertSucceeds(db.ref(path).remove());
 console.log('PASS friend HD: owner write, mutual-friend read, denied stranger/list access, limits, preview binding, consent and friendship revocation');
}finally{await env.cleanup();}
