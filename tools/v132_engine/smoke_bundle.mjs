import fs from 'node:fs';import vm from 'node:vm';import assert from 'node:assert/strict';
const html=fs.readFileSync(process.argv[2],'utf8');
const words=JSON.parse(fs.readFileSync(process.argv[3],'utf8'));
const sandbox={window:{}};vm.runInNewContext(html.slice(html.indexOf('<script>')+8,html.lastIndexOf('</script>')),sandbox,{timeout:1000});
const base={levels:['A1','A2','B1'],size:20,seconds:30,hints:3,retries:1,topic:'Tüm konular'};
for(const mode of ['team','quiz']){
 const result=JSON.parse(sandbox.window.wqGenerate(JSON.stringify({words,settings:{...base,mode},seed:2026})));
 assert.equal(result.ok,true,result.error);
 const data=mode==='quiz'?result.data:result.data.words;
 assert.equal(data.length,20);for(const level of base.levels)assert.ok(data.some(w=>(mode==='quiz'?w.level:w[2])===level));
}
console.log('PASS: packaged WebView entry point generates real mixed crossword and quiz');
