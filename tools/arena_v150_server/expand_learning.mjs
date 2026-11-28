import {readFile,writeFile} from 'node:fs/promises';
import {appendLearningWords} from './learning_mix.mjs';
import {createHash} from 'node:crypto';
import {pathToFileURL} from 'node:url';
const base=new URL('../v175/content/',import.meta.url);
const rank=s=>createHash('sha256').update(s).digest('hex');
const types=new Set(['İsimler','Fiiller','Sıfatlar','Zarflar','Diğer','Belirteçler','Bağlaçlar','Zamirler','Edatlar']);
const boardTypes=new Set(['İsimler','Fiiller','Sıfatlar','Zarflar']);
export function parseLicensedLine(line,expectedLevel){
 const [level,category,wordType,answer,meaning,...extra]=line.split('\t');
 if(extra.length||level!==expectedLevel||!category?.trim()||!types.has(wordType)||
    !meaning?.trim()||!answer||answer.length>80||!/^[A-Z]+(?:[ '-][A-Z]+)*$/.test(answer))return null;
 const quizOnly=!/^[A-Z]{2,22}$/.test(answer)||!boardTypes.has(wordType);
 return {level,category,wordType,answer,meaning,...(quizOnly?{quizOnly:true}:{})};
}
export async function expandLearning(input){
 const licensed=[];
 for(const level of ['A1','A2','B1','B2','C1']){
  const lines=(await readFile(new URL('wordquest_en_tr_'+level+'.tsv',base),'utf8')).trim().split('\n');
  // Only an existing CEFR record suppresses a CEFR addition. An explicitly
  // tagged YDS sense is retained separately by appendLearningWords.
  const existing=new Set(input.filter(w=>w.level===level&&!w.yds).map(w=>String(w.answer).toUpperCase()));
  const rows=lines.map(l=>parseLicensedLine(l,level)).filter(w=>w&&!existing.has(w.answer));
  rows.sort((a,b)=>rank(a.answer).localeCompare(rank(b.answer)));
  licensed.push(...rows);
 }
 return appendLearningWords(input,licensed);
}
if(process.argv[1] && import.meta.url===pathToFileURL(process.argv[1]).href){
 if(!process.argv[2]||!process.argv[3])throw Error('usage: expand_learning.mjs input output-file');
 const raw=JSON.parse(await readFile(process.argv[2],'utf8'));
 const out=await expandLearning(raw);
 await writeFile(process.argv[3],JSON.stringify(out));
 console.log('Expanded vocabulary:',raw.length,'->',out.length);
}
