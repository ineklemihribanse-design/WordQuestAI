#!/usr/bin/env python3
"""1.0.25: replace spherical mannequin with connected-profile human anatomy.

The original v1.0.24 rendering is correctly three-dimensional but used large
unconnected ellipsoids and protruding eye/mouth meshes. Keep its native GLES2
renderer, camera zoom/rotation and 2D fallback; replace only the geometry,
lighting and material layout. No external resources, keys or network.
"""
from pathlib import Path
import re

repo = Path(__file__).resolve().parents[1]
src = repo / "source"
pkg = src / "app/src/main/java/com/kelimebulmacasi/a1"
file = pkg / "Teacher3DRenderer.kt"
body = file.read_text(encoding="utf-8")

def once(old, new, label="renderer"):
    global body
    assert body.count(old) == 1, f"{label}: expected unique anchor ({old[:65]!r}), got {body.count(old)}"
    body = body.replace(old, new, 1)

once("    private lateinit var mesh: SphereMesh\n", """    private lateinit var mesh: SphereMesh
    private lateinit var torso: ProfileMesh
    private lateinit var limb: ProfileMesh
    private lateinit var face: ProfileMesh
""")
once("            mesh = SphereMesh()\n", """            mesh = SphereMesh()
            torso = ProfileMesh(floatArrayOf(
                -1f,.30f, -.92f,.53f, -.65f,.70f, -.25f,.68f,
                .15f,.77f, .57f,.91f, .85f,.77f, 1f,.28f,
            ))
            limb = ProfileMesh(floatArrayOf(
                -1f,.16f, -.85f,.62f, -.48f,.82f,
                0f,1f, .45f,.94f, .85f,.70f, 1f,.20f,
            ))
            face = ProfileMesh(floatArrayOf(
                -1f,.21f, -.85f,.56f, -.45f,.89f, 0f,1f,
                .45f,1f, .80f,.83f, 1f,.35f,
            ))
""")
once("""                  vNormal = normalize(mat3(uModel) * aNormal);
""", """                  vNormal = normalize(mat3(uModel) * aNormal);
""")
once("""                  vec3 key = normalize(vec3(-0.48, 0.88, 1.0));
                  vec3 fill = normalize(vec3(0.8, 0.28, 0.6));
                  float diffuse = max(dot(n,key), 0.0);
                  float bounce = max(dot(n,fill), 0.0);
                  float rim = pow(1.0-max(dot(n,vec3(0.0,0.0,1.0)),0.0),2.0);
                  vec3 col = uColor * (0.39+0.50*diffuse+0.17*bounce);
                  col += vec3(0.05,0.10,0.11)*rim;
                  gl_FragColor=vec4(col,1.0);
""", """                  vec3 key = normalize(vec3(-0.48, 0.90, 1.0));
                  vec3 fill = normalize(vec3(0.85, 0.32, 0.52));
                  float diffuse = max(dot(n,key), 0.0);
                  float bounce = max(dot(n,fill), 0.0);
                  float rim = pow(1.0-max(dot(n,vec3(0.0,0.0,1.0)),0.0),2.0);
                  vec3 col = uColor * (0.58+0.34*diffuse+0.12*bounce);
                  col += vec3(0.035,0.055,0.060)*rim;
                  gl_FragColor=vec4(min(col,vec3(1.0)),1.0);
""")
once("            mesh.bind(posLoc, normalLoc)\n", "")
once("""            Matrix.rotateM(head,0,faceIdle,0f,1f,0f)
            Matrix.translateM(head,0,0f,-1.22f,0f)
""","""            Matrix.rotateM(head,0,faceIdle,0f,1f,0f)
            val headNod = if(state.speaking) sin(seconds*3.3f)*1.9f else sin(seconds*.9f)*.7f
            Matrix.rotateM(head,0,headNod,1f,0f,0f)
            Matrix.translateM(head,0,0f,-1.22f,0f)
""")
once("""        headPart: Boolean = false,
    ) {""", """        headPart: Boolean = false,
        shape: Int = 0,
    ) {""")
