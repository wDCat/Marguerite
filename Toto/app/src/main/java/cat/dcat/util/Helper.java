package cat.dcat.util;

import android.util.Log;
import com.alibaba.fastjson.JSONObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Created by DCat on 2017/10/5.
 */
public class Helper {
    public static String readAllFromFile(File file) throws IOException {
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(file.getAbsolutePath()));
        ByteArrayOutputStream out = new ByteArrayOutputStream(1024);

        //System.out.println("Available bytes:" + in.available());
        byte[] temp = new byte[1024];
        int size = 0;
        while ((size = in.read(temp)) != -1) {
            out.write(temp, 0, size);
        }
        in.close();

        byte[] content = out.toByteArray();
        return new String(content, "utf-8");
    }

    public static String readAllFromFile(String src) throws IOException {
        File file = new File(src);
        return readAllFromFile(file);

    }

    public static String trimURI(String uri) {
        uri = uri.trim();
        while (uri.length() > 2 && uri.charAt(0) == '/' && uri.charAt(1) == '/') {
            uri = uri.substring(1);
        }
        return uri;
    }

    private final static char blkedChars[] = new char[]{'/', '\\', '.', '?', '&', '*'};

    public static String prettyFileName(String str) {
        for (int x = 0; x < blkedChars.length; x++) {
            str = str.replace(blkedChars[x], '_');
        }
        return str;
    }

    public static List<Integer> tryToUnGzip(List<Integer> bb) {
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
            GZIPInputStream gis = new GZIPInputStream(new TmpIS(bb, 0), bb.size());
            List<Integer> ret = new ArrayList<>();
            int b = gis.read();
            while (b != -1) {
                ret.add(b);
                b = gis.read();
            }
            Log.d("UNGZIP", "ungzip done.data size " + bb.size() + " --> " + ret.size());
            return ret;
        } catch (Exception e) {
            Log.e("UNGZIP", "ungzip failed", e);
            return bb;
        }

    }

    public static String googleTranslateAPI(String word) {
        try {
            JSONObject jo = new JSONObject();
            jo.put("q", word);
            jo.put("source", "ja");
            jo.put("target", "zh");
            jo.put("format", "text");
            OkHttpClient o3client = new OkHttpClient();
            Request.Builder reqbuilder = new Request.Builder().url("https://translation.googleapis.com/language/translate/v2?key=${}");
            reqbuilder.post(RequestBody.create(MediaType.parse("application/json"), jo.toJSONString()));
            okhttp3.Response o3resp = o3client.newCall(reqbuilder.build()).execute();
            jo = JSONObject.parseObject(o3resp.body().string());
            String ret = jo.getJSONObject("data").getJSONArray("translations").getString(0);
            Log.d("GAPI", "ret" + ret);
            return ret;
        } catch (Exception e) {
            return word;
        }
    }
}
