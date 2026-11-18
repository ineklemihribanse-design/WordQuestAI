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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
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
        // Keep the last diagnostic over upgrades, so a report can be opened
        // even days after an isolated-process crash. No user text/API keys.
        if (System.currentTimeMillis() - since > 7L * 24 * 60 * 60 * 1000) return ""
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return "Son 3D denemesi: $phase"
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return "Son 3D denemesi: $phase"
        val recent = runCatching {
            manager.getHistoricalProcessExitReasons(context.packageName, 0, 15)
                .filter { info ->
                    info.processName.endsWith(":teacher3d") && info.timestamp >= since - 2000L
                }
                .maxByOrNull { it.timestamp }
        }.getOrNull()
        // Even if the UI logged "finished", native GPU teardown may crash
        // after that line; do NOT discard a later Android crash exit record.
        if (phase == "finished" && (recent == null || recent.reason !in listOf(
            ApplicationExitInfo.REASON_CRASH_NATIVE,
            ApplicationExitInfo.REASON_CRASH,
            ApplicationExitInfo.REASON_ANR
        ))) return ""
        return if (recent == null) "Son 3D yükleme aşaması: $phase (Android çıkış nedeni henüz raporlanmadı)." else when (recent.reason) {
            ApplicationExitInfo.REASON_CRASH_NATIVE -> {
                val sample = if (recent.pss > 0L) "${recent.pss / 1024} MiB"
                    else "ölçüm alınamadı"
                val details = TeacherNativeCrashSummary.fromExitInfo(recent)
                "3D motoru SIG${recent.status} ile çöktü (PSS: $sample; aşama: $phase)." +
                    if (details.isNotBlank()) "\n$details" else
                        "\nYerel hata izi Android tarafından sağlanmadı."
            }
            ApplicationExitInfo.REASON_LOW_MEMORY -> "3D işlemi bellek yetersizliğinden kapandı."
            ApplicationExitInfo.REASON_CRASH ->
                "3D Java/Kotlin işlemi çöktü (son aşama: $phase)."
            ApplicationExitInfo.REASON_ANR -> "3D işlemi yanıt vermedi."
            else -> "3D işlemi sonlandı: Android neden kodu ${recent.reason}"
        }
    }
}

/** Camera presets alter framing, not the character skeleton/anatomy. */
private data class TeacherCameraProfile(
    val asset: String, val fullDistance: Float, val waistDistance: Float,
    val faceDistance: Float, val waistTargetY: Float, val faceTargetY: Float,
) {
    fun eye(preset: Int) = when(preset) {
        1 -> Position(0f, waistTargetY, waistDistance)
        2 -> Position(0f, faceTargetY, faceDistance)
        else -> Position(0f, 0f, fullDistance)
    }
    fun target(preset: Int) = when(preset) {
        1 -> Position(0f, waistTargetY, 0f)
        2 -> Position(0f, faceTargetY, 0f)
        else -> Position(0f, 0f, 0f)
    }
}

private fun teacherCameraProfile(actor: TeacherAvatar) = when(actor) {
    TeacherAvatar.Elif -> TeacherCameraProfile("avatars/emma-dense-face.glb",
        3.55f, 2.25f, 1.27f, .22f, .86f)
    TeacherAvatar.Deniz -> TeacherCameraProfile("avatars/sophia-dense-face.glb",
        3.58f, 2.28f, 1.29f, .21f, .83f)
    TeacherAvatar.Mert -> TeacherCameraProfile("avatars/ethan-mobile.glb",
        3.65f, 2.30f, 1.32f, .18f, .80f)
}

