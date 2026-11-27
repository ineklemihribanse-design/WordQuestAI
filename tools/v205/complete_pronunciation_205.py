#!/usr/bin/env python3
"""Cover the entire bundled crossword vocabulary, including the v175 addendum.
Keep existing file IDs, correct all known short lexical words and resolve missing
IPA via the same standalone en-US/en-GB synthesis input used for its audio.
No gameplay, UI, or vocabulary changes.
"""
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
import hashlib
import io
import json
import os
import re
import runpy
import subprocess
import unicodedata
import numpy as np
import soundfile as sf

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "source/app/src/main/assets"
BASE = ASSETS / "pronunciation"
SRC = ROOT / "source/app/src/main/java/com/kelimebulmacasi/a1"
PREVIOUS = runpy.run_path(str(ROOT / "tools/v204/fix_short_word_audio_204.py"))
REGIONS = ("en-us", "en-gb")
SHORT = PREVIOUS["PHONEMES"]

def normal(text):
    # Match OfflineAccentPlayer.normalize: preserve phrase boundaries.
    return " ".join(text.strip().lower().split())

def asset_id(word):
    return hashlib.sha1(word.encode("utf-8")).hexdigest()[:16]

def bundled_words(assets, src):
    words = set()
    counts = {}
    def record(name, items):
        items = {normal(x) for x in items if x.strip()}
        assert items, name
        counts[name] = len(items)
        words.update(items)
    catalog = (src / "Catalog.kt").read_text("utf-8")
    # These are the actual catalog files, including the addendum that the
    # original offline-audio generator did not cover. Fail on schema drift.
    for name in ("puzzles.json", "expanded-puzzles.json"):
        data = json.loads((assets/name).read_text("utf-8"))
        record(name, [str(e[0]) for game in data for e in game["words"]])
    vocabulary_files = {"vocabulary-extra.tsv", "vocabulary-addendum-v175.tsv"}
    vocabulary_files.update(re.findall(r'assets\.open\("([^"]*vocabulary[^"]*\.tsv)"\)', catalog))
    for name in sorted(vocabulary_files):
        rows = [line.split("\t") for line in (assets/name).read_text("utf-8").splitlines()
                if line.strip() and not line.startswith("#")]
        assert all(len(row) == 5 for row in rows), name
        record(name, [row[3] for row in rows])
    assert counts["vocabulary-addendum-v175.tsv"] >= 4000
    learning_manifest = assets / "learning-pronunciation-words.json"
    if (src / "Wq150AcademicWords.kt").exists():
        assert learning_manifest.is_file(), "Authored YDS vocabulary pronunciation manifest missing"
    if learning_manifest.exists():
        learning_words = json.loads(learning_manifest.read_text("utf-8"))
        assert isinstance(learning_words, list) and len(learning_words) == len(set(learning_words))
        assert all(isinstance(w,str) and re.fullmatch(r"[a-z]{2,24}",w) for w in learning_words)
        record(learning_manifest.name, learning_words)
    verb_rows = [line.split("\t") for line in (assets/"verb-forms.txt").read_text("utf-8").splitlines()
                 if line.strip() and not line.startswith("#")]
    assert all(len(row) == 7 for row in verb_rows)
    record("verb-forms.txt", [word for row in verb_rows for field in row[1:4] for word in field.split("|")])
    # These common lexical entries must work even if not in the catalog.
    words.update(("do", "hi", "my", "me", "we", "he", "go", "no"))
    assert len(words) > 5000, ("unexpected current vocabulary size", len(words))
    print("Bundled vocabulary coverage sources:", counts, "unique:", len(words), flush=True)
    return words, counts

def pronunciation_input(word, region):
    if word in SHORT:
        p = SHORT[word]
        if isinstance(p, tuple):
            p = p[REGIONS.index(region)]
        return "[[" + p + "]]"
    if len(word) == 2 and word in PREVIOUS["INITIALISMS"]:
        # Genuine abbreviations are pronounced as such, not invented words.
        return word.upper()
    return word

def espeak(*args):
    return subprocess.run(["espeak", *args], check=True, capture_output=True, timeout=30).stdout

