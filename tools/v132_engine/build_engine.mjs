import fs from 'node:fs';
const dir=new URL('.',import.meta.url);
const source=fs.readFileSync(new URL('engine.mjs',dir),'utf8').replace(/^export /gm,'');
if(/<\/script/i.test(source))throw new Error('invalid embedded source');
const html='<!DOCTYPE html><html><head><meta charset="utf-8"><meta http-equiv="Content-Security-Policy" content="default-src 'none'; script-src 'unsafe-inline'; style-src 'none'; img-src 'none'; connect-src 'none'; frame-src 'none'; object-src 'none'; base-uri 'none'; form-action 'none'"></head><body><script>\n'+source+'\nwindow.wqGenerate=function(raw){try{const r=JSON.parse(raw);return JSON.stringify({ok:true,data:r.settings.mode==="quiz"?quiz(r.words,r.settings,r.seed):crossword(r.words,r.settings,r.seed)})}catch(e){return JSON.stringify({ok:false,error:String(e.message).slice(0,200)})}};\n</script></body></html>';
fs.writeFileSync(process.argv[2],html);
