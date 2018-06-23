package cat.dcat.suto

import cat.dcat.chromapack.ChromaPack
import cat.dcat.mercdump.embeddedMasterRestore
import cat.dcat.util.derr
import cat.dcat.util.dlog
import com.alibaba.fastjson.*
import com.alibaba.fastjson.parser.Feature
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.nio.charset.Charset
import java.util.*
import javax.imageio.ImageIO
import kotlin.system.exitProcess

//TODO bad coding...
/**
 * Created by DCat on 2017/12/9.
 */
class tr : TypeReference<LinkedHashMap<String, Any>>() {

}

fun String.readJSONObjectFileUtf8() = JSON.parseObject(File(this).readText(Charset.forName("utf-8")))

fun String.readJSONArrayFileUtf8() = JSON.parseArray(File(this).readText(Charset.forName("utf-8")))
val sutoUnitJSON = "./predata/units.json".readJSONArrayFileUtf8()
val emUnitJSON = "./predata/em_units.json".readJSONObjectFileUtf8()
val sutoMonsterJSON = "./predata/monsters.json".readJSONArrayFileUtf8()
val emMonsterJSON = "./predata/em_monsters.json".readJSONObjectFileUtf8()
val countryJSON = "./predata/country.json".readJSONArrayFileUtf8()
val wikiFixJSON = "./predata/wiki_fix.json".readJSONObjectFileUtf8()
val seedsJSON = "./predata/seeds.json".readJSONObjectFileUtf8()
val unitPreSetData = "./predata/unit_data.json".readJSONObjectFileUtf8()
val weaponType = listOf("斩击", "突击", "打击", "弓箭", "魔法", "铳弹", "回复")
val weaponJPType = listOf("斩击", "突击", "打击", "弓箭", "魔法", "銃弾", "回复")
val countries = HashMap<Int, String>()
val largeImgOutput = "./result/units/original"
val largeNSImgOutput = "./result/units/foreground"
val smallImgOutput = "./result/units/icon"
val recImgOutput = "./result/units/thumbnail"
val largeMonsterImgOutput = "./result/monsters/original"
val smallMonsterImgOutput = "./result/monsters/icon"
val recMonsterImgOutput = "./result/monsters/thumbnail"
val nameMap = HashMap<String, ArrayList<JSONObject>>()
fun filterOutNewUnits(): Map<Int, JSONObject> {
    val emMap = HashMap<Int, JSONObject>()
    val sutoBS = BitSet()
    sutoUnitJSON.forEach {
        it as JSONObject
        val uid = it["id"] as Int
        if (uid < 3000)
            sutoBS.set(uid)
    }
    dlog("new units:")
    emUnitJSON.getJSONObject("data").getJSONArray("user_units").forEach {
        it as JSONObject
        val uid = it["id"] as Int
        val name = (it["main_name"] as String).trim()
        if (!nameMap.containsKey(name))
            nameMap.put(name, ArrayList())
        val tlist = nameMap.get(name)
        tlist!!.add(it)
        if (uid < 3000 && !sutoBS[uid]) {
            print("${uid} ")
            emMap.put(uid, it)
        }
    }
    dlog("-------")
    return emMap
}

fun filterOutNewMonsters(): Map<Int, JSONObject> {
    val emMap = HashMap<Int, JSONObject>()
    val sutoBS = BitSet()
    sutoMonsterJSON.forEach {
        it as JSONObject
        val uid = it["id"] as Int
        if (uid < 3000)
            sutoBS.set(uid)
    }
    dlog("new monsters:")
    emMonsterJSON.getJSONObject("monsters").forEach {
        val obj = it.value as JSONObject
        val uid = obj["id"] as Int
        val name = (obj["name"] as String).trim()
        if (!nameMap.containsKey(name))
            nameMap.put(name, ArrayList())
        val tlist = nameMap.get(name)
        tlist!!.add(obj)
        if (uid < 3000 && !sutoBS[uid]) {
            print("${uid} ")
            emMap.put(uid, obj)
        }
    }
    dlog("-------")
    return emMap
}

