package com.kelimebulmacasi.a1

import org.json.JSONObject
import java.net.URI
import java.security.MessageDigest

/** Public GitHub Releases feed; there are deliberately no credentials in the APK. */
internal object UpdateProtocol {
    const val REPOSITORY = "ineklemihribanse-design/WordQuestAI"
    const val APK_NAME = "WordQuestAI.apk"
    const val MANIFEST_NAME = "update.json"
    const val MAX_APK_BYTES = 250L * 1024L * 1024L
    const val CHECK_INTERVAL_MS = 12L * 60L * 60L * 1000L
    const val FAILURE_RETRY_MS = 60L * 60L * 1000L
    private val fingerprint = Regex("^[0-9a-fA-F]{64}$")
    private val tagPattern = Regex("^v[0-9]+\\.[0-9]+\\.[0-9]+$")

    fun allowedUrl(value: String, initial: Boolean = false): Boolean = try {
        val uri = URI(value)
        if (uri.scheme != "https" || uri.userInfo != null || uri.port != -1 || uri.fragment != null) false
        else if (initial) uri.host == "api.github.com" || uri.host == "github.com"
        else uri.host == "github.com" || uri.host == "api.github.com" ||
            uri.host == "release-assets.githubusercontent.com" || uri.host == "objects.githubusercontent.com"
    } catch (_: Exception) { false }

    fun releaseFileUrl(tag: String, name: String): String {
        require(tagPattern.matches(tag) && (name == APK_NAME || name == MANIFEST_NAME))
        return "https://github.com/$REPOSITORY/releases/download/$tag/$name"
    }

    fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }

    fun equalsSha256(expected: String, actual: String): Boolean =
        fingerprint.matches(expected) && fingerprint.matches(actual) &&
            MessageDigest.isEqual(expected.lowercase().toByteArray(), actual.lowercase().toByteArray())

    fun parseRelease(apiJson: String, manifestJson: String, installedVersionCode: Long,
                     installedPackage: String): UpdateOffer? {
        val api = JSONObject(apiJson)
        require(!api.optBoolean("draft") && !api.optBoolean("prerelease")) { "Taslak sürümler yüklenmez." }
        val tag = api.getString("tag_name")
        require(tagPattern.matches(tag)) { "GitHub sürüm etiketi geçersiz." }
        val assets = api.getJSONArray("assets")
        var apkAsset: JSONObject? = null
        var manifestAsset: JSONObject? = null
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            when (asset.optString("name")) {
                APK_NAME -> apkAsset = asset
                MANIFEST_NAME -> manifestAsset = asset
            }
        }
        val apk = requireNotNull(apkAsset) { "Sürüm APK dosyası içermiyor." }
        require(manifestAsset != null) { "Sürüm güncelleme manifesti içermiyor." }
        require(apk.getString("browser_download_url") == releaseFileUrl(tag, APK_NAME)) {
            "APK bağlantısı beklenen depodan değil."
        }
        require(manifestAsset!!.getString("browser_download_url") == releaseFileUrl(tag, MANIFEST_NAME)) {
            "Manifest bağlantısı beklenen depodan değil."
        }
        val manifest = JSONObject(manifestJson)
        require(manifest.getString("tag") == tag && manifest.getString("packageName") == installedPackage) {
            "Paket kimliği veya sürüm etiketi uyuşmuyor."
        }
        val name = manifest.getString("versionName")
        require(name.substringBefore('-') == tag.removePrefix("v")) { "Sürüm adı uyuşmuyor." }
        val code = manifest.getLong("versionCode")
        require(code > 0L) { "Sürüm kodu geçersiz." }
        val byteCount = manifest.getLong("sizeBytes")
        require(byteCount > 0 && byteCount <= MAX_APK_BYTES && byteCount == apk.getLong("size")) {
            "APK boyutu uyuşmuyor."
        }
        val hash = manifest.getString("sha256").lowercase()
        require(fingerprint.matches(hash)) { "SHA-256 özeti geçersiz." }
        val githubDigest = apk.optString("digest")
        if (githubDigest.isNotBlank()) {
            require(githubDigest.startsWith("sha256:") &&
                equalsSha256(hash, githubDigest.removePrefix("sha256:"))) { "GitHub SHA-256 doğrulanamadı." }
        }
        if (code <= installedVersionCode) return null
        return UpdateOffer(tag, code, name, manifest.optString("notes").take(4000),
            byteCount, hash, releaseFileUrl(tag, APK_NAME))
    }
}

internal data class UpdateOffer(
    val tag: String,
    val versionCode: Long,
    val versionName: String,
    val notes: String,
    val sizeBytes: Long,
    val sha256: String,
    val apkUrl: String,
) {
    val sizeLabel: String get() = "%.1f MB".format(java.util.Locale.ROOT, sizeBytes / (1024.0 * 1024.0))
}
