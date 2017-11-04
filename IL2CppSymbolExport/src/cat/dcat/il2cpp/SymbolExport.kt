package cat.dcat.il2cpp

import one.elf.ElfReader
import one.elf.ElfSection
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import kotlin.system.exitProcess

/**
 * Created by DCat on 2017/10/19.
 */
fun dlog(msg: Any) {
    println(msg)
}

fun readInt32(data: ByteArray, offset: Int): Int {
    try {
        val tmpBa = ByteArray(4)
        for (x in 0..3)
            tmpBa[x] = data[offset + x]
        val bb = ByteBuffer.wrap(tmpBa).order(ByteOrder.LITTLE_ENDIAN)
        return bb.int
    } catch (any: Throwable) {
        return 0
    }
}

fun readUInt32(data: ByteArray, offset: Int): Long {
    try {
        val tmpBa = ByteArray(8)
        for (x in 0..3)
            tmpBa[x] = data[offset + x]
        for (x in 4..7)
            tmpBa[x] = 0
        val bb = ByteBuffer.wrap(tmpBa).order(ByteOrder.LITTLE_ENDIAN)
        return bb.long
    } catch (any: Throwable) {
        return 0
    }
}

fun readInt16(data: ByteArray, offset: Int): Short {
    try {
        val tmpBa = ByteArray(2)
        for (x in 0..1)
            tmpBa[x] = data[offset + x]
        val bb = ByteBuffer.wrap(tmpBa).order(ByteOrder.LITTLE_ENDIAN)
        return bb.short
    } catch (any: Throwable) {
        return 0
    }
}

inline fun isInSection(elfSection: ElfSection, addr: Long): Boolean {
    return addr > elfSection.address() && addr < elfSection.address() + elfSection.size()
}

fun getOffsetByVAddr(elf: ElfReader, addr: Long): Long {
    for (section in elf.sections()) {
        if (section != null && isInSection(section, addr)) {
            return addr - section.address() + section.offset()
        }
    }
    return 0
}


class Il2CppSymbolExport(val ilSo: File, val metadata: File) {
    var strCount: Int = 0
    var strOffset: Int = 0
    var typeCount: Int = 0
    var typeOffset: Int = 0
    var methodOffset: Int = 0
    var soMethodStart: Int = -1
    lateinit var relroSection: ElfSection
    lateinit var rellocalSection: ElfSection
    lateinit var textSection: ElfSection
    lateinit var bssSection: ElfSection
    lateinit var roData: ByteArray
    lateinit var roLocalData: ByteArray
    val sizeofType = 120
    val sizeofMethod = 56
    var symbols = HashMap<String, Int>()

    init {
        initSymbolTable()
    }

    fun getString(data: ByteArray, offset: Int): String {
        if (offset > data.size || offset < 0) return ""
        val sb = StringBuilder()
        var x = 0
        while (true) {
            if (strOffset + offset + x > data.size) break
            val b = data[strOffset + offset + x]
            if (b == 0.toByte()) break
            sb.append(b.toChar())
            x++
        }
        return sb.toString()
    }

