import test from 'node:test';
import assert from 'node:assert/strict';
import {Readable} from 'node:stream';
import {createArenaHandler} from './arena_gateway.mjs';
const settings={kind:'duel',mode:'classic',levels:['A1'],count:10};
const bank=['APPLE','HOUSE','RIVER','CHAIR','WATER','GREEN','TABLE','MOUSE','BREAD','PLANT'].map((answer,i)=>({id:'w'+i,answer,clue:'clue '+i,level:'A1',cells:Array.from({length:5},(_,j)=>i+','+j)}));
function fixture(enabled=true){
 const records=new Map();let time=1000,builds=0;
 const database={ref:path=>{
  const val=()=>records.get(path)??null,snap=()=>({val});
  return {get:async()=>snap(),transaction:async fn=>{const n=fn(val());if(n!==undefined)records.set(path,n);return {committed:n!==undefined,snapshot:snap()};}};
 }};
 const auth={verifyIdToken:async token=>({uid:token})};
 const handler=createArenaHandler({auth,database,clock:()=>++time,roomSecret:enabled?'private-room-secret-with-at-least-32-characters':null,
 bankBuilder:enabled?async()=>{builds++;return bank;}:null});
 async function req(method,url,uid,body){
  const r=Readable.from(body===undefined?[]:[JSON.stringify(body)]);r.method=method;r.url=url;r.headers=uid?{authorization:'Bearer '+uid}:{};
  let result;const s={setHeader(){},end(x){result=JSON.parse(x);}};await handler(r,s);return {status:s.statusCode,...result};
 }
 return {req,records,builds:()=>builds};
}
const root='/v150/lobbies';
test('create is authenticated and idempotent; server owns room code',async()=>{
 const f=fixture(),c={id:'create1',settings};
 assert.equal((await f.req('POST',root,null,c)).status,401);
 assert.equal((await f.req('POST',root,'a',{...c,host:'b'})).status,400);
 const a=await f.req('POST',root,'a',c),b=await f.req('POST',root,'a',c);
 assert.equal(a.status,200);assert.equal(a.id,b.id);assert.equal(b.committed,false);assert.equal(f.records.size,1);
 assert.equal((await f.req('GET',root+'/'+a.id,'b')).status,404);
});
test('real lobby route: join, ready, start and match command without exposing private answers',async()=>{
 const f=fixture(),created=await f.req('POST',root,'a',{id:'create1',settings}),url=root+'/'+created.id,action=url+'/commands';
 assert.equal((await f.req('POST',action,'a',{id:'start0',revision:0,type:'start'})).status,409);
 assert.equal((await f.req('POST',action,'b',{id:'join',revision:0,type:'join'})).status,200);
 assert.equal((await f.req('POST',action,'a',{id:'ready1',revision:1,type:'ready',configRevision:0,ready:true})).status,200);
 assert.equal((await f.req('POST',action,'b',{id:'ready2',revision:2,type:'ready',configRevision:0,ready:true})).status,200);
 assert.equal((await f.req('POST',action,'b',{id:'start',revision:3,type:'start'})).status,404);
 const started=await f.req('POST',action,'a',{id:'start',revision:3,type:'start'});
 assert.equal(started.status,200);assert.equal(started.view.phase,'playing');assert.equal(started.view.game.words[0].answer,undefined);
 const again=await f.req('POST',action,'a',{id:'start',revision:3,type:'start'});
 assert.equal(again.status,200);assert.equal(again.committed,false);assert.equal(f.builds(),1);
 const active=started.view.game.turn;
 const game=await f.req('POST','/v150/matches/'+created.id+'/commands',active,{id:'guess',revision:0,type:'submit',word:0,answer:'APPLE'});
 assert.equal(game.status,200);assert.equal(game.view.game.stats[active].correct,1);
});
test('no trusted bank means lobby endpoints fail closed',async()=>{
 const f=fixture(false);assert.equal((await f.req('POST',root,'a',{id:'create1',settings})).status,503);
});
test('client cannot provide its own question bank or seed',async()=>{
 const f=fixture();assert.equal((await f.req('POST',root,'a',{id:'create1',settings:{...settings,bank}})).status,400);
 const a=await f.req('POST',root,'a',{id:'create1',settings});
 assert.equal((await f.req('POST',root+'/'+a.id+'/commands','a',{id:'x',revision:0,type:'start',seed:'client'})).status,400);
});

test('invitee joins without private revision even if host already changed settings',async()=>{
 const f=fixture(),created=await f.req('POST',root,'a',{id:'create1',settings}),url=root+'/'+created.id,action=url+'/commands';
 assert.equal((await f.req('POST',action,'a',{id:'change',revision:0,type:'settings',settings:{...settings,hints:5}})).status,200);
 assert.equal((await f.req('GET',url,'b')).status,404);
 const joined=await f.req('POST',action,'b',{id:'join',type:'join'});
 assert.equal(joined.status,200);assert.equal(joined.view.settings.hints,5);assert.equal(joined.view.members.length,2);
 const retry=await f.req('POST',action,'b',{id:'join',type:'join'});
 assert.equal(retry.status,200);assert.equal(retry.committed,false);
});
