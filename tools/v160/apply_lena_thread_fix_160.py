#!/usr/bin/env python3
"""v1.0.60: fix the exact IllegalArgumentException after Lena receives AI response.

AiSession.reply() saves a turn then persists the legacy "ethan_thread_ids"
thread visibility marker. TeacherStore.setSetting used to allow ONLY avatar,
notes, personality; its require() caused a post-response
IllegalArgumentException and obscured a successfully fetched AI reply.

Do not touch protected credentials, change the database schema or AI provider,
or remove old teacher conversations. Keep the legacy marker namespace intact.
"""
from pathlib import Path
import shutil

ROOT = Path(__file__).resolve().parents[2]
APP = ROOT / "source/app"
PKG = APP / "src/main/java/com/kelimebulmacasi/a1"
TESTS = APP / "src/test/java/com/kelimebulmacasi/a1"
GRADLE = APP / "build.gradle.kts"
SRC = ROOT / "tools/v160"

def once(s, before, after, label):
    count = s.count(before)
    if count != 1:
        raise AssertionError(f"{label}: expected one occurrence, found {count}")
    return s.replace(before, after, 1)

def main():
    g = GRADLE.read_text("utf-8")
    assert g.count("versionCode = 84") == 1
    assert g.count('versionName = "1.0.59-lena-saved-key"') == 1
    assert 'applicationIdSuffix = ".fresh"' in g

    ai = (PKG / "AiSession.kt").read_text("utf-8")
    assert 'store?.setSetting("ethan_thread_ids",' in ai
    assert 'val savedId = activeStore.append(activeThread, answer, level)' in ai
    assert 'if (ethanThreadIds.add(savedId)) persistEthanThreadIds()' in ai

    f = PKG / "TeacherStore.kt"
    s = f.read_text("utf-8")
    s = once(
        s,
        'require(name in setOf("avatar", "notes", "personality"))',
        'require(name in setOf("avatar", "notes", "personality", "ethan_thread_ids"))',
        "persisted thread-id allowlist",
    )
    f.write_text(s, "utf-8")

    TESTS.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(SRC / "LenaThreadPersistenceRegressionTest.kt",
                    TESTS / "LenaThreadPersistenceRegressionTest.kt")

    g = once(g, "versionCode = 84", "versionCode = 85", "versionCode")
    g = once(g, 'versionName = "1.0.59-lena-saved-key"',
             'versionName = "1.0.60-lena-thread-persistence"', "versionName")
    GRADLE.write_text(g, "utf-8")

    assert (PKG / "Teacher2DPortrait.kt").is_file(), "Alex must remain"
    assert (PKG / "ArenaApprovedHome.kt").is_file(), "Arena must remain"
    assert (PKG / "VocabularyNeonHome.kt").is_file(), "Word Memory must remain"
    assert (PKG / "LenaIsolatedVoiceTeacherActivity.kt").is_file(), "Lena must remain"
    assert (PKG / "TeacherCredentialBridgeProvider.kt").is_file(), "saved-key bridge must remain"
    print("v1.0.60: fixed post-AI-response IllegalArgumentException in TeacherStore.setSetting.")
    print("Legacy existing thread marker still named ethan_thread_ids; no migrations.")
    print("No changes to stored credentials, chats, Alex, Lena GLB, Arena, Word Memory or signer.")

if __name__ == "__main__":
    main()
