package com.kelimebulmacasi.a1

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.net.URL
import java.security.MessageDigest
import java.security.Signature
import java.security.cert.CertificateFactory
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Data only: no APK, dex, scripts, native libraries, or external storage. */
internal object WqContentUpdates {
    private const val BASE = "https://raw.githubusercontent.com/ineklemihribanse-design/WordQuestAI/content-channel/"
    private const val MAX_FILE = 12L * 1024 * 1024
    private const val MAX_TOTAL = 96L * 1024 * 1024
    private val safePath = Regex("(arena_v120|arena_v122|new_game_198)/[a-zA-Z0-9_-]{1,80}\\.webp")
    private fun allowedPath(path:String)=safePath.matches(path)||path==Wq128LobbyDesign.PATH||path==Wq130WebBundle.PATH
    private fun extension(path:String)=when(path){Wq128LobbyDesign.PATH->".json";Wq130WebBundle.PATH->".html";else->".webp"}
    private val hashPattern = Regex("[a-f0-9]{64}")
    private val versionPattern = Regex("1\\.0\\.[0-9]{1,8}")
    @Volatile private var active: File? = null
    @Volatile private var activeVersion: String? = null
    private var started = false
    private var checking = false
    private val _status = MutableStateFlow("İçerik kontrolü hazırlanıyor…")
    val status: StateFlow<String> = _status.asStateFlow()
    private val _checking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _checking.asStateFlow()

    @Suppress("DEPRECATION")
    private fun core(c: Context): Pair<Int,String> {
        val info = c.packageManager.getPackageInfo(c.packageName,0)
        return info.versionCode to (info.versionName ?: "1.0.0")
    }
    private fun prefs(c: Context) = c.getSharedPreferences("wq_signed_content_v1",Context.MODE_PRIVATE)
    private fun root(c: Context) = File(c.noBackupFilesDir,"signed-content-v1")
    fun version(c: Context): String = activeVersion ?: core(c).second

    fun open(c: Context, path: String): InputStream {
        if (allowedPath(path)) {
            val f = active?.let { File(it,path) }
            if (f != null && f.isFile) {
                try { return f.inputStream() } catch (_: java.io.IOException) { }
            }
        }
        return c.assets.open(path)
    }

    @Synchronized fun start(c: Context) {
        if (started) return
        started = true
        val app = c.applicationContext ?: c
        val (code, _) = core(app)
        val p = prefs(app)
        val id = p.getString("active",null)
        if (p.getInt("core",-1)==code && id != null && hashPattern.matches(id)) {
            val dir = File(root(app),id)
            val v = p.getString("version",null)
            if (dir.isDirectory && v != null && versionPattern.matches(v)) {
                active = dir
                activeVersion = v
            }
        }
        checkNow(app)
    }

    fun pendingVersion(c: Context): String? {
        val p = prefs(c)
        if (p.getInt("core",-1) != core(c).first) return null
        val v = p.getString("version",null) ?: return null
        return v.takeIf { versionPattern.matches(it) && it != activeVersion }
    }

    @Synchronized fun checkNow(c: Context) {
        if (checking) return
        checking = true
        _checking.value = true
        _status.value = "APK'sız içerikler kontrol ediliyor…"
        val app = c.applicationContext ?: c
        Thread({
            try {
                _status.value = refresh(app)
            } catch (e: Exception) {
                Log.w("WqContentUpdates","Content update skipped; bundled/active content retained",e)
                _status.value = "İçerik kontrolü başarısız. Mevcut içerik korundu. İnterneti kontrol edip tekrar dene."
            } finally {
                synchronized(this) { checking = false; _checking.value = false }
            }
        },"wq-content-check").start()
    }

