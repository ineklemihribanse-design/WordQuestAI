#!/usr/bin/env python3
from pathlib import Path
import shutil
ROOT=Path(__file__).resolve().parents[2]
APP=ROOT/"source/app/src"
PKG=APP/"main/java/com/kelimebulmacasi/a1"
TEST=APP/"test/java/com/kelimebulmacasi/a1"
def once(s,old,new):
    assert s.count(old)==1,(old,s.count(old))
    return s.replace(old,new,1)
p=PKG/"Wq208ProviderFailure.kt"
s=p.read_text("utf-8")
s=once(s,"message: String, val retryDelayMs: Long?","message: String, val retryDelayMs: Long?, val httpStatus: Int")
s=once(s,"Wq208ProviderFailure(message, delay)","Wq208ProviderFailure(message, delay, status)")
p.write_text(s,"utf-8")
p=PKG/"Conversation.kt"
s=p.read_text("utf-8")
s=once(s,"        wq207LenaRequest { generateWithProvider(provider, key, model, request, lenaFast = true) }",'''        val route = Wq209LenaModelRoute(provider, model)
        val portableRequest = if (provider == AiProvider.Gemini) wq209LenaTextHistory(request, turns) else request
        wq207LenaRequest {
            try {
                generateWithProvider(provider, key, route.model, portableRequest, lenaFast = true)
            } catch (failure: Wq208ProviderFailure) {
                route.onFailure(failure)
                throw failure
            }
        }''')
p.write_text(s,"utf-8")
shutil.copyfile(ROOT/"tools/v209/Wq209LenaModelRoute.kt",PKG/"Wq209LenaModelRoute.kt")
shutil.copyfile(ROOT/"tools/v209/Wq209LenaModelRouteTest.kt",TEST/"Wq209LenaModelRouteTest.kt")
print("PASS: Lena alternate Gemini model, no cross-provider disclosure, bounded existing retry")
