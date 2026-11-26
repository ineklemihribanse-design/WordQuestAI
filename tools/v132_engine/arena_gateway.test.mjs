import test from 'node:test';
import assert from 'node:assert/strict';
import {Readable} from 'node:stream';
import {createMatch} from './match_service.mjs';
import {createArenaHandler} from './arena_gateway.mjs';
const words=['APPLE','HOUSE','RIVER','CHAIR','WATER','GREEN','TABLE','MOUSE','BREAD','PLANT'];
const bank=words.map((answer,i)=>({id:'w'+i,answer,clue:'clue '+i,level:'A1',cells:Array.from({length:5},(_,j)=>i+','+j)}));
function fixture(){
 let value=createMatch({settings:{kind:'duel',mode:'classic',levels:['A1'],count:10,hints:2},bank,players:['a','b'],epochs:{a:1,b:1},seed:'private',now:1000});
 const snapshot=()=>({val:()=>value});
 const ref={get:async()=>snapshot(),transaction:async f=>{const next=f(value);if(next!==undefined)value=next;return {committed:next!==undefined,snapshot:snapshot()};}};
 const handler=createArenaHandler({auth:{verifyIdToken:async(token,revoked)=>{assert.equal(revoked,true);if(token==='invalid')throw Error('bad token');return {uid:token};}},database:{ref:path=>{assert.equal(path,'arenaV150Private/room123');return ref;}},clock:()=>1001});
 async function request(method,path,token,body){
  const req=Readable.from(body===undefined?[]:[JSON.stringify(body)]);req.method=method;req.url=path;req.headers=token?{authorization:'Bearer '+token}:{};
  const headers={};let response;const res={setHeader:(k,v)=>{headers[k]=v;},end:s=>{response=JSON.parse(s);}};
  await handler(req,res);return {status:res.statusCode,body:response,headers};
 }
 return {request,state:()=>value};
}
const url='/v150/matches/room123';
test('authentication and outsider checks hide private state',async()=>{
 const f=fixture();assert.equal((await f.request('GET',url,null)).status,401);assert.equal((await f.request('GET',url,'invalid')).status,401);
 assert.equal((await f.request('GET',url,'outsider')).status,404);
 const own=await f.request('GET',url,'a');assert.equal(own.status,200);assert.equal(own.body.view.game.words[0].answer,undefined);assert.equal(own.headers['Cache-Control'],'no-store');
});
test('server controls identity and time, prevents unauthorized command fields',async()=>{
 const f=fixture(),p=f.state().game.turn,c={id:'x',revision:0,type:'submit',word:0,answer:f.state().game.words[0].answer};
 assert.equal((await f.request('POST',url+'/commands',p,{...c,actor:p})).status,400);
 assert.equal((await f.request('POST',url+'/commands',p,{...c,now:9999})).status,400);
 assert.equal((await f.request('POST',url+'/commands','outsider',c)).status,404);
 assert.equal((await f.request('POST',url+'/commands',p,{...c,type:'leave'})).status,400);
 const a=await f.request('POST',url+'/commands',p,c);assert.equal(a.status,200);assert.equal(a.body.committed,true);
 const b=await f.request('POST',url+'/commands',p,c);assert.equal(b.status,200);assert.equal(b.body.committed,false);
 assert.equal(f.state().game.stats[p].correct,1);
});
test('bad method, unknown path, stale revision and oversized body rejected',async()=>{
 const f=fixture(),p=f.state().game.turn;assert.equal((await f.request('POST',url,p)).status,405);
 assert.equal((await f.request('GET','/v150/matches/../bad',p)).status,404);
 assert.equal((await f.request('POST',url+'/commands',p,{id:'x',revision:99,type:'pass',word:0})).status,409);
 assert.equal((await f.request('POST',url+'/commands',p,{id:'x',revision:0,type:'submit',word:0,answer:'x'.repeat(9000)})).status,413);
});
