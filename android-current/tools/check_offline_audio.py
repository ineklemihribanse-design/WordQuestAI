#!/usr/bin/env python3
"""Standalone source asset checker. DOES NOT perform Android/device/audio perception tests."""
from pathlib import Path
import json
import hashlib
import soundfile as sf
import sys

ROOT = Path(__file__).resolve().parents[1]
DIR = ROOT/'app/src/main/assets/pronunciation'


def run():
    manifest = json.loads((DIR/'manifest.json').read_text('utf8'))
    words = manifest['words']
    problems=[]
    mismatched=0
    for text, id in words.items():
        if id != hashlib.sha1(text.encode('utf8')).hexdigest()[:16]:
            problems.append(f'Invalid ID {text}')
        paths=[DIR/region/(id+'.ogg') for region in ('en-us','en-gb')]
        for p in paths:
            if not p.exists(): problems.append(f'Missing {p}')
            elif p.stat().st_size<500: problems.append(f'Empty {p}')
        if all(p.exists() for p in paths) and paths[0].read_bytes()!=paths[1].read_bytes():mismatched+=1
    differences={}
    for w in ['water','tomato','better',"can't",'schedule']:
        id=words.get(w)
        if not id:
            problems.append(f'Comparison word missing: {w}')
            continue
        files=[DIR/reg/(id+'.ogg') for reg in ('en-us','en-gb')]
        audios=[sf.info(str(p)) for p in files]
        differences[w]={
            'en-US_duration': round(audios[0].duration,2),
            'en-GB_duration': round(audios[1].duration,2),
            'file_bytes_different': files[0].read_bytes()!=files[1].read_bytes(),
            'both_audio_decodable': all(a.duration>0.1 and a.samplerate>8000 for a in audios)
        }
        if not differences[w]['file_bytes_different'] or not differences[w]['both_audio_decodable']:
            problems.append(f'Comparison files invalid/identical: {w}')
    result={
        'word_count':len(words),
        'pair_files_different':mismatched,
        'total_audio_files':sum(1 for p in DIR.rglob('*.ogg')),
        'pronunciation_comparison':differences,
        'errors':problems[:50],
        'device_playback_not_tested': True,
        'intelligibility_not_tested': True,
    }
    print(json.dumps(result,ensure_ascii=False,indent=2))
    return 1 if problems else 0

if __name__=='__main__':sys.exit(run())
