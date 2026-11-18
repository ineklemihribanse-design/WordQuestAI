#!/usr/bin/env python3
"""Emergency crash-containment for Android teacher entry, version 1.0.29.

Older v1.0.28 eagerly initialized SceneView/Filament with unapproved Rocketbox
GLBs on entering the conversation screen. These assets were not device-tested;
native engine aborts cannot be caught with Kotlin runCatching. Default to the
existing 2D live teacher, preserving microphone, voice, chat, API key and data.

One-session optional 3D opt-in remains for diagnostics, but is NEVER saved:
after a restart the assistant always opens safely. Do not claim this fixes
the native 3D root cause; the 3D source is retained for further testing.
"""
from pathlib import Path
root=Path(__file__).resolve().parents[2]
src=root/"source"
stage=src/"app/src/main/java/com/kelimebulmacasi/a1/TeacherStage3D.kt"
gradle=src/"app/build.gradle.kts"
s=stage.read_text(encoding="utf-8")

def one(text,old,new):
    count=text.count(old)
    assert count==1,(old[:110],count)
    return text.replace(old,new,1)

s=one(s,
      'import androidx.compose.runtime.mutableIntStateOf\n',
      'import androidx.compose.runtime.mutableIntStateOf\nimport androidx.compose.runtime.mutableStateOf\n')
s=one(s,
      '    var cameraPreset by remember { mutableIntStateOf(0) }\n',
      '    var cameraPreset by remember { mutableIntStateOf(0) }\n'
      '    // DO NOT persist an experimental 3D opt-in. After any native crash,\n'
      '    // returning to this screen must always use the safe live 2D teacher.\n'
      '    var experimental3D by remember { mutableStateOf(false) }\n')
s=one(s,
      '        key(asset, resetCounter, cameraPreset) {\n',
      '        if (!experimental3D) {\n'
      '            // 2D fallback runs before rememberEngine/ModelLoader/native\n'
      '            // Filament initialization. A GLB crash cannot occur on entry.\n'
      '            TeacherFallback2D(\n'
      '                actor, speaking, listening, thinking,\n'
      '                speechRate, Modifier.fillMaxSize(),\n'
      '            )\n'
      '        } else {\n'
      '          key(asset, resetCounter, cameraPreset) {\n')
s=one(s,
      '        }\n        Row(\n            modifier = Modifier\n',
      '          }\n        }\n        Row(\n            modifier = Modifier\n')
old='''            listOf("Tam Boy", "Bel", "Yüz").forEachIndexed { index, caption ->
                TextButton(onClick = { cameraPreset = index; resetCounter++ }) {
                    Text(
                        caption,
                        color = if (cameraPreset == index) Color(0xFFF2D9A5)
                                else Color.White,
                    )
                }
            }
            TextButton(onClick = { cameraPreset = 0; resetCounter++ }) {
                Text("↺", color = Color(0xFFF2D9A5))
            }
'''
new='''            TextButton(onClick = {
                // Deliberate opt-in, only for this session. A fresh screen
                // always uses 2D, even if 3D crashed on the previous visit.
                experimental3D = !experimental3D
                resetCounter++
            }) {
                Text(
                    if (experimental3D) "2D Güvenli" else "3D Dene (Beta)",
                    color = Color(0xFFF2D9A5),
                )
            }
            if (experimental3D) {
                listOf("Tam Boy", "Bel", "Yüz").forEachIndexed { index, caption ->
                    TextButton(onClick = { cameraPreset = index; resetCounter++ }) {
                        Text(
                            caption,
                            color = if (cameraPreset == index) Color(0xFFF2D9A5)
                                    else Color.White,
                        )
                    }
                }
                TextButton(onClick = { cameraPreset = 0; resetCounter++ }) {
                    Text("↺", color = Color(0xFFF2D9A5))
                }
            }
'''
s=one(s,old,new)
assert 'if (!experimental3D)' in s
assert s.index('if (!experimental3D)')<s.index('val engine = rememberEngine()')
assert 'remember { mutableStateOf(false) }' in s
assert 'getSharedPreferences' not in s
stage.write_text(s,encoding="utf-8")

g=gradle.read_text(encoding="utf-8")
assert g.count("versionCode = 53")==1 and g.count('versionName = "1.0.28-human-teachers-preview"')==1
assert 'applicationIdSuffix = ".offline101"' in g
g=g.replace("versionCode = 53","versionCode = 54",1)
g=g.replace('versionName = "1.0.28-human-teachers-preview"',
            'versionName = "1.0.29-teacher-crash-safemode"',1)
gradle.write_text(g,encoding="utf-8")
print("1.0.29 safe teacher: immediate 2D fallback; optional session-only native 3D; code54")
