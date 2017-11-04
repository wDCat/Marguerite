package cat.dcat.toto;

import android.util.Log;
import cat.dcat.util.G;

import java.io.*;

/**
 * Created by DCat on 2017/10/3.
 */
public class ResRedirector {
    private final static String TAG = ResRedirector.class.getSimpleName();

    public static boolean doIt(String uri, PrintStream out) {
        if (G.cfg == null || G.cfg.getIntValue("res_redirect") == 0) return false;
        File tfile = new File(String.format("%s/%s", G.RES_ROOT, uri));
        if (tfile.exists() && tfile.isDirectory())
            tfile = new File(String.format("%s/%s/index.json", G.RES_ROOT, uri));
        if (tfile.exists() && tfile.isFile()) {
            try {
                Log.d(TAG, "redirect to file:" + tfile.getAbsolutePath());
                out.print(String.format("HTTP/1.1 %d %s\n", 200, "OK"));
                out.print("Content-Type:application/json\n");
                out.print(String.format("Content-Length:%d\n", tfile.length()));
                out.println();
                InputStream fis = new BufferedInputStream(new FileInputStream(tfile));
                byte buff[] = new byte[Math.min(new Long(tfile.length()).intValue(), 1024)];
                for (int x = 0; x < tfile.length(); x += buff.length) {
                    fis.read(buff);
                    out.write(buff);
                }
                out.flush();
                fis.close();
                Log.d(TAG, "file done:" + tfile.getAbsolutePath());
                return true;
            } catch (IOException ioe) {
                Log.e(TAG, "io exception:" + uri, ioe);
                return false;
            }
        }
        return false;
    }
}
