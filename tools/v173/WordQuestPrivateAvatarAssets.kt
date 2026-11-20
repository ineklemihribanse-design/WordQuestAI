package com.kelimebulmacasi.a1

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Private on-device importer for the user's purchased/received GLBs.
 * No models are redistributed in the public repository or release binaries.
 * 1x import of the two user-supplied ZIP archives via Android document picker.
 */
internal object WordQuestPrivateAvatarAssets {
    private data class Model(
        val name:String, val size:Long, val digest:String, val id:String
    )
    private val expected=listOf(
        Model("astra_autorig_qa.glb",2665860L,
            "68aaf643c35a0adbb7349aa9688b960bdf31c44ec996afd3db4ee85a7ddb6248","astra"),
        Model("piko_autorig_qa.glb",4984000L,
            "c97f59d8c20f472bff094afcba24eddcf9b7d584931c19c6a1c9a133da8eb7d9","piko"),
        Model("nobi_autorig_qa.glb",5056852L,
            "89e25bc4616a64f90102e686c3ef6a470bc7cfc03b405947d6c7e58fc6d7989f","nobi"),
        Model("tera_autorig_qa.glb",27163048L,
            "eede14fe9a72025ee3398efedf071644056968d689a9bd7f4baba44a2bd45ce1","tera"),
        Model("lopi_autorig_private_qa.glb",7166648L,
            "951772a14bd7aca714657c2ac214ee9963c0f9833b32c8e15cb3f6e9e29c44a9","lopi"),
        Model("tomba_autorig_private_qa.glb",5539004L,
            "1693450f13ea5a37fd29941ef93419ad0350a245dce6d413dda866cb30c714a0","tomba"),
        Model("foksi_autorig_private_qa.glb",1722488L,
            "e46540e142af7974186f139d10fb07b467fa8fc7db328941e29b4e7970f1a118","foksi"),
        Model("moka_autorig_private_qa.glb",2427868L,
            "427f1badea527262013dcabc9100c6b158825f23c6d8107a4dcbbbc6fc2ad0e2","moka"),
        Model("pera_autorig_private_qa.glb",2391308L,
            "fbc9d2c3d7ada142582b837c22db10dd70a5928cfb1af397d077bec221167f1d","pera"),
        Model("gigi_autorig_private_qa.glb",2239556L,
            "a5b7b2f9a640831fa957480778df1428a0ad79c57de0699ba5d7a522572edf2a","gigi")
    )
    private val byName=expected.associateBy{it.name}
    private val byId=expected.associateBy{it.id}
    private const val ROOT="wordquest/private_avatars_v173"
    private const val PREVIEW_NAME="wq173_source_models_preview.webp"
    private const val PREVIEW_SIZE=6606L
    private const val PREVIEW_SHA="5309a66b51463da64d852861a90c1535e57528f01b97a425bff0fd69a5b54a7f"

    private fun folder(context:Context):File =
        File(context.filesDir,ROOT).apply { if(!exists())mkdirs() }

    fun modelFile(context:Context,id:String):File? {
        val model=byId[id]?:return null
        val file=File(folder(context),model.name)
        return file.takeIf{it.isFile && it.length()==model.size}
    }

    /** Portraits created from the ten real purchased GLBs; private on device. */
    fun previewFile(context:Context):File? {
        val file=File(folder(context),PREVIEW_NAME)
        return file.takeIf{it.isFile && it.length()==PREVIEW_SIZE}
    }

    fun installed(context:Context,id:String)=modelFile(context,id)!=null
    fun count(context:Context):Int=expected.count{installed(context,it.id)}

    data class ImportResult(val newCount:Int,val total:Int,val names:List<String>)

    /**
     * Run on Dispatchers.IO. Streams ZIP and validates exact user-source bytes
     * before atomically moving them into private app storage.
     */
    fun importZip(context:Context,uri:Uri):ImportResult {
        val root=folder(context)
        val staged=mutableListOf<Pair<File,File>>()
        val imported=mutableListOf<String>()
        val found=mutableSetOf<String>()
        var aggregate=0L
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ZipInputStream(stream.buffered()).use { zip ->
                    while(true) {
                        val entry:ZipEntry=zip.nextEntry?:break
                        val name=entry.name.substringAfterLast('/')
                        if(!entry.isDirectory && name==PREVIEW_NAME) {
                            val previewTemp=File.createTempFile("wq173_preview_","_staging",root)
                            staged+=previewTemp to File(root,PREVIEW_NAME)
                            val previewDigest=MessageDigest.getInstance("SHA-256")
                            var previewLength=0L
                            FileOutputStream(previewTemp).buffered().use{out ->
                                val buffer=ByteArray(8192)
                                while(true) {
                                    val n=zip.read(buffer)
                                    if(n<0)break
                                    previewLength+=n
                                    aggregate+=n
                                    require(previewLength<=PREVIEW_SIZE &&
                                        aggregate<=75_000_000L){"Oversized private model preview"}
                                    out.write(buffer,0,n)
                                    previewDigest.update(buffer,0,n)
                                }
                            }
                            val checksum=previewDigest.digest().joinToString("") {
                                "%02x".format(it.toInt() and 0xff)
                            }
                            require(previewLength==PREVIEW_SIZE && checksum==PREVIEW_SHA) {
                                "Model portrait atlas does not match your private package"
                            }
                            zip.closeEntry()
                            continue
                        }
                        val model=byName[name]
                        if(entry.isDirectory || model==null) {
                            zip.closeEntry()
                            continue
                        }
                        require(found.add(model.id)){"Repeated model: " + model.id}
                        require(!entry.name.contains("..")){"Invalid ZIP entry"}
                        require(entry.size<0 || entry.size==model.size) {
                            "Wrong size for " + model.id
                        }
                        val file=File(root,model.name)
                        val temp=File.createTempFile("wq173_","_staging",root)
                        staged+=temp to file
                        val digest=MessageDigest.getInstance("SHA-256")
                        var length=0L
                        FileOutputStream(temp).buffered().use{out ->
                            val buffer=ByteArray(32768)
                            while(true) {
                                val n=zip.read(buffer)
                                if(n<0)break
                                length+=n
                                aggregate+=n
                                require(length<=model.size && aggregate<=75_000_000L) {
                                    "Unexpected archive size"
                                }
                                out.write(buffer,0,n)
                                digest.update(buffer,0,n)
                            }
                        }
                        val actual=digest.digest().joinToString(""){"%02x".format(it.toInt() and 0xff)}
                        require(length==model.size && actual==model.digest) {
                            "Model " + model.id + " differs from uploaded file"
                        }
                        temp.inputStream().use{
                            val header=ByteArray(12)
                            require(it.read(header)==12){"GLB header truncated"}
                            require(String(header.copyOfRange(0,4),Charsets.US_ASCII)=="glTF"
                                && header[4].toInt()==2){"Unsupported GLB"}
                        }
                        imported+=model.id
                        zip.closeEntry()
                    }
                }
            }?:throw IllegalArgumentException("Cannot read selected ZIP")
            require(imported.isNotEmpty()){"No WordQuest 1.0.73 models in this ZIP"}
            for((source,target) in staged) {
                if(target.exists())require(target.delete()){"Cannot replace existing model"}
                require(source.renameTo(target)){"Cannot store selected model"}
            }
            Log.i("WQAvatarImport","Installed " + imported.size + " user avatars")
            return ImportResult(imported.size,count(context),imported)
        } finally {
            staged.forEach{(tmp,_) -> if(tmp.exists())tmp.delete()}
        }
    }
}
