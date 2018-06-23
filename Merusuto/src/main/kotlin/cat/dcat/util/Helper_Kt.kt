package cat.dcat.util

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject

/**
 * Created by DCat on 2017/1/18.
 */
fun getCallerName(): String {
    try {
        throw RuntimeException("ass we can")
    } catch (e: RuntimeException) {
        val sts = e.stackTrace
        if (sts.size < 3) return "<???>"
        return sts.get(2).methodName
    }
}

fun dlog(msg: Any = "\n") {
    print("[${getCallerName()}] ")
    println(msg)
}

fun derr(msg: Any = "\n") {
    System.err.print("[${getCallerName()}] ")
    System.err.println(msg)
}

fun derr(msg: Any = "\n", e: Throwable) {
    System.err.print("[${getCallerName()}]")
    System.err.println(msg)
    e.printStackTrace(System.err)
}

fun parseJSON(src: String): JSONObject {
    val data = Helper.readAllFromFile(src)
    return JSON.parseObject(data)
}

fun parseJSONArray(src: String): JSONArray {
    val data = Helper.readAllFromFile(src)
    return JSON.parseArray(data)
}