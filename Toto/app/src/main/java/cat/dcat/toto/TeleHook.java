package cat.dcat.toto;

import android.util.Log;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by DCat on 2017/10/22.
 */
public class TeleHook implements IXposedHookLoadPackage {
    private final static String TAG = "nekoCat2";
    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.contains("com.android.provider")) {
            try {
                XposedBridge.hookAllMethods(XposedHelpers.findClass("android.database.sqlite.SQLiteOpenHelper", lpparam.classLoader), "onDowngrade", new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                        Log.d(TAG, "on downgrade called.");
                        return null;
                    }
                });
            } catch (Throwable ignored) {

            }
            Log.d(TAG, "hook done:" + lpparam.packageName);
        }
    }
}