fun parseCountryJSON() {
    var c = 0
    countryJSON.forEach {
        it as JSONObject
        countries[it["id"] as Int] = it["name"] as String
        c++
    }
    dlog("parsed $c countries")
}

fun checkResultData(jo: JSONObject): Boolean {
    val toRemove = Stack<String>()
    jo.forEach {
        if (it.value == null) {
            toRemove.push(it.key)
        } else {
            val v = it.value.toString()
            if ((v == "0" || v == "0.0") && it.key != "gender") {
                derr("--->Empty entry:${it.key}")
                toRemove.push(it.key)
                //return false
            }
        }
    }
    toRemove.forEach {
        jo.remove(it)
    }
    return true
}

fun monsterSkill(seedid: Int) = {
    if (seedsJSON.containsKey("" + seedid)) {
        val v = seedsJSON["" + seedid] as JSONObject
        if (v["name"].toString().length > 1) {
            "${v["name"]}：(${v["jp_name"]}) ${v["desc"]}"
        } else {
            "暂缺"
        }
    } else
        "未知："
}()

fun monsterObtain(area: String, rare: Int, wiki: MonsterDataFromWiki) = {
    var ret = "${area} "
    println(ret)
    if (rare as Int >= 3) {
        try {
            ret += "「.+」".toRegex().find(wiki.obtain)!!.groupValues[0]
        } catch (lazyFish: Throwable) {
            ret += wiki.obtain
        }
    }
    ret
}()

fun monsterSkillMax(wiki: MonsterDataFromWiki) = {
    if (wiki.skmax.length > 1) {
        if (wiki.skmax.contains("～")) {
            wiki.skmax.split("～")[1].trimToNum().toFloatS()
        } else {
            wiki.skmax.trimToNum().toFloatS()
        }
    } else {
        0.0f
    }
}()

fun convertMonsterEmToSuto(jo: JSONObject): JSONObject? {
    var wiki = fetchMonsterDataFromWiki(jo["name"] as String)
    val r = JSONObject(true)
    r["id"] = jo["id"]
    r["sklmax"] = monsterSkillMax(wiki)
    r["name"] = jo["name"]
    r["rare"] = jo["rare_type_count"]
    r["element"] = jo["attr_id"]
    r["skin"] = jo["hardness_id"]
    r["sklsp"] = jo["cost"]
    r["sklcd"] = wiki.cd
    r["anum"] = wiki.atk_num
    r["mspd"] = wiki.mspd
    r["aarea"] = wiki.aarea
    r["aspd"] = wiki.aspd
    r["tenacity"] = wiki.endure
    r["skill"] = monsterSkill(jo["seed_skill_id"] as Int)
    r["obtain"] = monsterObtain(jo["appear_stage_names"] as String, r["rare"] as Int, wiki)
    r["sarea"] = wiki.sarea
    r["remark"] = "${jo["description"]}" +
            "\n\n" +
            "（部分数据来自 https://メルクストーリア.gamerch.com/ ）" +
            "\n(正在测试中，数据可能会有变动...)"
    r["seed"] = jo["seed_skill_id"]
    return r
}

