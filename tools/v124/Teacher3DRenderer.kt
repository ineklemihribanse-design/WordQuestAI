package com.kelimebulmacasi.a1

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Native OpenGL ES 2.0 3D stage. No WebView or remote model/image fetching.
 * Stable camera controls: one finger = unlimited yaw, two fingers = distance,
 * double tap = reset; head-centred close-up at maximum zoom.
 */
internal class Teacher3DSettings {
    @Volatile var actor = "Elif" // Old enum names are kept for saved chat compatibility.
    @Volatile var speaking = false
    @Volatile var listening = false
    @Volatile var thinking = false
    @Volatile var speechRate = 1f
    @Volatile var yaw = 0f
    @Volatile var pitch = 0f
    @Volatile var distance = 7.2f

    fun reset() {
        yaw = 0f
        pitch = 0f
        distance = 7.2f
    }
}

internal class NativeTeacher3DView(
    context: Context,
    private val settings: Teacher3DSettings,
    onFirstFrame: () -> Unit,
    onError: () -> Unit,
) : GLSurfaceView(context) {
    private val main = Handler(Looper.getMainLooper())
    private val scene = Teacher3DRenderer(
        settings,
        onReady = { main.post { onFirstFrame() } },
        onFailure = { main.post { onError() } },
    )
    private var previousX = 0f
    private var previousY = 0f
    private var previousPinch = 0f
    private var lastTap = 0L

    init {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        preserveEGLContextOnPause = true
        setRenderer(scene)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val pointerCount = event.pointerCount
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (event.eventTime - lastTap < 320L) settings.reset()
                lastTap = event.eventTime
                previousX = event.x
                previousY = event.y
                previousPinch = 0f
                return true
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                previousPinch = if (pointerCount > 1) {
                    hypot(event.getX(0) - event.getX(1), event.getY(0) - event.getY(1))
                } else 0f
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (pointerCount > 1) {
                    val span = hypot(event.getX(0) - event.getX(1), event.getY(0) - event.getY(1))
                    if (previousPinch > 16f && span > 16f) {
                        val change = previousPinch / span
                        settings.distance = (settings.distance * change).coerceIn(2.15f, 9.2f)
                    }
                    previousPinch = span
                } else {
                    val dx = event.x - previousX
                    val dy = event.y - previousY
                    settings.yaw += dx * .32f
                    settings.pitch = (settings.pitch + dy * .13f).coerceIn(-22f, 22f)
                    previousX = event.x
                    previousY = event.y
                }
                return true
            }
            MotionEvent.ACTION_POINTER_UP -> {
                previousPinch = 0f
                if (event.pointerCount > 1) {
                    val remaining = if (event.actionIndex == 0) 1 else 0
                    previousX = event.getX(remaining)
                    previousY = event.getY(remaining)
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                previousPinch = 0f
                return true
            }
        }
        return true
    }
}

