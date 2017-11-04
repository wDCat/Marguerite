package cat.dcat.toto.fakeserver;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class TotoServerService extends Service {
    private final static String TAG = TotoServerService.class.getSimpleName();
    private TotoServer totoServer = new TotoServer();

    public TotoServerService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        totoServer.start();
        return START_STICKY_COMPATIBILITY;
    }

    @Override
    public void onDestroy() {
        totoServer.interrupt();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