fun convertEmToSuto(jo: JSONObject): JSONObject? {
    var fail_dump = false
    val r = JSONObject(true)
    val fname = "「${jo["prefix_name"]}」${jo["main_name"]}"
    if (jo["prefix_name"].toString().length < 1) {
        derr("Prefix name is empty:${jo["main_name"]},ignored.")
        return null
    }
    var wiki = fetchUnitDataFromWiki(fname)
    val snL = nameMap.get(jo["main_name"])
    if (snL == null) return null
    if (unitPreSetData.containsKey(fname)) {
        dlog("loading preset data...")
        val psjson = unitPreSetData[fname] as JSONObject
        wiki = psjson.toJavaObject(UnitDataFromWiki::class.java)
    }
    dlog(wiki)
    if (wiki.fire == 0f || wiki.atk_num == 0 || wiki.speed == 0) {
        var succ = false
        for (afterfix in listOf("", "（" + weaponJPType[jo["weapon_id"] as Int - 1] + "）")) {
            if (succ) break
            for (x in 0..snL.size - 1) {

                val aofname = "「${snL[x]["prefix_name"]}」${snL[x]["main_name"]}${afterfix}"
                dlog("trying ${aofname}...")
                wiki = fetchUnitDataFromWiki(aofname)
                dlog(wiki)
                if (!(wiki.fire == 0f || wiki.atk_num == 0 || wiki.speed == 0)) {
                    succ = true
                    dlog("${aofname}  ===>> ${fname}")
                    break
                }
            }
        }
        if (!succ) {
            derr("cannot found data for ${fname}")
            //fail_dump=true
        }
    }
    val origData = snL.minWith(Comparator { o1: JSONObject, o2: JSONObject -> o1["id"] as Int - o2["id"] as Int })!!
    val origID = origData["id"]
    dlog("min ID:${origID} selfId:${jo["id"]}")
    var isPrem = !(origID == jo["id"] || origData["rare_type_count"] != jo["rare_type_count"])
    //

    dlog(wiki)
    dlog("processing $fname")
    r["id"] = jo["id"]
    if (isPrem) {
        r["title"] = "❤[" + jo["prefix_name"] + "]"
    } else {
        r["title"] = "[" + jo["prefix_name"] + "]"
    }
    r["name"] = jo["main_name"]
    r["country"] = countries[jo["country_id"] as Int] ?: "未知"
    r["rare"] = jo["rare_type_count"]
    r["element"] = jo["attr_id"]
    r["weapon"] = jo["weapon_id"]
    r["type"] = jo["growth_type_id"]
    r["anum"] = jo["attackable_count"] ?: wiki.atk_num
    r["life"] = jo["hp_min"]
    r["atk"] = jo["at_min"]
    if (jo.containsKey("speed"))
        r["mspd"] = jo.getFloatValue("speed").toInt()
    else
        r["mspd"] = wiki.speed
    if (jo.containsKey("attack_ms"))
        r["aspd"] = jo.getIntValue("attack_ms").toFloat() / 1000f
    else
        r["aspd"] = wiki.atk_speed
    if (jo.containsKey("reach"))
        r["aarea"] = jo.getFloatValue("reach").toInt()
    else
        r["aarea"] = wiki.reach
    if (jo.containsKey("taghness"))
        r["tenacity"] = jo.getFloatValue("taghness").toInt()
    else
        r["tenacity"] = wiki.tag
    r["fire"] = wiki.fire
    r["aqua"] = wiki.water
    r["wind"] = wiki.wind
    r["light"] = wiki.light
    r["dark"] = wiki.dark
    r["title_jp"] = r["title"]
    r["name_jp"] = r["name"]
    r["obtain"] = wiki.from
    r["gender"] = mapOf(1 to 2, 2 to 3, 3 to 1)[jo["sex_id"] as Int]
    r["gacha"] = JSONArray()
    r["age"] = jo["age_sort"]
    r["career"] = jo["profession"]
    r["interest"] = jo["favorite"]
    r["nature"] = jo["personality"]
    r["remark"] = jo["description"] ?: "暂无"
    if (wiki.hits > 0)
        r["hits"] = wiki.hits
    r["server"] = 1

    if (fail_dump) {
        derr(r)
        return null
    }
    return r
}

val resfetcher = TotoResourceFetcher("./tmp")

