package com.kelimebulmacasi.a1

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

/** Never performs a silent installation. Uses GitHub public releases and Android's native installer. */
internal class AppUpdater(private val context: Context, private val scope: CoroutineScope) {
    private val prefs = context.getSharedPreferences("wordquest_github_updates", Context.MODE_PRIVATE)
    private val dir = File(context.cacheDir, "updates")
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(35, TimeUnit.SECONDS)
        .followSslRedirects(false)
        .addNetworkInterceptor { chain ->
            require(UpdateProtocol.allowedUrl(chain.request().url.toString())) { "Güvensiz indirme yönlendirmesi." }
            chain.proceed(chain.request())
        }.build()
    @Suppress("DEPRECATION")
    private val currentCode: Long get() {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
    }
    private val currentName: String get() = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"

    var automatic by mutableStateOf(prefs.getBoolean("automatic", true)); private set
    var available by mutableStateOf<UpdateOffer?>(null); private set
    var showPrompt by mutableStateOf(false); private set
    var status by mutableStateOf("Henüz kontrol edilmedi"); private set
    var lastChecked by mutableStateOf(prefs.getLong("last_success", 0L)); private set
    var checking by mutableStateOf(false); private set
    var downloading by mutableStateOf(false); private set
    var progress by mutableStateOf(0); private set
    var installPending by mutableStateOf(false); private set
    private var activeDownload: Job? = null
    @Volatile private var activeCall: Call? = null
    @Volatile private var cancelRequested = false
    private var permissionReturnPending = false

    init { cleanupAfterUpgrade() }

