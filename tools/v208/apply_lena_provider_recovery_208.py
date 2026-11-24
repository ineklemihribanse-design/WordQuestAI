#!/usr/bin/env python3
from pathlib import Path
import shutil

ROOT=Path(__file__).resolve().parents[2]
APP=ROOT/"source/app/src"
PKG=APP/"main/java/com/kelimebulmacasi/a1"
TEST=APP/"test/java/com/kelimebulmacasi/a1"
def once(s,old,new):
    assert s.count(old)==1, (old,s.count(old))
    return s.replace(old,new,1)

p=PKG/"AiProviders.kt"
s=p.read_text("utf-8")
s=once(s,"        if (status !in 200..299) {",'''        if (status !in 200..299) {
            if (lenaFast && (status == 429 || status == 503)) {
                val detail = runCatching {
                    connection.errorStream?.bufferedReader()?.use { reader ->
                        val buffer = CharArray(3000)
                        val count = reader.read(buffer)
                        if (count > 0) String(buffer, 0, count) else ""
                    }
                }.getOrNull().orEmpty()
                val info = runCatching { JSONObject(detail).optJSONObject("error") }.getOrNull()
                throw wq208ProviderFailure(provider.label, status,
                    info?.optString("code").orEmpty(), info?.optString("type").orEmpty(),
                    connection.getHeaderField("Retry-After"))
            }''')
p.write_text(s,"utf-8")
p=PKG/"Wq207LenaRequest.kt"
s=p.read_text("utf-8")
s=once(s,"                val outcome = runCatching {",'''                val outcome = runCatching {
                    val start = System.nanoTime()''')
s=once(s,'''                        // Connection refused before submission: retry once only.
                        // Never retry a timeout, HTTP response, invalid key or paid request.
                        delay(250)
                        request()
                    }''','''                        // Refused connection: at most one retry.
                        delay(250)
                        request()
                    } catch (failure: Wq208ProviderFailure) {
                        val pause = failure.retryDelayMs ?: throw failure
                        val elapsed = (System.nanoTime() - start) / 1_000_000
                        // Retrying is useful only if enough of the original budget remains.
                        if (elapsed + pause + 7_000 >= timeoutMs) throw failure
                        delay(pause)
                        request()
                    }''')
s=once(s,"                continuation.resumeWith(outcome)",'''                val failure = outcome.exceptionOrNull()
                if (failure is Wq208ProviderFailure) {
                    continuation.resumeWith(Result.failure(WordRequestException(failure.message.orEmpty())))
                } else {
                    continuation.resumeWith(outcome)
                }''')
p.write_text(s,"utf-8")
shutil.copyfile(ROOT/"tools/v208/Wq208ProviderFailure.kt",PKG/"Wq208ProviderFailure.kt")
shutil.copyfile(ROOT/"tools/v208/Wq208ProviderFailureTest.kt",TEST/"Wq208ProviderFailureTest.kt")
print("PASS: Lena HTTP 429/503 classified; retry limited to one within original deadline")
