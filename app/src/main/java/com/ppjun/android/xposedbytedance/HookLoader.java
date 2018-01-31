package com.ppjun.android.xposedbytedance;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import dalvik.system.PathClassLoader;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by ppjun on 1/31/18.
 */

public class HookLoader implements IXposedHookLoadPackage{
// 按照实际使用情况修改下面几项的值
    /**
     * 当前 Xposed 模块的包名, 方便寻找 apk 文件
     */
    private final String modulePackage = "com.ppjun.android.xposedbytedance";
    /**
     * 宿主程序的包名 (允许多个), 过滤无意义的包名, 防止无意义的 apk 文件加载
     */
    private static List<String> hostAppPackages = new ArrayList<>();

    static {
        // TODO: Add the package name of application your want to hook!
        hostAppPackages.add("com.ss.android.ugc.aweme");
    }

    /**
     * 实际 hook 逻辑处理类
     */
    private final String handleHookClass = HookLogic.class.getName();
    /**
     * 实际 hook 逻辑处理类的入口方法
     */
    private final String handleHookMethod = "handleLoadPackage";

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (hostAppPackages.contains(loadPackageParam.packageName)) {
            // 将 loadPackageParam 的 classloader 替换为宿主程序 Application 的 classloader, 解决宿主程序存在多个. dex 文件时, 有时候 ClassNotFound 的问题
            XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Context context=(Context) param.args[0];
                    loadPackageParam.classLoader = context.getClassLoader();
                    invokeHandleHookMethod(context,modulePackage, handleHookClass, handleHookMethod, loadPackageParam);
                }
            });
        }
    }

    /**
     * 安装 app 以后，系统会在 / data/app / 下备份了一份. apk 文件，通过动态加载这个 apk 文件，调用相应的方法
     * 这样就可以实现，只需要第一次重启，以后修改 hook 代码就不用重启了
     * @param context context 参数
     * @param modulePackageName 当前模块的 packageName
     * @param handleHookClass   指定由哪一个类处理相关的 hook 逻辑
     * @param loadPackageParam  传入 XC_LoadPackage.LoadPackageParam 参数
     * @throws Throwable 抛出各种异常, 包括具体 hook 逻辑的异常, 寻找 apk 文件异常, 反射加载 Class 异常等
     */
    private void invokeHandleHookMethod(Context context,String modulePackageName, String handleHookClass, String handleHookMethod, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
//        File apkFile = findApkFileBySDK(modulePackageName);// 会受其它 Xposed 模块 hook 当前宿主程序的 SDK_INT 的影响
        //        File apkFile = findApkFile(modulePackageName);
        // 原来的两种方式不是很好, 改用这种新的方式
        File apkFile=findApkFile(context,modulePackageName);
        if (apkFile==null){
            throw new RuntimeException(" 寻找模块 apk 失败 ");
        }
        // 加载指定的 hook 逻辑处理类，并调用它的 handleHook 方法
        PathClassLoader pathClassLoader = new PathClassLoader(apkFile.getAbsolutePath(), ClassLoader.getSystemClassLoader());
        Class<?> cls = Class.forName(handleHookClass, true, pathClassLoader);
        Object instance = cls.newInstance();
        Method method = cls.getDeclaredMethod(handleHookMethod, XC_LoadPackage.LoadPackageParam.class);
        method.invoke(instance, loadPackageParam);
    }

    /**
     * 根据包名构建目标 Context, 并调用 getPackageCodePath() 来定位 apk
     * @param context context 参数
     * @param modulePackageName 当前模块包名
     * @return return apk file
     */
    private File findApkFile(Context context,String modulePackageName){
        if (context==null){
            return null;
        }
        try {
            Context moudleContext = context.createPackageContext(modulePackageName, Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
            String apkPath=moudleContext.getPackageCodePath();
            return new File(apkPath);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
    /**
     * 寻找这个 Android 设备上的当前 apk 文件, 不受其它 Xposed 模块 hook SDK_INT 的影响
     *
     * @param modulePackageName 当前模块包名
     * @return File 返回 apk 文件
     * @throws FileNotFoundException 在 / data/app / 下的未找到本模块 apk 文件, 请检查本模块包名配置是否正确.
     *                               具体检查 build.gradle 中的 applicationId 和 AndroidManifest.xml 中的 package
     */
    @Deprecated
    private File findApkFile(String modulePackageName) throws FileNotFoundException {
        File apkFile = null;
        try {
            apkFile = findApkFileAfterSDK21(modulePackageName);
        } catch (Exception e) {
            try {
                apkFile = findApkFileBeforeSDK21(modulePackageName);
            } catch (Exception e2) {
                // 忽略这个异常
            }
        }
        if (apkFile == null) {
            throw new FileNotFoundException(" 没在 / data/app / 下找到文件对应的 apk 文件 ");
        }
        return apkFile;
    }
    /**
     * 根据当前的 SDK_INT 寻找这个 Android 设备上的当前 apk 文件
     *
     * @param modulePackageName 当前模块包名
     * @return File 返回 apk 文件
     * @throws FileNotFoundException 在 / data/app / 下的未找到本模块 apk 文件, 请检查本模块包名配置是否正确.
     *                               具体检查 build.gradle 中的 applicationId 和 AndroidManifest.xml 中的 package
     */
    @Deprecated
    private File findApkFileBySDK(String modulePackageName) throws FileNotFoundException {
        File apkFile;
        // 当前 Xposed 模块 hook 了 Build.VERSION.SDK_INT 不用担心，因为这是发生在 hook 之前，不会有影响
        // 但是其它的 Xposed 模块 hook 了当前宿主的这个值以后，就会有影响了, 所以这里没有使用这个方法
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            apkFile = findApkFileAfterSDK21(modulePackageName);
        } else {
            apkFile = findApkFileBeforeSDK21(modulePackageName);
        }
        return apkFile;
    }

    /**
     * 寻找 apk 文件 (api_21 之后)
     * 在 Android sdk21 以及之后，apk 文件的路径发生了变化
     *
     * @param packageName 当前模块包名
     * @return File 返回 apk 文件
     * @throws FileNotFoundException apk 文件未找到
     */
    @Deprecated
    private File findApkFileAfterSDK21(String packageName) throws FileNotFoundException {
        File apkFile;
        File path = new File(String.format("/data/app/%s-%s", packageName, "1"));
        if (!path.exists()) {
            path = new File(String.format("/data/app/%s-%s", packageName, "2"));
        }
        if (!path.exists() || !path.isDirectory()) {
            throw new FileNotFoundException(String.format(" 没找到目录 / data/app/%s-%s", packageName, "1/2"));
        }
        apkFile = new File(path, "base.apk");
        if (!apkFile.exists() || apkFile.isDirectory()) {
            throw new FileNotFoundException(String.format(" 没找到文件 / data/app/%s-%s/base.apk", packageName, "1/2"));
        }
        return apkFile;
    }

    /**
     * 寻找 apk 文件 (api_21 之前)
     *
     * @param packageName 当前模块包名
     * @return File 返回 apk 文件
     * @throws FileNotFoundException apk 文件未找到
     */
    @Deprecated
    private File findApkFileBeforeSDK21(String packageName) throws FileNotFoundException {
        File apkFile = new File(String.format("/data/app/%s-%s.apk", packageName, "1"));
        if (!apkFile.exists()) {
            apkFile = new File(String.format("/data/app/%s-%s.apk", packageName, "2"));
        }
        if (!apkFile.exists() || apkFile.isDirectory()) {
            throw new FileNotFoundException(String.format(" 没找到文件 / data/app/%s-%s.apk", packageName, "1/2"));
        }
        return apkFile;
    }
}