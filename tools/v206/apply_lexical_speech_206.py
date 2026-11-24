#!/usr/bin/env python3
"""Correct the actual device-vs-offline routing, not just displayed IPA."""
from pathlib import Path
import hashlib
import shutil
import io
import json
import re
import subprocess
import numpy as np
import soundfile as sf

ROOT = Path(__file__).resolve().parents[2]
HERE = Path(__file__).resolve().parent
SRC = ROOT / "source/app/src/main/java/com/kelimebulmacasi/a1"
TEST = ROOT / "source/app/src/test/java/com/kelimebulmacasi/a1"
BASE = ROOT / "source/app/src/main/assets/pronunciation"

def one(s, a, b):
    assert s.count(a) == 1, (a[:120], s.count(a))
    return s.replace(a,b,1)

def patch(s):
    old = '''        if (mode == PronunciationMode.ClassicOffline) {
            voiceDetails = "${preferred.locale.toLanguageTag()} · eski çevrimdışı eSpeak kaydı (robotik olabilir)"
            bundled.play(words, preferred, rate, volume, { error = it })
            return
        }'''
    new = '''        val verifiedShort = Wq206LexicalSpeech.useVerifiedRecording(words, bundled::contains)
        if (mode == PronunciationMode.ClassicOffline || verifiedShort) {
            voiceDetails = if (verifiedShort)
                "${preferred.locale.toLanguageTag()} · doğrulanmış kısa kelime ses kaydı"
            else "${preferred.locale.toLanguageTag()} · eski çevrimdışı eSpeak kaydı (robotik olabilir)"
            bundled.play(words.map(Wq206LexicalSpeech::key), preferred, rate, volume, { error = it })
            return
        }'''
    # Kotlin interpolation must be preserved literally.
    old = old.replace("\\${", "${")
    new = new.replace("\\${", "${")
    s = one(s,old,new)
    s = one(s,"        devicePlayer().play(words, preferred,",
            "        devicePlayer().play(Wq206LexicalSpeech.deviceText(words), preferred,")
    assert "bundled.play(remaining, preferred, rate, volume" in s
    assert s.index("if (mode == PronunciationMode.ClassicOffline || verifiedShort)") < s.index("devicePlayer().play(")
    return s

def audit_audio():
    words=json.loads((BASE/"manifest.json").read_text("utf-8"))["words"]
    entries=json.loads((BASE/"ipa.json").read_text("utf-8"))["entries"]
    checks={"us":("'Vs","ʌs"),"do":("d'u:","duː"),"hi":("h'aI","haɪ"),"my":("m'aI","maɪ")}
    for word,(phonemes,expected) in checks.items():
        assert words[word]==hashlib.sha1(word.encode()).hexdigest()[:16]
        for region,key in (("en-us","us"),("en-gb","uk")):
            raw="[[ "+phonemes+" ]]".replace(" ","")
            ipa=subprocess.check_output(["espeak","-q","--ipa","-v",region,raw]).decode().strip()
            assert re.sub(r"[\sˈˌ]","",ipa)==expected,(word,region,ipa)
            assert re.sub(r"[\sˈˌ]","",entries[word][key])==expected
            wav=subprocess.check_output(["espeak","--stdout","-v",region,"-s","145",raw])
            ref,rate=sf.read(io.BytesIO(wav),dtype="float32")
            path=BASE/region/(words[word]+".ogg")
            pcm,rate2=sf.read(path,dtype="float32")
            assert rate==rate2 and len(ref)==len(pcm)
            corr=float(np.corrcoef(ref,pcm)[0,1])
            assert corr>.90,(word,region,corr)
            if word=="us":
                spelled,_=sf.read(io.BytesIO(subprocess.check_output(
                    ["espeak","--stdout","-v",region,"-s","145","US"])),dtype="float32")
                assert len(spelled)!=len(pcm)
            print("PASS exact spoken waveform",word,region,ipa,round(corr,4),flush=True)

def main():
    p=SRC/"WordPronunciation.kt"
    old=p.read_text("utf-8")
    protected={f:hashlib.sha256(f.read_bytes()).hexdigest() for f in SRC.glob("*.kt") if f!=p}
    new=patch(old)
    audit_audio()
    p.write_text(new,"utf-8")
    shutil.copyfile(HERE/"Wq206LexicalSpeech.kt",SRC/"Wq206LexicalSpeech.kt")
    shutil.copyfile(HERE/"Wq206LexicalSpeechTest.kt",TEST/"Wq206LexicalSpeechTest.kt")
    for f,digest in protected.items():
        assert hashlib.sha256(f.read_bytes()).hexdigest()==digest,f
    print("PASS: actual WordPronunciation routes verified short words before the device voice; all existing non-audio Kotlin sources unchanged")

if __name__=="__main__":
    main()
