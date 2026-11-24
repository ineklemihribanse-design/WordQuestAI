#!/usr/bin/env python3
"""Apply only Lena connection responsiveness changes to the verified assembled source."""
from pathlib import Path
import re
import shutil

ROOT=Path(__file__).resolve().parents[2]
APP=ROOT/"source/app/src"
PKG=APP/"main/java/com/kelimebulmacasi/a1"
TEST=APP/"test/java/com/kelimebulmacasi/a1"
def once(s,old,new,label):
    if s.count(old)!=1:
        raise AssertionError(f"{label}: expected one anchor, found {s.count(old)}")
    return s.replace(old,new,1)

p=PKG/"AiProviders.kt"
s=p.read_text("utf-8")
s=once(s,"originalRequest: JSONObject): String = withContext(Dispatchers.IO)",
       "originalRequest: JSONObject, lenaFast: Boolean = false): String = withContext(Dispatchers.IO)","provider signature")
s=once(s,"connection.connectTimeout = 15_000","connection.connectTimeout = if (lenaFast) 6_000 else 15_000","connect")
s=once(s,"connection.readTimeout = 45_000","connection.readTimeout = if (lenaFast) 14_000 else 45_000","read")
p.write_text(s,"utf-8")

p=PKG/"Conversation.kt"
s=p.read_text("utf-8")
old="""    val response = generateWithProvider(provider, key, model,
        conversationRequest(turns, message, language, level, mode, notes, learningContext, learnerName, avatarPrompt))"""
new="""    val request = conversationRequest(turns, message, language, level, mode, notes, learningContext, learnerName, avatarPrompt)
    val response = if (avatarPrompt.contains("Lena", ignoreCase = true)) {
        wq207LenaRequest { generateWithProvider(provider, key, model, request, lenaFast = true) }
    } else {
        generateWithProvider(provider, key, model, request)
    }"""
s=once(s,old,new,"Lena-only request routing")
p.write_text(s,"utf-8")

p=PKG/"ConversationScreen.kt"
s=p.read_text("utf-8")
s=once(s,"    var sending by remember { mutableStateOf(false) }",
       "    var sending by remember { mutableStateOf(false) }\n    var replyJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }","chat job")
start=s.index("    fun send() {")
end=s.index("val turn = ai.reply(",start)
part=s[start:end]
part=once(part,"scope.launch {","replyJob = scope.launch {","cancel actual send job")
s=s[:start]+part+s[end:]
s=once(s,'if (sending) item(key = "pending") {',
       '''if (sending) item(key = "pending") {
                    TextButton(onClick = { replyJob?.cancel() }) {
                        Text("Yanıtı iptal et")
                    }''',"cancel in pending item")
s=s.replace('"Yanıt hazırlanıyor…"','"Lena yazıyor…"').replace('"Yanıt alınıyor…"','"Lena yazıyor…"')
p.write_text(s,"utf-8")

for filename in ("LiveTeacherScreen.kt","LenaIsolatedVoiceTeacherActivity.kt"):
    p=PKG/filename
    s=p.read_text("utf-8")
    s=s.replace('"Lena yanıtını hazırlıyor…"','"Lena yazıyor…"')
    s=s.replace('"Lena yanıt hazırlıyor"','"Lena yazıyor…"')
    s=s.replace('"Yanıt alınıyor…"','"Lena yazıyor…"')
    p.write_text(s,"utf-8")

shutil.copyfile(ROOT/"tools/v207/Wq207LenaRequest.kt",PKG/"Wq207LenaRequest.kt")
TEST.mkdir(parents=True,exist_ok=True)
shutil.copyfile(ROOT/"tools/v207/Wq207LenaRequestTest.kt",TEST/"Wq207LenaRequestTest.kt")
print("PASS: Lena bounded response, chat cancellation and legacy provider timeout preservation")
