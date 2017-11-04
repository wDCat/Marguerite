package cat.dcat.toto

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import cat.dcat.toto.fakeserver.TotoServerService
import cat.dcat.util.G
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.find
import org.jetbrains.anko.uiThread
import org.jsoup.Jsoup

class TotoServerActivity : AppCompatActivity() {
    val TAG = "TotoServerActivity"
    var debugOut: TextView? = null
    fun println(obj: Any = "\n") {
        if (debugOut != null)
            debugOut!!.text = "${debugOut!!.text}\n[*]${obj}"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_toto_server)
        debugOut = find(R.id.textView)
        find<Button>(R.id.button_start).setOnClickListener {
            val intent = Intent(this, TotoServerService::class.java)
            startService(intent)
            println("asking service to start...")
        }
        find<Button>(R.id.button_stop).setOnClickListener {
            val intent = Intent(this, TotoServerService::class.java)
            stopService(intent)
            println("asking service to stop...")
        }
        find<Button>(R.id.button_status).setOnClickListener {
            checkTotoServerPort();
        }
    }

    fun checkTotoServerPort() {
        println("checking service status...")
        doAsync {
            try {
                Jsoup.connect("http://127.0.0.1:${G.port}/_dump").timeout(10000).get()
                uiThread {
                    println("Service status:OK")
                }

            } catch (e: Exception) {
                uiThread {
                    println("Service status:Error")
                    println(e)
                }
            }
        }

    }
}
