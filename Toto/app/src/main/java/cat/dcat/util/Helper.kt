package cat.dcat.util

import android.util.Log
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject

/**
 * Created by DCat on 2017/1/18.
 */
fun println(msg: Any = "\n") {
    Log.d("Kotlin", "" + msg)
}

fun dlog(msg: Any = "\n") {
    println(msg)
}

fun String.d(msg: Any = "") {
    Log.d(this, "" + msg)
}

fun parseJSON(src: String): JSONObject {
    val data = Helper.readAllFromFile(src);
    return JSON.parseObject(data)
}

fun parseJSONArray(src: String): JSONArray {
    val data = Helper.readAllFromFile(src);
    return JSON.parseArray(data)
}