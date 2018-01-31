# XposedByteDance
抖音去除检测Xposed的提示



  特别感谢[https://www.52pojie.cn/thread-684757-1-1.html](https://www.52pojie.cn/thread-684757-1-1.html)

官方教程:[https://github.com/rovo89/XposedBridge/wiki/Development-tutorial](https://github.com/rovo89/XposedBridge/wiki/Development-tutorial)

安装了xposed后你的手机就可以自动抢微信，qq红包了，还有可以搜索最近很多的答题app答案。


   下面以抖音去Toast为例子,教你如何制作一个xposed插件。

   首先安装了xposed框架后，抖音会在framework检测到XposedBridge.jar文件，就会提示检测到Xposed，要求卸装。
   
   思路是通过jadx打开抖音1.7.2版本。在resource-resource.arsc-res-values-strings.xml.查询到
   ```
       <string name="a_9">检测到你使用%s,请卸载后重试</string>
 ```

 再次全局搜索a_9在哪里被引用 找到package com.ss.android.ugc.aweme.app.b.a的g类，我们要做就是替换到里面的a方法。
  
  在AndroidManifest.xml，添加是否为xposed项目，xposed描述，最小的xposed版本

```java
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.ppjun.android.xposedbytedance">

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/AppTheme">
        <activity android:name=".MainActivity">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        <meta-data
            android:name="xposedmodule"
            android:value="true" />
        <meta-data
            android:name="xposeddescription"
            android:value="@string/app_desc" />
        <meta-data
            android:name="xposedminversion"
            android:value="53" />
    </application>

</manifest>
```

首先在app/build.gradle
```java
    provided 'de.robv.android.xposed:api:82'
    provided 'de.robv.android.xposed:api:82:sources'
```

然后创建类HookLogic，实现IXposedHookLoadPackage接口，实现它的handleLoadPackage方法。

```java
    if (lpparam.packageName.equals(DOUYIN_PAKCAGENAME)) {
            /**
             * 兼容douyi不同版本之间，方法名不同
             */
            findAndHookMethod(HOOK_METHOD_I, lpparam.classLoader, TARGET_METHOD, XC_MethodReplacement.returnConstant(null));
            findAndHookMethod(HOOK_METHOD_G, lpparam.classLoader, TARGET_METHOD, XC_MethodReplacement.returnConstant(null));

        }
```
因为xposed是替换了android的zygote进程，需要重启才能替换。
这里使用[https://github.com/shuihuadx/XposedHook](https://github.com/shuihuadx/XposedHook) 能让你的手机不重启就可以让xposed代码生效。需要在创建HookLoader类。
 最后为了让xposed知道插件的入口，要在assets文件夹下创建xposed_init指向你的HookLoader,规则是包名+类名(com.ppjun.android.xposedbytedance.HookLoader）
 然后就可以安装在你的手机上。

 注意as默认开启instant run，多次修改代码后，xposed install日志会显示ClassNotFindException的,这个xposed的issue有提到。

第一次安装，要重启手机才能让HookLoader代码生效。

