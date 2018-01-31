package com.ppjun.android.xposedbytedance;

import android.content.Context;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

/**
 * Created by ppjun on 1/30/18.
 */

public class HookLogic implements IXposedHookLoadPackage {
    /**
     * 抖音包名
     */
    private final String DOUYIN_PAKCAGENAME = "com.ss.android.ugc.aweme";
    /**
     * 抖音旧版本方法名
     */
    private final String HOOK_METHOD_I = "com.ss.android.ugc.aweme.app.b.a.i";
    /**
     * 抖音新版本方法名
     */
    private final String HOOK_METHOD_G = "com.ss.android.ugc.aweme.app.b.a.g";
    /**
     * 要hook的方法
     */
    private final String TARGET_METHOD = "a";

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        Context context = (Context) callMethod(callStaticMethod(findClass("android.app.ActivityThread", null), "currentActivityThread", new Object[0]), "getSystemContext", new Object[0]);
        String versionName = context.getPackageManager().getPackageInfo(lpparam.packageName, 0).versionName;

        try {
            XposedHelpers.findAndHookMethod("android.app.Application", lpparam.classLoader, "onCreate", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    Context context1= (Context) param.thisObject;
                   String name= context1.getPackageManager().getPackageInfo(lpparam.packageName,0).versionName;

                    log("Found douyin version " + name);

                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }



        if (lpparam.packageName.equals(DOUYIN_PAKCAGENAME)) {
            /**
             * 兼容抖音不同版本之间，要hook的方法名不同
             */
            findAndHookMethod(HOOK_METHOD_I, lpparam.classLoader, TARGET_METHOD, XC_MethodReplacement.returnConstant(null));
            findAndHookMethod(HOOK_METHOD_G, lpparam.classLoader, TARGET_METHOD, XC_MethodReplacement.returnConstant(null));

        }
    }

    private void findAndHookMethod(String hook_method_i, ClassLoader classLoader, String target_method, XC_MethodReplacement xc_methodReplacement) {
        try {
            XposedHelpers.findAndHookMethod(hook_method_i, classLoader, target_method, xc_methodReplacement);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
