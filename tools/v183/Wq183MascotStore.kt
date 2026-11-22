package com.kelimebulmacasi.a1

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import org.json.JSONObject
import java.io.*
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipInputStream

/** Local-only WordQuest Mascot Pack v1. No network, executable code or global shared files. */
internal object Wq183MascotStore {
    data class Item(val id: String, val name: String)
    private const val ZIP_MAX = 25L * 1024 * 1024
    private const val UNPACKED_MAX = 80L * 1024 * 1024
    private const val ENTRY_MAX = 25L * 1024 * 1024
    private const val FILE_MAX = 40
    private val idPattern = Regex("custom_[a-f0-9]{32}")
    private val pathPattern = Regex("images/(idle|happy|sad|thinking|win|frame_[0-9]{2})\\.(png|webp)")
    private val ownerPrefs = "wq183_maskot_active_owner"

    fun isCustomId(id: String?): Boolean = id != null && idPattern.matches(id)

    private fun owner(profile: String): String = MessageDigest.getInstance("SHA-256")
        .digest(profile.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }.take(24)

    fun activate(context: Context, profile: String) {
        require(profile.isNotBlank())
        context.getSharedPreferences(ownerPrefs, Context.MODE_PRIVATE).edit()
            .putString("owner", owner(profile)).apply()
    }

    private fun root(context: Context, profile: String): File =
        File(context.noBackupFilesDir, "wq183_mascots/" + owner(profile)).apply { mkdirs() }

    fun exists(context: Context, profile: String, id: String): Boolean =
        isCustomId(id) && File(root(context, profile), "$id/manifest.json").isFile

    private fun storedName(dir: File): String = runCatching {
        JSONObject(File(dir, "manifest.json").readText(Charsets.UTF_8))
            .getString("name").take(40)
    }.getOrDefault("Kişisel maskot")

    fun list(context: Context, profile: String): List<Item> =
        root(context, profile).listFiles().orEmpty()
            .filter { it.isDirectory && isCustomId(it.name) &&
                File(it, "manifest.json").isFile }
            .map { Item(it.name, storedName(it)) }
            .sortedBy { it.name }

    /** Only active-profile local artwork is readable by portrait rendering. */
    fun picture(context: Context, id: String, victory: Boolean): Bitmap? {
        if (!isCustomId(id)) return null
        val token = context.getSharedPreferences(ownerPrefs, Context.MODE_PRIVATE)
            .getString("owner", null) ?: return null
        if (!Regex("[a-f0-9]{24}").matches(token)) return null
        val folder = File(context.noBackupFilesDir, "wq183_mascots/$token/$id")
        if (!File(folder, "manifest.json").isFile) return null
        return runCatching {
            val manifest = JSONObject(File(folder, "manifest.json").readText())
            val pictures = manifest.getJSONObject("images")
            val preferred = if (victory) "win" else "idle"
            val path = pictures.optString(preferred).takeIf { it.isNotBlank() }
                ?: pictures.getString("idle")
            require(pathPattern.matches(path))
            val target = File(folder, path)
            if (!target.isFile) return@runCatching null
            BitmapFactory.decodeFile(target.absolutePath, BitmapFactory.Options().apply {
                inSampleSize = 2
            })
        }.getOrNull()
    }

