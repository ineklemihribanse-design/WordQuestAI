// Review candidates, not automatic verdicts on meaning or CEFR.
import fs from 'node:fs';
import {allLearningQuestions as questions} from './learning_mix.mjs';
const words=s=>String(s).normalize('NFKC').toLowerCase().match(/[\p{L}\p{N}]+/gu)??[];
const norm=s=>words(s).join(' ');
const similarity=(a,b)=>{const x=new Set(words(a)),y=new Set(words(b));return [...x].filter(w=>y.has(w)).length/Math.max(1,new Set([...x,...y]).size);};
const flags={punctuation:[],pronounCue:[],answerLengthCue:[],nearEquivalentOptions:[],sameGlossCue:[],weakReadingDistractors:[],blankWithoutSeparator:[],negativeStem:[],technicalTierReview:[]};
const types={},levels={};
for(const q of questions){
 const answer=q.options[q.correct],other=q.options.filter((_,i)=>i!==q.correct);
 const type=q.kind??q.topic;
 types[type]=(types[type]??0)+1;levels[q.level]=(levels[q.level]??0)+1;
 const row={id:q.id,level:q.level,type};
 if(/[ \t]+[.,!?;:](?=\s|$)/.test(q.prompt)||q.options.some(o=>/[ \t]+[.,!?;:](?=\s|$)/.test(o)))flags.punctuation.push(row);
 const pronoun=answer.match(/^(He|She)\b/i)?.[1]?.toLowerCase();
 if(pronoun&&other.some(o=>new RegExp('^'+(pronoun==='he'?'she':'he')+'\\b','i').test(o)))flags.pronounCue.push(row);
 const lens=other.map(o=>words(o).length).sort((a,b)=>a-b),len=words(answer).length;
 if(len>=10&&len>=2*lens[2]&&len>=Math.max(...lens)+3)flags.answerLengthCue.push(row);
 for(let i=0;i<q.options.length;i++)for(let j=i+1;j<q.options.length;j++){
  const a=q.options[i],b=q.options[j];
  if(norm(a)===norm(b)||words(a).length>=4&&words(b).length>=4&&similarity(a,b)>=0.86)flags.nearEquivalentOptions.push({...row,indices:[i,j],involvesAnswer:i===q.correct||j===q.correct});
 }
 if(other.some(o=>norm(o)===norm(answer)))flags.sameGlossCue.push(row);
 const passage=q.prompt.split(/\n\n/)[0];
 if(/reading|paragraph|restatement/i.test(type)&&words(passage).length>=35&&len>=5){
  const answerSim=similarity(answer,passage),distractorSim=other.map(o=>similarity(o,passage));
  if(answerSim>=0.12&&distractorSim.filter(x=>x<=0.025).length>=3)flags.weakReadingDistractors.push(row);
 }
 if(/____[ \t]+[A-Z][a-z]+\s/.test(q.prompt)&&/ParagraphCompletion|Dialogue/.test(type)&&!/[.!?]$/.test(answer))flags.blankWithoutSeparator.push(row);
 if(/\b(?:not supported|not mentioned|not belong|irrelevant|unjustified|goes beyond|except|cannot be inferred)\b/i.test(q.prompt))flags.negativeStem.push(row);
 if(['A1','A2','B1'].includes(q.level)&&/\b(?:heterogeneity|epistemic|endogeneity|counterfactual|falsifiability|heteroscedasticity|stoichiometry|epistemology)\b/i.test(q.prompt))flags.technicalTierReview.push(row);
}
const summary={total:questions.length,levels,types,counts:Object.fromEntries(Object.entries(flags).map(([k,v])=>[k,v.length])),examples:Object.fromEntries(Object.entries(flags).map(([k,v])=>[k,v.slice(0,8)])),limitation:'Heuristic flags require contextual review; a flag does not prove an error, and zero flags does not prove linguistic correctness.'};
if(process.argv[2])fs.writeFileSync(process.argv[2],JSON.stringify({...summary,flags},null,2)+'\n');
console.log('EDITORIAL_QUALITY',JSON.stringify(summary));
