import {readFile,writeFile} from 'node:fs/promises';
import {appendLearningWords} from './learning_mix.mjs';
import {createHash} from 'node:crypto';
const base=new URL('../v175/content/',import.meta.url);
const rank=s=>createHash('sha256').update(s).digest('hex');
export async function expandLearning(input){
 const licensed=[];
 for(const level of ['A1','A2','B1','B2','C1']){
  const lines=(await readFile(new URL('wordquest_en_tr_'+level+'.tsv',base),'utf8')).trim().split('\n');
  const existing=new Set(input.filter(w=>w.level===level).map(w=>String(w.answer).toUpperCase()));
  const rows=lines.map(l=>l.split('\t')).filter(a=>a.length===5&&a[0]===level&&/^[A-Z]{2,22}$/.test(a[3])&&a[4].trim()&&!existing.has(a[3])&&['İsimler','Fiiller','Sıfatlar','Zarflar'].includes(a[2]));
  rows.sort((a,b)=>rank(a[3]).localeCompare(rank(b[3])));
  // Full compatible licensed list, deduplicated. CEFR attribution is preserved; do not
  // silently assign YDS labels to words from general CEFR lists.
  for(const [level,category,wordType,answer,meaning] of rows)licensed.push({level,category,wordType,answer,meaning});
 }
 return appendLearningWords(input,licensed);
}
if(process.argv[2]&&process.argv[3]){
 const raw=JSON.parse(await readFile(process.argv[2],'utf8'));
 const out=await expandLearning(raw);
 await writeFile(process.argv[3],JSON.stringify(out));
 console.log('Expanded vocabulary:',raw.length,'->',out.length);
}
