package com.sunteng.wechatluckmoney;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Method;

/**
 * WeChatLuckMoney Created by baishixian on 2016/12/7.
 */
public class Utils {

    public static boolean isFirstStartUp = true;

    public static boolean isWifi(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting()
                && activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
    }

    public static void installApk(Context context, String path) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        if (Build.VERSION.SDK_INT >= 24){
            Utils.printInfo("path is " + path);
            File file = new File(path);
            if (file == null || context == null){
                Utils.printInfo("is null point");
                return;
            }

            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            // 反射获取FileProvider类型
            Class<?> clazz = null;
            try {
                clazz = Class.forName("android.support.v4.content.FileProvider");
            } catch (ClassNotFoundException e) {
                Utils.printInfo("ClassNotFoundException FileProvider");
            }
            if (clazz == null) {
                Utils.printInfo("can't find class android.support.v4.content.FileProvider");
                return ;
            }
            // 通过反射调用FileProvider.getUriForFile
            //com.sunteng.wechatluckmoney.fileProvider
            Uri contentUri = (Uri) Utils.invokeReflectMethod("android.support.v4.content.FileProvider", "getUriForFile"
                    , null, new Class[]{Context.class, String.class, File.class},
                    new Object[]{context, context.getPackageName() + ".fileProvider", file});
            if (contentUri == null) {
                Utils.printInfo("file location is " + file.toString());
                Utils.printInfo("install failed, contentUri is null!");
                return ;
            }
            Utils.printInfo("uri is " + contentUri.toString());
            intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
        }else {
            intent.setDataAndType(Uri.parse("file://" + path),
                    "application/vnd.android.package-archive");
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static Object invokeReflectMethod(String className, String methodName, Object instance,
                                             Class<?>[] paramTypes, Object[] params) {
        try {
            Class<?> clazz = Class.forName(className);
            Method method = clazz.getDeclaredMethod(methodName, paramTypes);
            method.setAccessible(true);
            return method.invoke(instance, params);
        } catch (Exception e) {
            Utils.printInfo("invokeReflectMethod: "+ Log.getStackTraceString(e));
            return null;
        }
    }

    public static void printInfo(String msg) {
        Log.i("LuckyMoneyHelper", msg);
    }

    public static void printErrorInfo(String msg) {
        Log.e("LuckyMoneyHelper", msg);
    }

    /**
     * 判断文件是否存在
     * @param path
     * @return
     */
    public static boolean fileExist(String path) {
        File file = new File(path);
        if (file == null)
            return false;
        return file.exists();
    }

    public static boolean fileExist(File file) {
        if (file == null)
            return  false;
        return file.exists();
    }

    public static String getUpdateDirPath(Context context) {
        return context.getExternalFilesDir("Downloads").getAbsolutePath() + File.separator;
    }

    /**
     * 判断屏幕是否点亮
     * @param context
     */
    public static boolean isScreenOn(Context context){
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        boolean isScreenOn = pm.isScreenOn();//如果为true，则表示屏幕“亮”了，否则屏幕“暗”了。
        Utils.printInfo("isScreenOn " +isScreenOn);
        return isScreenOn;
    }

    /**
     * 判断屏幕是否上锁
     * @param context
     * @return
     */
    public static boolean isScreenLock(Context context){
        KeyguardManager mKeyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        boolean flag = mKeyguardManager.inKeyguardRestrictedInputMode();
        Utils.printInfo("isScreenLock " +flag);
        return flag;
    }
}
