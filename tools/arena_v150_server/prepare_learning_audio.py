#!/usr/bin/env python3
"""Late assembly: extend offline audio after all learning content is installed.
Only new clips are produced; existing audio bytes and lookup IDs remain unchanged.
Homographs are explicitly flagged rather than guessing the intended sense.
"""
import hashlib, json, os, re, subprocess, sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
ASSETS=ROOT/"source/app/src/main/assets"
BASE=ASSETS/"pronunciation"
def load(p):return json.loads(p.read_text("utf-8"))
def write(p,obj):p.write_text(json.dumps(obj,ensure_ascii=False,separators=(",",":"))+"\n","utf-8")
def main():
    expanded=load(Path(sys.argv[1]))
    required={w["answer"].strip().lower() for w in expanded if w.get("level") in ("A1","A2","B1","B2","C1","C2") and re.fullmatch(r"[A-Za-z]{2,24}",w.get("answer","")) and w.get("meaning","").strip()}
    index=ASSETS/"learning-pronunciation-words.json"
    authored=set(load(index))
    assert required and authored
    write(index,sorted(required|authored))
    original=load(BASE/"manifest.json")["words"]
    original_ipa=load(BASE/"ipa.json")["entries"]
    def digest(p):return hashlib.sha256(p.read_bytes()).hexdigest()
    previous={p.relative_to(BASE).as_posix():digest(p) for p in BASE.rglob("*.ogg")}
    env=dict(os.environ,WQ_PRESERVE_EXISTING_AUDIO="1")
    subprocess.run([sys.executable,str(ROOT/"tools/v205/complete_pronunciation_205.py")],check=True,env=env)
    manifest=load(BASE/"manifest.json")
    ipa=load(BASE/"ipa.json")
    assert all(manifest["words"].get(k)==v for k,v in original.items())
    assert all(ipa["entries"].get(k)==v for k,v in original_ipa.items())
    assert all(digest(BASE/p)==sha for p,sha in previous.items()),"Existing audio bytes changed"
    assert required<=set(manifest["words"]) and required<=set(ipa["entries"])
    variants=load(Path(__file__).with_name("pronunciation_variants.json"))
    marked=[]
    for word,readings in variants.items():
        if word not in ipa["entries"]:continue
        ipa["entries"][word]["possible_multiple_readings"]=True
        marked.append(word)
    write(BASE/"ipa.json",ipa)
    write(ASSETS/"learning-pronunciation-variants.json",{w:variants[w] for w in marked})
    print("LEARNING_AUDIO_INTEGRATION",json.dumps({"expandedRequired":len(required),"missing":len(required-set(manifest["words"])),"newEntries":len(manifest["words"])-len(original),"oldClipsPreserved":len(previous),"ambiguousMarked":len(marked),"note":"Variant IPA reference and existing UI ambiguity flags; sense-specific playback not implemented."},ensure_ascii=False))
if __name__=="__main__":main()