    fun installedVersion(): String = currentName
    fun lastCheckLabel(): String = if (lastChecked <= 0) "Henüz yapılmadı"
        else DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(lastChecked))

    fun changeAutomatic(enabled: Boolean) {
        automatic = enabled
        prefs.edit().putBoolean("automatic", enabled).apply()
    }

    fun dismissOffer() { showPrompt = false }

    fun checkIfDue() {
        val now = System.currentTimeMillis()
        val due = if (prefs.getLong("last_success", 0L) > 0) UpdateProtocol.CHECK_INTERVAL_MS
            else UpdateProtocol.FAILURE_RETRY_MS
        if (automatic && now - prefs.getLong("last_attempt", 0L) >= due) check(force = false)
    }

    fun check(force: Boolean = true) {
        if (checking || downloading) return
        if (!force && !automatic) return
        checking = true
        status = "Yeni sürüm kontrol ediliyor…"
        prefs.edit().putLong("last_attempt", System.currentTimeMillis()).apply()
        scope.launch {
            try {
                val offer = withContext(Dispatchers.IO) { getOffer() }
                lastChecked = System.currentTimeMillis()
                prefs.edit().putLong("last_success", lastChecked).apply()
                available = offer
                status = if (offer == null) "Uygulamanız güncel" else "Yeni sürüm: ${offer.versionName}"
                if (offer != null) showPrompt = true
            } catch (e: Exception) {
                status = if (e is IOException) "İnternet bağlantısı yok veya GitHub'a ulaşılamıyor. Uygulama çalışmaya devam eder."
                    else "Güncelleme denetlenemedi: ${e.message.orEmpty().take(180)}"
            } finally { checking = false }
        }
    }

    private fun fetchText(url: String): String {
        require(UpdateProtocol.allowedUrl(url, initial = true)) { "Güvenli GitHub bağlantısı değil." }
        val req = Request.Builder().url(url).header("User-Agent", "WordQuestAI-Updater")
            .header("Accept", "application/vnd.github+json").build()
        client.newCall(req).execute().use { response ->
            if (!response.isSuccessful) throw IOException("GitHub HTTP ${response.code}")
            val body = response.body ?: throw IOException("GitHub cevabı boş")
            if (body.contentLength() > 1024 * 1024) throw IOException("Sürüm bilgisi çok büyük")
            val text = body.byteStream().bufferedReader().use { it.readText() }
            if (text.length > 1024 * 1024) throw IOException("Sürüm bilgisi çok büyük")
            return text
        }
    }

    private fun getOffer(): UpdateOffer? {
        val api = "https://api.github.com/repos/${UpdateProtocol.REPOSITORY}/releases/latest"
        val metadata = fetchText(api)
        val tag = org.json.JSONObject(metadata).getString("tag_name")
        val jsonUrl = UpdateProtocol.releaseFileUrl(tag, UpdateProtocol.MANIFEST_NAME)
        return UpdateProtocol.parseRelease(metadata, fetchText(jsonUrl), currentCode, context.packageName)
    }

    private fun destination(offer: UpdateOffer): File = File(dir,
        "WordQuestAI-${offer.versionCode}-${offer.sha256.take(12)}.apk")

    private fun fileHash(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(128 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
    }

    private fun verified(file: File, offer: UpdateOffer): Boolean = file.isFile &&
        file.length() == offer.sizeBytes && UpdateProtocol.equalsSha256(offer.sha256, fileHash(file))

    private fun cleanupAfterUpgrade() {
        dir.mkdirs()
        dir.listFiles()?.filter { it.name.endsWith(".part") }?.forEach { it.delete() }
        val last = prefs.getLong("install_version", 0L)
        if (last in 1..currentCode) {
            dir.listFiles()?.filter { it.extension == "apk" }?.forEach { it.delete() }
            prefs.edit().remove("install_version").apply()
            status = "Güncelleme tamamlandı, geçici APK temizlendi."
        } else {
            // Keep only the last verified candidate when a user cancels installation.
            val keep = prefs.getString("install_file", null)
            dir.listFiles()?.filter { it.extension == "apk" && it.name != keep }?.forEach { it.delete() }
        }
    }

    fun updateNow(activity: Activity) {
        val offer = available ?: return
        if (downloading || checking) return
        showPrompt = false
        cancelRequested = false
        downloading = true
        progress = 0
        status = "APK hazırlanıyor…"
        activeDownload = scope.launch {
            try {
                val apk = withContext(Dispatchers.IO) { download(offer) { percent ->
                    scope.launch { progress = percent }
                } }
                if (cancelRequested) throw CancellationException("İndirme iptal edildi")
                verifyPackage(apk, offer)
                downloading = false
                status = "İndirme doğrulandı. Android kurulum onayı gerekiyor."
                installPending = true
                prefs.edit().putLong("install_version", offer.versionCode)
                    .putString("install_file", apk.name).apply()
                requestInstall(activity, apk)
            } catch (e: CancellationException) {
                downloading = false
                status = "İndirme iptal edildi."
            } catch (e: Exception) {
                downloading = false
                installPending = false
                status = "Güncelleme kurulamadı: ${e.message.orEmpty().take(180)}"
            }
        }
    }

    fun cancelDownload() {
        cancelRequested = true
        activeCall?.cancel()
        activeDownload?.cancel()
        downloading = false
        status = "İndirme iptal edildi. Mevcut uygulama korunuyor."
    }

    private fun download(offer: UpdateOffer, onPercent: (Int) -> Unit): File {
        dir.mkdirs()
        val target = destination(offer)
        if (verified(target, offer)) { onPercent(100); return target }
        target.delete()
        val temp = File(dir, "${target.name}.part")
        temp.delete()
        try {
            val req = Request.Builder().url(offer.apkUrl).header("User-Agent", "WordQuestAI-Updater").build()
            val call = client.newCall(req)
            activeCall = call
            call.execute().use { response ->
                if (!response.isSuccessful) throw IOException("APK HTTP ${response.code}")
                val body = response.body ?: throw IOException("APK içeriği yok")
                if (body.contentLength() > UpdateProtocol.MAX_APK_BYTES) throw IOException("APK çok büyük")
                val digest = MessageDigest.getInstance("SHA-256")
                var total = 0L
                var sentPercent = -1
                FileOutputStream(temp).use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(128 * 1024)
                        while (true) {
                            if (cancelRequested || Thread.currentThread().isInterrupted) throw IOException("İndirme durduruldu")
                            val n = input.read(buffer)
                            if (n < 0) break
                            total += n
                            if (total > offer.sizeBytes) throw IOException("APK boyutu aşıldı")
                            digest.update(buffer, 0, n)
                            output.write(buffer, 0, n)
                            val percent = ((total * 100L) / offer.sizeBytes).toInt()
                            if (percent != sentPercent) { onPercent(percent); sentPercent = percent }
                        }
                    }
                    output.fd.sync()
                }
                val actual = digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
                if (total != offer.sizeBytes || !UpdateProtocol.equalsSha256(offer.sha256, actual))
                    throw IOException("APK eksik veya SHA-256 yanlış")
            }
            if (!temp.renameTo(target)) throw IOException("İndirilen APK taşınamadı")
            return target
        } finally { activeCall = null; temp.delete() }
    }

    @Suppress("DEPRECATION")
    private fun verifyPackage(file: File, offer: UpdateOffer) {
        val mgr = context.packageManager
        @Suppress("DEPRECATION")
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES
            else PackageManager.GET_SIGNATURES
        @Suppress("DEPRECATION")
        val incoming = mgr.getPackageArchiveInfo(file.absolutePath, flags)
            ?: throw SecurityException("APK paket bilgisi okunamadı")
        require(incoming.packageName == context.packageName) { "APK paket adı farklı" }
        val incomingCode = if (Build.VERSION.SDK_INT >= 28) incoming.longVersionCode
            else incoming.versionCode.toLong()
        require(incomingCode == offer.versionCode && incomingCode > currentCode) {
            "APK sürüm kodu hatalı veya eski"
        }
        @Suppress("DEPRECATION")
        val installed = mgr.getPackageInfo(context.packageName, flags)
        val originalSignatures = if (Build.VERSION.SDK_INT >= 28)
            installed.signingInfo?.apkContentsSigners else installed.signatures
        val newSignatures = if (Build.VERSION.SDK_INT >= 28)
            incoming.signingInfo?.apkContentsSigners else incoming.signatures
        require(!originalSignatures.isNullOrEmpty() && !newSignatures.isNullOrEmpty()) {
            "İmza sertifikası denetlenemiyor; kurulum durduruldu"
        }
        require(originalSignatures.map { UpdateProtocol.sha256(it.toByteArray()) }.toSet() ==
            newSignatures.map { UpdateProtocol.sha256(it.toByteArray()) }.toSet()) {
            "İmza sertifikası mevcut uygulamayla eşleşmiyor"
        }
    }

    private fun requestInstall(activity: Activity, apk: File) {
        if (!context.packageManager.canRequestPackageInstalls()) {
            permissionReturnPending = true
            status = "Bu kaynaktan uygulama yükleme iznini ver. Ardından Android kurulum ekranı açılacak."
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"))
            activity.startActivity(intent)
            return
        }
        permissionReturnPending = false
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.update_apks", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        activity.startActivity(intent)
        status = "Android kurulum onayı açıldı. İptal edersen güncellemeyi yeniden başlatabilirsin."
    }

    fun onResume(activity: Activity) {
        if (!permissionReturnPending || !context.packageManager.canRequestPackageInstalls()) return
        permissionReturnPending = false
        val offer = available ?: return
        scope.launch {
            val file = destination(offer)
            val okay = withContext(Dispatchers.IO) { verified(file, offer) }
            if (okay) requestInstall(activity, file)
            else status = "Önceki indirme bulunamadı; yeniden dene."
        }
    }
}