fun fetchAndDecodeRes(type: String, subType: String?, id: Int): File? {
    val totores = resfetcher.fetchResFromServer(resfetcher.getItemName(type, subType, id), id)
    if (totores == null) return null
    val png = resfetcher.extractImg(totores)
    if (png == null) return null
    val image = ImageIO.read(png)
    val ofile = File(png.absolutePath + ".d.png")
    var retImg: BufferedImage
    if (type == "unit") {
        if (subType == "large" || subType == "large_ns") {
            retImg = ChromaPack.decodeUnitImage(image, false)
        } else if (subType == "btn") {
            retImg = ChromaPack.decodeBackground(image, false, true, 0.05f)
        } else {
            retImg = ChromaPack.decodeBackground(image, false, true, 0.1f)
        }
    } else if (type == "monster") {
        retImg = RABGToARBG(image)
    } else {
        derr("Unknown type:${type}")
        return null
    }
    png.delete()
    png.createNewFile()
    ImageIO.write(retImg, "PNG", ofile)
    totores.removeTmpFile()
    return ofile

}

fun combineMonsterJSON(out: PrintWriter, oldjo: JSONArray, nJO: JSONArray) {
    val monsters = HashMap<Int, JSONObject>()
    (oldjo + nJO).forEach {
        it as JSONObject
        val id = it["id"] as Int
        if (monsters.containsKey(id)) {
            derr("multi unit:${id}")
            return
        }
        if (nJO.contains(it)) {
            it["server"] = 1
        } else if (it["server"] == 1) {
            it.remove("server")
        }
        monsters[id] = it
    }
    dlog("monsters count:${monsters.count()}")
    var lastid = 0
    out.print("[\n")
    monsters.toSortedMap(Comparator { o1, o2 -> o1 - o2 }).forEach {
        if (lastid > 0) out.println(",")
        val id = it.key
        val jo = it.value
        if (id - lastid > 1) {
            out.print("\n\n")
        }
        var skill = jo["skill"].toString().trim()
        var refetch = false
        if (skill.length < 2 || skill.contains("暂")) {
            skill = "暂无"
            refetch = false
        } else {
            if (skill.length >= 5 && skill.startsWith("双色") && skill[3] != '（') {
                val t = skill[2]
                var v = ""
                val jpEle = listOf("炎", "水", "風", "光", "闇")
                val cnEle = listOf("火", "水", "风", "光", "暗")
                for (x in 0..4)
                    if (skill.contains(jpEle[x]) || skill.contains(cnEle[x])) v += cnEle[x]
                if (v.length == 2) {
                    skill = skill.replace("双色$t", "双色$t（$v）")
                } else {
                    //refetch from wiki
                    refetch = true
                    skill = skill.replace("双色$t", "双色$t（未知）")
                }
            }
        }
        if (refetch) {
            try {
                skill = monsterSkill(emMonsterJSON.getJSONObject("monsters").getJSONObject("" + jo["id"])!!["seed_skill_id"] as Int)
            } catch (ignored: Throwable) {

            }

        }
        if ((!jo.containsKey("sklmax") || jo.getDoubleValue("sklmax") == 0.0) && jo["rare"] as Int >= 3) {
            var jpName: String
            try {
                jpName = emMonsterJSON.getJSONObject("monsters").getJSONObject("" + jo["id"])!!["name"].toString()
            } catch (ignored: Throwable) {
                jpName = jo["name"].toString()
            }
            dlog("trying get sklmax:${jo["name"]} $jpName")
            val wiki = fetchMonsterDataFromWiki(jpName)
            val sklmax = monsterSkillMax(wiki)
            if (sklmax > 0.0f) {
                dlog("update sklmax:${jo["name"]}::$sklmax")
                jo["sklmax"] = sklmax
            }
        }
        if (jo["skill"].toString().trim() != skill) {
            dlog("fix skill($id)  ${jo["skill"]}  ==> $skill")
        }
        jo["skill"] = skill
        out.print(jo)
        lastid = id
    }
    out.print("\n]")
}

