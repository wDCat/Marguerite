package cat.dcat.mercdump

import cat.dcat.suto.readJSONObjectFileUtf8
import cat.dcat.util.derr
import cat.dcat.util.dlog
import cat.dcat.util.parseJSON
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.*
import java.nio.charset.Charset

/**
 * Created by DCat on 2017/11/4.
 */
var em: JSONObject = JSON.parseObject("{}")
val unitSimple = parseJSON("./predata/unit_example.json")
val userUnitsJson = parseJSON("./predata/user_units.json")
fun createUserDeskJson() {
    unitSimple.keys.forEach {
        unitSimple[it] = null
    }
    val userUnits = userUnitsJson.getJSONObject("data").getJSONArray("user_units")
    userUnits.clear()
    (em["units"] as JSONObject).forEach {
        val uid = it.key as String
        val uobj = it.value as JSONObject
        val robj = unitSimple.clone() as JSONObject
        uobj.forEach {
            robj[it.key] = it.value
            when (it.key) {
                "name" -> {
                    val name = it.value as String
                    //println("processing " + name + "   " + uobj["reach"])
                    try {
                        val n1 = name.substring(1..name.indexOf("]") - 1)
                        val n2 = name.substring(name.indexOf("]") + 1)
                        robj["prefix_name"] = n1
                        robj["main_name"] = n2
                    } catch (e: Throwable) {
                        robj["prefix_name"] = ""
                        robj["main_name"] = name
                    }
                }
                "hide" -> {
                    robj["hide"] = false
                }
                "is_showable" -> {
                    robj["is_showable"] = "true"
                }
            }
        }
        robj["orig_hp"] = uobj["hp_15_max"]
        robj["orig_at"] = uobj["hp_15_at"]
        robj["hp"] = uobj["hp_15_max"]
        robj["at"] = uobj["at_15_max"]
        robj["max_level"] = (uobj["rare_type_count"] as Int) * 10 + 85
        robj["level"] = robj["max_level"]
        //robj["story_id"] = storiesMapJson["" + uid]
        userUnits.add(robj)
    }
    val out = PrintWriter(FileWriter("./predata/em_units.json"))
    out.print(userUnitsJson.toJSONString())
    out.flush()
    out.close()

}

fun getEMFromServer(version: Int, writeTo: String): Boolean {
    val o3client = OkHttpClient()
    val url = "https://toto.hekk.org/embedded_master/${version}.json?version=19700101090003"
    val reqbuilder = Request.Builder().url(url)
    var o3resp: Response
    o3resp = o3client.newCall(reqbuilder.build()).execute()
    if (!o3resp.isSuccessful) {
        derr("req failed.errcode:${o3resp.code()} url:${url}")
        return false
    }
    val ofile = File(writeTo)
    if (!ofile.exists()) ofile.createNewFile()
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
    return true
}

fun digUpConstData(fn: String) {
    val consts = fn.readJSONObjectFileUtf8()!!
    val seedsArray = (consts["data"] as JSONObject)["seed_type"] as JSONArray
    val seedsObj = JSONObject()
    seedsArray.forEach {
        it as JSONObject
        seedsObj[it["id"].toString()] = {
            val v = JSONObject(true)
            v["jp_name"] = it["name"]
            v["name"] = ""
            v["desc"] = ""
            v
        }()
    }
    var out = PrintWriter(FileWriter("./predata/seeds.json"))
    out.print(seedsObj.toJSONString())
    out.flush()
    out.close()
}
fun embeddedMasterRestore(){
    //digUpConstData("./predata/embedded_master_consts.json")
    fromAss("./predata/embedded_master_skill.json", "./tmp/em_s15.tmp.json", "skills")
    fromAss("./predata/embedded_master_monster.json", "./tmp/em_m15.tmp.json", "monsters")
    fromAss("./predata/em.json", "./tmp/em_15.tmp.json")
    for (x in 11..20) {
        val version = 105000 + x * 10
        dlog("fetching em ${version}")
        val fn = "./tmp/em_${x}.json"
        if (!getEMFromServer(version, fn)) {
            dlog("fetch failed: em ${version}")
            break
        }
        combineEM("./tmp/em_15.tmp.json", fn)
        combineEM("./tmp/em_m15.tmp.json", fn, "monsters")
        combineEM("./tmp/em_s15.tmp.json", fn, "skills")
        dlog("fetched em ${version}")
    }
    em = parseJSON("./tmp/em_15.tmp.json")
    createUserDeskJson()
    File("./tmp/em_m15.tmp.json").copyTo(File("./predata/em_monsters.json"), true)
    File("./tmp/em_s15.tmp.json").copyTo(File("./predata/em_skills.json"), true)
    dlog("done.")
}
fun main(args: Array<String>) {
   embeddedMasterRestore()
}

fun combineEM(new: String, with: String, type: String = "units") {
    val nJo = JSON.parseObject(String(File(new).readBytes(), Charset.forName("utf-8")))
    val wJo = JSON.parseObject(String(File(with).readBytes(), Charset.forName("utf-8")))
    val units = nJo[type] as JSONObject
    (wJo[type] as JSONObject).forEach {
        println("adding(${type}) " + it.key)
        units[it.key] = it.value
    }
    val out = PrintWriter(FileWriter(File(new)))
    out.print(nJo.toJSONString())
    out.flush()
    out.close()
}

fun fromAss(ass: String, out: String, type: String = "units") {
    val result = JSONObject()
    val jo = JSON.parseObject(String(File(ass).readBytes(), Charset.forName("utf-8")))
    val keyMap = jo["keys"] as JSONArray
    val typeMap = jo["types"] as JSONArray
    (jo["data"] as JSONObject).forEach {

        val uid = it.key
        val d = it.value as JSONArray
        val unit = JSONObject()
        //println("processing " + uid)
        for (x in 0..keyMap.size - 1) {
            val k = keyMap[x] as String
            unit[k] = d[x]
        }
        result[uid] = unit
    }
    val outJo = JSONObject()
    outJo.put(type, result)
    val outFile = File(out)
    outFile.createNewFile()
    val out = PrintWriter(FileWriter(outFile))
    out.print(outJo.toJSONString())
    out.flush()
    out.close()

}
