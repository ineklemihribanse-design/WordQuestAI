package com.kelimebulmacasi.a1

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class UpdateProtocolTest {
    private val packageName = "com.kelimebulmacasi.a1.practice.offline101"
    private val tag = "v1.0.17"
    private val digest = "a".repeat(64)

    private fun release(sha: String = digest, size: Long = 95123456, included: Boolean = true): String {
        val assets = JSONArray().put(JSONObject().put("name", "WordQuestAI.apk")
            .put("size", size).put("digest", "sha256:$sha")
            .put("browser_download_url", UpdateProtocol.releaseFileUrl(tag, "WordQuestAI.apk")))
        if (included) assets.put(JSONObject().put("name", "update.json")
            .put("browser_download_url", UpdateProtocol.releaseFileUrl(tag, "update.json")))
        return JSONObject().put("tag_name", tag).put("draft", false)
            .put("prerelease", false).put("assets", assets).toString()
    }

    private fun manifest(packageId: String = packageName, sha: String = digest,
                         size: Long = 95123456, versionCode: Long = 40): String = JSONObject()
        .put("tag", tag).put("versionCode", versionCode)
        .put("versionName", "1.0.17-regular-update")
        .put("packageName", packageId).put("sha256", sha)
        .put("sizeBytes", size).put("notes", "İyileştirmeler").toString()

    @Test fun newerVersionIsOfferedWithDetails() {
        val update = UpdateProtocol.parseRelease(release(), manifest(), 39, packageName)!!
        assertEquals(40, update.versionCode)
        assertEquals("1.0.17-regular-update", update.versionName)
        assertEquals(95123456, update.sizeBytes)
        assertEquals("İyileştirmeler", update.notes)
        assertEquals(UpdateProtocol.releaseFileUrl(tag, "WordQuestAI.apk"), update.apkUrl)
    }

    @Test fun noUpdateForSameOrNewerInstalledVersion() {
        assertNull(UpdateProtocol.parseRelease(release(), manifest(), 40, packageName))
        assertNull(UpdateProtocol.parseRelease(release(), manifest(), 41, packageName))
    }

    @Test fun rejectsWrongPackageOrDowngradeMetadata() {
        assertThrows(IllegalArgumentException::class.java) {
            UpdateProtocol.parseRelease(release(), manifest(packageId = "other.app"), 39, packageName)
        }
        assertThrows(IllegalArgumentException::class.java) {
            UpdateProtocol.parseRelease(release(), manifest(versionCode = 0), 39, packageName)
        }
    }

    @Test fun refusesCorruptedHashOrTruncatedDownloadMetadata() {
        assertThrows(IllegalArgumentException::class.java) {
            UpdateProtocol.parseRelease(release(), manifest(sha = "b".repeat(64)), 39, packageName)
        }
        assertThrows(IllegalArgumentException::class.java) {
            UpdateProtocol.parseRelease(release(), manifest(size = 10), 39, packageName)
        }
        assertFalse(UpdateProtocol.equalsSha256(digest, "b".repeat(64)))
    }

    @Test fun refusesMissingUpdateManifestOrUnsignedHost() {
        assertThrows(IllegalArgumentException::class.java) {
            UpdateProtocol.parseRelease(release(included = false), manifest(), 39, packageName)
        }
        assertFalse(UpdateProtocol.allowedUrl("http://github.com/owner/repo/releases/latest"))
        assertFalse(UpdateProtocol.allowedUrl("https://github.com.evil.example/latest"))
        assertFalse(UpdateProtocol.allowedUrl("https://evil.example/file.apk"))
        assertTrue(UpdateProtocol.allowedUrl("https://release-assets.githubusercontent.com/a/b"))
    }

    @Test fun digestIsStableAndRecognizesWrongBytes() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            UpdateProtocol.sha256("abc".toByteArray()))
        assertFalse(UpdateProtocol.equalsSha256(UpdateProtocol.sha256("abc".toByteArray()),
            UpdateProtocol.sha256("ab".toByteArray())))
    }
}