fun combineUnitJSON(out: PrintWriter, oldjo: JSONArray, nJO: JSONArray) {
    val units = HashMap<Int, JSONObject>()
    (oldjo + nJO).forEach {
        it as JSONObject
        val id = it["id"] as Int
        if (units.containsKey(id)) {
            derr("multi unit:${id}")
            return
        }
        if (nJO.contains(it)) {
            it["server"] = 1
        } else if (it["server"] == 1) {
            it.remove("server")
        }
        units[id] = it
    }
    dlog("units count:${units.count()}")
    var lastuid = 0
    out.print("[\n")
    units.toSortedMap(Comparator { o1, o2 -> o1 - o2 }).forEach {
        if (lastuid > 0) out.println(",")
        val id = it.key
        val jo = it.value
        //country name の
        var country = jo["country"].toString().trim()
        mapOf(//saltyfish's way
                "の国" to "之国",
                "動物" to "动物",
                "お菓子" to "点心",
                "機械" to "机械",
                "恐竜" to "恐龙"
        ).forEach { country = country.replace(it.key, it.value) }
        jo["country"] = country
        if (id - lastuid > 1) {
            out.print("\n\n")
        }
        out.print(jo)
        lastuid = id
    }
    out.print("\n]")
}

fun parseJSONArrayOrder(str: String): JSONArray {
    //little trick
    var data = str
    if (data[0] == '\uFEFF')
        data = data.substring(1)

    class TR : TypeReference<LinkedHashMap<String, Any>>()

    val j = JSON.parseObject("{\"data\":${data}}", TR(), Feature.OrderedField)
    return j["data"] as JSONArray
}

fun updateMonsterAlbum(ofile: File) {
    val result = JSONArray()
    var s = 0
    var f = 0
    var c = 0
    val newMonsters = filterOutNewMonsters()
    val newMonstersCount = newMonsters.size
    newMonsters.forEach {
        val rare = it.value["rare_type_count"] as Int
        if (rare < 1 || rare > 4) return@forEach
        var succ = false
        println("-----------------------------------------------   <-----%.2f%% (%d/%d)"
                .format((c.toFloat() / newMonstersCount) * 100, c++, newMonstersCount))
        println("[*]Processing ${it.key} ....")
        val r = convertMonsterEmToSuto(it.value)

        if (r != null) {
            dlog(r)
            if (checkResultData(r)) {
                val uid = r["id"] as Int
                val largeImgOut = File("${largeMonsterImgOutput}/${uid}.png")
                val smallImgOut = File("${smallMonsterImgOutput}/${uid}.png")
                val recImgOut = File("${recMonsterImgOutput}/${uid}.png")
                if (!largeImgOut.exists() || !smallImgOut.exists() || !recImgOut.exists()) {
                    val largeImg = fetchAndDecodeRes("monster", "large_ns", uid)
                    val smallImg = fetchAndDecodeRes("monster", "square", uid)
                    val recImg = fetchAndDecodeRes("monster", "btn", uid)
                    if (largeImg == null || smallImg == null || recImg == null) {
                        derr("process failed:cannot fetch resource " +
                                "l:${largeImg != null} s:${smallImg != null} " +
                                "rec:${recImg != null}")

                    } else {
                        largeImg.renameTo(largeImgOut)
                        smallImg.renameTo(smallImgOut)
                        recImg.renameTo(recImgOut)
                        result.add(r)
                        succ = true
                    }
                } else {
                    dlog("local resources found:${uid}")
                    result.add(r)
                    succ = true
                }
            }
        }
        if (succ) {
            println("[*]Processed ${it.key}")
            s++
            if (s % 10 == 0) {
                val out = PrintWriter(FileWriter(ofile))
                out.print(result.toJSONString())
                out.close()
            }
        } else {
            f++
            System.err.println("[*]Failed: ${it.key}")
        }
    }
    dlog("Process done. succ:$s fail:$f")
    val out = PrintWriter(FileWriter(ofile))
    val j = parseJSONArrayOrder(File("predata/monsters.json").readText(Charset.forName("utf-8")).trim())
    combineMonsterJSON(out, j, result)
    //out.print(result.toJSONString())
    out.close()
}

