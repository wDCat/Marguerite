package cat.dcat.toto

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import cat.dcat.il2cpp.Il2CppSymbolExport
import cat.dcat.util.G
import cat.dcat.util.Helper
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.PrintStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Created by DCat on 2017/11/4.
 */

class MainActivity : AppCompatActivity() {
    var cfgbox: EditText? = null

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            666 -> if (grantResults.size <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "no storage permission!", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permissionCheck = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if (permissionCheck == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        666)
            }

        }
        if (G.timeMark == null) {
            val sdf = SimpleDateFormat("yyyy-MM-DD_kk_mm")
            G.timeMark = sdf.format(Date())
            Log.d(TAG, "timeMark:" + G.timeMark)
        }
        if (!Dumper.createDumpRoot()) {
            Log.e(TAG, "fail to make dump root")
            Toast.makeText(this, "fail to make dump root", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        if (File("/data/d_self_area/libtoto.so").exists()) {
            (findViewById(R.id.neko) as TextView).text = "Library is OK."

        } else {
            (findViewById(R.id.neko) as TextView).text = "Library is Missing.(/data/d_self_area/libtoto.so not found or no permission)"
        }
        cfgbox = findViewById(R.id.editText2) as EditText
        findViewById(R.id.button).setOnClickListener { startActivity(Intent(this@MainActivity, TotoServerActivity::class.java)) }
        findViewById(R.id.button2).setOnClickListener { loadCfg() }
        findViewById(R.id.button3).setOnClickListener { saveCfg() }
        findViewById(R.id.button_rsym).setOnClickListener {
            try {
                reloadSymbols()
            } catch (e: Exception) {
                Log.e(TAG, "error", e)
            }
        }
        loadCfg()
    }

    fun loadCfg() {
        var result = "{\"error\": 1}"
        try {
            val fp = File(G.CFG_PATH)
            if (!fp.exists()) {
                fp.createNewFile()
                val out = PrintStream(FileOutputStream(fp))
                out.print("{\n" +
                        "  \"enable\": 1,\n" +
                        "  \"dump\": 0,\n" +
                        "  \"res_redirect\": 1,\n" +
                        "  \"base_url\":\"https://toto.hekk.org\"\n" +
                        "}")
                out.flush()
                out.close()
            }
            result = Helper.readAllFromFile(G.CFG_PATH)
            G.cfg = JSONObject.parseObject(result)
        } catch (e: Exception) {
            Log.e(TAG, "fail to load cfg", e)
        }

        cfgbox!!.setText(result)
    }

    fun saveCfg() {
        try {
            val fp = File(G.CFG_PATH)
            if (!fp.exists())
                fp.createNewFile()
            val out = PrintStream(FileOutputStream(fp))
            out.print(cfgbox!!.text)
            out.flush()
            out.close()
            loadCfg()
        } catch (e: Exception) {
            Toast.makeText(this, "fail to save", Toast.LENGTH_LONG).show()
            Log.e(TAG, "", e)
        }

    }

    @Throws(IOException::class)
    fun reloadSymbols() {
        var pkg: String? = null
        for (app in packageManager.getInstalledApplications(0)) {
            if (app.packageName == "jp.co.happyelements.toto") {
                pkg = app.sourceDir
                Log.d(TAG, "pkg:" + pkg!!)
                break
            }
        }
        if (pkg == null) {
            Toast.makeText(this, "Fail to get toto's apk!", Toast.LENGTH_LONG).show()
            return
        }
        var il2cpp = false
        var meta = false
        val zip = ZipFile(pkg)
        val iter = zip.entries()
        while (iter.hasMoreElements()) {
            val entry = iter.nextElement() as ZipEntry
            val zis = zip.getInputStream(entry)
            if (entry.name == "assets/bin/Data/Managed/Metadata/global-metadata.dat") {
                val tmp = File("/mnt/sdcard/toto/tmp.dat")
                if (!tmp.exists()) tmp.createNewFile()
                val fos = FileOutputStream(tmp)
                val buff = ByteArray(1024)
                while (true) {
                    val x = zis.read(buff)
                    if (x <= 0) break
                    fos.write(buff, 0, x)
                }
                fos.flush()
                fos.close()
                meta = true
            }
            if (entry.name == "lib/armeabi-v7a/libil2cpp.so") {
                val tmp = File("/mnt/sdcard/toto/tmp.so")
                if (!tmp.exists()) tmp.createNewFile()
                val fos = FileOutputStream(tmp)
                val buff = ByteArray(1024)
                while (true) {
                    val x = zis.read(buff)
                    if (x <= 0) break
                    fos.write(buff, 0, x)
                }
                fos.flush()
                fos.close()
                il2cpp = true
            }
        }
        if (!meta || !il2cpp) {
            Toast.makeText(this, "Fail to unzip file!", Toast.LENGTH_LONG).show()
            return
        }
        val jo = JSON.parseObject(cfgbox!!.text.toString())
        val se = Il2CppSymbolExport(File("/mnt/sdcard/toto/tmp.so"), File("/mnt/sdcard/toto/tmp.dat"))
        jo.put("damaged", se.findSymbol("Piece", "Damaged"))
        jo.put("getbaseurl", se.findSymbol("Setting", "get_BaseURL"))
        jo.put("getendpoint", se.findSymbol("Setting", "GetEndpoint"))
        cfgbox!!.setText(jo.toJSONString())
        saveCfg()
        Toast.makeText(this, "Update done!", Toast.LENGTH_LONG).show()
        Log.d(TAG, "find result:" + se.findSymbol("Piece", "Damaged"))
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }
}
