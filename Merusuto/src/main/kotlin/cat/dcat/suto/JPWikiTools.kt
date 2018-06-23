package cat.dcat.suto

import cat.dcat.util.dlog
import org.jsoup.Jsoup
import java.text.DecimalFormat

/**
 * Created by DCat on 2017/12/9.
 */
open class UnitDataFromWiki {
    var atk_num = 0
    var reach = 0
    var fire = 0.0f
    var water = 0.0f
    var wind = 0.0f
    var light = 0.0f
    var dark = 0.0f
    var atk_speed = 0.0f
    var speed = 0
    var tag = 0.0f
    var hits = 0
    var from = "未知"
    override fun toString(): String {
        return "UnitDataFromWiki(atk_num=$atk_num, reach=$reach, fire=$fire, water=$water, wind=$wind, light=$light, dark=$dark, atk_speed=$atk_speed, speed=$speed, tag=$tag, hits=$hits, from='$from')"
    }


}

open class MonsterDataFromWiki {
    var atk_num = 0
    var hits = 0
    var cd = 0.0f
    var mspd = 0
    var aarea = 0
    var reach = 0
    var endure = 0
    var aspd = 0.0f
    var sarea = 0
    var skmax = ""
    var obtain = ""
    override fun toString(): String {
        return "MonsterDataFromWiki(atk_num=$atk_num, hits=$hits, cd=$cd, mspd=$mspd, aarea=$aarea, reach=$reach, endure=$endure, aspd=$aspd, sarea=$sarea, obtain='$obtain')"
    }

}

fun String.parseDInt(): Int {
    try {
        return DecimalFormat(",###").parse(this).toInt()
    } catch (ignored: Throwable) {
        return 0
    }
}

fun String.toIntS(): Int {
    try {
        return this.toInt()
    } catch (ignored: Throwable) {
        return 0
    }
}

fun String.toFloatS(): Float {
    try {
        return this.toFloat()
    } catch (ignored: Throwable) {
        return 0.0f
    }
}

fun String.trimToNum(): String {
    var ret = this
    var removed = true
    while (removed) {
        removed = false
        if (ret.length > 0 && ret[0] - '0' > 9) {
            ret = ret.substring(1)
            removed = true
        }
        val last = ret.length - 1
        if (ret.length > 0 && ret[last] - '0' > 9) {
            ret = ret.substring(0, last)
            removed = true
        }
    }
    return ret
}

fun fetchMonsterDataFromWiki(uname_: String): MonsterDataFromWiki {
    var uname = uname_
    if (wikiFixJSON.contains(uname)) {
        uname = wikiFixJSON[uname].toString()
        dlog("name fix: ${uname_} --> ${uname}")
    }
    val body = Jsoup.connect("https://xn--cckza4aydug8bd3l.gamerch.com/$uname").get().body() ?: throw Exception("fail to get document")
    val ret = MonsterDataFromWiki()
    body.select(".ui_wikidb_title").forEach {
        val title = it.text().trim()
        val parent = it.parent()

        it.remove()
        val value = parent.text().trim()
        when (title) {
            "再召喚" -> ret.cd = value.trimToNum().toFloatS()
            "同時攻撃数" -> ret.atk_num = value.trimToNum().toIntS()
            "移動速度" -> ret.mspd = value.toIntS()
            "攻撃間隔" -> ret.aspd = value.toFloatS()
            "タフネス" -> ret.endure = value.toIntS()
            "攻撃範囲" -> ret.aarea = value.toIntS()
            "リーチ" -> ret.sarea = value.toIntS()
            "攻撃段数" -> ret.hits = value.trimToNum().toIntS()
            "入手方法" -> ret.obtain = value
            "規格外スキル効果量" -> ret.skmax = value
        }
    }
    return ret
}

fun fetchUnitDataFromWiki(uname_: String): UnitDataFromWiki {
    var uname = uname_
    if (wikiFixJSON.contains(uname)) {
        uname = wikiFixJSON[uname].toString()
        dlog("name fix: ${uname_} --> ${uname}")
    }
    val body = Jsoup.connect("https://xn--cckza4aydug8bd3l.gamerch.com/$uname").get().body() ?: throw Exception("fail to get document")
    val ret = UnitDataFromWiki()
    body.select(".ui_wikidb_title").forEach {
        val title = it.text().trim()
        val parent = it.parent()

        it.remove()
        val value = parent.text().trim()
        when (title) {
            "リーチ" -> ret.reach = value.parseDInt()
            "同時攻撃数" -> ret.atk_num = value.replace('体', ' ').trim().parseDInt()
            "攻撃間隔" -> ret.atk_speed = value.toFloatS()
            "移動速度" -> ret.speed = value.parseDInt()
            "タフネス" -> ret.tag = value.toFloatS()
            "攻撃段数" -> {

                val ds = value.replace('段', ' ').trim()
                if (ds.length > 0) {
                    ret.hits = ds.toIntS()
                }
            }
            "追加日" -> {
                parent.select("span").forEach {
                    ret.from = it.text()
                }
            }

        }
        //dlog("test:${title} parent:${value}")
    }

    body.select(".zokusei_hono").forEach {
        var v = it.parent().text().trim()
        v=v.replace('％','%')
        val match = Regex("[0-9]*%").findAll(v).toList()
        try {
            for (x in 0..4) {
                var b = match[x].value.replace('%', ' ').trim().toFloat() / 100
                when (x) {
                    0 -> ret.fire = b
                    1 -> ret.water = b
                    2 -> ret.wind = b
                    3 -> ret.light = b
                    4 -> ret.dark = b
                }
            }
        } catch (ignored: Throwable) {

        }
    }
    return ret
}

fun main(args: Array<String>) {
    println(fetchMonsterDataFromWiki("ジェラシオン"))
    //println("0.12秒".trimToNum())
    //println(fetchUnitDataFromWiki("「幽幻なる呪果」ブレイデン"))
}