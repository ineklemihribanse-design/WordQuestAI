#!/usr/bin/env python3
"""Create phoneme transcriptions from the EXACT eSpeak voices used to synthesize bundled OGG clips.

Provenance note: this is a reproducible speech-engine phonemization, NOT a human-verified
IPA lexicon or a claim that the source word has just one English pronunciation. The
US CMU Pronouncing Dictionary is used to flag multi-reading words for review.
Unknown/ambiguous homographs are intentionally omitted from display.
"""
import json, re, subprocess, unicodedata
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'app/src/main/assets/pronunciation'
WORDS = json.loads((ASSETS/'manifest.json').read_text(encoding='utf8'))['words']
# Homographs without sense/part-of-speech context. A synthesizer choosing one reading
# cannot establish which one applies in the student's crossword or AI sentence.
UNRESOLVED = {
    'read', 'lead', 'live', 'wind', 'tear', 'record', 'present', 'produce',
    'minute', 'bow', 'bass', 'close', 'object', 'permit', 'refuse', 'content',
    'conduct', 'project', 'contract', 'digest', 'entrance', 'invalid', 'resume',
    'separate', 'use', 'wound', 'row', 'does', 'polish', 'desert', 'dove',
    'sow', 'axes', 'attribute', 'alternate', 'moderate', 'progress', 'abstract',
}
# Turkish approximations are expressly CURATED, not auto-invented. These are optional
# learning aids, never substitutes for the IPA.
# Manually checked stress-boundary formatting for the five accent-comparison
# examples. These spell out the phones actually used by the eSpeak OGG clips.
DEMO_IPA = {
    'water': ('ˈwɔːɾɚ', 'ˈwɔːtə'),
    'tomato': ('təˈmeɪɾoʊ', 'təˈmɑːtəʊ'),
    'better': ('ˈbɛɾɚ', 'ˈbɛtə'),
    "can't": ('ˈkænt', 'ˈkɑːnt'),
    'schedule': ('ˈskɛdʒuːl', 'ˈʃɛdjuːl'),
}
TR = {
    'water': ['votır', 'votı'],
    'tomato': ['tımeyto', 'tımato'],
    'better': ['bedır', 'betı'],
    "can't": ['kent', 'kaant'],
    'schedule': ['skecul', 'şedyul'],
}

def phonemize(text, region):
    p = subprocess.run(['espeak','-q','-v',region,'--ipa',text],
                       stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=10, check=True)
    ipa = unicodedata.normalize('NFC', p.stdout.decode('utf8').strip())
    ipa = re.sub(r'\s+', ' ', ipa).strip()
    # Remove any stray control characters, but preserve stress/ties/diphthong marks.
    if not ipa or any(unicodedata.category(c) in ('Cc', 'Cf') for c in ipa):
        return None
    # eSpeak may pronounce unexpected input punctuation as punctuation; reject
    # before treating it as verified pronunciation.
    if len(ipa) > max(30, len(text) * 9):
        return None
    return ipa

try:
    import cmudict
    CMU = cmudict.dict()
except ImportError:
    CMU = {}

entries={}; missing={}; multiple=[]; cmu_count=0
for word in sorted(WORDS):
    if word in UNRESOLVED:
        missing[word]='multiple senses/readings; choose a sense before displaying IPA'
        continue
    try:
        us=phonemize(word,'en-us'); uk=phonemize(word,'en-gb')
    except (subprocess.SubprocessError, UnicodeError) as e:
        missing[word]=type(e).__name__
        continue
    if not us or not uk:
        missing[word]='phonemizer returned no trustworthy output'
        continue
    # CMU entries are not a complete UK lexicon. We retain their availability as
    # provenance metadata, but avoid representing generated UK IPA as a dictionary citation.
    is_simple = bool(re.fullmatch(r"[a-z]+(?:'[a-z]+)?",word))
    refs=CMU.get(word, []) if is_simple else []
    if refs: cmu_count += 1
    different={tuple(v) for v in refs}
    if len(different)>1: multiple.append(word)
    if word in DEMO_IPA:
        us, uk = DEMO_IPA[word]
    ent={'us':us,'uk':uk,'origin':'espeak-phonemic','cmu_us':bool(refs)}
    if len(different)>1: ent['possible_multiple_readings']=True
    if word in TR: ent['tr_us'],ent['tr_uk']=TR[word]
    entries[word]=ent

payload={
    'scheme':'wordquest-ipa-v1',
    'source':'eSpeak voice-specific IPA generated using exactly the en-us/en-gb models used for the bundled sound recordings; automatically generated, not editorially dictionary-verified',
    'dictionary_crosscheck':'CMU Pronouncing Dictionary 0.7b US vocabulary coverage (informational; not used to claim editorial accuracy)',
    'entries':entries,
    'unresolved':missing,
}
(ASSETS/'ipa.json').write_text(json.dumps(payload,ensure_ascii=False,separators=(',',':'),sort_keys=True),encoding='utf8')
print(json.dumps({'audio_words':len(WORDS),'ipa_pairs':len(entries),'unresolved':len(missing),
                  'cmu_us_coverage':cmu_count,'cmu_multiple_readings':len(multiple),
                  'bytes':(ASSETS/'ipa.json').stat().st_size},ensure_ascii=False))
