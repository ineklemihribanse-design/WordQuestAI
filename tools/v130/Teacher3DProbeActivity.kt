package com.kelimebulmacasi.a1

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.sceneview.Scene
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 3D rendering runs in :teacher3d using optimized copies of the real rigged\n * Emma/Sophia/Ethan GLBs, not proxy primitives. Original masters remain untouched.\n * A fatal Filament/driver SIGABRT or SIGSEGV
 * must not kill the main chat process. The remote Activity holds no API keys,
 * text conversations, microphone stream or account state.
 */
internal object Teacher3DHealth {
    private const val FILE = "wq_3d_probe_status.txt"
    fun mark(context: Context, phase: String) {
        val safe = phase.replace(Regex("[^A-Za-z0-9_-]"), "_").take(70)
        runCatching { File(context.filesDir, FILE).writeText("${System.currentTimeMillis()}|$safe") }
        Log.i("WQ3DProbe", "phase=$safe")
    }

    fun lastResult(context: Context): String {
        val saved = runCatching { File(context.filesDir, FILE).readText() }.getOrNull() ?: return ""
        val parts = saved.split("|", limit = 2)
        val since = parts.firstOrNull()?.toLongOrNull() ?: return ""
        val phase = parts.getOrNull(1).orEmpty()
        if (phase == "finished" || System.currentTimeMillis() - since > 24L * 60 * 60 * 1000) return ""
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return "Son 3D denemesi: $phase"
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return "Son 3D denemesi: $phase"
        val recent = runCatching {
            manager.getHistoricalProcessExitReasons(context.packageName, 0, 15)
                .firstOrNull { info ->
                    info.processName.endsWith(":teacher3d") && info.timestamp >= since - 2000L
                }
        }.getOrNull()
        return if (recent == null) "3D test durumu: $phase" else when (recent.reason) {
            ApplicationExitInfo.REASON_CRASH_NATIVE -> "3D grafik motoru yerel olarak çöktü (SIGSEGV/SIGABRT)."
            ApplicationExitInfo.REASON_LOW_MEMORY -> "3D işlemi bellek yetersizliğinden kapandı."
            ApplicationExitInfo.REASON_CRASH -> "3D işleminde Java/Kotlin çökmesi gerçekleşti."
            ApplicationExitInfo.REASON_ANR -> "3D işlemi yanıt vermedi."
            else -> "3D işlemi sonlandı: Android neden kodu ${recent.reason}"
        }
    }
}

internal class Teacher3DProbeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val actor = when (intent.getStringExtra("actor")) {
            TeacherAvatar.Deniz.name -> TeacherAvatar.Deniz
            TeacherAvatar.Mert.name -> TeacherAvatar.Mert
            else -> TeacherAvatar.Elif
        }
        val asset = when (actor) {
            TeacherAvatar.Elif -> "avatars/emma-mobile.glb"
            TeacherAvatar.Deniz -> "avatars/sophia-mobile.glb"
            TeacherAvatar.Mert -> "avatars/ethan-mobile.glb"
        }
        Teacher3DHealth.mark(this, "process_started_${actor.name}")
        // Read only the GLB header before creating a native Filament engine.
        // No 12-17 MB allocation just for validation.
        val assetOk = runCatching {
            assets.open(asset).use { stream ->
                val header = ByteArray(12)
                require(stream.read(header) == header.size)
                val data = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
                require(data.int == 0x46546C67) { "not glTF" }
                require(data.int == 2) { "not glTF 2" }
                require(data.int >= 20) { "incomplete GLB" }
            }
            true
        }.getOrElse {
            Log.e("WQ3DProbe", "Asset preflight failed for $asset", it)
            false
        }
        if (!assetOk) {
            Teacher3DHealth.mark(this, "asset_validation_failed")
            setContent { Text("3D model dosyası okunamadı. Ana sohbet korunuyor.") }
            return
        }
        Teacher3DHealth.mark(this, "asset_valid_${actor.name}")
        setContent {
            GlbProbeScreen(
                actor,
                onClose = {
                    Teacher3DHealth.mark(this, "finished")
                    finish()
                },
                onFailure = { finish() },
            )
        }
    }
}

