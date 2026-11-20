package com.kelimebulmacasi.a1

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.sceneview.Scene
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes

/**
 * Small GLB renderer for the purchaser's privately imported models.
 *
 * Runs only when the exact user's model has been imported. This is NOT an
 * alternative invented mascot, sprite animation, or an externally hosted model.
 * Filament's native methods remain on Compose's main/UI thread.
 *
 * Warning: not a claim of device-level QA for auto-weighted 3D rigs.
 */
@Composable
internal fun WordQuestAvatarStage3D(
    avatarId:String,
    modifier:Modifier=Modifier,
    motion:String="idle",
) {
    val context=LocalContext.current
    // Re-check after the user's ZIP import; do not cache the previous 'missing'
    // result for a profile that has just installed its purchased models.
    val modelFile=WordQuestPrivateAvatarAssets.modelFile(context,avatarId)
    if(modelFile==null) {
        WordQuestAvatarPortrait(avatarId,modifier)
        return
    }

    key(avatarId,modelFile.absolutePath,modelFile.lastModified()) {
        val engine=rememberEngine()
        val loader=rememberModelLoader(engine)
        val node=remember(loader,modelFile) {
            runCatching {
                ModelNode(
                    modelInstance=loader.createModelInstance(modelFile),
                    autoAnimate=false,
                    scaleToUnits=1.8f,
                    centerOrigin=Position(0f,0f,0f),
                ).apply {
                    // Original purchased animal-pack meshes are exported Y-inverted;
                    // upright the source model at its parent node, without editing skin weights.
                    if(avatarId=="foksi" || avatarId=="moka" ||
                        avatarId=="pera" || avatarId=="gigi") {
                        rotation=Rotation(0f,0f,180f)
                    }
                }
            }.onFailure{Log.e("WQ173Avatar3D","GLB load failed for " + avatarId,it)}
                .getOrNull()
        }
        val clips=remember(node) {
            node?.let{model ->
                (0 until model.animationCount).associate { index ->
                    (model.animator.getAnimationName(index)?:"clip_$index") to index
                }
            }.orEmpty()
        }
        LaunchedEffect(node,motion) {
            val model=node?:return@LaunchedEffect
            val choice=when(motion) {
                "select","wave","celebrate","trophy_raise","loss" -> motion
                else -> "idle"
            }
            val index=clips[choice]?:clips["idle"]?:return@LaunchedEffect
            runCatching {
                model.playingAnimations.keys.toList().forEach(model::stopAnimation)
                model.playAnimation(index,loop=choice=="idle",speed=1f)
            }.onFailure{Log.e("WQ173Avatar3D","Animation error",it)}
        }

        Box(modifier.background(
            Brush.verticalGradient(listOf(Color(0xFFF5EFFF),Color(0xFFFFFAFE)))
        )) {
            if(node==null) {
                Text("3D model bu cihazda açılamadı",
                    modifier=Modifier.align(Alignment.Center),
                    color=Color(0xFF674585))
            } else {
                val eye=Position(0f,0.2f,3.2f)
                val target=Position(0f,0f,0f)
                val camera=rememberCameraNode(engine){
                    position=eye
                    lookAt(target)
                }
                Scene(
                    modifier=Modifier.fillMaxSize(),
                    engine=engine,
                    modelLoader=loader,
                    cameraNode=camera,
                    cameraManipulator=rememberCameraManipulator(
                        orbitHomePosition=eye,
                        targetPosition=target,
                    ),
                    mainLightNode=rememberMainLightNode(engine){
                        intensity=80_000f
                    },
                    childNodes=rememberNodes { add(node) },
                    isOpaque=false,
                )
            }
        }
    }
}
