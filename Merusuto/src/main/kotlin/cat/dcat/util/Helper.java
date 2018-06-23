package cat.dcat.util;

import java.io.*;
import java.net.*;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Created by DCat on 2017/1/18.
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
}
