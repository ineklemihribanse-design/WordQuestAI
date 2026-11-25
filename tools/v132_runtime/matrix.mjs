import fs from 'node:fs';
import vm from 'node:vm';
import assert from 'node:assert/strict';
const html=fs.readFileSync(process.argv[2],'utf8');
const words=JSON.parse(fs.readFileSync(process.argv[3],'utf8'));
const sandbox={window:{}};vm.runInNewContext(html.slice(html.indexOf('<script>')+8,html.lastIndexOf('</script>')),sandbox,{timeout:1000});
const run=(settings,seed)=>JSON.parse(sandbox.window.wqGenerate(JSON.stringify({words,settings,seed})));
const levels=['A1','A2','B1','B2','C1','C2'];
const base={seconds:30,hints:3,retries:1,topic:'Tüm konular'};
let pass=0;const failures=[];
function verify(s,seed){
 const r=run(s,seed);if(!r.ok)throw Error(r.error);
 const data=s.mode==='quiz'?r.data:r.data.words;assert.equal(data.length,s.size);
 assert.deepEqual([...new Set(data.map(w=>s.mode==='quiz'?w.level:w[2]))].sort(),[...s.levels].sort());
 if(s.mode==='quiz'){
   assert.equal(new Set(data.map(w=>w.answer.toLowerCase())).size,s.size);
   for(const q of data){assert.equal(q.options.length,5);assert.equal(new Set(q.options.map(x=>x.toLowerCase())).size,5);assert.equal(q.options[q.correct],q.answer);}
 }else{
   const cells=new Map(),seen=new Set(),links=data.map(()=>new Set());
   data.forEach(([answer,meaning,l,r,c,d],n)=>{
      assert.ok(!seen.has(answer));seen.add(answer);assert.ok(/^[A-Z]{2,24}$/.test(answer));
      assert.ok(['A','D'].includes(d));
      [...answer].forEach((ch,k)=>{
         const y=r+(d==='D'?k:0),x=c+(d==='A'?k:0);
         assert.ok(y>=0&&x>=0&&y<80&&x<80);
         const key=y+','+x;
         if(cells.has(key)){const v=cells.get(key);assert.equal(v.ch,ch);assert.notEqual(v.d,d);links[v.n].add(n);links[n].add(v.n);}
         else cells.set(key,{ch,d,n});
      })
   });
   const reached=new Set([0]),queue=[0];while(queue.length)for(const n of links[queue.shift()])if(!reached.has(n)){reached.add(n);queue.push(n)}
   assert.equal(reached.size,s.size);
 }
 pass++;
}
for(const pool of [...levels.map(l=>[l]),['A1','A2','B1'],['B1','B2','C1','C2'],levels]){
 for(const mode of ['duel','team','quiz'])for(const size of mode==='quiz'?[5,10,12,15,20]:[20,30,40,50]){
   if(size<pool.length)continue;
   for(const seed of [42,2026]){
     const s={...base,levels:pool,mode,size};
     try{verify(s,seed)}catch(e){failures.push({mode,pool,size,seed,error:e.message});console.log('FAIL',JSON.stringify(failures.at(-1)))}
   }
 }
}
console.log(JSON.stringify({passed:pass,failed:failures.length}));
assert.equal(failures.length,0,'Not all all-topic mode/level/count combinations generated valid rounds');