@Composable
private fun GlbProbeScreen(actor: TeacherAvatar, onClose: () -> Unit, onFailure: () -> Unit) {
    var playingIdle by remember { mutableStateOf(false) }
    var modelStatus by remember { mutableStateOf("GLB yükleniyor…") }
    val context = androidx.compose.ui.platform.LocalContext.current
    val asset = when (actor) {
        TeacherAvatar.Elif -> "avatars/emma-mobile.glb"
        TeacherAvatar.Deniz -> "avatars/sophia-mobile.glb"
        TeacherAvatar.Mert -> "avatars/ethan-mobile.glb"
    }
    // This entire composition exists only in the isolated Android process.
    // The engine and ModelLoader are remembered ONCE, never recreated by
    // camera/animation buttons (the old stage recreated the engine on key()).
    val engine = rememberEngine()
    val loader = rememberModelLoader(engine)
    val camera = rememberCameraNode(engine) {
        position = Position(0f, 0.12f, 3.8f)
        lookAt(Position(0f, 0.12f, 0f))
    }
    val manipulator = rememberCameraManipulator(
        orbitHomePosition = Position(0f, 0.12f, 3.8f),
        targetPosition = Position(0f, 0.12f, 0f),
    )
    val light = rememberMainLightNode(engine) { intensity = 100_000f }
    val model = remember(loader, asset) {
        Teacher3DHealth.mark(context, "model_load_begin")
        runCatching {
            ModelNode(
                modelInstance = loader.createModelInstance(asset),
                autoAnimate = false,
                scaleToUnits = 2.7f,
                centerOrigin = Position(0f, 0f, 0f),
            )
        }.onFailure {
            Log.e("WQ3DProbe", "Model load failed", it)
            Teacher3DHealth.mark(context, "model_load_exception")
        }.onSuccess {
            Teacher3DHealth.mark(context, "model_load_success")
        }.getOrNull()
    }
    LaunchedEffect(model) {
        if (model == null) {
            Teacher3DHealth.mark(context, "model_load_failed")
            onFailure()
        }
    }
    LaunchedEffect(model, playingIdle) {
        val node = model ?: return@LaunchedEffect
        try {
            node.playingAnimations.keys.toList().forEach(node::stopAnimation)
            if (playingIdle && node.animationCount > 0) {
                val idle = (0 until node.animationCount).firstOrNull {
                    (node.animator.getAnimationName(it) ?: "").equals("Idle", true)
                } ?: 0
                node.playAnimation(idle, speed = 1f, loop = true)
                Teacher3DHealth.mark(context, "idle_animation_started")
            }
        } catch (e: Exception) {
            modelStatus = "Animasyon hatası: ${e.javaClass.simpleName}"
            Log.e("WQ3DProbe", "Animation error", e)
        }
    }
    Box(Modifier.fillMaxSize().background(Color(0xFF19343C))) {
        if (model != null) {
            var observedFrame by remember(model) { mutableStateOf(false) }
            // Some GPU drivers can stall without throwing; never trap the
            // user in a blank renderer Activity when the chat is still alive.
            LaunchedEffect(model) {
                kotlinx.coroutines.delay(10_000L)
                if (!observedFrame) {
                    Teacher3DHealth.mark(context, "render_timeout")
                    onFailure()
                }
            }
            Scene(
                modifier = Modifier.fillMaxSize(),
                engine = engine,
                modelLoader = loader,
                cameraNode = camera,
                cameraManipulator = manipulator,
                mainLightNode = light,
                childNodes = rememberNodes { add(model) },
                isOpaque = true,
                onFrame = {
                    if (!observedFrame) {
                        observedFrame = true
                        Teacher3DHealth.mark(context, "scene_frame_received")
                        modelStatus = "Sahne çiziliyor — modeli görsel olarak kontrol edin"
                    }
                },
            )
        } else {
            Text("Model yüklenemedi. Logcat: WQ3DProbe",
                modifier = Modifier.align(Alignment.Center),
                color = Color.White)
        }
        Column(
            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).background(Color(0xCC102B33)).padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("${when(actor) { TeacherAvatar.Elif -> "Emma"; TeacherAvatar.Deniz -> "Sophia"; TeacherAvatar.Mert -> "Ethan" }} — GERÇEK GLB 3D TEST", color = Color.White)
            Text(modelStatus, color = Color(0xFFF1DFC1))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { playingIdle = !playingIdle }, enabled = model != null) {
                    Text(if (playingIdle) "Animasyonu durdur" else "Idle hareketini dene")
                }
                Button(onClick = onClose) { Text("Sohbete dön") }
            }
        }
    }
}
