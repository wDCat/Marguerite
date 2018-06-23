package cat.dcat.suto

import java.io.File

/**
 * Created by DCat on 2017/12/10.
 */
val pythonExe = "./python36-32/python.exe"

class UnityPack(var binPath: String, var tmpPath: String) {
    fun extractImg(bundlePath: String, id: Int, outputFN: String) {
        val shell = "\"${pythonExe}\" \"${binPath}\" --images \"${bundlePath}\" -o \"${tmpPath}\" -f \"${outputFN}\""
        val p = Runtime.getRuntime().exec(shell)
        p.waitFor()
    }
    fun extractSound(bundlePath: String, id: Int, outputFN: String) {
        val shell = "\"${pythonExe}\" \"${binPath}\" --audio \"${bundlePath}\" -o \"${tmpPath}\" -f \"${outputFN}\""
        val p = Runtime.getRuntime().exec(shell)
        p.waitFor()
    }
}


val unityPack = UnityPack("./unitypack/unityextract.py", "./tmp")