    fun initSymbolTable() {
        var mid = 0
        var methodPointerCount = 0
        val data = metadata.readBytes()
        strOffset = readInt32(data, 6 * 4)
        strCount = readInt32(data, 7 * 4)
        typeOffset = readInt32(data, 40 * 4)
        typeCount = readInt32(data, 41 * 4) / sizeofType
        methodOffset = readInt32(data, 12 * 4)
        dlog("File len:${data.size}\n" +
                "String offset:${strOffset} count:${strCount}\n" +
                "Type offset:${typeOffset} count:${typeCount}\n" +
                "Method offset:${methodOffset}")
        dlog("first string:%s".format(getString(data, 0)))
        for (x in 0..typeCount - 1) {
            var found = false
            val typeBase = typeOffset + sizeofType * x
            val nameIndex = readInt32(data, typeBase)
            if (nameIndex < 0 || nameIndex + strOffset > data.size) continue
            val typeName = getString(data, nameIndex)
            val methodStart = readInt32(data, typeBase + 17 * 4)
            val methodCount = readInt16(data, typeBase + 24 * 4)
            dlog("class:%s method start:%x count:%d".format(typeName, methodStart, methodCount))
            for (methodNo in methodStart..methodStart + methodCount - 1) {
                val methodBase = methodOffset + (methodNo * sizeofMethod)
                if (methodBase < 0 || methodBase > data.size) continue
                dlog("methodbase:%x".format(methodBase))
                val methodNameIndex = readInt32(data, methodBase)
                val methodIndex = readInt32(data, methodBase + 6 * 4)
                if (methodIndex >= 0) methodPointerCount++
                val methodName = getString(data, methodNameIndex)
                val methodMark = "${typeName}.${methodName}"
                dlog("method Name:${typeName}${methodName}")
                symbols[methodMark] = methodIndex
            }
            if (found) break
        }
        val elf = ElfReader(ilSo.absolutePath)
        relroSection = elf.section(".data.rel.ro")
        rellocalSection = elf.section(".data.rel.ro.local")
        textSection = elf.section(".text")
        bssSection = elf.section(".bss")
        if (relroSection == null || rellocalSection == null || textSection == null || bssSection == null) {
            dlog("cannot found sections")
            exitProcess(1)
        }
        roData = ByteArray(relroSection.size().toInt())
        roLocalData = ByteArray(rellocalSection.size().toInt())
        var fis = FileInputStream(ilSo)
        fis.skip(relroSection.offset())
        if (fis.read(roData) != relroSection.size().toInt()) {
            dlog("[E]cannot read relroSection!")
            exitProcess(1)
        }
        fis.close()
        fis = FileInputStream(ilSo)
        fis.skip(rellocalSection.offset())
        if (fis.read(roLocalData) != rellocalSection.size().toInt()) {
            dlog("[E]cannot read rellocalSection!")
            exitProcess(1)
        }
        dlog("relroSection start:%x vaddr:%x".format(relroSection.offset() + relroSection.entrySize(), relroSection.address()))
        dlog("mpc:%x off:%x".format(methodPointerCount, relroSection.address() + (4 * mid)))
        var s = 0

        while (true) {
            var found = true
            for (x in 0..20) {
                val co = readInt32(roData, (s + x) * 4)
                if (x == 0) {
                    if (isInSection(rellocalSection, co.toLong())) {
                        dlog("checking:%x".format(co))
                        val off = getOffsetByVAddr(elf, co.toLong())
                        if (off == 0L) break
                        for (y in 0..5) {
                            val addr = readInt32(roLocalData, (off + y * 4 - rellocalSection.offset()).toInt())
                            dlog("sub addr:%x".format(addr))
                            if (!isInSection(bssSection, addr.toLong())) {
                                found = false
                                break
                            }
                        }
                    } else {
                        found = false
                        break
                    }
                } else {
                    if (!isInSection(textSection, co.toLong())) {
                        found = false
                        break
                    }
                }
                if (!found) break
            }
            if (found) {
                soMethodStart = (s + 1) * 4
                break
            }
            s++
        }
        if (soMethodStart == -1)
            throw Exception("cannot get method start in libil2cpp.so")

        dlog("method may start at %x".format(relroSection.address() + soMethodStart))

    }

    fun findSymbol(type: String, method: String): Long {
        val methodMark = "${type}.${method}"
        val index = symbols[methodMark]
        if (index == null) return -1
        val offset = soMethodStart + (index * 4)
        dlog("index:${index} offset:%x %x".format(offset, roData.size))
        return readUInt32(roData, offset)
    }
}

fun main(args: Array<String>) {
    val s = Il2CppSymbolExport(File("libil2cpp.so"),
            File("global-metadata.dat"))
    dlog("test find result:%x".format(s.findSymbol("Piece", "Damaged")))
}