    private fun digest(bytes: ByteArray) =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }
    private fun readLimited(input: InputStream, limit: Long): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(32768)
        var total = 0L
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            total += n
            require(total <= limit)
            out.write(buffer,0,n)
        }
        return out.toByteArray()
    }
    private fun get(relative: String, max: Long): ByteArray {
        val conn = URL(BASE+relative).openConnection() as HttpsURLConnection
        conn.instanceFollowRedirects = false
        conn.connectTimeout = 10000
        conn.readTimeout = 15000
        conn.useCaches = false
        try {
            val status = conn.responseCode
            if (status != 200) throw java.io.IOException("Content HTTP $status")
            return conn.inputStream.use { readLimited(it,max) }
        } finally { conn.disconnect() }
    }
    @Suppress("DEPRECATION")
    private fun verify(c: Context, payload: ByteArray, sig: ByteArray) {
        val info = c.packageManager.getPackageInfo(c.packageName,PackageManager.GET_SIGNATURES)
        val certs = info.signatures ?: error("No signing certificate")
        require(certs.size == 1)
        val cert = CertificateFactory.getInstance("X.509")
            .generateCertificate(certs[0].toByteArray().inputStream())
        val algorithm = when(cert.publicKey.algorithm) {
            "RSA" -> "SHA256withRSA"
            "EC" -> "SHA256withECDSA"
            else -> error("Unsupported signing algorithm")
        }
        require(Signature.getInstance(algorithm).run {
            initVerify(cert.publicKey); update(payload); verify(sig)
        })
    }
    private fun imageValid(bytes: ByteArray) {
        require(bytes.size >= 12 && String(bytes,0,4,Charsets.US_ASCII)=="RIFF" &&
            String(bytes,8,4,Charsets.US_ASCII)=="WEBP")
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes,0,bytes.size,options)
        require(options.outWidth in 1..4096 && options.outHeight in 1..4096)
        require(options.outWidth.toLong()*options.outHeight <= 12_000_000L)
    }

    private fun refresh(c: Context): String {
        val envelope = JSONObject(String(get("index.json",262144),Charsets.UTF_8))
        val payload = Base64.decode(envelope.getString("payload"),Base64.NO_WRAP)
        val sig = Base64.decode(envelope.getString("signature"),Base64.NO_WRAP)
        verify(c,payload,sig)
        val m = JSONObject(String(payload,Charsets.UTF_8))
        val (code, name) = core(c)
        require(m.getInt("schema")==1 && m.getString("packageName")==c.packageName)
        if (m.getInt("coreCode")!=code) return "Bu Android sürümüne uygun yeni içerik paketi yok."
        val version = m.getString("version")
        require(versionPattern.matches(version) && versionPattern.matches(name))
        val sequence = m.getLong("sequence")
        val p = prefs(c)
        val prior = if(p.getInt("core",-1)==code) p.getLong("sequence",0) else 0
        if (sequence <= prior || version.substringAfterLast('.').toLong() <=
            (if(p.getInt("core",-1)==code) p.getString("version",name) ?: name else name).substringAfterLast('.').toLong()) {
            val pending = pendingVersion(c)
            return if (pending != null) "İçerik $pending indirildi. Uygulamayı tamamen kapatıp aç."
                else "İçerik güncel: ${version(c)}"
        }
        val files = m.getJSONArray("files")
        require(files.length() in 1..100)
        val id = digest(payload)
        val base = root(c)
        check(base.mkdirs() || base.isDirectory)
        val staging = File(base,"staging")
        staging.deleteRecursively()
        check(staging.mkdir())
        val dest = File(base,id)
        val seen = HashSet<String>()
        var total = 0L
        try {
            for(i in 0 until files.length()) {
                val entry = files.getJSONObject(i)
                val path = entry.getString("path")
                val hash = entry.getString("sha256")
                val size = entry.getLong("size")
                require(allowedPath(path) && seen.add(path) && hashPattern.matches(hash))
                require(size in 1..(when(path){Wq128LobbyDesign.PATH->Wq128LobbyDesign.MAX_BYTES.toLong();Wq130WebBundle.PATH->Wq130WebBundle.MAX_BYTES.toLong();else->MAX_FILE}))
                total += size
                require(total<=MAX_TOTAL)
                // Never add arbitrary new asset paths in schema 1.
                c.assets.open(path).close()
                val old = active?.let { File(it,path) }
                val cached = if(old != null && old.isFile && old.length()==size)
                    old.inputStream().use { readLimited(it,size) }.takeIf { digest(it)==hash } else null
                val bytes = cached ?: get("objects/"+hash+extension(path),size)
                require(bytes.size.toLong()==size && digest(bytes)==hash)
                when(path){Wq128LobbyDesign.PATH->Wq128LobbyDesign.parse(bytes);Wq130WebBundle.PATH->Wq130WebBundle.validate(bytes);else->imageValid(bytes)}
                val target = File(staging,path)
                check(target.parentFile?.let { it.mkdirs() || it.isDirectory } == true)
                target.outputStream().use { it.write(bytes); it.fd.sync() }
            }
            // Commit only a complete snapshot; the running process keeps its old snapshot.
            if (dest.exists()) check(dest.deleteRecursively())
            check(staging.renameTo(dest))
            check(p.edit().putString("active",id).putInt("core",code)
                .putLong("sequence",sequence).putString("version",version).commit())
        } finally { staging.deleteRecursively() }
        return "İçerik $version indirildi. APK kurulumu gerekmez; uygulamayı tamamen kapatıp aç."
    }
}

/** Initializes before the first activity; no installer or install permission. */
class WqContentInitProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        context?.let { WqContentUpdates.start(it) }
        return true
    }
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?,
        selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
