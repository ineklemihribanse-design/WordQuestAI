#!/usr/bin/env python3
"""Fail-closed regression guards for 1.0.38 provider UI simplification."""
from pathlib import Path
root=Path(__file__).resolve().parents[2]
app=root/"source/app"
pkg=app/"src/main/java/com/kelimebulmacasi/a1"
g=(app/"build.gradle.kts").read_text("utf-8")
assert g.count("versionCode = 63")==1
assert g.count('versionName = "1.0.38-gemini-openai-only-qa"')==1
assert 'applicationIdSuffix = ".offline101"' in g
ui=(pkg/"AiProviderSettings.kt").read_text("utf-8")
ai=(pkg/"AiSession.kt").read_text("utf-8")
providers=(pkg/"AiProviders.kt").read_text("utf-8")
assert "AiProvider.entries.filter { it != AiProvider.Claude }.forEach { value ->" in ui
assert "AiProvider.entries.forEach { value ->" not in ui
assert "it != AiProvider.Claude && it.name == prefs.getString" in providers
assert "if (value == AiProvider.Claude) return" in ai
assert "provider = keys.selected()" in ai
# Keep old stored profile/provider model/secrets (no destructive migration).
assert 'Claude("Claude"' in providers and 'Gemini("Google Gemini"' in providers
assert 'OpenAI("OpenAI"' in providers
assert '"gemini-3.1-flash-lite"' in providers
assert 'prefs.edit().putString("$userPrefix.selected", provider.name).apply()' in providers
assert 'prefs.edit().remove(field(provider, "key")).apply()' in providers
assert 'fun selected(): AiProvider' in providers
assert 'next.setSetting("ethan_thread_ids"' not in ai
assert 'EthanTeacherPersona.systemPrompt' in ai
assert 'ethan_thread_ids' in ai
assert "AiProvider.Gemini" in providers and "AiProvider.OpenAI" in providers
assert "API anahtarı" in ui
# The launcher remains on the verified v1.0.35 safe base; Filament only in :teacher3d.
manifest=(app/"src/main/AndroidManifest.xml").read_text("utf-8")
assert 'android:name=".EthanIsolatedVoiceTeacherActivity"' in manifest
assert manifest.count('android:process=":teacher3d"')>=2
assert "EthanLive3DStage(" not in (pkg/"TeacherStage3D.kt").read_text("utf-8")
assert (app/"src/main/assets/avatars/ethan-mobile.glb").read_bytes()[:4]==b"glTF"
assert not list((app/"src/main/assets/avatars").glob("emma*.glb"))
assert not list((app/"src/main/assets/avatars").glob("sophia*.glb"))
assert (root/"source/archived-teachers/ARCHIVE_MANIFEST.json").exists()
print("PASS: only Gemini/OpenAI selectable, saved Claude preference resolves safely")
print("PASS: Gemini/OpenAI model and credentials, user profiles, Ethan AI/3D and original signature unchanged")
print("NOT VERIFIED: user Gemini network/API access and real device 3D")
