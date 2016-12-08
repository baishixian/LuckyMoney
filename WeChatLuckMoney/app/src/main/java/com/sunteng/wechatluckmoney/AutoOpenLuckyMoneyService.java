package com.sunteng.wechatluckmoney;

import android.accessibilityservice.AccessibilityService;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.provider.Contacts;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

/**
 * WeChatLuckMoney Created by baishixian on 2016/12/8.
 */

public class AutoOpenLuckyMoneyService extends AccessibilityService {
    private static final String TAG = AutoOpenLuckyMoneyService.class.getSimpleName();

    private static final int MSG_BACK_HOME = 0;
    private static final int MSG_BACK_ONCE = 1;
    boolean hasNotify = false;
    boolean hasLuckyMoney = true;
    private Object currentActivityName;


    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int eventType = event.getEventType(); // 事件类型
        switch (eventType) {
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED: // 通知栏事件
                Log.i(TAG, "TYPE_NOTIFICATION_STATE_CHANGED");
                if(PhoneController.isLockScreen(this)) { // 锁屏
                    PhoneController.wakeAndUnlockScreen(this);   // 唤醒点亮屏幕
                }

                openAppByNotification(event); // 打开微信
                hasNotify = true;
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                String className = event.getClassName().toString();
                Utils.printInfo("setCurrentActivityName " +  className);
                if (className.equals("com.tencent.mm.ui.LauncherUI")) {
                    hasNotify = true;
                } else if (className.equals("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI")) {
                    hasNotify = true;
                } else if (className.equals("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI")) {
                    hasNotify = true;
                } else if (className.equals("com.tencent.mm.ui.chatting.ChattingUI")) {
                    hasNotify = true;
                }
            default:
                Log.i(TAG, "DEFAULT");
                if(hasNotify) {
                    AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                    clickLuckyMoney(rootNode); // 点击红包

                    className = event.getClassName().toString();
                    if (className.equals(UI.LUCKY_MONEY_RECEIVE_UI)) { //红包接收界面
                        if(!openLuckyMoney()) { // 如果红包被抢光了，就返回主界面
                            backToHome();
                            hasNotify = false;
                        }
                        hasLuckyMoney = true;
                    } else if (className.equals(UI.LUCKY_MONEY_DETAIL_UI)) { // 抢到红包
                        backToHome();
                        hasNotify = false;
                        hasLuckyMoney = true;
                    } else { // 处理没红包的情况，直接返回主界面
                        if(!hasLuckyMoney) {
                            handler.sendEmptyMessage(MSG_BACK_ONCE);
                            hasLuckyMoney = true;   // 防止后退多次
                        }
                    }
                }
                break;
        }
    }

    @Override
    public void onInterrupt() {
    }


    /**
     * 打开微信
     * @param event 事件
     */
    private void openAppByNotification(AccessibilityEvent event) {
        if (event.getParcelableData() != null  && event.getParcelableData() instanceof Notification) {
            Notification notification = (Notification) event.getParcelableData();
            try {
                PendingIntent pendingIntent = notification.contentIntent;
                pendingIntent.send();
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 搜索并点击红包
     */
    private void clickLuckyMoney(AccessibilityNodeInfo rootNode) {
        if(rootNode != null) {
            int count = rootNode.getChildCount();
            for (int i = count - 1; i >= 0; i--) {  // 倒序查找最新的红包
                AccessibilityNodeInfo node = rootNode.getChild(i);
                if (node == null)
                    continue;

                CharSequence text = node.getText();
                if (text != null && (text.toString().equals("领取红包") || text.toString().equals("查看红包"))) {
                    AccessibilityNodeInfo parent = node.getParent();
                    while (parent != null) {
                        if (parent.isClickable()) {
                            parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            break;
                        }
                        parent = parent.getParent();
                    }
                }

                clickLuckyMoney(node);
            }
        }
    }

    /**
     * 打开红包
     */
    private boolean openLuckyMoney() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if(rootNode != null) {
            List<AccessibilityNodeInfo> nodes =
                    rootNode.findAccessibilityNodeInfosByText(UI.OPEN_LUCKY_MONEY_BUTTON_ID);
            for(AccessibilityNodeInfo node : nodes) {
                if(node.isClickable()) {
                    Log.i(TAG, "open LuckyMoney");
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    return true;
                }
            }
        }

        return false;
    }

    private void backToHome() {
        if(handler.hasMessages(MSG_BACK_HOME)) {
            handler.removeMessages(MSG_BACK_HOME);
        }
        handler.sendEmptyMessage(MSG_BACK_HOME);
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if(msg.what == MSG_BACK_HOME) {
                performGlobalAction(GLOBAL_ACTION_BACK);
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        performGlobalAction(GLOBAL_ACTION_BACK);
                        hasLuckyMoney = false;
                    }
                }, 1500);
            } else if(msg.what == MSG_BACK_ONCE) {
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "click back");
                        performGlobalAction(GLOBAL_ACTION_BACK);
                        hasLuckyMoney = false;
                        hasNotify = false;
                    }
                }, 1500);
            }
        }
    };
}
