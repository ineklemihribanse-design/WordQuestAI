package com.kelimebulmacasi.a1

import kotlin.math.PI
import kotlin.math.sin

/** Five selectable characters; existing IDs remain backward compatible with Firebase rooms. */
internal data class ArenaMascotCharacter(val id: String, val name: String, val family: String)
internal data class ArenaRigBone(val id: String, val parent: String?, val pivotX: Float, val pivotY: Float)

/**
 * Explicit hierarchical 2.5D armature. Coordinates are normalized on a 160x180
 * canvas. Hands are descendants of forearms; the trophy is attached to the
 * LEFT_HAND bone, not a floating sticker that stays fixed as the avatar moves.
 */
internal object ArenaMascotRig {
    val characters = listOf(
        ArenaMascotCharacter("robot", "Bip", "robot"),
        ArenaMascotCharacter("fox", "Pufi", "fox"),
        ArenaMascotCharacter("owl", "Mimo", "owl"),
        ArenaMascotCharacter("rabbit", "Lila", "rabbit"),
        ArenaMascotCharacter("bear", "Tiko", "bear")
    )
    val choices: List<Pair<String,String>> = characters.map { it.id to it.name }
    private val ids = characters.map { it.id }.toSet()
    fun isValid(id: String?): Boolean = id != null && id in ids
    fun safe(id: String?): String = if (isValid(id)) id!! else "robot"

    val bones: List<ArenaRigBone> = listOf(
        ArenaRigBone("ROOT", null, 80f, 123f),
        ArenaRigBone("PELVIS", "ROOT", 80f, 119f),
        ArenaRigBone("SPINE", "PELVIS", 80f, 100f),
        ArenaRigBone("CHEST", "SPINE", 80f, 88f),
        ArenaRigBone("NECK", "CHEST", 80f, 76f),
        ArenaRigBone("HEAD", "NECK", 80f, 58f),
        ArenaRigBone("LEFT_EAR", "HEAD", 58f, 20f),
        ArenaRigBone("RIGHT_EAR", "HEAD", 102f, 20f),
        ArenaRigBone("LEFT_EYE", "HEAD", 65f, 61f),
        ArenaRigBone("RIGHT_EYE", "HEAD", 95f, 61f),
        ArenaRigBone("MOUTH", "HEAD", 80f, 75f),
        ArenaRigBone("LEFT_SHOULDER", "CHEST", 46f, 99f),
        ArenaRigBone("LEFT_UPPER_ARM", "LEFT_SHOULDER", 46f, 113f),
        ArenaRigBone("LEFT_ELBOW", "LEFT_UPPER_ARM", 46f, 131f),
        ArenaRigBone("LEFT_FOREARM", "LEFT_ELBOW", 46f, 144f),
        ArenaRigBone("LEFT_HAND", "LEFT_FOREARM", 46f, 155f),
        ArenaRigBone("RIGHT_SHOULDER", "CHEST", 114f, 99f),
        ArenaRigBone("RIGHT_UPPER_ARM", "RIGHT_SHOULDER", 114f, 113f),
        ArenaRigBone("RIGHT_ELBOW", "RIGHT_UPPER_ARM", 114f, 131f),
        ArenaRigBone("RIGHT_FOREARM", "RIGHT_ELBOW", 114f, 144f),
        ArenaRigBone("RIGHT_HAND", "RIGHT_FOREARM", 114f, 155f),
        ArenaRigBone("LEFT_HIP", "PELVIS", 65f, 130f),
        ArenaRigBone("LEFT_THIGH", "LEFT_HIP", 65f, 140f),
        ArenaRigBone("LEFT_KNEE", "LEFT_THIGH", 65f, 151f),
        ArenaRigBone("LEFT_SHIN", "LEFT_KNEE", 65f, 159f),
        ArenaRigBone("LEFT_FOOT", "LEFT_SHIN", 65f, 167f),
        ArenaRigBone("RIGHT_HIP", "PELVIS", 95f, 130f),
        ArenaRigBone("RIGHT_THIGH", "RIGHT_HIP", 95f, 140f),
        ArenaRigBone("RIGHT_KNEE", "RIGHT_THIGH", 95f, 151f),
        ArenaRigBone("RIGHT_SHIN", "RIGHT_KNEE", 95f, 159f),
        ArenaRigBone("RIGHT_FOOT", "RIGHT_SHIN", 95f, 167f),
        ArenaRigBone("TROPHY_PIVOT", "LEFT_HAND", 46f, 155f)
    )

    fun isHierarchical(): Boolean {
        val seen = mutableSetOf<String>()
        for (b in bones) {
            if (b.id in seen || (b.parent != null && b.parent !in seen) ||
                !b.pivotX.isFinite() || !b.pivotY.isFinite()) return false
            seen += b.id
        }
        return bones.size >= 30 && "TROPHY_PIVOT" in seen
    }

    data class Pose(
        val bodyBob: Float, val torsoAngle: Float, val headAngle: Float,
        val armLeft: Float, val elbowLeft: Float,
        val armRight: Float, val elbowRight: Float,
        val legLeft: Float, val legRight: Float,
        val eyelid: Float, val ears: Float, val breathing: Float,
        val holdsTrophy: Boolean
    )

    /** t in radians, one cycle = 2π. No random jitter or per-frame allocations of bones. */
    fun pose(kind: String, t: Float, trophy: Boolean = false): Pose {
        val offset = when(safe(kind)) {
            "robot" -> 0f
            "fox" -> 0.6f
            "owl" -> 1.2f
            "rabbit" -> 1.8f
            else -> 2.4f
        }
        val p = t + offset
        val fraction = (p % (2f * PI.toFloat()) + 2f * PI.toFloat()) %
            (2f * PI.toFloat())
        val closing = fraction > 6.03f && fraction < 6.21f
        return Pose(
            bodyBob = sin(p) * 1.6f,
            torsoAngle = sin(p * 0.6f) * 2.2f,
            headAngle = sin(p * 0.74f) * 3.8f,
            armLeft = if(trophy) -139f + sin(p) * 4f else 16f + sin(p) * 5.5f,
            elbowLeft = if(trophy) -20f + sin(p * 1.3f) * 7f else sin(p) * 8f,
            armRight = -16f + sin(p + 1.2f) * 6f,
            elbowRight = sin(p * 0.92f) * 8f,
            legLeft = sin(p * 0.82f) * 4f,
            legRight = -sin(p * 0.82f) * 4f,
            eyelid = if(closing) 0.94f else 0f,
            ears = sin(p * 1.2f) * 3.5f,
            breathing = sin(p * 1.3f) * 1.8f,
            holdsTrophy = trophy
        )
    }
}
