import fs from 'node:fs';import vm from 'node:vm';import assert from 'node:assert/strict';
const html=fs.readFileSync(process.argv[2],'utf8');
const words=JSON.parse(fs.readFileSync(process.argv[3],'utf8'));
const sandbox={window:{},atob,Uint8Array,TextDecoder};
vm.runInNewContext(html.slice(html.indexOf('<script>')+8,html.lastIndexOf('</script>')),sandbox,{timeout:1000});
const settings={mode:'duel',levels:['A1','A2','B1','B2','C1','C2'],size:50,seconds:30,hints:3,retries:1,topic:'Tüm konular'};
for(const mode of ['duel','team','quiz']){
 const request=JSON.stringify({words,settings:{...settings,mode,size:mode==='quiz'?20:50},seed:-2147483648});
 const raw=Buffer.from(request,'utf8');assert.ok(raw.length<=8*1024*1024);
 const encoded=raw.toString('base64'),chunks=encoded.match(/.{1,24576}/g);
 assert.ok(chunks.length>1,'actual large request should require multiple chunks');
 assert.equal(vm.runInNewContext("(function(){if(typeof window.wqGenerate!=='function')return false;window.__wqChunks=[];return true;})()",sandbox),true);
 for(const [index,chunk] of chunks.entries())assert.equal(vm.runInNewContext("window.__wqChunks.push("+JSON.stringify(chunk)+")",sandbox),index+1);
 const call="(function(){var encoded=window.__wqChunks.join('');delete window.__wqChunks;var binary=atob(encoded);var bytes=new Uint8Array(binary.length);for(var j=0;j<binary.length;j++)bytes[j]=binary.charCodeAt(j);return window.wqGenerate(new TextDecoder('utf-8',{fatal:true}).decode(bytes));})()";
 const response=vm.runInNewContext(call,sandbox,{timeout:45000});const r=JSON.parse(response);
 assert.equal(r.ok,true,r.error);assert.equal(response,sandbox.window.wqGenerate(request));
 assert.ok(Buffer.byteLength(response)<=48*1024);assert.ok(!sandbox.window.__wqChunks);
 const data=mode==='quiz'?r.data:r.data.words;assert.equal(data.length,mode==='quiz'?20:50);
 assert.deepEqual([...new Set(data.map(w=>mode==='quiz'?w.level:w[2]))].sort(),[...settings.levels].sort());
 console.log('PASS chunked actual vocabulary',mode,raw.length,'bytes',chunks.length,'chunks',Buffer.byteLength(response),'response bytes');
}
