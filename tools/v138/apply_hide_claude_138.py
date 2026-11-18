#!/usr/bin/env python3
"""v1.0.38: remove the Claude option without changing user data or launch safety.

The legacy enum and decoder are kept so older encrypted preferences still
deserialize; Claude cannot be shown, selected or reactivated by stale profile
settings. Gemini/OpenAI providers and secrets remain untouched.
"""
from pathlib import Path

root = Path(__file__).resolve().parents[2]
app = root / "source/app"
pkg = app / "src/main/java/com/kelimebulmacasi/a1"
gradle = app / "build.gradle.kts"

def replace_once(content, old, new, name):
    n=content.count(old)
    assert n==1, f"{name}: source changed (anchor seen {n} times)"
    return content.replace(old,new,1)

g=gradle.read_text("utf-8")
assert g.count("versionCode = 62")==1
assert g.count('versionName = "1.0.37-ethan-isolated-3d-voice-qa"')==1
assert 'applicationIdSuffix = ".offline101"' in g

settings=pkg/"AiProviderSettings.kt"
s=settings.read_text("utf-8")
s=replace_once(s,
    "AiProvider.entries.forEach { value ->",
    "AiProvider.entries.filter { it != AiProvider.Claude }.forEach { value ->",
    "Only Gemini and OpenAI in AI settings UI")
settings.write_text(s,"utf-8")

providers=pkg/"AiProviders.kt"
p=providers.read_text("utf-8")
p=replace_once(p,
    '''fun selected(): AiProvider = AiProvider.entries.firstOrNull {
        it.name == prefs.getString("$userPrefix.selected", null)
    } ?: AiProvider.Gemini''',
    '''fun selected(): AiProvider = AiProvider.entries.firstOrNull {
        // Historical Claude choices are left encrypted on disk, never made active.
        it != AiProvider.Claude && it.name == prefs.getString("$userPrefix.selected", null)
    } ?: AiProvider.Gemini''',
    "Read-only legacy Claude -> Gemini selection fallback")
assert 'Gemini("Google Gemini", "gemini-3.1-flash-lite"' in p
assert 'OpenAI("OpenAI"' in p
assert 'Claude("Claude"' in p, "Keep enum for backwards-compatible encrypted credentials"
providers.write_text(p,"utf-8")

ai=pkg/"AiSession.kt"
a=ai.read_text("utf-8")
a=replace_once(a,
    '''    internal fun chooseProvider(value: AiProvider) {
        credentials?.choose(value)''',
    '''    internal fun chooseProvider(value: AiProvider) {
        // Disabled provider cannot be re-selected from an older component.
        if (value == AiProvider.Claude) return
        credentials?.choose(value)''',
    "Reject legacy Claude selection without changing stored data")
assert 'provider = keys.selected()' in a, "Selected provider must still bind to per-profile keys"
assert 'next.setSetting("ethan_thread_ids"' not in a, "Never reintroduce launcher profile migration"
ai.write_text(a,"utf-8")

g=g.replace("versionCode = 62","versionCode = 63",1)
g=g.replace('versionName = "1.0.37-ethan-isolated-3d-voice-qa"',
            'versionName = "1.0.38-gemini-openai-only-qa"',1)
gradle.write_text(g,"utf-8")
print("QA 1.0.38 code63: user-visible provider choices Google Gemini and OpenAI only")
print("Legacy encrypted Claude credentials remain on disk, but cannot be chosen")
print("No changes to Gemini model/keys/network code, Ethan, 3D isolation or US/UK audio")
