#!/usr/bin/env python3
"""Regenerate short lexical pronunciations, preserving exact offline lookup IDs.
Only pronunciation OGG assets are changed; ordinary English words are not spelt.
US/GB profiles remain independent. No TTS, game or interface changes."""
from pathlib import Path
import hashlib
import json
import re
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[2]
BASE = ROOT / "source/app/src/main/assets/pronunciation"
REGIONS = ("en-us", "en-gb")
# eSpeak phonemes are input notation, NOT literal text to be read out.
# Common two-letter words are deliberately specified, including strong forms.
PHONEMES = {
    "do": "d'u:", "hi": "h'aI", "go": "g'oU", "no": "n'oU",
    "me": "m'i:", "he": "h'i:", "we": "w'i:", "be": "b'i:",
    "by": "b'aI", "my": "m'aI", "to": "t'u:", "us": "'Vs",
    "am": "'am", "is": "'Iz", "it": "'It", "in": "'In",
    "on": "'0n", "at": "'at", "up": "'Vp", "as": "'az",
    "an": "'an", "or": "'O@", "so": "s'oU", "if": "'If",
    "ox": "'0ks", "of": ("'Vv", "'0v"), "ok": ",oUk'eI",
    "ad": "'ad", "ah": "'A:", "oh": "'oU", "ha": "h'A:",
    "eh": "'eI", "ax": "'aks", "ex": "'Eks",
    "ma": "m'A:", "pa": "p'A:", "pi": "p'aI",
    "er": "'3:", "um": "'Vm", "uh": "'V", "ow": "'aU",
    "oy": "'OI", "yo": "j'oU", "ye": "j'i:", "lo": "l'oU",
}
# A genuine initialism should not be forced into a fabricated word sound.
INITIALISMS = {"tv", "uk", "eu", "un", "pc", "cd", "id", "ip", "ai",
              "am", "pm", "dr", "mr", "ms", "dj", "hr", "it", "pr",
              "qr", "vr", "ar", "ac", "dc", "ny", "la", "ca", "bc",
              "gp", "mp", "cc", "cv", "iq", "ad", "ed", "rx", "vs"}

def run(*cmd, **kwargs):
    return subprocess.run(cmd, check=True, capture_output=True, timeout=45, **kwargs).stdout

def ipa(region, text):
    return run("espeak", "-q", "--ipa", "-v", region, text).decode("utf-8").strip()

def plain_ipa(text):
    return re.sub(r"[\sˈˌ]", "", text)

def source_for(word, region):
    p = PHONEMES.get(word)
    if isinstance(p, tuple):
        p = p[REGIONS.index(region)]
    if p is not None:
        return "[[" + p + "]]"
    if word in INITIALISMS:
        return None
    # Any other two-letter lexical entry goes through lowercase dictionary
    # lookup. Do not invent a phoneme or alter the vocabulary.
    return word

def make_clip(region, source, path):
    wav = run("espeak", "--stdout", "-v", region, "-s", "145", source)
    assert wav.startswith(b"RIFF") and wav[8:12] == b"WAVE"
    with tempfile.TemporaryDirectory() as tmp:
        out = Path(tmp) / "word.ogg"
        run("ffmpeg", "-v", "error", "-y", "-f", "wav", "-i", "pipe:0",
            "-c:a", "libvorbis", "-q:a", "6", str(out), input=wav)
        # Decode the finished OGG; verify usable audio, not just the header.
        pcm = run("ffmpeg", "-v", "error", "-i", str(out),
                  "-f", "s16le", "-ac", "1", "pipe:1")
        data = out.read_bytes()
        assert len(data) > 1000 and data.startswith(b"OggS")
        assert len(pcm) > 2000 and any(pcm), (region, source)
        path.write_bytes(data)
        return hashlib.sha256(data).hexdigest()