    private fun checkImage(file: File, relative: String) {
        val bytes = ByteArray(12)
        FileInputStream(file).use { DataInputStream(it).readFully(bytes) }
        val png = bytes.take(8) == listOf(137,80,78,71,13,10,26,10)
            .map { it.toByte() }
        val webp = String(bytes, 0, 4, Charsets.US_ASCII) == "RIFF" &&
            String(bytes, 8, 4, Charsets.US_ASCII) == "WEBP"
        require(if (relative.endsWith(".png")) png else webp) {
            "Dosyanın gerçek biçimi uzantısıyla uyuşmuyor: $relative"
        }
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, opts)
        require(opts.outWidth in 1..2048 && opts.outHeight in 1..2048) {
            "Görsel çözünürlüğü geçersiz: $relative"
        }
    }

    /** Call on Dispatchers.IO. All extraction remains in app-private storage. */
    fun importPack(context: Context, uri: Uri, profile: String): Item {
        require(profile.isNotBlank()) { "Önce profil seçilmeli" }
        // Reject excess packs before allocating temporary files or staging dirs.
        require(list(context, profile).size < 20) { "En fazla 20 kişisel maskot saklanabilir" }
        val ownedRoot = root(context, profile)
        val stage = File(ownedRoot, ".staging_" + UUID.randomUUID())
        val input = File.createTempFile("wq183_zip_", ".tmp", context.cacheDir)
        try {
            require(stage.mkdirs()) { "Geçici maskot klasörü açılamadı" }
            val stream = context.contentResolver.openInputStream(uri)
                ?: error("ZIP dosyası açılamadı")
            stream.use { source ->
                FileOutputStream(input).use { destination ->
                    val buffer = ByteArray(32768)
                    var count = 0L
                    while (true) {
                        val n = source.read(buffer)
                        if (n < 0) break
                        count += n
                        require(count <= ZIP_MAX) { "ZIP 25 MB sınırını aşıyor" }
                        destination.write(buffer, 0, n)
                    }
                }
            }
            val seen = mutableSetOf<String>()
            var total = 0L
            var entriesSeen = 0
            ZipInputStream(BufferedInputStream(FileInputStream(input))).use { zip ->
                var files = 0
                while (true) {
                    val entry = zip.nextEntry ?: break
                    entriesSeen++
                    require(entriesSeen <= FILE_MAX + 1) { "ZIP çok fazla giriş içeriyor" }
                    if (entry.isDirectory) {
                        require(entry.name == "images/") { "Geçersiz ZIP klasörü" }
                        zip.closeEntry()
                        continue
                    }
                    files++
                    require(files <= FILE_MAX) { "ZIP en fazla 40 dosya içerebilir" }
                    val name = entry.name
                    require(name == "manifest.json" || name == "README_TR.txt" || pathPattern.matches(name)) {
                        "Beklenmeyen dosya veya güvenli olmayan yol: $name"
                    }
                    require(seen.add(name)) { "Tekrarlanan ZIP girdisi: $name" }
                    val output = File(stage, name)
                    output.parentFile?.mkdirs()
                    FileOutputStream(output).use { destination ->
                        val buffer = ByteArray(32768)
                        var perEntry = 0L
                        while (true) {
                            val n = zip.read(buffer)
                            if (n < 0) break
                            perEntry += n
                            total += n
                            require(perEntry <= ENTRY_MAX && total <= UNPACKED_MAX) {
                                "ZIP açılmış veri sınırını aşıyor"
                            }
                            if (name == "manifest.json" || name == "README_TR.txt")
                                require(perEntry <= 65536L) { "Manifest çok büyük" }
                            destination.write(buffer, 0, n)
                        }
                    }
                    zip.closeEntry()
                }
            }
            require("manifest.json" in seen) { "manifest.json eksik" }
            val manifest = JSONObject(File(stage, "manifest.json").readText(Charsets.UTF_8))
            require(manifest.optInt("formatVersion", -1) == 1) { "Paket sürümü desteklenmiyor" }
            require(manifest.optInt("packageVersion", -1) in 1..1000000) { "Paket sürümü hatalı" }
            val name = manifest.optString("name").trim()
            require(name.isNotBlank() && name.length <= 40) { "Maskot adı 1–40 karakter olmalı" }
            require(Regex("[A-Za-z0-9_-]{1,40}").matches(manifest.optString("id"))) {
                "Maskot kimliği geçersiz"
            }
            require(manifest.optString("animation", "static") in listOf("static", "frames")) {
                "Animasyon biçimi desteklenmiyor"
            }
            val images = manifest.getJSONObject("images")
            val idle = images.getString("idle")
            require(pathPattern.matches(idle) && idle.startsWith("images/idle.")) {
                "Temel idle görseli eksik"
            }
            for (state in listOf("idle", "happy", "sad", "thinking", "win")) {
                if (images.has(state)) {
                    val path = images.getString(state)
                    require(pathPattern.matches(path) && path.startsWith("images/$state.")) {
                        "Geçersiz ifade yolu: $state"
                    }
                    require(path in seen) { "Görsel ZIP'te yok: $path" }
                }
            }
            for (nameInZip in seen.filter { it != "manifest.json" && it != "README_TR.txt" }) {
                checkImage(File(stage, nameInZip), nameInZip)
            }
            val id = "custom_" + UUID.randomUUID().toString().replace("-", "")
            val target = File(ownedRoot, id)
            require(stage.renameTo(target)) { "Maskot özel depolamaya kaydedilemedi" }
            activate(context, profile)
            return Item(id, name)
        } finally {
            input.delete()
            if (stage.exists()) stage.deleteRecursively()
        }
    }

    fun rename(context: Context, profile: String, id: String, newName: String): Boolean {
        val name = newName.trim()
        if (!isCustomId(id) || name.isBlank() || name.length > 40) return false
        val dir = File(root(context, profile), id)
        val source = File(dir, "manifest.json")
        if (!source.isFile) return false
        return runCatching {
            val json = JSONObject(source.readText(Charsets.UTF_8))
            json.put("name", name)
            val tmp = File(dir, "manifest.pending")
            tmp.writeText(json.toString(2), Charsets.UTF_8)
            if (!tmp.renameTo(source)) {
                require(source.delete() && tmp.renameTo(source)) { "Ad kaydedilemedi" }
            }
            true
        }.getOrDefault(false)
    }

    fun delete(context: Context, profile: String, id: String): Boolean {
        if (!isCustomId(id)) return false
        val path = File(root(context, profile), id)
        return path.isDirectory && path.deleteRecursively()
    }
}