once("""        mesh.draw()
    }

    private fun renderCharacter""", """        val geometry: CharacterMesh = when(shape) {
            1 -> torso
            2 -> limb
            3 -> face
            else -> mesh
        }
        geometry.bind(posLoc,normalLoc)
        geometry.draw()
    }

    private fun renderCharacter""")

new_character = r"""
    private fun renderCharacter(t: Float) {
        val male = state.actor == "Mert"
        val sophia = state.actor == "Deniz"
        val skin = if(male) 0xC9967D else if(sophia) 0xE4B391 else 0xEDBFA5
        val skinShadow = if(male) 0xBC856F else if(sophia) 0xD49F81 else 0xDCAA92
        val hair = if(male) 0x282B32 else if(sophia) 0x744730 else 0x462B2B
        val hairLight = if(male) 0x3B4148 else if(sophia) 0x986347 else 0x684239
        val coat = if(male) 0x4F5D72 else if(sophia) 0x47749D else 0x176B65
        val coatShade = if(male) 0x3A4A60 else if(sophia) 0x385E83 else 0x15554F
        val trousers = if(male) 0x333A49 else if(sophia) 0x39485A else 0x2B4448
        val inner = if(male) 0xD6D5C9 else if(sophia) 0xE5E2D9 else 0xE3DCCF
        val eyes = if(sophia) 0x4B7081 else if(male) 0x594C38 else 0x65513E
        val gesture = if(state.speaking) sin(t*2.5f)*5.5f else if(state.listening) 2.5f else sin(t*.9f)*1.3f
        val blink = abs((t%4.7f)-3.65f) < .09f
        val glance = sin(t*.48f)*.008f

        // Full body: continuous tapered clothing meshes rather than floating spheres.
        // Shoes and legs intentionally overlap to avoid severed joints.
        for(side in listOf(-1f,1f)) {
            draw(side*.235f,-1.24f,.005f,.225f,.65f,.24f,trousers,rz=side*1.2f,shape=2)
            draw(side*.235f,-1.76f,.115f,.22f,.175f,.35f,0x31343B)
            draw(side*.235f,-1.80f,.31f,.205f,.085f,.23f,0x3D3E43)
        }
        draw(0f,-.67f,-.01f,.47f,.27f,.34f,trousers,shape=1)
        draw(0f,.035f,-.04f,.61f,.76f,.365f,coat,shape=1)

        // Tailored shirt with small overlapping jacket lapels, no bright white giant chest oval.
        draw(0f,.24f,.305f,.22f,.44f,.065f,inner,shape=1)
        draw(-.20f,.36f,.319f,.11f,.36f,.068f,coatShade,rz=-18f,shape=2)
        draw(.20f,.36f,.319f,.11f,.36f,.068f,coatShade,rz=18f,shape=2)
        draw(0f,.77f,.035f,.125f,.22f,.14f,skin,shape=2)

        for(side in listOf(-1f,1f)) {
            // Overlapping upper and lower sleeve meshes with correctly sized hands.
            draw(side*.59f,.45f,-.025f,.235f,.22f,.27f,coat)
            draw(side*.73f,.09f,-.025f,.19f,.47f,.215f,coat,rz=side*(12f+gesture),shape=2)
            draw(side*.80f,-.40f,.015f,.158f,.33f,.19f,coat,rz=side*(2f+gesture*.65f),shape=2)
            draw(side*.82f,-.71f,.05f,.134f,.165f,.126f,skin)
            draw(side*.82f,-.56f,.06f,.162f,.061f,.19f,coatShade)
        }

        // One tapered jaw-and-cheek surface under a non-spherical hair cap.
        // Front facial landmarks sit near the face surface instead of floating.
        draw(0f,1.38f,-.095f,.46f,.61f,.35f,hair,shape=3,headPart=true)
        if(!male) for(side in listOf(-1f,1f)) {
            draw(side*.392f,1.05f,-.15f,.145f,.50f,.23f,hair,shape=2,headPart=true)
            draw(side*.416f,1.11f,-.01f,.102f,.36f,.13f,hairLight,shape=2,headPart=true)
        }
        draw(0f,1.29f,.11f,.424f,.545f,.373f,skin,shape=3,headPart=true)
        for(side in listOf(-1f,1f)) {
            draw(side*.415f,1.225f,.11f,.09f,.14f,.096f,skinShadow,headPart=true)
            // Narrow natural cheek colour, embedded along face contour.
            draw(side*.273f,1.22f,.379f,.065f,.039f,.012f,skinShadow,headPart=true)
        }

        // Layered top hair, with groomed short hair for Ethan.
        draw(0f,1.77f,-.055f,.435f,.18f,.353f,hair,headPart=true)
        draw(-.225f,1.715f,.212f,.205f,.085f,.155f,hair,rz=-12f,headPart=true)
        draw(.215f,1.755f,.185f,.20f,.075f,.155f,hairLight,rz=11f,headPart=true)
        if(male) {
            draw(0f,1.827f,-.035f,.40f,.12f,.31f,hair,headPart=true)
        } else {
            draw(-.372f,1.51f,.035f,.093f,.275f,.20f,hair,rz=-7f,headPart=true)
            draw(.372f,1.52f,.035f,.095f,.27f,.19f,hair,rz=7f,headPart=true)
        }

        // Proportional eye openings: flattened almonds, subtle brows and embedded irises.
        for(side in listOf(-1f,1f)) {
            val x = side*.182f
            draw(x,1.49f,.427f,.104f,.017f,.018f,hair,rz=side*4f,headPart=true)
            draw(x,1.402f,.443f,.098f,if(blink).007f else .045f,.018f,0xFBF3E8,headPart=true)
            if(!blink) {
                draw(x+glance,1.401f,.458f,.037f,.039f,.013f,eyes,headPart=true)
                draw(x+glance,1.401f,.468f,.022f,.026f,.010f,0x28262B,headPart=true)
                draw(x+glance-.009f,1.415f,.478f,.012f,.012f,.007f,0xFFFFFF,headPart=true)
            }
            draw(x,1.357f,.436f,.104f,.011f,.016f,skinShadow,headPart=true)
        }

        // Soft integrated nose bridge, smaller nostrils and natural closed mouth.
        draw(0f,1.30f,.463f,.046f,.10f,.052f,skin,headPart=true)
        draw(0f,1.222f,.497f,.067f,.044f,.052f,skinShadow,headPart=true)
        val mouthOpen = if(state.speaking)
            .012f+abs(sin(t*10.5f*state.speechRate.coerceIn(.6f,1.4f)))*.037f
            else .010f
        draw(0f,1.090f,.452f,.125f,mouthOpen,.013f,0x864A50,headPart=true)
        draw(0f,1.064f-mouthOpen*.24f,.452f,.114f,.016f,.014f,0xBE7776,headPart=true)

        // Thin spectacle frames for Emma instead of two opaque eyeball discs.
        if(state.actor=="Elif") {
            for(side in listOf(-1f,1f)) {
                val x=side*.182f
                draw(x,1.455f,.476f,.113f,.010f,.010f,0x6A514B,headPart=true)
                draw(x,1.349f,.473f,.108f,.010f,.010f,0x6A514B,headPart=true)
                draw(side*.293f,1.400f,.473f,.010f,.060f,.010f,0x6A514B,headPart=true)
                draw(side*.071f,1.400f,.473f,.010f,.06f,.010f,0x6A514B,headPart=true)
            }
            draw(0f,1.415f,.480f,.067f,.011f,.012f,0x6A514B,headPart=true)
        } else if(sophia) {
            for(side in listOf(-1f,1f)) {
                draw(side*.438f,1.09f,.20f,.035f,.05f,.030f,0xC8A978,headPart=true)
            }
        }

        // Contact shadow anchors the feet to a floor.
        draw(0f,-1.94f,-.10f,.65f,.020f,.37f,0x10282C)
    }
"""

