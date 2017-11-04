package cat.dcat.toto.fakeserver;

import android.util.Log;
import cat.dcat.util.G;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by DCat on 2017/10/3.
 */
public class TotoServer extends Thread {
    private final static String TAG = TotoServer.class.getSimpleName();
    ServerSocket server;
    private boolean shouldInterrupt = false;

    @Override
    public void interrupt() {
        Log.d(TAG, "ask server to stop");
        shouldInterrupt = true;
    }

    @Override
    public boolean isInterrupted() {

        return shouldInterrupt;
    }

    @Override
    public void run() {
        try {
            server = new ServerSocket(G.port);
        } catch (IOException e) {
            Log.e(TAG, "fail to start server", e);
        }
        Log.d(TAG, "server start");
        while (true) {
            try {
                Socket client = server.accept();
                if (shouldInterrupt) {
                    Log.d(TAG, "server stopped.");
                    break;
                }
                Log.d(TAG, "accept:" + client.getRemoteSocketAddress());
                new TotoHandler2(client).start();
            } catch (IOException ignored) {
            }

        }

    }
}
