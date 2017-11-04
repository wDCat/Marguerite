package cat.dcat.toto;

import android.util.Log;
import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by DCat on 2017/3/3.
 */
public class Hook implements IXposedHookLoadPackage {
    private final static String TAG = "nekoCat";

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        Log.d(TAG,"pkg:"+lpparam.packageName);
        if (lpparam.packageName.equals("jp.co.happyelements.toto")) {
            Log.d(TAG, "found toto");
            try {
                Log.d(TAG, "try to load lib in system");
                System.loadLibrary("toto");

            } catch (Throwable e) {
                Log.e(TAG, "Error:", e);
                try {
                    Log.d(TAG, "try to load lib in /data");
                    System.load("/data/d_self_area/libtoto.so");
                } catch (Throwable e2) {
                    Log.e(TAG, "Error:", e2);
                }

            }
            XC_MethodReplacement nullIt = new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    Log.d(TAG, "called:" + methodHookParam.method.getName());
                    return null;
                }
            };
            //block pause
            XposedBridge.hookAllMethods(XposedHelpers.findClass("com.unity3d.player.UnityPlayer", lpparam.classLoader), "pause", nullIt);
            XposedBridge.hookAllMethods(XposedHelpers.findClass("com.unity3d.player.UnityPlayer", lpparam.classLoader), "onWindowFocusChanged", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!((boolean) param.args[0]))
                        param.setResult(null);
                }
            });

            Log.d(TAG, "All done.v2");
        }
    }
}
