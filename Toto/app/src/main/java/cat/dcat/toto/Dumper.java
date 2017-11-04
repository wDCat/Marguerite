package cat.dcat.toto;

import android.util.Log;
import cat.dcat.util.G;
import cat.dcat.util.Helper;

import java.io.*;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Created by DCat on 2017/10/3.
 */
public class Dumper {
    private static int t = 0;
    private final static String TAG = Dumper.class.getSimpleName();

    public static boolean createDumpRoot() {
        if (G.cfg == null || G.cfg.getIntValue("dump") == 0) return true;
        assert (G.timeMark != null);
        boolean ret = true;
        if (!new File(G.DUMP_ROOT).exists())
            ret &= new File(G.DUMP_ROOT).mkdirs();
        if (!new File(G.DUMP_ROOT + "/" + G.timeMark + "/").exists())
            ret &= new File(G.DUMP_ROOT + "/" + G.timeMark + "/").mkdirs();
        return ret;
    }

    public static synchronized File getFileByUri(String uri) {
        assert (G.timeMark != null);
        File result;
        do {
            result = new File(String.format("%s/%s/%d_%s.json", G.DUMP_ROOT, G.timeMark, t++, Helper.prettyFileName(uri)));
        } while (result.exists());
        return result;
    }

    public static void saveByteList(File file, List<Integer> bb) {
        class TmpIS extends InputStream {
            List<Integer> bb;
            int c = 0;

            public TmpIS(List<Integer> bb, int c) {
                this.bb = bb;
                this.c = c;
            }

            @Override
            public int read() throws IOException {
                if (c >= bb.size()) return -1;
                return bb.get(c++);
            }

            @Override
            public int available() throws IOException {
                return bb.size() - c;
            }

            @Override
            public long skip(long n) throws IOException {
                if (c + n > bb.size())
                    n = bb.size() - c;
                c += n;
                return n;
            }

            @Override
            public synchronized void reset() throws IOException {
                c = 0;
            }

        }
        try {

            int size = bb.size();
            long time = System.currentTimeMillis();
            Log.d(TAG, String.format("saving %d bytes to %s", size, file.getAbsolutePath()));
            if (!file.exists())
                file.createNewFile();
            GZIPInputStream gis = new GZIPInputStream(new TmpIS(bb, 0), bb.size());
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
            byte buff[] = new byte[1024];

            while (true) {
                int x = gis.read(buff);
                if (x <= 0) break;
                bos.write(buff, 0, x);

            }
            bos.flush();
            bos.close();
            Log.d(TAG, String.format("saved %d bytes to %s cost %d", size, file.getAbsolutePath(), System.currentTimeMillis() - time));
        } catch (IOException ioe) {
            Log.e(TAG, "fail to save:" + file.getAbsolutePath(), ioe);
        }

    }

    public static void doDump(String uri, List<Integer> bb) {
        if (G.cfg == null || G.cfg.getIntValue("dump") == 0) return;
        Dumper.createDumpRoot();
        uri = Helper.trimURI(uri);
        if (uri.contains("/users/")) {
            saveByteList(getFileByUri(uri), bb);
        } else saveByteList(getFileByUri(uri), bb);
    }
}
