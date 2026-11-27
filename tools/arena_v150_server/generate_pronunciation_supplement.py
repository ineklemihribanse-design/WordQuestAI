#!/usr/bin/env python3
"""Generate only missing offline recordings for the exact expanded Arena vocabulary.
Output is a review artifact, NOT an APK, release or signed content update.
Existing lookup IDs, IPA and recordings are preserved.
"""
import argparse, concurrent.futures, hashlib, importlib.util, json, re, zipfile
from pathlib import Path

from audit_pronunciation import inspect, normal

def write_json(path, obj):
    path.write_text(json.dumps(obj, ensure_ascii=False, separators=(",", ":"))+"\n", "utf-8")

def main():
    p=argparse.ArgumentParser()
    p.add_argument("apk", type=Path)
    p.add_argument("bank", type=Path)
    p.add_argument("output", type=Path)
    args=p.parse_args()
    bank=json.loads(args.bank.read_text("utf-8"))
    report=inspect(args.apk, bank)
    for field in ("invalidIds","missingClips","invalidClips","invalidIpa","unresolved","orphanIpa"):
        assert not report[field], (field, report[field])
    with zipfile.ZipFile(args.apk) as z:
        manifest=json.loads(z.read("assets/pronunciation/manifest.json"))
        ipa=json.loads(z.read("assets/pronunciation/ipa.json"))
    original_words=dict(manifest["words"])
    original_ipa=dict(ipa["entries"])
    required={normal(w["answer"]) for w in bank if w.get("level") in ("A1","A2","B1","B2","C1","C2") and re.fullmatch(r"[A-Za-z]{2,24}",w.get("answer","")) and w.get("meaning","").strip()}
    missing=sorted(required-set(original_words))
    assert len(missing)==report["expandedMissing"], (len(missing),report["expandedMissing"])
    out=args.output
    out.mkdir(parents=True,exist_ok=False)
    base=out/"pronunciation"
    base.mkdir()
    generator_path=Path(__file__).resolve().parents[1]/"v205/complete_pronunciation_205.py"
    spec=importlib.util.spec_from_file_location("wq_pronunciation",generator_path)
    generator=importlib.util.module_from_spec(spec)
    spec.loader.exec_module(generator)
    generator.BASE=base
    for region in generator.REGIONS:
        (base/region).mkdir()
    ids={w:hashlib.sha1(w.encode()).hexdigest()[:16] for w in missing}
    assert len(set(ids.values()))==len(ids)
    assert not set(ids.values())&set(original_words.values())
    def render_one(word):
        results={}
        for region in generator.REGIONS:
            transcription,digest=generator.render(word,ids[word],region)
            clip=base/region/(ids[word]+".ogg")
            assert hashlib.sha256(clip.read_bytes()).hexdigest()==digest
            results[region]={"ipa":transcription,"sha256":digest}
        return word,results
    generated={}
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
        for word,result in pool.map(render_one,missing):
            generated[word]=result
    assert set(generated)==set(missing)
    manifest["words"].update(ids)
    manifest["clipsPerRegion"]=len(manifest["words"])
    for word,result in generated.items():
        ipa["entries"][word]={"origin":"espeak-phonemic","us":result["en-us"]["ipa"],"uk":result["en-gb"]["ipa"]}
    assert all(manifest["words"][w]==v for w,v in original_words.items())
    assert all(ipa["entries"][w]==v for w,v in original_ipa.items())
    assert required<=set(manifest["words"]) and required<=set(ipa["entries"])
    assert all(len(list((base/r).glob("*.ogg")))==len(missing) for r in generator.REGIONS)
    write_json(base/"manifest.json",manifest)
    write_json(base/"ipa.json",ipa)
    summary={"previouslyCovered":len(required&set(original_words)),"newWords":len(missing),"newClips":len(missing)*2,"expandedHeadwords":len(required),"remainingMissing":len(required-set(manifest["words"])),"originalEntriesPreserved":len(original_words),"totalMergedEntries":len(manifest["words"]),"baseApkSha256":hashlib.file_digest(args.apk.open("rb"),"sha256").hexdigest(),"generated":generated,"limitation":"Supplement only; apply merged manifest and IPA plus new clips to matching base. No listening, device test, signing or release."}
    write_json(out/"report.json",summary)
    print("PRONUNCIATION_SUPPLEMENT",json.dumps({k:v for k,v in summary.items() if k!="generated"},ensure_ascii=False))
if __name__=="__main__":
    main()