internal class Teacher3DProbeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install before any model/Filament access. Record only the throwable
        // CLASS and app method name, never message/API keys/conversation text.
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                val where = error.stackTrace.firstOrNull { frame ->
                    frame.className.startsWith("com.kelimebulmacasi.a1")
                }?.methodName.orEmpty().take(25)
                Teacher3DHealth.mark(
                    applicationContext,
                    "java_${error.javaClass.simpleName.take(28)}_$where"
                )
            }
            if (previous != null) previous.uncaughtException(thread, error)
            else android.os.Process.killProcess(android.os.Process.myPid())
        }
        super.onCreate(savedInstanceState)
        val actor = when (intent.getStringExtra("actor")) {
            TeacherAvatar.Deniz.name -> TeacherAvatar.Deniz
            TeacherAvatar.Mert.name -> TeacherAvatar.Mert
            else -> TeacherAvatar.Elif
        }
        val withoutMorph = intent.getBooleanExtra("withoutMorph", false)
            && actor != TeacherAvatar.Mert
        // The diagnostic is STILL a skinned real human model, not a 2D
        // stand-in: only face GPU morphs are disabled to isolate a crash.
        val asset = if (withoutMorph) when(actor) {
            TeacherAvatar.Elif -> "avatars/emma-no-morph-diagnostic.glb"
            TeacherAvatar.Deniz -> "avatars/sophia-no-morph-diagnostic.glb"
            TeacherAvatar.Mert -> teacherCameraProfile(actor).asset
        } else teacherCameraProfile(actor).asset
        val variant = if (withoutMorph) "base_geometry" else "dense_52morph"
        Teacher3DHealth.mark(this, "process_started_${actor.name}_${variant}")
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
        Teacher3DHealth.mark(this, "asset_valid_${actor.name}_${variant}")
        setContent {
            GlbProbeScreen(
                actor = actor,
                asset = asset,
                variant = variant,
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
private fun GlbProbeScreen(
    actor: TeacherAvatar,
    asset: String,
    variant: String,
    onClose: () -> Unit,
    onFailure: () -> Unit,
) {
    var playingIdle by remember { mutableStateOf(true) }
    var cameraPreset by remember { mutableIntStateOf(0) }
    var cameraReset by remember { mutableIntStateOf(0) }
    var modelStatus by remember { mutableStateOf("GLB yükleniyor…") }
    val context = androidx.compose.ui.platform.LocalContext.current
    // This entire composition exists only in the isolated Android process.
    // The engine and ModelLoader are remembered ONCE, never recreated by
    // camera/animation buttons (the old stage recreated the engine on key()).
    val engine = rememberEngine()
    val loader = rememberModelLoader(engine)
    val profile = remember(actor) { teacherCameraProfile(actor) }
    val startedAtMs = remember { SystemClock.elapsedRealtime() }
    val performanceWindow = remember { longArrayOf(0L, 0L) }
    val model = remember(loader, asset) {
        Teacher3DHealth.mark(context, "model_load_begin_${actor.name}_${variant}")
        runCatching {
            Teacher3DHealth.mark(context, "asset_instance_begin_${actor.name}_${variant}")
            val instance = loader.createModelInstance(asset)
            Teacher3DHealth.mark(context, "asset_instance_ready_${actor.name}_${variant}")
            val node = ModelNode(
                modelInstance = instance,
                autoAnimate = false,
                scaleToUnits = 2.7f,
                centerOrigin = Position(0f, 0f, 0f),
            )
            Teacher3DHealth.mark(context, "model_node_ready_${actor.name}_${variant}")
            node
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
    var observedFrame by remember(model) { mutableStateOf(false) }
    LaunchedEffect(model, observedFrame, playingIdle) {
        val node = model ?: return@LaunchedEffect
        if (!observedFrame) return@LaunchedEffect
        kotlinx.coroutines.delay(450L) // Pose only after first GL frame
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
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(Color(0xFF254A55), Color(0xFF17343D), Color(0xFF0F2730))
            )
        )
    ) {
        if (model != null) {
            // Some GPU drivers can stall without throwing; never trap the
            // user in a blank renderer Activity when the chat is still alive.
            LaunchedEffect(model) {
                kotlinx.coroutines.delay(10_000L)
                if (!observedFrame) {
                    Teacher3DHealth.mark(context, "render_timeout")
                    onFailure()
                }
            }
            // Only camera/scene are keyed; NEVER recreate Engine/Loader/GLB
            // on preset changes (avoids repeated 12-17 MiB GPU allocations).
            key(cameraPreset, cameraReset) {
                val eye = profile.eye(cameraPreset)
                val target = profile.target(cameraPreset)
                val camera = rememberCameraNode(engine) {
                    position = eye
                    lookAt(target)
                }
                val manipulator = rememberCameraManipulator(
                    orbitHomePosition = eye,
                    targetPosition = target,
                )
                Scene(
                    modifier = Modifier.fillMaxSize(),
                    engine = engine,
                    modelLoader = loader,
                    cameraNode = camera,
                    cameraManipulator = manipulator,
                    mainLightNode = rememberMainLightNode(engine) {
                        intensity = 100_000f
                    },
                    childNodes = rememberNodes { add(model) },
                    isOpaque = false,
                    onFrame = {
                        val now = SystemClock.elapsedRealtime()
                        if (performanceWindow[0] == 0L) performanceWindow[0] = now
                        performanceWindow[1] += 1L
                        val windowMs = now - performanceWindow[0]
                        if (windowMs >= 5000L) {
                            val fps = performanceWindow[1] * 1000f / windowMs
                            Log.i("WQ3DProbe", "fps5s=$fps actor=${actor.name}")
                            performanceWindow[0] = now
                            performanceWindow[1] = 0L
                        }
                        if (!observedFrame) {
                            observedFrame = true
                            val elapsedMs = SystemClock.elapsedRealtime() - startedAtMs
                            Teacher3DHealth.mark(context, "scene_frame_received_${actor.name}_${variant}")
                            val mem = android.os.Debug.MemoryInfo()
                            android.os.Debug.getMemoryInfo(mem)
                            Log.i("WQ3DProbe", "firstFrameMs=$elapsedMs " +
                                "graphicsKb=${mem.getMemoryStat("summary.graphics")} " +
                                "pssKb=${mem.totalPss} actor=${actor.name}")
                            modelStatus = "3D sahnesi hazır — ${elapsedMs} ms"
                        }
                    },
                )
            }
        } else {
            Text("Model yüklenemedi. Logcat: WQ3DProbe",
                modifier = Modifier.align(Alignment.Center),
                color = Color.White)
        }
        Column(
            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).background(Color(0xCC102B33)).padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                "${when(actor) { TeacherAvatar.Elif -> "Emma"; TeacherAvatar.Deniz -> "Sophia"; TeacherAvatar.Mert -> "Ethan" }}" +
                (if (variant == "base_geometry") " — 3D YÜZ HESAPLAMA TANI" else " — GERÇEK GLB 3D TEST"),
                color = Color.White
            )
            Text(modelStatus, color = Color(0xFFF1DFC1))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("Tam Boy", "Bel", "Yüz").forEachIndexed { index, label ->
                    Button(onClick = { cameraPreset = index }, enabled = model != null) {
                        Text(label)
                    }
                }
                Button(onClick = { cameraPreset = 0; cameraReset++ }) { Text("↺") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { playingIdle = !playingIdle }, enabled = model != null) {
                    Text(if (playingIdle) "Animasyonu durdur" else "Idle hareketini dene")
                }
                Button(onClick = onClose) { Text("Sohbete dön") }
            }
        }
    }
}
