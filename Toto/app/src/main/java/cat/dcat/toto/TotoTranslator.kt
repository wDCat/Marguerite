package cat.dcat.toto

import android.util.Log
import cat.dcat.util.G
import cat.dcat.util.Helper
import cat.dcat.util.d
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import okhttp3.Response
import java.io.PrintStream
import java.util.zip.GZIPOutputStream

/**
 * Created by DCat on 2017/10/6.
 */
fun translate(k: String): String {
    return Helper.googleTranslateAPI(k)
}

fun translateJO(jo: JSONObject) {
    jo.keys.forEach {
        try {
            val obj = jo.get(it)
            when (obj!!.javaClass) {
                String::class.java -> {
                    if (it.toString().equals("text") || it.toString().equals("name")) {
                        "translate".d("translating ${it}:$obj")
                        try {
                            jo.set(it, translate(obj as String))
                        } catch (e: Throwable) {
                            Log.e("translate", "failed", e)
                        }
                    }
                }
                JSONObject::class.java -> {
                    translateJO(obj as JSONObject)
                }
                JSONArray::class.java -> {
                    translateJA(obj as JSONArray)
                }
            }
        } catch (ignored: Throwable) {

        }
    }
}

fun translateJA(ja: JSONArray) {
    for (x in 0..ja.size - 1) {
        val obj = ja.get(x)
        when (obj!!.javaClass) {
            String::class.java -> {
                //ja.set(x, translate(obj as String))
            }
            JSONObject::class.java -> {
                translateJO(obj as JSONObject)
            }
            JSONArray::class.java -> {
                translateJA(obj as JSONArray)
            }
        }
    }
}

fun totoTranslateIt(bb: MutableList<Int>, out: PrintStream, o3resp: Response): Boolean {

    if (G.cfg == null || G.cfg.getIntValue("translate") == 0)
        return false
    val bg = Helper.tryToUnGzip(bb)
    val ba = ByteArray(bg.size)
    for (x in 0..bg.size - 1) {
        ba[x] = bg.get(x).toByte()
    }
    try {
        val jo = JSONObject.parseObject(String(ba))
        translateJO(jo)
        val str = jo.toString()
        out.print(String.format("HTTP/1.1 %d %s\n", 200, "OK"))
        for (key in o3resp.headers().names()) {
            val key2 = key.toLowerCase()
            if ("transfer-encoding" == key2) continue//block chunked
            //Log.d(TAG, String.format("ret key[%s] value:[%s]", key, o3resp.header(key)));
            out.print(String.format("%s:%s\n", key2, o3resp.header(key)))
        }
        out.println()
        "toto".d(str)
        val gout = PrintStream(GZIPOutputStream(out))
        gout.print(str)
        gout.flush()
        gout.close()
        "toto".d("translate done...")
        return true
    } catch (e: Throwable) {
        Log.e("toto", "translate failed", e)
        return false
    }
    return false
}
