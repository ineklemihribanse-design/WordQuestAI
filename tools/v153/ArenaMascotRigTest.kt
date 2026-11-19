package com.kelimebulmacasi.a1

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.PI

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
        assertEquals(31,ArenaMascotRig.bones.size)
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
            assertTrue(poses.all { it.holdsTrophy && it.armLeft in -145f..-132f })
            assertTrue(poses.any { it.elbowLeft != poses[0].elbowLeft })
            assertTrue(poses.any { it.headAngle != poses[0].headAngle })
        }
    }
}
