package com.kelimebulmacasi.a1

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.Assert.*
import java.io.File

class WqDeltaMetadataTest {
    @Test fun exactBaseAndTargetAndAssetRequired() {
        val base=File.createTempFile("wqd-base",".apk")
        try {
            base.writeBytes(ByteArray(100){it.toByte()})
            val bh=WqDeltaApplier.sha(base)
            val h="a".repeat(64);val ph="b".repeat(64)
            val name="WordQuestAI-141-to-142.wqd.gz"
            val tag="v1.0.116"
            val url="https://github.com/"+UpdateProtocol.REPOSITORY+"/releases/download/$tag/$name"
            val o=UpdateOffer(tag,142,"1.0.116","",1000,h,UpdateProtocol.releaseFileUrl(tag,UpdateProtocol.APK_NAME))
            val d=JSONObject().put("format","WQD1-gzip").put("fromVersionCode",141).put("toVersionCode",142)
                .put("baseSizeBytes",100).put("baseSha256",bh).put("sizeBytes",100)
                .put("targetSizeBytes",1000).put("targetSha256",h).put("sha256",ph).put("file",name)
            val asset=JSONObject().put("name",name).put("size",100).put("digest","sha256:$ph")
                .put("state","uploaded").put("browser_download_url",url)
            fun choose()=WqDeltaMetadata.select(JSONObject().put("assets",JSONArray().put(asset)).toString(),
                JSONObject().put("deltas",JSONArray().put(d)).toString(),o,141,base)
            assertEquals(100L,choose()!!.size)
            d.put("targetSha256","0".repeat(64));assertNull(choose());d.put("targetSha256",h)
            d.put("fromVersionCode",140);assertNull(choose());d.put("fromVersionCode",141)
            asset.put("browser_download_url","https://evil.example/$name");assertNull(choose());asset.put("browser_download_url",url)
            asset.put("digest","sha256:"+"0".repeat(64));assertNull(choose());asset.put("digest","sha256:$ph")
            base.writeBytes(ByteArray(100));assertNull(choose())
        } finally { base.delete() }
    }
}