pos_start=body.index("    private fun renderCharacter(t: Float) {")
pos_end=body.index("\n}\n\nprivate class SphereMesh {",pos_start)
body=body[:pos_start]+new_character.rstrip("\n")+body[pos_end:]

# Mesh interface and geometries: cross-section profiles with continuous rings
# produce shoulders, waists, calves and jaw tapers with proper 3D surfaces.
body=body.replace("private class SphereMesh {", "private interface CharacterMesh {\n    fun bind(posLoc: Int, normalLoc: Int)\n    fun draw()\n}\n\nprivate class SphereMesh : CharacterMesh {",1)
body=body.replace("    fun bind(posLoc: Int, normalLoc: Int) {", "    override fun bind(posLoc: Int, normalLoc: Int) {",1)
body=body.replace("    fun draw() {", "    override fun draw() {",1)

profile_mesh = r"""
/** Rotational 3D mesh with tapered, continuous ring sections. */
private class ProfileMesh(private val profile: FloatArray) : CharacterMesh {
    private val buffer: FloatBuffer
    private val indices: ShortBuffer
    private val count: Int
    init {
        require(profile.size >= 8 && profile.size % 2 == 0)
        val rings = profile.size / 2
        val segments = 32
        val vertices = FloatArray(rings*(segments+1)*6)
        var k = 0
        for (i in 0 until rings) {
            val y=profile[2*i]
            val r=profile[2*i+1]
            val prev=max(0,i-1)
            val next=min(rings-1,i+1)
            val dr=profile[2*next+1]-profile[2*prev+1]
            val dy=profile[2*next]-profile[2*prev]
            val slope=if(abs(dy)<.00001f) 0f else dr/dy
            for (j in 0..segments) {
                val a=2f*Math.PI.toFloat()*j/segments
                val xx=cos(a)
                val zz=sin(a)
                val scale=1f/hypot(1f,slope)
                vertices[k++]=xx*r
                vertices[k++]=y
                vertices[k++]=zz*r
                vertices[k++]=xx*scale
                vertices[k++]=-slope*scale
                vertices[k++]=zz*scale
            }
        }
        val ii=ShortArray((rings-1)*segments*6)
        k=0
        for (i in 0 until rings-1) for (j in 0 until segments) {
            val a=(i*(segments+1)+j).toShort()
            val b=((i+1)*(segments+1)+j).toShort()
            ii[k++]=a;ii[k++]=(a+1).toShort();ii[k++]=b
            ii[k++]=b;ii[k++]=(a+1).toShort();ii[k++]=(b+1).toShort()
        }
        buffer=ByteBuffer.allocateDirect(vertices.size*4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        buffer.put(vertices).position(0)
        indices=ByteBuffer.allocateDirect(ii.size*2).order(ByteOrder.nativeOrder()).asShortBuffer()
        indices.put(ii).position(0)
        count=ii.size
    }
    override fun bind(posLoc: Int, normalLoc: Int) {
        buffer.position(0)
        GLES20.glVertexAttribPointer(posLoc,3,GLES20.GL_FLOAT,false,24,buffer)
        buffer.position(3)
        GLES20.glVertexAttribPointer(normalLoc,3,GLES20.GL_FLOAT,false,24,buffer)
        buffer.position(0)
    }
    override fun draw() {
        indices.position(0)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES,count,GLES20.GL_UNSIGNED_SHORT,indices)
    }
}
"""
body += "\n"+profile_mesh
file.write_text(body,encoding="utf-8")

# Keep stable application ID and signing. The update installs over v1.0.24.
gradle=src/"app/build.gradle.kts"
g=gradle.read_text(encoding="utf-8")
assert g.count("versionCode = 47")==1 and g.count('versionName = "1.0.24-native-fullbody-3d"')==1
assert 'applicationIdSuffix = ".offline101"' in g
g=g.replace("versionCode = 47","versionCode = 48")
g=g.replace('versionName = "1.0.24-native-fullbody-3d"',
            'versionName = "1.0.25-teacher-anatomy-polish"')
gradle.write_text(g,encoding="utf-8")
print("1.0.25: continuous 3D anatomy, integrated eye/face, natural mouth and camera preserved")
