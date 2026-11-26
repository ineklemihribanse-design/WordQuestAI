import test from 'node:test';
import assert from 'node:assert/strict';
import {encodeRoom,decodeRoom,arenaDatabase,activeRoomIds} from './room_storage.mjs';
import {createLobby,applyLobby} from './lobby.mjs';
import {createMatch,matchCommand,matchView} from './match_service.mjs';
const settings={kind:'duel',mode:'classic',levels:['A1'],count:10,seconds:30};
const bank=['APPLE','HOUSE','RIVER','CHAIR','WATER','GREEN','TABLE','MOUSE','BREAD','PLANT'].map((answer,i)=>({id:'w'+i,answer,clue:'clue '+i,level:'A1',cells:Array.from({length:5},(_,j)=>i+','+j)}));
const lobby=()=>createLobby({host:'a',settings,now:1000});
const match=()=>createMatch({settings,bank,players:['a','b'],epochs:{a:1,b:1},seed:'private',now:1000});
test('codec retains nullable settings, empty arrays and maps',()=>{
 for(const s of [lobby(),match()])assert.deepEqual(decodeRoom(encodeRoom(s)),JSON.parse(JSON.stringify(s)));
 const m=decodeRoom(encodeRoom(match()));
 assert.equal(m.settings.topic,null);assert.deepEqual(m.game.attempted,[]);assert.deepEqual(m.receipts,{});
});
test('corrupt and non-versioned records fail closed',()=>{
 assert.throws(()=>decodeRoom({schema:150}));assert.throws(()=>decodeRoom({codec:1,active:true,payload:'{}'}));
 assert.throws(()=>decodeRoom({...encodeRoom(lobby()),active:false}));
});
test('transaction adapter preserves null and empty fields on storage round-trip',async()=>{
 let raw=null;
 const ref={get:async()=>({val:()=>raw}),transaction:async fn=>{const n=fn(raw);if(n!==undefined)raw=n;return {committed:n!==undefined,snapshot:{val:()=>raw}}}};
 const db=arenaDatabase({ref:()=>ref}),room=db.ref('arenaV150Private/room123');
 await room.transaction(()=>lobby());assert.equal(raw.codec,1);assert.equal(raw.active,true);
 await room.transaction(s=>applyLobby(s,'b',{id:'join',type:'join',revision:0},1001));
 const s=(await room.get()).val();assert.equal(s.members.length,2);assert.equal(s.settings.topic,null);
 await room.transaction(()=>match());
 await room.transaction(s=>matchCommand(s,s.game.turn,{id:'pass',revision:0,type:'pass',word:s.game.active},1001));
 assert.equal(matchView((await room.get()).val(),'a').schema,150);
 assert.equal(db.ref('users/a'),ref);
});
test('closed room is not active',()=>{
 const s=applyLobby(lobby(),'a',{id:'leave',type:'leave',revision:0},1001);assert.equal(encodeRoom(s).active,false);
});