/** Stylized, genuine 3D figure made of lit triangle meshes, drawn at full body. */
private class Teacher3DRenderer(
    private val state: Teacher3DSettings,
    private val onReady: () -> Unit,
    private val onFailure: () -> Unit,
) : GLSurfaceView.Renderer {
    private var program = 0
    private var posLoc = -1
    private var normalLoc = -1
    private var mvpLoc = -1
    private var modelLoc = -1
    private var colorLoc = -1
    private lateinit var mesh: SphereMesh
    private val projection = FloatArray(16)
    private val camera = FloatArray(16)
    private val vp = FloatArray(16)
    private val root = FloatArray(16)
    private val head = FloatArray(16)
    private var width = 1
    private var height = 1
    private var ready = false
    private var broken = false
    private val started = System.nanoTime()
    private var currentYaw = 0f
    private var currentPitch = 0f
    private var currentDistance = 7.2f

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        ready = false
        broken = false
        try {
            val vertex = """
                uniform mat4 uMvp;
                uniform mat4 uModel;
                attribute vec3 aPos;
                attribute vec3 aNormal;
                varying vec3 vNormal;
                varying vec3 vWorld;
                void main() {
                  vec4 world = uModel * vec4(aPos, 1.0);
                  vWorld = world.xyz;
                  vNormal = normalize(mat3(uModel) * aNormal);
                  gl_Position = uMvp * vec4(aPos, 1.0);
                }
            """.trimIndent()
            val fragment = """
                precision mediump float;
                uniform vec3 uColor;
                varying vec3 vNormal;
                varying vec3 vWorld;
                void main() {
                  vec3 n = normalize(vNormal);
                  vec3 key = normalize(vec3(-0.48, 0.88, 1.0));
                  vec3 fill = normalize(vec3(0.8, 0.28, 0.6));
                  float diffuse = max(dot(n,key), 0.0);
                  float bounce = max(dot(n,fill), 0.0);
                  float rim = pow(1.0-max(dot(n,vec3(0.0,0.0,1.0)),0.0),2.0);
                  vec3 col = uColor * (0.39+0.50*diffuse+0.17*bounce);
                  col += vec3(0.05,0.10,0.11)*rim;
                  gl_FragColor=vec4(col,1.0);
                }
            """.trimIndent()
            val vert = compile(GLES20.GL_VERTEX_SHADER, vertex)
            val frag = compile(GLES20.GL_FRAGMENT_SHADER, fragment)
            program = GLES20.glCreateProgram()
            GLES20.glAttachShader(program, vert)
            GLES20.glAttachShader(program, frag)
            GLES20.glLinkProgram(program)
            val ok = IntArray(1)
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, ok, 0)
            check(ok[0] == GLES20.GL_TRUE) { GLES20.glGetProgramInfoLog(program) }
            GLES20.glDeleteShader(vert)
            GLES20.glDeleteShader(frag)
            posLoc = GLES20.glGetAttribLocation(program, "aPos")
            normalLoc = GLES20.glGetAttribLocation(program, "aNormal")
            mvpLoc = GLES20.glGetUniformLocation(program, "uMvp")
            modelLoc = GLES20.glGetUniformLocation(program, "uModel")
            colorLoc = GLES20.glGetUniformLocation(program, "uColor")
            check(posLoc >= 0 && normalLoc >= 0 && mvpLoc >= 0 && modelLoc >= 0 && colorLoc >= 0)
            mesh = SphereMesh()
            GLES20.glEnable(GLES20.GL_DEPTH_TEST)
            GLES20.glDepthFunc(GLES20.GL_LEQUAL)
            GLES20.glDisable(GLES20.GL_CULL_FACE)
            GLES20.glDisable(GLES20.GL_BLEND)
        } catch (_: Throwable) {
            broken = true
            onFailure()
        }
    }

    private fun compile(kind: Int, src: String): Int {
        val id = GLES20.glCreateShader(kind)
        check(id != 0)
        GLES20.glShaderSource(id, src)
        GLES20.glCompileShader(id)
        val ok = IntArray(1)
        GLES20.glGetShaderiv(id, GLES20.GL_COMPILE_STATUS, ok, 0)
        check(ok[0] == GLES20.GL_TRUE) { GLES20.glGetShaderInfoLog(id) }
        return id
    }

    override fun onSurfaceChanged(gl: GL10?, w: Int, h: Int) {
        width = max(1, w)
        height = max(1, h)
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        if (broken || program == 0) return
        try {
            val seconds = (System.nanoTime() - started) / 1_000_000_000f
            currentYaw += (state.yaw - currentYaw) * .15f
            currentPitch += (state.pitch - currentPitch) * .15f
            currentDistance += (state.distance - currentDistance) * .16f
            val zoom = ((7.2f-currentDistance) / 5.05f).coerceIn(0f,1f)
            val targetY = zoom * 1.33f
            val aspect = width.toFloat() / height.toFloat()
            val fov = if (aspect < .8f) 43f else 40f
            Matrix.perspectiveM(projection, 0, fov, aspect, .1f, 50f)
            Matrix.setLookAtM(camera, 0, 0f, targetY, currentDistance, 0f, targetY, 0f, 0f, 1f, 0f)
            Matrix.multiplyMM(vp,0,projection,0,camera,0)
            GLES20.glClearColor(.061f, .158f, .183f, 1f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
            GLES20.glUseProgram(program)
            GLES20.glEnableVertexAttribArray(posLoc)
            GLES20.glEnableVertexAttribArray(normalLoc)
            mesh.bind(posLoc, normalLoc)
            Matrix.setIdentityM(root,0)
            Matrix.rotateM(root,0,currentYaw,0f,1f,0f)
            Matrix.rotateM(root,0,currentPitch,1f,0f,0f)
            Matrix.translateM(root,0,0f,sin(seconds*1.4f)*.012f,0f)
            val faceIdle = sin(seconds*.75f)*.8f
            System.arraycopy(root,0,head,0,16)
            Matrix.translateM(head,0,0f,1.22f,0f)
            Matrix.rotateM(head,0,faceIdle,0f,1f,0f)
            Matrix.translateM(head,0,0f,-1.22f,0f)
            renderCharacter(seconds)
            // A successful GL call alone is not enough: verify real geometry
            // changed a framebuffer pixel before hiding the working 2D fallback.
            if (!ready && GLES20.glGetError()==GLES20.GL_NO_ERROR) {
                val sample = ByteBuffer.allocateDirect(4)
                GLES20.glReadPixels(
                    width / 2, height / 2, 1, 1,
                    GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, sample,
                )
                val red = sample.get(0).toInt() and 255
                val green = sample.get(1).toInt() and 255
                val blue = sample.get(2).toInt() and 255
                val drawn = abs(red - 16) + abs(green - 40) + abs(blue - 47) > 32
                if (drawn && GLES20.glGetError()==GLES20.GL_NO_ERROR) {
                    ready = true
                    onReady()
                }
            }
        } catch (_: Throwable) {
            broken = true
            onFailure()
        }
    }

    private fun draw(
        x: Float, y: Float, z: Float,
        sx: Float, sy: Float, sz: Float,
        color: Int, rz: Float = 0f, ry: Float = 0f,
        headPart: Boolean = false,
    ) {
        val local = FloatArray(16)
        Matrix.setIdentityM(local,0)
        Matrix.translateM(local,0,x,y,z)
        if (rz != 0f) Matrix.rotateM(local,0,rz,0f,0f,1f)
        if (ry != 0f) Matrix.rotateM(local,0,ry,0f,1f,0f)
        Matrix.scaleM(local,0,sx,sy,sz)
        val model = FloatArray(16)
        Matrix.multiplyMM(model,0,if(headPart)head else root,0,local,0)
        val mvp = FloatArray(16)
        Matrix.multiplyMM(mvp,0,vp,0,model,0)
        GLES20.glUniformMatrix4fv(modelLoc,1,false,model,0)
        GLES20.glUniformMatrix4fv(mvpLoc,1,false,mvp,0)
        GLES20.glUniform3f(colorLoc,
            ((color shr 16) and 255)/255f, ((color shr 8) and 255)/255f, (color and 255)/255f)
        mesh.draw()
    }

    private fun renderCharacter(t: Float) {
        val actor = state.actor
        val male = actor == "Mert"
        val sophia = actor == "Deniz"
        val skin = if(male) 0xC7957C else if(sophia) 0xE6B18E else 0xE7C3A7
        val hair = if(male) 0x252A31 else if(sophia) 0x82523A else 0x513228
        val hairLight = if(male) 0x48505A else if(sophia) 0xA5754B else 0x734D3B
        val cloth = if(male) 0x54617B else if(sophia) 0x416FA9 else 0x136C67
        val pants = if(male) 0x2D3547 else if(sophia) 0x343F59 else 0x28404B
        val shirt = if(male) 0xE0D4C1 else if(sophia) 0xDCE8F0 else 0xD2DFCD
        val dark = 0x312B2A
        val gesture = if(state.speaking) sin(t*3.1f)*9f else if(state.listening) 5f else sin(t*.9f)*2f
        val nod = if(state.speaking) sin(t*5.7f)*.025f else sin(t*.7f)*.012f

        // Feet, trouser legs, lower body: a full-height articulated character.
        for(side in listOf(-1f,1f)) {
            draw(side*.28f,-1.20f,0f,.23f,.68f,.26f,pants,rz=side*1.6f)
            draw(side*.29f,-1.78f,.18f,.26f,.18f,.43f,0x25252C)
            draw(side*.29f,-1.83f,.40f,.24f,.09f,.20f,0x373942)
            draw(side*.18f,-.66f,0f,.38f,.20f,.28f,pants)
        }
        draw(0f,-.73f,-.02f,.54f,.34f,.31f,pants)
        // Upper body, jacket and collar.
        draw(0f,.03f,0f,.63f,.82f,.34f,cloth)
        draw(0f,.24f,.28f,.31f,.48f,.085f,shirt)
        draw(-.24f,.39f,.37f,.18f,.35f,.11f,cloth,rz=-25f)
        draw(.24f,.39f,.37f,.18f,.35f,.11f,cloth,rz=25f)
        draw(0f,.78f,0f,.14f,.22f,.14f,skin)
        // Arms with gesturing forearms, distinct rounded shoulders.
        for(side in listOf(-1f,1f)) {
            draw(side*.69f,.30f,0f,.28f,.34f,.31f,cloth)
            draw(side*.87f,-.08f,.02f,.19f,.46f,.22f,cloth,rz=side*(17f+gesture))
            draw(side*.98f,-.57f,.20f,.19f,.39f,.20f,cloth,rz=side*(-14f+gesture*.6f))
            draw(side*1.02f,-.89f,.27f,.18f,.19f,.15f,skin)
        }

        // Head: full 3D volume, jaw, ears, scalp and individual locks.
        draw(0f,1.24f,-.10f,.55f,.69f,.49f,hair,headPart=true)
        if (!male) {
            for(side in listOf(-1f,1f)) {
                draw(side*.49f,1.01f,-.18f,.18f,.66f,.30f,hair,headPart=true)
                draw(side*.53f,.78f,-.04f,.15f,.42f,.23f,hairLight,headPart=true)
            }
        }
        draw(0f,1.22f,.13f,.48f,.58f,.45f,skin,headPart=true)
        draw(0f,.92f,.31f,.36f,.20f,.27f,skin,headPart=true)
        for(side in listOf(-1f,1f)) {
            draw(side*.46f,1.20f,.11f,.13f,.21f,.14f,skin,headPart=true)
            draw(side*.29f,1.42f,.49f,.15f,.036f,.042f,hair,rz=side*5f,headPart=true)
        }
        // Upper hair silhouette and swept fringe.
        draw(0f,1.73f,-.07f,.54f,.28f,.49f,hair,headPart=true)
        draw(-.22f,1.63f,.31f,.34f,.13f,.26f,hair,rz=-14f,headPart=true)
        draw(.24f,1.68f,.27f,.29f,.13f,.23f,hairLight,rz=13f,headPart=true)
        if(male) draw(0f,1.77f,.14f,.50f,.16f,.34f,hair,headPart=true)
        // Eyes and iris highlights.
        val blinking = abs((t%4.5f)-3.6f)<.08f
        for(side in listOf(-1f,1f)) {
            draw(side*.22f,1.38f,.525f,.14f,if(blinking).012f else .093f,.07f,0xFFF9F3,headPart=true)
            if(!blinking) {
                draw(side*.22f,1.38f,.588f,.065f,.071f,.029f,if(sophia)0x55758A else 0x523D33,headPart=true)
                draw(side*.22f-.018f,1.411f,.613f,.022f,.021f,.012f,0xFFFFFF,headPart=true)
            }
            draw(side*.33f,1.16f,.495f,.12f,.055f,.028f,0xD6A38D,headPart=true)
        }
        // Protruding nose and mouth. Motion follows TTS speaking state (not phoneme data).
        draw(0f,1.24f,.587f,.105f,.18f,.13f,skin,headPart=true)
        draw(0f,1.16f,.67f,.14f,.06f,.062f,0xC99080,headPart=true)
        val opening = if(state.speaking) .018f+abs(sin(t*14f*state.speechRate))*.092f else .012f
        draw(0f,1.015f+nod,.57f,.19f,.035f+opening,.05f,0x6E363C,headPart=true)
        draw(0f,.97f-opening*.4f,.58f,.17f,.036f,.04f,0xB97579,headPart=true)
        // Teacher 1 (Emma) wears 3D glasses; Sophia has earrings.
        if(actor=="Elif") {
            for(side in listOf(-1f,1f)) {
                draw(side*.22f,1.40f,.605f,.185f,.118f,.025f,0x705951,headPart=true)
                draw(side*.22f,1.40f,.628f,.154f,.09f,.018f,0xFFF6E3,headPart=true)
                draw(side*.22f,1.40f,.643f,.067f,.067f,.015f,0x51382E,headPart=true)
                draw(side*.22f-.018f,1.426f,.660f,.025f,.027f,.009f,0xFFFFFF,headPart=true)
            }
            draw(0f,1.41f,.62f,.13f,.03f,.025f,0x705951,headPart=true)
        } else if(sophia) {
            for(side in listOf(-1f,1f)) draw(side*.54f,1.02f,.20f,.05f,.09f,.05f,0xD7C092,headPart=true)
        }
    }
}

