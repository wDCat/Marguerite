package cat.dcat.toto.fakeserver;

import android.util.Log;
import cat.dcat.toto.Dumper;
import cat.dcat.toto.ResRedirector;
import cat.dcat.toto.TotoTranslatorKt;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by DCat on 2017/10/3.
 */
public class TotoHandler2 extends Thread {
    public static String baseServer = "toto.hekk.org";
    private final static String TAG = TotoHandler2.class.getSimpleName();
    Socket client;

    public TotoHandler2(Socket client) {
        this.client = client;
    }

    @Override
    public void run() {
        try {
            client.setSoTimeout(30000);
            PrintStream out = new PrintStream(client.getOutputStream());
            BufferedReader buf = new BufferedReader(new InputStreamReader(client.getInputStream()));
            String data = buf.readLine();
            Log.d("ASSHOLE", data);
            String r[] = data.split(" ");
            if (r.length < 2) {
                Log.e(TAG, "bad request:" + data);
                return;
            }

            final String url = String.format("https://%s%s", baseServer, r[1]);
            if (r[1].contains("?")) {
                r[1] = r[1].substring(0, r[1].indexOf("?"));
            }
            final String uri = r[1];
            String method = r[0].trim().toLowerCase();
            Log.d(TAG, String.format("%s %s %s", r[0], url, uri));
            OkHttpClient o3client = new OkHttpClient();
            Request.Builder reqbuilder = new Request.Builder().url(url);
            int clen = -1;
            try {
                String s = buf.readLine();
                while (s != null && !"".equals(s)) {
                    Log.d("ASSHOLE", s);
                    int ko = s.indexOf(": ");
                    if (ko > 0) {
                        String key = s.substring(0, ko).trim();
                        String val = s.substring(ko + 2).trim();
                        while (val.length() > 0 && (val.charAt(val.length() - 1) == '\n' || val.charAt(val.length() - 1) == ';'))
                            val = val.substring(0, val.length() - 1);
                        if ("host".equals(key.toLowerCase())) {
                            val = baseServer;
                        }

                        reqbuilder.addHeader(key, val);
                        if (s.toLowerCase().contains("content-length")) {
                            clen = Integer.valueOf(val);
                        }
                    }
                    s = buf.readLine();
                }
                if ("post".equals(method)) {
                    if (clen == -1) {
                        Log.d(TAG, "bad post.");
                        out.println("400");
                        return;
                    }
                    byte reqdata[] = new byte[clen];
                    int b, x = 0;
                    try {
                        while (x < clen && (b = buf.read()) != -1) {
                            reqdata[x++] = (byte) b;
                        }
                    } catch (Exception e) {
                    }
                    String rr = "{";
                    char hexs[] = "0123456789ABCDEF".toCharArray();
                    for (int y = 0; y < clen; y++) {
                        rr += reqdata[y];
                        rr += ",";
                    }
                    rr += "}";
                    Log.d("ASSHOLE", rr);
                    Log.d(TAG, "[req]read " + clen + " bytes");
                    reqbuilder.post(RequestBody.create(MediaType.parse(""), reqdata));
                } else if ("get".equals(method)) {
                    reqbuilder.get();
                } else {
                    throw new Error("Unknown Error");
                }
                if (!ResRedirector.doIt(uri, out)) {
                    okhttp3.Response o3resp = o3client.newCall(reqbuilder.build()).execute();
                    if (!o3resp.isSuccessful()) {
                        Log.e(TAG, String.format("okhttp failed:%d:%s", o3resp.code(), o3resp.message()));
                        out.println("404-1");
                    } else if (o3resp.body() == null) {
                        Log.e(TAG, String.format("okhttp failed(null):%d:%s", o3resp.code(), o3resp.message()));
                        out.println("404-2");
                    } else Log.d(TAG, "okhttp succ");
                    InputStream is = o3resp.body().byteStream();
                    final List<Integer> bb = new ArrayList<>();
                    StringBuffer sb = new StringBuffer();
                    try {
                        int b = is.read(), c = 0;
                        while (b != -1) {
                            bb.add(b);
                            c++;
                            sb.append((char) b);
                            b = is.read();
                        }
                        Log.d(TAG, "[resp]read " + c + " bytes");
                    } catch (Exception ignored) {
                        Log.d(TAG, "read buff error", ignored);
                    }
                    /*
                    final List<Integer> dbb = Helper.tryToUnGzip(bb);
                    byte str[] = new byte[dbb.size()];
                    for (int x = 0; x < dbb.size(); x++)
                        str[x] = dbb.get(x).byteValue();
                    Log.d(TAG, "context:" + new String(str, "utf-8"));*/
                    if ((url.contains("stories")) && TotoTranslatorKt.totoTranslateIt(bb, out, o3resp)) {

                    } else {
                        out.print(String.format("HTTP/1.1 %d %s\n", o3resp.code(), o3resp.message()));
                        for (String key : o3resp.headers().names()) {
                            key = key.toLowerCase();
                            if ("transfer-encoding".equals(key)) continue;//block chunked
                            //Log.d(TAG, String.format("ret key[%s] value:[%s]", key, o3resp.header(key)));
                            out.print(String.format("%s:%s\n", key, o3resp.header(key)));
                        }
                        out.print("\n");
                        out.flush();
                        {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    Dumper.doDump(uri, bb);
                                }
                            }).start();
                            for (Integer b_ : bb) {
                                byte b = b_.byteValue();
                                try {
                                    out.write(b);
                                } catch (Exception ignored) {
                                }
                            }
                            out.flush();
                        }
                    }
                    client.close();
                }
                Log.d(TAG, "acc done:" + url);
            } catch (NullPointerException e) {
                Log.e(TAG, "", e);
            }
        } catch (Exception e) {
            Log.e(TAG, "", e);
        }
    }
}
