// Production HTTP runtime. Deploy with Firebase Admin ADC (no service-account
// key in the repo). All matches are stored in private, lossless room records.
import {createServer} from 'node:http';
import {timingSafeEqual} from 'node:crypto';
import {readFile} from 'node:fs/promises';
import {initializeApp,applicationDefault} from 'firebase-admin/app';
import {getAuth} from 'firebase-admin/auth';
import {getDatabase} from 'firebase-admin/database';
import {createArenaHandler} from '../v132_engine/arena_gateway.mjs';
import {arenaDatabase,activeRoomIds} from '../v132_engine/room_storage.mjs';
import {sweepRoom} from '../v132_engine/presence.mjs';
import {createTrustedBank} from './trusted_bank.mjs';

const env=name=>{const v=process.env[name];if(!v)throw Error('Missing configuration: '+name);return v;};
const secret=name=>{const s=env(name);if(s.length<32)throw Error('Invalid secret configuration: '+name);return s;};
const projectId=env('FIREBASE_PROJECT_ID'),databaseURL=env('FIREBASE_DATABASE_URL');
const roomSecret=secret('ARENA_ROOM_SECRET'),schedulerSecret=secret('ARENA_SCHEDULER_SECRET');
if(roomSecret===schedulerSecret)throw Error('Secrets must be distinct');
const rawBank=JSON.parse(await readFile(env('ARENA_BANK_FILE'),'utf8'));
const bankBuilder=createTrustedBank(rawBank);
const app=initializeApp({credential:applicationDefault(),projectId,databaseURL});
const raw=getDatabase(app),database=arenaDatabase(raw);
const handler=createArenaHandler({auth:getAuth(app),database,bankBuilder,roomSecret,presenceEnabled:true,
 log:(event,name)=>console.error(event,name)});
const json=(res,status,value)=>{res.statusCode=status;res.setHeader('Content-Type','application/json');res.setHeader('Cache-Control','no-store');res.end(JSON.stringify(value));};
function permitted(value){
 const actual=Buffer.from(typeof value==='string'?value:'');
 const expected=Buffer.from(schedulerSecret);
 return actual.length===expected.length&&timingSafeEqual(actual,expected);
}
let sweeping=false;
async function sweep(){
 if(sweeping)return {busy:true};
 sweeping=true;
 try{
  const ids=await activeRoomIds(raw);let failed=0;
  // Bounded batches; each room is updated through its own transaction.
  for(let i=0;i<ids.length;i+=16){
   const results=await Promise.allSettled(ids.slice(i,i+16).map(id=>sweepRoom(database.ref('arenaV150Private/'+id),Date.now)));
   failed+=results.filter(x=>x.status==='rejected').length;
  }
  return {rooms:ids.length,failed};
 }finally{sweeping=false;}
}
const server=createServer(async(req,res)=>{
 try{
  const path=new URL(req.url,'http://localhost').pathname;
  if(path==='/healthz'&&req.method==='GET')return json(res,200,{ok:true,schema:150});
  if(path==='/internal/sweep'){
   if(req.method!=='POST'||!permitted(req.headers['x-arena-scheduler-secret']))return json(res,404,{ok:false});
   const result=await sweep();
   return json(res,result.failed?503:200,{ok:!result.failed,...result});
  }
  await handler(req,res);
 }catch(e){console.error('arena-runtime-error',e?.name??'Error');if(!res.headersSent)json(res,500,{ok:false,code:'server-error'});else res.end();}
});
server.requestTimeout=25000;server.headersTimeout=10000;
server.listen(Number(process.env.PORT??8080),'0.0.0.0');