def main():
    manifest = BASE / "manifest.json"
    manifest_before = manifest.read_bytes()
    manifest_data = json.loads(manifest_before)
    words = manifest_data["words"]
    old_words = dict(words)
    ipa_path = BASE / "ipa.json"
    ipa_data = json.loads(ipa_path.read_text("utf-8"))
    added = sorted({"do", "hi"} - set(words))
    print("Missing requested audio entries:", added, flush=True)
    for word in added:
        file_id = hashlib.sha1(word.encode()).hexdigest()[:16]
        assert file_id not in words.values(), (word, "existing asset collision")
        words[word] = file_id
    assert len(words) == len(old_words) + len(added)
    assert all(words[k] == v for k, v in old_words.items())
    player = BASE.parents[1] / "java/com/kelimebulmacasi/a1/OfflineAccentPlayer.kt"
    # Assembled app must still use the verified whole-word offline playback.
    source = player.read_text("utf-8")
    assert "words.map(::normalize)" in source and "queue.addAll(filtered)" in source
    assert "val filename = known[word]" in source
    all_short = sorted(w for w in words if re.fullmatch(r"[a-z]{2}", w))
    print("Two-letter dictionary entries:", ", ".join(all_short))
    changed = set()
    report = {}
    for word in all_short:
        file_id = words[word]
        assert file_id == hashlib.sha1(word.encode()).hexdigest()[:16]
        if source_for(word, REGIONS[0]) is None:
            print("Preserve existing initialism:", word)
            continue
        hashes = {}
        for region in REGIONS:
            spoken = source_for(word, region)
            actual = ipa(region, spoken)
            assert actual, (word, region)
            if word in ("do", "hi"):
                expected = {"do": "duː", "hi": "haɪ"}[word]
                assert plain_ipa(actual) == expected, (word, region, actual)
            if word in PHONEMES:
                # A phonemic override deliberately resolves dictionary ambiguity.
                # For known words use the explicit, whole-word phoneme sequence.
                assert spoken.startswith("[[") and spoken.endswith("]]")
                assert len(actual.split()) <= 2, (word, region, actual)
            else:
                print("Dictionary-based lexical entry (manual listening not verified):", word, region, actual)
            path = BASE / region / (file_id + ".ogg")
            if word not in added:
                assert path.is_file(), path
            else:
                path.parent.mkdir(parents=True, exist_ok=True)
            hashes[region] = make_clip(region, spoken, path)
            changed.add(path.relative_to(BASE).as_posix())
            report.setdefault(word, {})[region] = {"ipa": actual, "sha256": hashes[region]}
            print("PASS short pronunciation:", word, region, actual)
        assert hashes[REGIONS[0]] != hashes[REGIONS[1]], word
    # Keep existing dictionary IDs, add only explicitly missing lexical entries.
    # IPA for the requested words must agree with the generated phonemes.
    for word in ("do", "hi"):
        ipa_data["entries"][word] = {
            **ipa_data["entries"].get(word, {}),
            "origin": "espeak-phonemic",
            "us": report[word]["en-us"]["ipa"],
            "uk": report[word]["en-gb"]["ipa"],
        }
        unresolved = ipa_data["unresolved"]
        if isinstance(unresolved, dict):
            unresolved.pop(word, None)
        elif word in unresolved:
            unresolved.remove(word)
    ipa_path.write_text(json.dumps(ipa_data, ensure_ascii=False, separators=(",", ":")), "utf-8")
    manifest_data["clipsPerRegion"] = len(words)
    manifest.write_text(json.dumps(manifest_data, ensure_ascii=False, separators=(",", ":")), "utf-8")
    # Original full integrity verification ran before this patch. Keep every
    # verification condition; adjust only its pinned expected count by the
    # exact number of newly generated, explicitly requested entries.
    verifier = ROOT / "source/tools/verify_audio_ipa.py"
    verify_text = verifier.read_text("utf-8")
    count_anchor = "assert len(words) == 2203,"
    assert verify_text.count(count_anchor) == 1
    assert len(old_words) == 2203
    verifier.write_text(verify_text.replace(count_anchor, f"assert len(words) == {len(words)},", 1), "utf-8")
    assert len(changed) == 2 * len(report)
    assert "do" in report and "hi" in report
    print("PASS: regenerated", len(report), "whole-word recordings per accent; existing IDs unchanged; added " + str(len(added)) + " missing entries")
    import os
    if os.environ.get("RUNNER_TEMP"):
        (Path(os.environ["RUNNER_TEMP"]) / "wq204-pronunciation-report.json").write_text(
            json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")

if __name__ == "__main__":
    main()
