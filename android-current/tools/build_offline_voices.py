#!/usr/bin/env python3
"""Generate bundled, reproducible en-US / en-GB OGG clips for all first-party puzzle words.

Uses local eSpeak voice definitions, not Android device TTS. Output stays local. A reference
manifest enables app/tests to verify that each US/UK word pair really has two assets.
"""
from __future__ import annotations
from concurrent.futures import ThreadPoolExecutor, as_completed
from hashlib import sha1, sha256
import json
import io
import soundfile as sf
from pathlib import Path
import re
import subprocess
import sys
import unicodedata

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'app/src/main/assets'
DEST=ASSETS/'pronunciation'


def normalized(text: str) -> str:
    return re.sub(r'\s+', ' ', unicodedata.normalize('NFKC', text).strip().lower())


def digest(text: str) -> str:
    return sha1(normalized(text).encode('utf-8')).hexdigest()[:16]


def list_words() -> list[str]:
    words={'water','tomato','better',"can't",'schedule','hello'}
    for name in ['puzzles.json','expanded-puzzles.json']:
        for puzzle in json.loads((ASSETS/name).read_text('utf-8')):
            for entry in puzzle.get('words', []):
                words.add(normalized(str(entry[0])))
    for line in (ASSETS/'vocabulary-extra.tsv').read_text('utf-8').splitlines():
        if line and not line.startswith('#'):
            columns=line.split('\t')
            if len(columns)==5:words.add(normalized(columns[3]))
    for line in (ASSETS/'verb-forms.txt').read_text('utf-8').splitlines():
        if line and not line.startswith('#'):
            columns=line.split('\t')
            if len(columns)==7:
                for forms in columns[1:4]:
                    for f in forms.split('|'):words.add(normalized(f))
    return sorted(words)


def gen(pair:tuple[str,str]) -> tuple[str,str,int]:
    word, region=pair
    filename=f'{digest(word)}.ogg'
    directory=DEST/region
    directory.mkdir(parents=True, exist_ok=True)
    path=directory/filename
    if path.exists() and path.stat().st_size>300:
        return word,region,path.stat().st_size
    # eSpeak's en-us and en-gb profiles yield distinct pronunciation for key contrast examples.
    v={'en-us':'en-us','en-gb':'en-gb'}[region]
    source=subprocess.run(['espeak','--stdout','-v',v,'-s','145',word],capture_output=True,check=True)
    samples,samplerate=sf.read(io.BytesIO(source.stdout),dtype='float32')
    out=io.BytesIO()
    sf.write(out,samples,samplerate,format='OGG',subtype='VORBIS')
    encoded=out.getvalue()
    if len(encoded)<300: raise RuntimeError(f'Empty output: {word} {v}')
    path.write_bytes(encoded)
    return word,region,len(encoded)


def main():
    words=list_words()
    keys={}
    for w in words:
        k=digest(w)
        if k in keys and keys[k]!=w: raise RuntimeError(f'Hash collision {w}, {keys[k]}')
        keys[k]=w
    pairs=[(w,reg) for w in words for reg in ('en-us','en-gb')]
    print('word_count',len(words),'audio_count',len(pairs),flush=True)
    with ThreadPoolExecutor(max_workers=12) as pool:
        futures={pool.submit(gen, pair):pair for pair in pairs}
        done=0;bytes_total=0
        for future in as_completed(futures):
            word,region,length=future.result()
            done+=1;bytes_total+=length
            if done%350==0:print('Generated',done,'/',len(pairs),'~',round(bytes_total/1e6,1),'MB',flush=True)
    manifest={
      'scheme':'wordquest-offline-pronunciation-v1',
      'generator':'eSpeak en-us and en-gb; original synthesized offline clips, encoded as Ogg Vorbis',
      'speechProfiles':{'en-us':'eSpeak en-us','en-gb':'eSpeak en-gb'},
      'words':{w:digest(w) for w in words},
      'clipsPerRegion':len(words),
      'fileExt':'ogg',
    }
    (DEST/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,separators=(',',':')),encoding='utf-8')
    print('DONE',len(words),'words',len(pairs),'clips',round(bytes_total/1e6,1),'MB',flush=True)

if __name__=='__main__':main()
