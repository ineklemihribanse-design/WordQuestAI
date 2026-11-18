package com.kelimebulmacasi.a1

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.lifecycle.lifecycleScope

class MainActivity : ComponentActivity() {
    private lateinit var session: AppSession
    private lateinit var pronunciation: WordPronunciation
    private lateinit var updates: AppUpdater
    private val ai: AiSession by viewModels()
    private val arena: ArenaSession by viewModels()

    private fun receiveInvite(input: Intent?) {
        val link = input?.data ?: return
        if (link.scheme == ARENA_SCHEME && link.host == "arena" && link.pathSegments.firstOrNull() == "join") {
            link.lastPathSegment?.let(arena::invite)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        receiveInvite(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(0x00000000, 0x00000000),
            navigationBarStyle = SystemBarStyle.light(0x00000000, 0x00000000),
        )
        session = AppSession(this, Accounts(this), lifecycleScope)
        pronunciation = WordPronunciation(this)
        updates = AppUpdater(applicationContext, lifecycleScope)
        receiveInvite(intent)
        setContent {
            val systemDensity = LocalDensity.current
            val scale = session.preferences.textScale
            val appDensity = remember(systemDensity, scale) {
                if (scale == 1f) systemDensity else Density(systemDensity.density, systemDensity.fontScale * scale)
            }
            CompositionLocalProvider(LocalDensity provides appDensity, LocalAppUpdater provides updates) {
                MaterialTheme(
                    colorScheme = lightColorScheme(
                        primary = Forest,
                        onPrimary = Paper,
                        primaryContainer = Lime,
                        onPrimaryContainer = Ink,
                        background = Paper,
                        onBackground = Ink,
                        surface = White,
                        onSurface = Ink,
                        outline = Line,
                    ),
                ) {
                    Surface(modifier = Modifier.fillMaxSize(), color = Paper) {
                        CrosswordApp(session, ai, arena, pronunciation)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        arena.resume()
        updates.checkIfDue()
    }

    override fun onResume() {
        super.onResume()
        if (::updates.isInitialized) updates.onResume(this)
    }

    override fun onStop() {
        pronunciation.stop()
        arena.pause()
        session.flush()
        super.onStop()
    }

    override fun onDestroy() {
        pronunciation.close()
        session.close()
        super.onDestroy()
    }
}

internal val Paper = Color(0xFFF8F7F0)
internal val White = Color(0xFFFFFFFA)
internal val Ink = Color(0xFF152E27)
internal val Forest = Color(0xFF21554A)
internal val Board = Color(0xFF193B33)
internal val Block = Color(0xFF25483F)
internal val Lime = Color(0xFFD8F186)
internal val LightLime = Color(0xFFEBF4CA)
internal val Muted = Color(0xFF51685F)
internal val Line = Color(0xFFD8E1D7)
internal val ErrorInk = Color(0xFF802B21)
internal val ErrorFill = Color(0xFFF9DDD5)

internal val Sans = FontFamily(
    Font(R.font.dm_sans_400),
    Font(R.font.dm_sans_500, FontWeight.Medium),
    Font(R.font.dm_sans_600, FontWeight.SemiBold),
    Font(R.font.dm_sans_700, FontWeight.Bold),
)
internal val Serif = FontFamily(Font(R.font.dm_serif_text))