private class SphereMesh {
    private val buffer: FloatBuffer
    private val indexBuffer: ShortBuffer
    private val count: Int
    init {
        val rows = 22
        val cols = 30
        val vertices = FloatArray((rows+1)*(cols+1)*6)
        var ix = 0
        for(i in 0..rows) {
            val theta = Math.PI.toFloat()*i/rows
            for(j in 0..cols) {
                val phi = 2f*Math.PI.toFloat()*j/cols
                val x = sin(theta)*cos(phi)
                val y = cos(theta)
                val z = sin(theta)*sin(phi)
                vertices[ix++] = x;vertices[ix++] = y;vertices[ix++] = z
                vertices[ix++] = x;vertices[ix++] = y;vertices[ix++] = z
            }
        }
        val inds = ShortArray(rows*cols*6)
        var k = 0
        for(i in 0 until rows) for(j in 0 until cols) {
            val a = (i*(cols+1)+j).toShort()
            val b = ((i+1)*(cols+1)+j).toShort()
            inds[k++]=a; inds[k++]=(a+1).toShort(); inds[k++]=b
            inds[k++]=b; inds[k++]=(a+1).toShort(); inds[k++]=(b+1).toShort()
        }
        buffer = ByteBuffer.allocateDirect(vertices.size*4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        buffer.put(vertices).position(0)
        indexBuffer = ByteBuffer.allocateDirect(inds.size*2).order(ByteOrder.nativeOrder()).asShortBuffer()
        indexBuffer.put(inds).position(0)
        count = inds.size
    }

    fun bind(posLoc: Int, normalLoc: Int) {
        buffer.position(0)
        GLES20.glVertexAttribPointer(posLoc,3,GLES20.GL_FLOAT,false,6*4,buffer)
        buffer.position(3)
        GLES20.glVertexAttribPointer(normalLoc,3,GLES20.GL_FLOAT,false,6*4,buffer)
        buffer.position(0)
    }
    fun draw() {
        indexBuffer.position(0)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES,count,GLES20.GL_UNSIGNED_SHORT,indexBuffer)
    }
}
