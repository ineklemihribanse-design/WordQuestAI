package com.kelimebulmacasi.a1

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.os.Process

/**
 * Small same-app IPC bridge: Lena runs in :teacher3d while the saved encrypted
 * credentials belong to the main application process. SharedPreferences is NOT
 * cross-process safe. The provider is nonexported, read-only, and serves only
 * the currently selected profile/provider; no credential is placed in an Intent.
 */
internal class TeacherCredentialBridgeProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (method != "credential_snapshot" || Binder.getCallingUid() != Process.myUid()) return null
        val ctx = context ?: return null
        val profile = extras?.getString("profile")
            ?.takeIf { it.isNotBlank() && it.length <= 240 } ?: return null
        val keys = UserAiCredentials(ctx, profile)
        val provider = keys.selected()
        val result = Bundle()
        result.putString("provider", provider.name)
        result.putString("model", keys.model(provider))
        result.putString("key", keys.key(provider))
        return result
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?,
                       selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?,
                        selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?,
                        selectionArgs: Array<out String>?): Int = 0

    companion object {
        internal fun snapshot(context: Context, profile: String): Bundle? = runCatching {
            val authority = context.packageName + ".teacher_credentials"
            val uri = Uri.parse("content://$authority")
            context.contentResolver.call(uri, "credential_snapshot", null,
                Bundle().apply { putString("profile", profile) })
        }.getOrNull()
    }
}

/** Reusable, testable fail-closed rules for the provider result. */
internal object TeacherCredentialBridgeRules {
    fun selected(name: String?): AiProvider =
        AiProvider.entries.firstOrNull { it != AiProvider.Claude && it.name == name }
            ?: AiProvider.Gemini

    fun model(name: String?, provider: AiProvider): String =
        name?.takeIf { aiModelPattern.matches(it) } ?: provider.defaultModel

    fun key(snapshotProvider: String?, askedProvider: AiProvider, secret: String?): String =
        if (snapshotProvider == askedProvider.name && askedProvider != AiProvider.Claude)
            secret.orEmpty() else ""
}
