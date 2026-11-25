// Deterministic, portable web engine. No Android, network, DOM, or account access.
export const LEVELS=Object.freeze(["A1","A2","B1","B2","C1","C2"]);
export function normalizeSettings(input) {
  if(!input||!Array.isArray(input.levels)||input.levels.length<1||input.levels.length>6||
      input.levels.some(x=>!LEVELS.includes(x)))throw new Error("Seviye seçimi geçersiz");
  const levels=LEVELS.filter(x=>input.levels.includes(x));
  const mode=input.mode;
  if(!["duel","team","quiz"].includes(mode))throw new Error("Oyun türü geçersiz");
  const sizes=mode==="quiz"?[5,10,15,20,30,40,50]:[10,20,30,40,50,60,70];
  if(!sizes.includes(input.size))throw new Error("Sayı geçersiz");
  if(!Number.isInteger(input.seconds)||input.seconds<0||input.seconds>3600)throw new Error("Süre geçersiz");
  if(!Number.isInteger(input.hints)||input.hints<0||input.hints>20||!Number.isInteger(input.retries)||input.retries<0||input.retries>3)throw new Error("Hak geçersiz");
  if(typeof input.topic!=="string"||input.topic.length>75)throw new Error("Konu geçersiz");
  return Object.freeze({mode,levels:Object.freeze(levels),size:input.size,seconds:input.seconds,hints:input.hints,retries:input.retries,topic:input.topic});
}
export function seeded(seed) {
  let x=Number(seed)>>>0;if(!x)x=0x9e3779b9;
  return ()=>{x^=x<<13;x^=x>>>17;x^=x<<5;return (x>>>0)/4294967296};
}
function shuffle(xs,rng){const a=[...xs];for(let i=a.length-1;i>0;i--){const j=Math.floor(rng()*(i+1));[a[i],a[j]]=[a[j],a[i]]}return a}
const key=x=>String(x).trim().toLowerCase();
export function mixedPool(records,input,seed) {
  const s=normalizeSettings(input), rng=seeded(seed);
  const groups=new Map(s.levels.map(l=>[l,[]]));
  const seen=new Set();
  const sorted=[...records].filter(w=>w&&typeof w.answer==="string"&&typeof w.meaning==="string"&&w.answer.trim()&&w.meaning.trim()&&groups.has(w.level)&&(s.topic==="all"||w.topic===s.topic)).sort((a,b)=>{
    const x=JSON.stringify([a.level,key(a.answer),a.meaning,a.topic]),y=JSON.stringify([b.level,key(b.answer),b.meaning,b.topic]);return x<y?-1:x>y?1:0;
  });
  for(const w of sorted){const k=key(w.answer);if(!seen.has(k)){seen.add(k);groups.get(w.level).push({...w,answer:w.answer.trim()})}}
  for(const level of s.levels){if(!groups.get(level).length)throw new Error("Seçilen seviyede uygun kelime yok: "+level);groups.set(level,shuffle(groups.get(level),rng))}
  const result=[], order=shuffle(s.levels,rng);
  while(result.length<s.size){
    let advanced=false;
    for(const l of order){if(result.length===s.size)break;const w=groups.get(l).pop();if(w){result.push(w);advanced=true}}
    if(!advanced)throw new Error("Seçilen havuzda yeterli farklı kelime yok");
  }
  return result;
}
export function quiz(records,input,seed) {
  const s=normalizeSettings(input);if(s.mode!=="quiz")throw new Error("Quiz modu gerekli");
  const targets=mixedPool(records,s,seed),rng=seeded((Number(seed)^0x4d3c2b1a)>>>0);
  const answers=[...new Set(records.filter(w=>w&&s.levels.includes(w.level)&&(s.topic==="all"||w.topic===s.topic)&&typeof w.answer==="string"&&w.answer.trim()).map(w=>w.answer.trim()))].sort();
  return targets.map(t=>{
    const distractors=shuffle(answers.filter(a=>key(a)!==key(t.answer)),rng).filter((a,i,arr)=>arr.findIndex(b=>key(b)===key(a))===i).slice(0,4);
    if(distractors.length!==4)throw new Error("Beş seçenek için yeterli farklı kelime yok");
    const options=shuffle([t.answer,...distractors],rng);
    return {prompt:t.meaning,options,correct:options.indexOf(t.answer),answer:t.answer,level:t.level};
  });
}
