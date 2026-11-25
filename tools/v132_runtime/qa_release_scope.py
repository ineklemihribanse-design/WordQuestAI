#!/usr/bin/env python3
"""Conservative, byte-based release UI test selection. Unknown means FULL.
Run only after the mandatory signed APK upgrade/data preservation check.
A native code change (including any DEX), manifest, web engine, rules or
unknown asset change never qualifies for a reduced UI test set.
"""
import argparse, hashlib, json, os, re, subprocess, sys, zipfile
from pathlib import Path

FULL=("phone-crossword","phone-quiz","tablet-crossword","tablet-quiz")
SMOKE=("phone-start",)
ARENA=("phone-crossword","phone-quiz","tablet-crossword","tablet-quiz")
SIGNED_META=re.compile(r"^META-INF/(?:MANIFEST\.MF|[^/]+\.(?:SF|RSA|DSA|EC))$",re.I)
PRON=re.compile(r"^assets/pronunciation/(?:manifest\.json|ipa\.json|(?:en-us|en-gb)/[a-f0-9]{16}\.ogg)$")
ARENA_ART=re.compile(r"^assets/arena_v(?:120|122)/[^/]+\.(?:webp|png)$")
# Explicitly recognized data assets only. No broad 'assets/**' bypass.
def inventory(path):
    with zipfile.ZipFile(path) as z:
        names=[x.filename for x in z.infolist() if not x.is_dir()]
        if len(names)!=len(set(names)):
            raise ValueError("Duplicate APK entries")
        result={}
        for name in names:
            if SIGNED_META.fullmatch(name): continue
            with z.open(name) as f:
                result[name]=hashlib.file_digest(f,"sha256").hexdigest()
        return result

def select(old,new):
    changed=sorted(k for k in old.keys()|new.keys() if old.get(k)!=new.get(k))
    # Never reduce if actual native or unknown data changed, even when
    # a commit message claims a visual-only update.
    substantive=[k for k in changed if k!="AndroidManifest.xml"]
    # A changed manifest is conservatively full (could change permissions).
    if "AndroidManifest.xml" in changed:
        return "full",FULL,changed
    if not substantive:
        return "full",FULL,changed
    if all(PRON.fullmatch(k) for k in substantive):
        return "pronunciation",SMOKE,changed
    if all(ARENA_ART.fullmatch(k) for k in substantive):
        return "arena-art",ARENA,changed
    return "full",FULL,changed

MARKERS={
 "phone-start":"PASS: WordQuest foreground after launch",
 "phone-crossword":"PASS: actual Android mixed crossword gameplay opens",
 "phone-quiz":"PASS: actual Android web quiz gameplay opens",
 "tablet-crossword":"PASS: landscape Arena board, answers and mixed-level title",
 "tablet-quiz":"PASS: landscape Arena quiz and clean return",
}

def main():
    p=argparse.ArgumentParser()
    p.add_argument("base",type=Path);p.add_argument("target",type=Path)
    p.add_argument("--harness",type=Path)
    p.add_argument("--report",type=Path,required=True)
    args=p.parse_args()
    try:
        mode,scenarios,changed=select(inventory(args.base),inventory(args.target))
    except Exception as error:
        mode,scenarios,changed="full",FULL,[f"Inventory unavailable: {error}"]
    report=dict(scope=mode,scenarios=scenarios,changed_entries=changed)
    args.report.parent.mkdir(parents=True,exist_ok=True)
    args.report.write_text(json.dumps(report,ensure_ascii=False,indent=2))
    print("UI scope:",mode,"scenarios:",", ".join(scenarios),flush=True)
    if args.harness is None: return
    env=dict(os.environ,WQ_UI_SCENARIOS=",".join(scenarios))
    proc=subprocess.Popen([sys.executable,str(args.harness)],env=env,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
    seen=set()
    for line in proc.stdout:
        print(line,end="",flush=True)
        for case in scenarios:
            if MARKERS[case] in line: seen.add(case)
    if proc.wait()!=0 or seen!=set(scenarios):
        raise SystemExit(f"Required UI checks failed: {set(scenarios)-seen}")
    print("PASS: scope-selected release UI checks",flush=True)

if __name__=="__main__":
    main()
