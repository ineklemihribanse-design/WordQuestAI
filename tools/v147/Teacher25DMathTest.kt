package com.kelimebulmacasi.a1

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class Teacher25DMathTest {
    private fun vert(v: FloatArray, x: Int, y: Int): Pair<Float, Float> {
        val index=(y*(Teacher25DMath.SEG_X+1)+x)*2
        return v[index] to v[index+1]
    }

    @Test fun neutralMeshExactlyMatchesOneSourceImage() {
        val v=FloatArray(Teacher25DMath.FLOAT_COUNT)
        Teacher25DMath.buildVertices(Teacher25DMath.Pose(), v)
        for (row in 0..Teacher25DMath.SEG_Y) for (col in 0..Teacher25DMath.SEG_X) {
            val p=vert(v,col,row)
            assertEquals(col*8f,p.first,.0001f)
            assertEquals(row*8f,p.second,.0001f)
        }
    }

    @Test fun mouthDeformsOnlyLowerFaceAndDoesNotShakeHairOrEyes() {
        val neutral=FloatArray(Teacher25DMath.FLOAT_COUNT)
        val wide=FloatArray(Teacher25DMath.FLOAT_COUNT)
        Teacher25DMath.buildVertices(Teacher25DMath.Pose(),neutral)
        Teacher25DMath.buildVertices(Teacher25DMath.Pose(jaw=1f),wide)
        val lip=vert(wide,15,12)
        assertTrue("lower lip must move",lip.second>vert(neutral,15,12).second)
        for(y in 0..6) for(x in 0..Teacher25DMath.SEG_X) {
            assertEquals(neutral[(y*33+x)*2],wide[(y*33+x)*2],0f)
            assertEquals(neutral[(y*33+x)*2+1],wide[(y*33+x)*2+1],0f)
        }
    }

    @Test fun blinkingMovesEyeAreaButNotChinOrShirt() {
        val a=FloatArray(Teacher25DMath.FLOAT_COUNT)
        val b=FloatArray(Teacher25DMath.FLOAT_COUNT)
        Teacher25DMath.buildVertices(Teacher25DMath.Pose(),a)
        Teacher25DMath.buildVertices(Teacher25DMath.Pose(blink=1f),b)
        assertTrue("eye must deform",abs(vert(a,14,7).second-vert(b,14,7).second)>.01f)
        assertEquals(vert(a,16,30).second,vert(b,16,30).second,0f)
    }

    @Test fun speakingIsRequiredForMouthOrHandMovement() {
        val cue=AvatarMouthFrame()
        assertEquals(0f,Teacher25DMath.mouthTarget(
            AvatarMode.IDLE,cue,10000,9000),0f)
        assertEquals(0f,Teacher25DMath.gesture(12000,0,true),0f)
        assertEquals(0f,Teacher25DMath.gesture(12000,9000,false),0f)
        assertTrue(Teacher25DMath.mouthTarget(
            AvatarMode.SPEAKING,cue,10000,9000)>0f)
    }

    @Test fun softTransitionsRemainBoundedAndBlinkIsBrief() {
        assertTrue(Teacher25DMath.approach(0f,1f,33,48f) in 0f..1f)
        val peaks=(0L..4399L step 10L).count {
            Teacher25DMath.eyelid(it)>.01f
        }
        assertTrue("blink must last less than 300ms",peaks<=30)
        assertTrue("eyes reopen",peaks>2)
    }
}
