// Transport-independent HTTP handler. The hosting runtime supplies Firebase Admin
// auth/database instances. No Admin credentials are ever embedded in Android.
// Only verified participants can read their view or submit a command.
import {matchView,transactMatch,matchCommand} from './match_service.mjs';
const matchId=x=>typeof x==='string'&&/^[A-Za-z0-9_-]{6,64}$/.test(x)&&!['constructor','__proto__','prototype'].includes(x);
const allowed=new Set(['submit','answer','pass','skip','choose','hint','eliminate','half','time']);
const reply=(res,status,body)=>{res.setHeader('Cache-Control','no-store');res.setHeader('Content-Type','application/json; charset=utf-8');res.statusCode=status;res.end(JSON.stringify(body));};
const problem=(res,status,code)=>reply(res,status,{ok:false,code});
async function readBody(req){
 let bytes=0,parts=[];
 for await(const chunk of req){const b=Buffer.isBuffer(chunk)?chunk:Buffer.from(chunk);bytes+=b.length;if(bytes>8192)throw Error('body-too-large');parts.push(b);}
 const data=JSON.parse(Buffer.concat(parts).toString('utf8'));
 if(!data||typeof data!=='object'||Array.isArray(data))throw Error('invalid-body');return data;
}
function command(data){
 if(typeof data.id!=='string'||! /^[A-Za-z0-9_-]{1,100}$/.test(data.id)||!Number.isInteger(data.revision)||data.revision<0||!allowed.has(data.type))throw Error('invalid-command');
 // Never trust actor, time, points, answer keys, or raw state supplied by caller.
 if(Object.keys(data).some(k=>!['id','revision','type','word','index','item','answer','choice'].includes(k)))throw Error('unexpected-field');
 const out={id:data.id,revision:data.revision,type:data.type};
 for(const key of ['word','index','item','choice'])if(data[key]!==undefined){
  if(!Number.isInteger(data[key])||data[key]<0||data[key]>69)throw Error('invalid-index');out[key]=data[key];
 }
 if(data.answer!==undefined){
  if(typeof data.answer!=='string'||data.answer.length>80)throw Error('invalid-answer');
  out.answer=data.answer;
 }
 return out;
}
export function createArenaHandler({auth,database,clock=Date.now,log=()=>{}}){
 if(!auth||typeof auth.verifyIdToken!=='function'||!database||typeof database.ref!=='function')throw Error('Missing trusted dependencies');
 return async function handle(req,res){
  const path=new URL(req.url,'http://localhost').pathname;
  const match=/^\/v150\/matches\/([A-Za-z0-9_-]+)(\/commands)?$/.exec(path);
  if(!match||!matchId(match[1]))return problem(res,404,'not-found');
  const write=Boolean(match[2]);
  if(req.method!==(write?'POST':'GET'))return problem(res,405,'method-not-allowed');
  const bearer=/^Bearer ([^\s]+)$/.exec(req.headers.authorization??'');
  if(!bearer)return problem(res,401,'unauthenticated');
  let identity;
  try{identity=await auth.verifyIdToken(bearer[1],true);}catch{return problem(res,401,'unauthenticated');}
  if(typeof identity?.uid!=='string'||!identity.uid)return problem(res,401,'unauthenticated');
  const uid=identity.uid,ref=database.ref('arenaV150Private/'+match[1]);
  let cmd;
  if(write){try{cmd=command(await readBody(req));}catch(e){return problem(res,e.message==='body-too-large'?413:400,'invalid-command');}}
  try{
   const snap=await ref.get(),state=snap.val();
   if(!state)return problem(res,404,'not-found');
   try{matchView(state,uid);}catch{return problem(res,404,'not-found');}
   if(!write)return reply(res,200,{ok:true,view:matchView(state,uid)});
   // Fixed server arrival timestamp reused across Firebase transaction retries.
   const now=clock();
   try{matchCommand(state,uid,cmd,now);}catch{return problem(res,409,'command-rejected');}
   const result=await transactMatch(ref,uid,cmd,now);
   return reply(res,200,{ok:true,committed:result.committed,view:result.view});
  }catch(e){log('arena-request-failed',String(e?.name??'Error'));return problem(res,409,'refresh-required');}
 };
}
