#!/usr/bin/env python3
"""Apply narrowly scoped updater changes to assembled 1.0.115. Fail on unknown source."""
from pathlib import Path
import shutil
ROOT=Path(__file__).resolve().parents[2]
P=ROOT/'source/app/src/main/java/com/kelimebulmacasi/a1'
T=ROOT/'source/app/src/test/java/com/kelimebulmacasi/a1'
def once(s,a,b):
    if s.count(a)!=1:raise ValueError("Unexpected updater anchor: "+a[:150])
    return s.replace(a,b,1)
p=P/'UpdateProtocol.kt';s=p.read_text()
s=once(s,'MAX_APK_BYTES = 250L * 1024L * 1024L','MAX_APK_BYTES = 512L * 1024L * 1024L')
s=once(s,'    val apkUrl: String,\n) {','    val apkUrl: String,\n    val delta: WqDeltaOffer? = null,\n) {')
s=once(s,'    val sizeLabel: String get() =','    val fullSizeLabel: String get() =')
s=once(s,'sizeBytes / (1024.0 * 1024.0))','sizeBytes / 1_000_000.0)\n    val sizeLabel: String get() = delta?.let { "%.1f MB (fark)".format(java.util.Locale.ROOT,it.size / 1_000_000.0) } ?: fullSizeLabel\n    val savingPercent: Int get() = delta?.let { ((sizeBytes-it.size)*100L/sizeBytes).toInt() } ?: 0')
a=P/'AppUpdater.kt';u=a.read_text()
u=once(u,'        return UpdateProtocol.parseRelease(metadata, fetchText(jsonUrl), currentCode, context.packageName)', '''        val manifest = fetchText(jsonUrl)
        val installedCode = currentCode
        val offer = UpdateProtocol.parseRelease(metadata, manifest, installedCode, context.packageName) ?: return null
        val app = context.applicationInfo
        if (!app.splitSourceDirs.isNullOrEmpty()) return offer
        val base = File(app.sourceDir)
        return offer.copy(delta = WqDeltaMetadata.select(metadata, manifest, offer, installedCode, base))''')
u=once(u,'withContext(Dispatchers.IO) { download(offer) { percent ->','withContext(Dispatchers.IO) { downloadPreferred(offer) { percent ->')
u=once(u,'            } catch (e: Exception) {\n                downloading = false\n                installPending = false', '''            } catch (e: WqDeltaFallback) {
                downloading = false
                installPending = false
                if (cancelRequested) {
                    status = "İndirme iptal edildi. Mevcut uygulama korunuyor."
                } else {
                    available = offer.copy(delta = null)
                    progress = 0
                    status = "Fark paketi uygulanamadı. Tam APK için yeniden onay gerekli."
                    showPrompt = true
                }
            } catch (e: Exception) {
                downloading = false
                installPending = false''')
u=once(u,'    private fun download(offer: UpdateOffer, onPercent: (Int) -> Unit): File {', '''    private fun downloadPreferred(offer: UpdateOffer, onPercent: (Int) -> Unit): File {
        val delta = offer.delta ?: return download(offer, onPercent)
        dir.mkdirs()
        val target = destination(offer)
        if (verified(target, offer)) { onPercent(100); return target }
        target.delete()
        val patch = File(dir, "wq-"+offer.versionCode+"-"+delta.hash.take(12)+".wqd.part")
        patch.delete()
        try {
            if (cancelRequested) throw CancellationException("İptal edildi")
            val base = File(context.applicationInfo.sourceDir)
            if (dir.usableSpace < offer.sizeBytes + delta.size + 65536L)
                throw IOException("Yetersiz depolama")
            if (base.length() != delta.baseSize || !UpdateProtocol.equalsSha256(delta.baseHash,WqDeltaApplier.sha(base)))
                throw IOException("Kurulu APK fark paketiyle eşleşmiyor")
            downloadFile(delta.url,delta.size,delta.hash,patch) { onPercent(it * 90 / 100) }
            WqDeltaApplier.apply(base,patch,target,delta.baseSize,delta.size,offer.sizeBytes,
                delta.baseHash,delta.hash,offer.sha256) { cancelRequested }
            if (cancelRequested) throw CancellationException("İptal edildi")
            onPercent(100)
            return target
        } catch (e: CancellationException) {
            target.delete()
            throw e
        } catch (e: Exception) {
            target.delete()
            if (cancelRequested) throw CancellationException("İptal edildi")
            throw WqDeltaFallback(e)
        } finally { patch.delete() }
    }

    private fun download(offer: UpdateOffer, onPercent: (Int) -> Unit): File {''')
old='''        target.delete()
        val temp = File(dir, "${target.name}.part")
        temp.delete()'''
new='''        return downloadFile(offer.apkUrl,offer.sizeBytes,offer.sha256,target,onPercent)
    }

    private fun downloadFile(url: String, expectedSize: Long, expectedHash: String,
                             target: File, onPercent: (Int) -> Unit): File {
        require(UpdateProtocol.allowedUrl(url,initial=true))
        target.delete()
        val temp = File(dir, "${target.name}.part")
        temp.delete()'''
u=once(u,old,new)
start=u.index('    private fun downloadFile(');end=u.index('    @Suppress("DEPRECATION")\n    private fun verifyPackage',start)
seg=u[start:end].replace('url(offer.apkUrl)','url(url)').replace('offer.sizeBytes','expectedSize').replace('offer.sha256','expectedHash')
assert 'offer.' not in seg
u=u[:start]+seg+u[end:]
ui=P/'UpdateUi.kt';v=ui.read_text()
v=once(v,'                    Text("İndirme boyutu: ${offer.sizeLabel}")','''                    Text("İndirme boyutu: ${offer.sizeLabel}")
                    if (offer.delta != null) Text("Tam APK: ${offer.fullSizeLabel} · %${offer.savingPercent} tasarruf. Fark paketi başarısız olursa tam indirme için tekrar onay istenir.")''')
v=v.replace('APK indiriliyor: %','Güncelleme hazırlanıyor: %')
for file,content in [(p,s),(a,u),(ui,v)]:file.write_text(content)
for file in ('WqDeltaMetadata.kt',):shutil.copyfile(ROOT/'tools/delta_android'/file,P/file)
shutil.copyfile(ROOT/'tools/delta_android/WqDeltaApplier.java',P/'WqDeltaApplier.java')
T.mkdir(parents=True,exist_ok=True)
shutil.copyfile(ROOT/'tools/delta_android/WqDeltaMetadataTest.kt',T/'WqDeltaMetadataTest.kt')
print('PASS narrow updater patch; original package/signature/install path preserved')
