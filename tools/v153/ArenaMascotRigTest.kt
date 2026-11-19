package com.kelimebulmacasi.a1

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class ArenaMascotRigTest {
    @Test fun exactlyFiveDistinctSelectableCharacters() {
        assertEquals(listOf("Bip","Pufi","Mimo","Lila","Tiko"),
            ArenaMascotRig.characters.map { it.name })
        assertEquals(5,ArenaMascotRig.characters.map { it.id }.toSet().size)
    }

    @Test fun supportsOldAvatarsAndNewAvatarsSafely() {
        listOf("robot","fox","owl","rabbit","bear").forEach {
            assertTrue(ArenaMascotRig.isValid(it))
            assertEquals(it,ArenaMascotRig.safe(it))
        }
        assertFalse(ArenaMascotRig.isValid("stranger"))
        assertEquals("robot",ArenaMascotRig.safe("stranger"))
        assertEquals("robot",ArenaMascotRig.safe(null))
    }

    @Test fun everyJointHasAnExistingParentAndTrophyIsChildOfHand() {
        assertTrue(ArenaMascotRig.isHierarchical())
        assertEquals(32,ArenaMascotRig.bones.size)
        assertEquals("LEFT_HAND",
            ArenaMascotRig.bones.single {it.id=="TROPHY_PIVOT"}.parent)
        assertTrue(ArenaMascotRig.bones.any {it.id=="LEFT_ELBOW"})
        assertTrue(ArenaMascotRig.bones.any {it.id=="RIGHT_ELBOW"})
        assertTrue(ArenaMascotRig.bones.any {it.id=="LEFT_KNEE"})
        assertTrue(ArenaMascotRig.bones.any {it.id=="RIGHT_KNEE"})
    }

    @Test fun allFiveHaveBoundedSmoothIdleAndBlinkAnimation() {
        ArenaMascotRig.characters.forEach { actor ->
            val samples=(0..500).map {
                ArenaMascotRig.pose(actor.id,it*(2f*PI.toFloat())/500f)
            }
            assertTrue(samples.any {it.eyelid>0.8f})
            assertTrue(samples.any {it.eyelid==0f})
            assertTrue(samples.all {it.headAngle in -5f..5f})
            assertTrue(samples.all {it.bodyBob in -2f..2f})
            assertTrue(samples.all {it.legLeft in -5f..5f})
            assertTrue(samples.all {!it.holdsTrophy})
        }
    }

    @Test fun selectedCharacterAlwaysMovesHandWithRaisedTrophy() {
        ArenaMascotRig.characters.forEach { actor ->
            val poses=(0..24).map {ArenaMascotRig.pose(actor.id,it*.17f,true)}
            assertTrue(poses.all { it.holdsTrophy && it.armLeft in -164f..-158f })
            assertTrue(poses.any { it.elbowLeft != poses[0].elbowLeft })
            assertTrue(poses.any { it.headAngle != poses[0].headAngle })
        }
    }

    @Test fun wristAndTrophyStayWithinCanvasThroughoutFiveAnimations() {
        // Shoulder and wrist transformations must match ArenaRiggedMascot.
        ArenaMascotRig.characters.forEach { actor ->
            for (frame in 0..500) {
                val t=frame*(2f*PI.toFloat())/500f
                val p=ArenaMascotRig.pose(actor.id,t,true)
                val upper=Math.toRadians(p.armLeft.toDouble())
                val fore=Math.toRadians((p.armLeft+p.elbowLeft).toDouble())
                val wristX=111.0-31.0*sin(upper)-24.0*sin(fore)
                val wristY=103.0+31.0*cos(upper)+24.0*cos(fore)
                val torso=Math.toRadians(p.torsoAngle.toDouble())
                val dx=wristX-80.0
                val dy=wristY-115.0
                val cupX=80.0+dx*cos(torso)-dy*sin(torso)
                val cupY=115.0+dx*sin(torso)+dy*cos(torso)+p.bodyBob
                assertTrue("Right trophy edge cropped for ${actor.id}: $cupX",
                    cupX+28.0<160.0)
                assertTrue("Left trophy edge cropped for ${actor.id}: $cupX",
                    cupX-28.0>0.0)
                assertTrue("Upper trophy edge cropped for ${actor.id}: $cupY",
                    cupY-46.0>0.0)
            }
        }
    }
}
