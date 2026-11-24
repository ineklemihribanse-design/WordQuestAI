package com.kelimebulmacasi.a1

import org.json.JSONObject
import java.io.File

internal data class WqDeltaOffer(val baseSize: Long, val baseHash: String,
    val size: Long, val hash: String, val url: String)

internal object WqDeltaMetadata {
    fun select(apiText: String, manifestText: String, offer: UpdateOffer, installedCode: Long,
               base: File, hashFile: (File) -> String = { WqDeltaApplier.sha(it) }): WqDeltaOffer? {
        return try {
            val deltas = JSONObject(manifestText).optJSONArray("deltas") ?: return null
            val assets = JSONObject(apiText).getJSONArray("assets")
            if (!base.isFile || installedCode <= 0 || installedCode >= offer.versionCode) return null
            val name = "WordQuestAI-$installedCode-to-"+offer.versionCode+".wqd.gz"
            val url = "https://github.com/"+UpdateProtocol.REPOSITORY+"/releases/download/"+offer.tag+"/"+name
            if (!UpdateProtocol.allowedUrl(url, initial = true)) return null
            val candidates = (0 until deltas.length()).mapNotNull { deltas.optJSONObject(it) }
                .filter { it.optString("format") == "WQD1-gzip" && it.optLong("fromVersionCode") == installedCode &&
                    it.optLong("toVersionCode") == offer.versionCode && it.optString("file") == name }
            val d = candidates.singleOrNull() ?: return null
            val size = d.getLong("sizeBytes")
            val baseSize = d.getLong("baseSizeBytes")
            val hash = d.getString("sha256")
            val baseHash = d.getString("baseSha256")
            if (size <= 0 || size >= offer.sizeBytes * .85 || baseSize != base.length() ||
                !UpdateProtocol.equalsSha256(hash,hash) || !UpdateProtocol.equalsSha256(baseHash,baseHash) ||
                d.getLong("targetSizeBytes") != offer.sizeBytes ||
                !UpdateProtocol.equalsSha256(d.getString("targetSha256"),offer.sha256)) return null
            val asset = (0 until assets.length()).mapNotNull { assets.optJSONObject(it) }
                .filter { it.optString("name") == name }.singleOrNull() ?: return null
            if (asset.optLong("size") != size || asset.optString("browser_download_url") != url ||
                asset.optString("state") != "uploaded" ||
                !UpdateProtocol.equalsSha256(hash, asset.optString("digest").takeIf { it.startsWith("sha256:") }?.removePrefix("sha256:") ?: "")) return null
            if (!UpdateProtocol.equalsSha256(baseHash,hashFile(base))) return null
            WqDeltaOffer(baseSize,baseHash,size,hash,url)
        } catch (_: Exception) { null }
    }
}
internal class WqDeltaFallback(cause: Throwable): java.io.IOException("Küçük güncelleme uygulanamadı",cause)
