package com.sunteng.wechatluckmoney.FloadingWindow;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.sunteng.wechatluckmoney.Utils;
import com.sunteng.wechatluckmoney.core.StateController;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by baishixian on 2016/7/20.
 */
public class FloatWindowService extends Service {


    final String WE_CHAT_PACKAGE_NAME = "com.tencent.mm";

    /**
     * 用于在线程中创建或移除悬浮窗。
     */
    private Handler handler = new Handler();

    /**
     * 定时器，定时进行检测当前应该创建还是移除悬浮窗。
     */
    private Timer timer;
    private StateController mLuckyController;

    @Override
    public void onCreate() {
        super.onCreate();
        mLuckyController = StateController.getInstance();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 开启定时器，每隔0.5秒刷新一次
        Utils.printInfo("FloatWindowService onStartCommand");
        if (timer == null) {
            timer = new Timer();
            Utils.printInfo(" timer.schedule RefreshTask");
            timer.schedule(new RefreshTask(), 0, 500);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Service被终止的同时也停止定时器继续运行
        timer.cancel();
        timer = null;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    class RefreshTask extends TimerTask {

        @Override
        public void run() {
            // 当前界面是微信
            if (isWeChatApp()) {
                if (FloatWindowManager.shouldFloatWindowViewEnable() && !FloatWindowManager.isWindowShowing()){
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            FloatWindowManager.createFloatWindow(getApplicationContext());
                        }
                    });
                }else{
                    if (FloatWindowManager.isWindowShowing()){
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                FloatWindowManager.removeFloatWindow(getApplicationContext());
                            }
                        });
                    }
                }

            }
            // 当前界面不是微信
            else if (!isWeChatApp()) {
                if (FloatWindowManager.isWindowShowing()) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            FloatWindowManager.removeFloatWindow(getApplicationContext());
                        }
                    });
                }
            }
        }

    }

    /**
     * 判断当前界面是否是WeChat
     */
    private boolean isWeChatApp() {
        return true;
     /*  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            final int PROCESS_STATE_TOP = 2;
            ActivityManager.RunningAppProcessInfo currentInfo = null;
            Field field = null;
            try {
                field = ActivityManager.RunningAppProcessInfo.class.getDeclaredField("processState");
            } catch (Exception ignored) {
            }
            ActivityManager am = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> appList = am.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo app : appList) {
                if (app.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                        app.importanceReasonCode == 0 ) {
                    Integer state = null;
                    try {
                        state = field.getInt( app );
                    } catch (Exception ignored) {
                    }
                    if (state != null && state == PROCESS_STATE_TOP) {
                        currentInfo = app;
                        break;
                    }
                }
            }
            if (currentInfo != null && currentInfo.processName.contains(WE_CHAT_PACKAGE_NAME)){
                Utils.printInfo("isWeChatApp true");
                return true;
            }
        }else{
            ActivityManager mActivityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> runningAppProcesses  = mActivityManager.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo  runningAppInfo : runningAppProcesses){
                String packageName = runningAppInfo.processName;
                if (packageName.equals(WE_CHAT_PACKAGE_NAME) && runningAppInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND){
                    Utils.printInfo("isWeChatApp true");
                    return true;
                }
            }
        }
        return false;*/
    }

    public boolean isAppRunningForeground(){
        ActivityManager activityManager = (ActivityManager) getSystemService(Service.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningAppProcessInfoList = activityManager.getRunningAppProcesses();
        if (runningAppProcessInfoList==null){
            return false;
        }
        for (ActivityManager.RunningAppProcessInfo processInfo : runningAppProcessInfoList) {
            if (processInfo.processName.equals(WE_CHAT_PACKAGE_NAME)
                    && processInfo.importance==ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND){
                return true;
            }
        }
        return false;
    }
}
