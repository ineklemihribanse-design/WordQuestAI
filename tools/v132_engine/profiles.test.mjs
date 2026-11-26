import test from 'node:test';
import assert from 'node:assert/strict';
import {safeNickname,validatePortrait,profileMembers,readProfiles} from './profiles.mjs';
test('nickname never falls back to email',()=>{
 assert.equal(safeNickname('Kullanıcı 1'),'Kullanıcı 1');assert.equal(safeNickname('a@example.com'),'Oyuncu');
});
test('only actual lobby or non-removed match members are eligible',()=>{
 assert.deepEqual(profileMembers({phase:'lobby',members:[{uid:'a'}]}),['a']);
 assert.deepEqual(profileMembers({game:{},connection:{players:[{id:'a'},{id:'b',removed:true}]}}),['a']);
});
test('portrait rejects unrecognized avatars and malformed or oversized image',()=>{
 assert.deepEqual(validatePortrait({avatar:'piko'}),{avatar:'piko',preview:null});
 assert.throws(()=>validatePortrait({avatar:'piko',preview:'file'}));
 assert.throws(()=>validatePortrait({avatar:'custom_12345678',preview:'custom_12345678:AAAA'}));
 assert.throws(()=>validatePortrait({avatar:'custom_12345678',preview:'A'.repeat(180051)}));
});
test('only room participants are returned and version changes when consented image removed',async()=>{
 const values={'arenaV150Profiles/room':{a:{avatar:'astra',preview:'data'},outsider:{avatar:'piko'}},'users/a/nickname':'Alice'};
 const db={ref:p=>({get:async()=>({val:()=>values[p]??null})})};
 const first=await readProfiles(db,'room',['a']);assert.equal(first.profiles.outsider,undefined);
 values['arenaV150Profiles/room'].a={avatar:'piko',preview:null};
 const second=await readProfiles(db,'room',['a']);assert.notEqual(first.version,second.version);
});
