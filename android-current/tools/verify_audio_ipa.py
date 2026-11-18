#!/usr/bin/env python3
"""Fail-fast offline pronunciation integrity check (stdlib only)."""
import hashlib, json, re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / 'app/src/main/assets/pronunciation'

def stop(message):
    raise AssertionError(message)

audio = json.loads((BASE/'manifest.json').read_text(encoding='utf-8'))
ipa = json.loads((BASE/'ipa.json').read_text(encoding='utf-8'))
words = audio['words']
entries = ipa['entries']
unresolved = ipa['unresolved']
assert len(words) == 2203, f'Unexpected recorded vocabulary size {len(words)}'
assert set(entries).isdisjoint(unresolved), 'Ambiguous IPA word mistakenly displayed'
assert set(entries) | set(unresolved) == set(words), 'IPA index must account for every audio word'
assert all(isinstance(w, str) and len(w)>0 for w in words)
assert len(set(words.values())) == len(words), 'Audio filename collision'

for word, fileid in words.items():
    normalized = ' '.join(word.strip().lower().split())
    expected = hashlib.sha1(normalized.encode('utf8')).digest()[:8].hex()
    assert fileid == expected, f'Wrong audio clip mapping for {word}'
    recordings=[]
    for accent in ('en-us','en-gb'):
        clip=BASE/accent/(fileid+'.ogg')
        assert clip.is_file(),f'Missing {accent} audio for {word}'
        blob=clip.read_bytes()
        assert len(blob) > 1000 and blob.startswith(b'OggS'),f'Invalid Ogg header {accent}/{word}'
        recordings.append(blob)
    assert recordings[0]!=recordings[1],f'Identical accent recordings: {word}'
    if word in entries:
        e=entries[word]
        assert e['origin']=='espeak-phonemic',f'IPA source attribution lost for {word}'
        for accent in ('us','uk'):
            value=e[accent]
            assert isinstance(value,str) and 1<=len(value)<=max(30,len(word)*9),f'Bad IPA {word} {accent}'
            assert not any(c in value for c in ('<','>','/','\n','\r')),f'Unsupported IPA character {word}'

assert len(entries)>1800, f'Insufficient IPA coverage: {len(entries)}'
for word in ('water','tomato','better',"can't",'schedule'):
    assert word in entries and entries[word]['us']!=entries[word]['uk'],f'Missing contrast for {word}'
assert not any(p.name.endswith('.ogg') for p in BASE.glob('*.ogg'))
print(json.dumps({'result':'passed', 'voice_profiles':2, 'audio_entries':len(words),
                  'ogg_files':len(words)*2, 'ipa_pairs':len(entries),
                  'ambiguous_or_unresolved':len(unresolved),
                  'all_5_demonstration_words_have_distinct_ipa':True},ensure_ascii=False))
