#!/usr/bin/env python3
"""Generate a fully self-contained, offline HTML player for the five dialect comparison words."""
from pathlib import Path
import base64
import hashlib
import json

ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/'app/src/main/assets/pronunciation'
DEST=Path('/mnt/data/WordQuestAI_US_UK_Offline_Ses_Test.html')
words=['water','tomato','better',"can't",'schedule']
clips={}
for word in words:
    digest=hashlib.sha1(word.encode()).hexdigest()[:16]
    clips[word]={}
    for region in ('en-us','en-gb'):
        p=SOURCE/region/(digest+'.ogg')
        clips[word][region]='data:audio/ogg;base64,'+base64.b64encode(p.read_bytes()).decode('ascii')
html='''<!doctype html>
<html lang="tr"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>WordQuest AI · US/UK Çevrimdışı Ses Deneyi</title>
<style>
:root{font-family:system-ui,-apple-system,Segoe UI,sans-serif;color:#19392e;background:#f6f7f2}
*{box-sizing:border-box}body{margin:0;padding:28px 14px}.shell{max-width:740px;margin:auto}
header{background:#1e493a;color:#fff;padding:30px 28px;border-radius:24px}
.brand{font-size:14px;letter-spacing:.1em;text-transform:uppercase;opacity:.74}
h1{margin:9px 0;font-size:clamp(27px,5vw,38px)}.intro{color:#e1ecdd;line-height:1.5;margin-bottom:0}
.info{border-left:3px solid #acc87f;padding:10px 18px;margin:22px 0;line-height:1.55;background:#edf4e7;border-radius:6px}
.word{display:grid;grid-template-columns:1fr auto;gap:10px;align-items:center;background:white;padding:17px 19px;margin:12px 0;border:1px solid #dee7da;border-radius:17px;box-shadow:0 2px 6px #194b2a0c}
.english{font-size:23px;font-weight:700}.sub{color:#6b8073;font-size:12px;margin-top:3px}
.actions{display:flex;gap:8px;flex-wrap:wrap}.actions button{font-size:15px;padding:12px 17px;border-radius:12px;border:0;cursor:pointer;font-weight:650;background:#e3ecdf;color:#234931}
.actions button.us{background:#e6eefb;color:#224771}.actions button:hover{filter:brightness(.95)}.actions button:focus-visible{outline:3px solid #789d5b}
.status{min-height:24px;padding:10px 3px;font-size:14px;color:#22543b}footer{color:#66796e;font-size:13px;line-height:1.6;margin:24px 3px}
@media(max-width:540px){.word{grid-template-columns:1fr}.actions button{flex:1}}
</style></head><body><main class="shell"><header><div class="brand">WordQuest AI · Telaffuz Laboratuvarı</div>
<h1>Amerikan ve Britanya aksanlarını dinle</h1><p class="intro">5 sözcük · 10 bağımsız kayıt · İnternet veya telefonun metin okuma motoru gerekmez.</p></header>
<div class="info"><strong>Gerçekten farklı yöntem:</strong> Her düğme önceden üretilmiş <strong>ayrı OGG ses dosyasını</strong> oynatır. Bunlar eSpeak tabanlı sentez kayıtlarıdır; doğal insan sesi değildir ve bu HTML Android APK yerine geçmez.</div>
<section id="words"></section><div class="status" id="status" aria-live="polite"></div>
<footer>US için en-us, UK için en-gb eSpeak profilleri kullanıldı. Özellikle Tomato, Can't ve Schedule kelimelerini karşılaştır. Bu önizleme yalnızca ses dosyalarını test eder; v0.8.0 Android uygulamasının çalıştığını kanıtlamaz.</footer></main>
<script>
const clips=__CLIPS__;
const player=new Audio();
const labels={"water":"Water","tomato":"Tomato","better":"Better","can't":"Can't","schedule":"Schedule"};
const status=document.getElementById('status');
const container=document.getElementById('words');
for(const word of Object.keys(clips)){
 const row=document.createElement('article');row.className='word';
 const label=document.createElement('div');const strong=document.createElement('div');strong.className='english';strong.textContent=labels[word];
 const sub=document.createElement('div');sub.className='sub';sub.textContent='Aynı kelime, iki bağımsız ses profili';label.append(strong,sub);
 const buttons=document.createElement('div');buttons.className='actions';
 for(const [region,btn] of [['en-us','🇺🇸 US dinle'],['en-gb','🇬🇧 UK dinle']]){
   const b=document.createElement('button');b.className=region==='en-us'?'us':'uk';b.type='button';b.textContent=btn;
   b.addEventListener('click',()=>{
     player.pause();player.currentTime=0;player.src=clips[word][region];
     status.textContent=`Çalıyor: ${labels[word]} · ${region.toUpperCase()}`;
     player.play().catch(e=>{status.textContent='Tarayıcı sesi oynatamadı: '+e.message});
   });buttons.append(b);
 }
 row.append(label,buttons);container.append(row);
}
player.addEventListener('ended',()=>status.textContent='Dinleme tamamlandı.');
</script></body></html>'''
DEST.write_text(html.replace('__CLIPS__',json.dumps(clips,ensure_ascii=False)),encoding='utf-8')
print(DEST,DEST.stat().st_size)
