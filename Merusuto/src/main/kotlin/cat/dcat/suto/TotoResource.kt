package cat.dcat.suto

import cat.dcat.util.derr
import cat.dcat.util.dlog
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import java.io.*
import java.util.*

/**
 * Created by DCat on 2017/12/10.
 */
open class TotoResource(var u3dFile: File, var pngFile: File) {
    init {
        // u3dFile.deleteOnExit()
        // pngFile.deleteOnExit()
    }

    public fun removeTmpFile() {
        // u3dFile.delete()
        // pngFile.delete()
    }
}

class TotoResourceFetcher(var tmpDir: String) {
    init {
        val td = File(tmpDir)
        if (!td.exists()) td.mkdirs()
        tmpDir = td.absolutePath
    }

    val crcJSON = JSONObject()
    fun loadCrcJSON(jo: JSONObject): Int {
        val oc = crcJSON.size
        crcJSON.putAll(jo)
        return crcJSON.size - oc
    }

    fun loadCrcJSONFromLocal(path: String) {
        val ret = loadCrcJSON(path.readJSONObjectFileUtf8())
        dlog("loaded ${ret} crc items from local(${path})")
    }

    fun loadCrcJSONFromServer(version: String):Boolean {
        var body = ""
        try {
            body = Jsoup.connect("https://toto.hekk.org/asset_info/android/${version}.json").ignoreContentType(true).get().body().text()
        } catch (e: Throwable) {
            //derr("cannot get crc json from server", e)
            return false
        }
        val ret = loadCrcJSON(JSON.parseObject(body).getJSONObject("crc"))
        dlog("loaded ${ret} crc items from server(${version})")
        return true
    }

    fun getItemName(type: String, subType: String?, id: Int) = "/${type}/${type}_${subType ?: "\b"}_${id}"
    fun getCrc(name: String, platform: String = "Android"): Long = crcJSON.getLongValue("${name}.${platform}")
    fun getCrc(type: String, subType: String?, id: Int, platform: String = "Android") = getCrc(getItemName(type, subType, id), platform)
    fun fetchResFromServer(name: String, id: Int, platform: String = "Android"): TotoResource? {
        val tc = System.currentTimeMillis()
        val o3client = OkHttpClient()
        if (getCrc(name) < 1) {
            derr("Crc not found:${name}")
            return null
        }
        val url = "http://images.toto-japan.hekk.org/toto_image_s3/jp_v5${name}_${getCrc(name)}.${platform}.unity3d?nocache=${System.currentTimeMillis() - 123}"
        val reqbuilder = Request.Builder().url(url)
        var o3resp: Response
        var retry = 0
        while (true) {
            o3resp = o3client.newCall(reqbuilder.build()).execute()
            if (!o3resp.isSuccessful) {
                derr("[retry:${retry++}]req failed.errcode:${o3resp.code()} url:${url}")
                if (retry > 3)
                    return null
            } else break
        }
        val ofile = File("${tmpDir}/${name.replace('/', '_')}.unity3d")
        ofile.createNewFile()
        val pngFile = File("${tmpDir}/${name.replace('/', '_')}.png")
        val out = BufferedOutputStream(FileOutputStream(ofile))
        val iin = o3resp.body()!!.byteStream()
        var size = 0
        while (true) {
            val buff = ByteArray(200)
            val r = iin.read(buff)
            if (r <= 0) break
            size += r
            out.write(buff, 0, r)
        }
        iin.close()
        out.flush()
        out.close()
        dlog("${name} saved to ${tmpDir}.Size:${size} bytes cost:${System.currentTimeMillis() - tc} ms")
        return TotoResource(ofile, pngFile)
    }

    fun extractImg(tr: TotoResource): File? {
        if (tr.pngFile.exists()) {
            tr.pngFile.delete()
            if (tr.pngFile.exists()) {
                derr("cannot remove old file:${tr.pngFile.absolutePath}")
                return null
            }
        }
        unityPack.extractImg(tr.u3dFile.absolutePath, 0, tr.pngFile.name)
        if (!tr.pngFile.exists()) {
            derr("cannot found extracted png file:${tr.pngFile.absolutePath}")
            return null
        }
        return tr.pngFile
    }
}

fun main(args: Array<String>) {
    //Test
    val tr = TotoResourceFetcher("./tmp")
    tr.loadCrcJSONFromLocal("K:\\share_old\\toto_cat\\data\\crc\\105110_asset_crc.json")
    tr.loadCrcJSONFromLocal("K:\\share_old\\toto_cat\\data\\crc\\105110_asset_crc_android.json")
    tr.loadCrcJSONFromServer("105100")

    dlog((tr.extractImg(tr.fetchResFromServer("/unit/unit_large_1197", 1197)!!) ?: File("null")).absolutePath)
}