def render(word, fileid, region):
    spoken = pronunciation_input(word, region)
    ipa = " ".join(espeak("-q", "--ipa", "-v", region, spoken).decode("utf-8").split())
    assert ipa and any(c.isalpha() for c in ipa), (word, region, ipa)
    assert not any(c in ipa for c in "<>/\n\r"), (word, region, ipa)
    assert len(ipa) <= max(30, len(word)*9), (word, region, ipa)
    if word in ("do", "hi", "my"):
        expected = {"do":"duː","hi":"haɪ","my":"maɪ"}[word]
        assert re.sub(r"[\sˈˌ]", "", ipa) == expected, (word, region, ipa)
    wav = espeak("--stdout", "-v", region, "-s", "145", spoken)
    samples, rate = sf.read(io.BytesIO(wav), dtype="float32")
    assert rate > 0 and samples.size > 1000 and np.isfinite(samples).all()
    assert np.max(np.abs(samples)) > .001, (word, region, "silent source")
    out = io.BytesIO()
    sf.write(out, samples, rate, format="OGG", subtype="VORBIS")
    blob = out.getvalue()
    assert len(blob)>1000 and blob.startswith(b"OggS")
    # Decode again, independently of the source WAV.
    decoded, out_rate = sf.read(io.BytesIO(blob), dtype="float32")
    assert out_rate == rate and decoded.size == samples.size
    assert np.isfinite(decoded).all() and np.max(np.abs(decoded)) > .001
    path = BASE / region / (fileid + ".ogg")
    path.write_bytes(blob)
    return ipa, hashlib.sha256(blob).hexdigest()

def main():
    manifest_path = BASE / "manifest.json"
    ipa_path = BASE / "ipa.json"
    manifest = json.loads(manifest_path.read_text("utf-8"))
    ipa_data = json.loads(ipa_path.read_text("utf-8"))
    old_words = dict(manifest["words"])
    old_entries = dict(ipa_data["entries"])
    old_unresolved = set(ipa_data["unresolved"])
    required, counts = bundled_words(ASSETS, SRC)
    all_words = sorted(required | set(old_words))
    words = {w:asset_id(w) for w in all_words}
    assert all(words[w] == fileid for w, fileid in old_words.items())
    assert len(set(words.values())) == len(words)
    for region in REGIONS:
        (BASE/region).mkdir(exist_ok=True)
    # Existing valid recordings/IPA remain untouched, except the known short
    # words whose standalone lexical pronunciation is explicitly corrected.
    todo = [w for w in all_words if w not in old_words or w not in old_entries
            or w in old_unresolved or (w in SHORT and os.environ.get("WQ_PRESERVE_EXISTING_AUDIO") != "1") or any(
                not (BASE/r/(words[w]+".ogg")).is_file() for r in REGIONS)]
    def both(w):
        us, hu = render(w, words[w], "en-us")
        uk, hk = render(w, words[w], "en-gb")
        assert hu != hk, (w, "independent encoding")
        return w, {"origin":"espeak-phonemic","us":us,"uk":uk}
    entries = dict(old_entries)
    workers = min(8, max(2, os.cpu_count() or 2))
    print("Existing audio",len(old_words),"new audio",len(set(words)-set(old_words)),
          "existing unresolved",len(old_unresolved),"regenerate",len(todo),flush=True)
    with ThreadPoolExecutor(max_workers=workers) as pool:
        for i,(w,e) in enumerate(pool.map(both,todo),1):
            entries[w]=e
            if i%250==0 or i==len(todo):
                print(f"Pronunciation generated {i}/{len(todo)} words (both accents)",flush=True)
    assert set(entries) == set(words)
    assert required <= set(words)
    for w in ("do","hi","my"):
        for a in ("us","uk"):
            assert re.sub(r"[\sˈˌ]","",entries[w][a]) == {"do":"duː","hi":"haɪ","my":"maɪ"}[w]
    manifest["words"] = words
    manifest["clipsPerRegion"] = len(words)
    ipa_data["entries"] = {w:entries[w] for w in all_words}
    ipa_data["unresolved"] = {} if isinstance(ipa_data["unresolved"],dict) else []
    manifest_path.write_text(json.dumps(manifest,ensure_ascii=False,separators=(",",":")),"utf-8")
    ipa_path.write_text(json.dumps(ipa_data,ensure_ascii=False,separators=(",",":")),"utf-8")
    # Keep the earlier integrity verifier intact apart from its vocabulary size
    # pin. Add a strict zero-missing gate against the actual source corpus.
    verifier=ROOT/"source/tools/verify_audio_ipa.py"
    old=verifier.read_text("utf-8")
    updated,n=re.subn(r"assert len\(words\) == \d+,",f"assert len(words) == {len(words)},",old)
    assert n==1
    verifier.write_text(updated,"utf-8")
    report={"source_counts":counts,"required":len(required),"audio_words":len(words),
            "ipa_words":len(entries),"new_audio":len(set(words)-set(old_words)),
            "formerly_unresolved":len(old_unresolved),"remaining_missing":0,
            "regenerated":len(todo),"examples":{w:entries[w] for w in ("do","hi","my")},
            "limitation":"Synthesized standalone dictionary readings; contextual heteronyms and device listening require separate review."}
    if os.environ.get("RUNNER_TEMP"):
        (Path(os.environ["RUNNER_TEMP"])/"wq205-pronunciation-coverage.json").write_text(json.dumps(report,ensure_ascii=False,indent=2),"utf-8")
    print("PASS full bundled pronunciation coverage:",json.dumps(report,ensure_ascii=False),flush=True)

if __name__=="__main__":
    main()