fun updateUnitAlbum(ofile: File) {
    val result = JSONArray()
    var s = 0
    var f = 0
    var c = 0
    val newUnits = filterOutNewUnits()
    val newUnitsCount = newUnits.size
    newUnits.forEach {
        var succ = false
        println("-----------------------------------------------   <-----%.2f%% (%d/%d)"
                .format((c.toFloat() / newUnitsCount) * 100, c++, newUnitsCount))
        println("[*]Processing ${it.key} ....")
        val r = convertEmToSuto(it.value)
        if (r != null) {
            dlog(r)
            if (checkResultData(r)) {
                val uid = r["id"] as Int
                val largeImgOut = File("${largeImgOutput}/${uid}.png")
                val largeNSImgOut = File("${largeNSImgOutput}/${uid}.png")
                val smallImgOut = File("${smallImgOutput}/${uid}.png")
                val recImgOut = File("${recImgOutput}/${uid}.png")
                if (!largeImgOut.exists() || !largeNSImgOut.exists() || !smallImgOut.exists() || !recImgOut.exists()) {
                    val largeImg = fetchAndDecodeRes("unit", "large", uid)
                    val largeNsImg = fetchAndDecodeRes("unit", "large_ns", uid)
                    val smallImg = fetchAndDecodeRes("unit", "square", uid)
                    val recImg = fetchAndDecodeRes("unit", "btn", uid)
                    if (largeImg == null || largeNsImg == null || smallImg == null || recImg == null) {
                        derr("process failed:cannot fetch resource " +
                                "l:${largeImg != null} lns:${largeNsImg != null} s:${smallImg != null} " +
                                "rec:${recImg != null}")

                    } else {
                        largeImg.renameTo(largeImgOut)
                        largeNsImg.renameTo(largeNSImgOut)
                        smallImg.renameTo(smallImgOut)
                        recImg.renameTo(recImgOut)
                        result.add(r)
                        succ = true
                    }
                } else {
                    dlog("local resources found:${uid}")
                    result.add(r)
                    succ = true
                }
            }
        }
        if (succ) {
            println("[*]Processed ${it.key}")
            s++
            if (s % 10 == 0) {
                val out = PrintWriter(FileWriter(ofile))
                out.print(result.toJSONString())
                out.close()
            }
        } else {
            f++
            System.err.println("[*]Failed: ${it.key}")
        }
    }
    dlog("Process done. succ:$s fail:$f")
    val out = PrintWriter(FileWriter(ofile))
    val j = parseJSONArrayOrder(File("predata/units.json").readText(Charset.forName("utf-8")).trim())
    combineUnitJSON(out, j, result)
    //out.print(result.toJSONString())
    out.close()
}

fun menu(): Boolean {
    print("\nEnter password:")
    var type = 3
    try {
        type = readLine()!!.toInt()
    } catch (ignored: Throwable) {

    }
    when (type) {
        0 -> {
            embeddedMasterRestore()
            return false
        }
        1 -> {
            val ofile = File("./result/units.json")
            ofile.createNewFile()
            updateUnitAlbum(ofile)
            return false
        }
        2 -> {
            val ofile = File("./result/monsters.json")
            ofile.createNewFile()
            updateMonsterAlbum(ofile)
            return false
        }
        else -> {
            println("Unknown input:${type}")
            return true
        }
    }
}

fun main(args: Array<String>) {
    JSON.DEFAULT_PARSER_FEATURE = JSON.DEFAULT_PARSER_FEATURE and Feature.UseBigDecimal.mask.inv()
    resfetcher.loadCrcJSONFromLocal("./predata/105110_asset_crc.json")
    resfetcher.loadCrcJSONFromLocal("./predata/105110_asset_crc_android.json")
    for (x in 110..99999 step 10) {
        val version = 105000 + x
        if (!resfetcher.loadCrcJSONFromServer("" + (version))) {
            dlog("===========latest version:${version - 10}")
            break
        }
    }
    parseCountryJSON()
    while (menu()) {
    }
}