package com.sunteng.wechatluckmoney.core;

import android.accessibilityservice.AccessibilityService;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import com.sunteng.wechatluckmoney.LuckyActivity;
import com.sunteng.wechatluckmoney.LuckyMoneySignature;
import com.sunteng.wechatluckmoney.PowerUtil;
import com.sunteng.wechatluckmoney.R;
import com.sunteng.wechatluckmoney.UI;
import com.sunteng.wechatluckmoney.Utils;

import java.util.List;

import static android.app.Notification.VISIBILITY_PRIVATE;

/**
 *  Created by baishixian on 12/07/16.
 */
public class LuckyService extends AccessibilityService implements SharedPreferences.OnSharedPreferenceChangeListener{
    private static final String WECHAT_DETAILS_EN = "Details";
    private static final String WECHAT_DETAILS_CH = "红包详情";
    private static final String WECHAT_BETTER_LUCK_EN = "Better luck next time!";
    private static final String WECHAT_BETTER_LUCK_CH = "手慢了";
    private static final String WECHAT_EXPIRES_CH = "已超过24小时";
    private static final String WECHAT_VIEW_SELF_CH = "查看红包";
    private static final String WECHAT_VIEW_OTHERS_CH = "领取红包";
    private static final String WECHAT_NOTIFICATION_TIP = "[微信红包]";
    private static final String WECHAT_LUCKMONEY_RECEIVE_ACTIVITY = "LuckyMoneyReceiveUI";
    private static final String WECHAT_LUCKMONEY_DETAIL_ACTIVITY = "LuckyMoneyDetailUI";
    private static final String WECHAT_LUCKMONEY_GENERAL_ACTIVITY = "LauncherUI";
    private static final String WECHAT_LUCKMONEY_CHATTING_ACTIVITY = "ChattingUI";
    private static final String ACTION_DISMISS = "dismiss_notification";
    private String currentActivityName = WECHAT_LUCKMONEY_GENERAL_ACTIVITY;

    private AccessibilityNodeInfo rootNodeInfo, mReceiveNode, mUnpackNode;
    private boolean mLuckyMoneyPicked, mLuckyMoneyReceived;
    private int mUnpackCount = 0;
    private boolean mMutex = false, mListMutex = false, mChatMutex = false;
    private LuckyMoneySignature signature = new LuckyMoneySignature();

    private Handler handler =new Handler(Looper.getMainLooper());

    private PowerUtil powerUtil;
    private SharedPreferences sharedPreferences;
    private StateController mStateController;

    private boolean isSuspend = false;

    private int counts = 0;
    private NotificationManager notificationManager;


    @Override
    public void onCreate() {
        super.onCreate();
        mStateController = new StateController(this);
        updateNotification();
        Toast.makeText(this, "已启动抢红包功能", Toast.LENGTH_SHORT).show();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction() != null && intent.getAction().equals(ACTION_DISMISS)){
            if (notificationManager != null){
                notificationManager.cancelAll();
                mStateController.changeState(mStateController.mIdleState);
                mStateController.executeComment(State.COMMAND_EXIT_SERVICE, null);
            }
        }else {
            isSuspend = !isSuspend;
            updateNotification();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void updateNotification(){
        notificationManager = (NotificationManager)this.getSystemService(NOTIFICATION_SERVICE);

        String content = isSuspend ? "已暂停，点击恢复抢红包" : "已抢" + counts + "个红包，点击暂停";
        Intent notificationIntent = new Intent(this, LuckyService.class);
        notificationIntent.setFlags(5);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, notificationIntent, 0);
        notificationIntent.putExtra("key", "key");

        Intent settingsIntent = new Intent(this, LuckyActivity.class);
        PendingIntent  settingPendingIntent =
                PendingIntent.getActivity(this, 0, settingsIntent, 0);
        Notification.Action action = new Notification.Action.Builder(R.mipmap.ic_settings,
                "打开设置", settingPendingIntent).build();

        Intent dismissIntent = new Intent(this, LuckyService.class);
        dismissIntent.setAction(ACTION_DISMISS);
        PendingIntent piDismiss = PendingIntent.getService(
                this, 0, dismissIntent, 0);
        Notification.Action exitAction = new Notification.Action.Builder(R.mipmap.ic_stop,
                "彻底退出", piDismiss).build();


        Notification notification = new Notification.Builder(this)
                .setContentTitle("红包助手")
                .setContentText(content)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.mipmap.ic_settings)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setStyle(new Notification.BigTextStyle()
                        .bigText("利用系统辅助功能自动抢微信红包的开源App\n" + content))
                .addAction(action)
                .addAction(exitAction)
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(false)
                .setVisibility(VISIBILITY_PRIVATE)
                .setDefaults(Notification.DEFAULT_VIBRATE)
                .setOngoing(true)
                .setTicker("红包助手开始运行")
                .build();
        notificationManager.notify(101, notification);
    }

    /**
     * AccessibilityEvent
     *
     * @param event 事件
     */
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        if(isSuspend) {
            return;
        }

        if (sharedPreferences == null) return;

        int eventType = event.getEventType();
        switch (eventType) {
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                /* 检测通知消息 */
                if (sharedPreferences.getBoolean("pref_watch_notification", false)){
                    watchNotifications(event);
                }
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                String className = event.getClassName().toString();
                Utils.printInfo("CurrentActivityName " +  className);
                if (className.equals("com.tencent.mm.ui.LauncherUI")) {
                    mStateController.executeComment(State.COMMAND_FINDING_CHATTING_WINDOW_HONGBAO, event);
                } else if (className.equals("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI")) {
                    mStateController.changeState(mStateController.mOpeningState);
                    mStateController.executeComment(State.COMMAND_FIRE_HONGBAO, event);
                } else if (className.equals("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI")) {
                    mStateController.changeState(mStateController.mOpenedState);
                    mStateController.executeComment(State.COMMAND_BACK_FINISH, event);
                }else if("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyPrepareUI".equals(className)) { //若是点击发红包页面则清空
                    mStateController.changeState(mStateController.mIdleState);
                    mStateController.clear();
                }
                break;
        }
    }

    private boolean watchList(AccessibilityEvent event) {
        if (mListMutex) return false;
        mListMutex = true;
        AccessibilityNodeInfo eventSource = event.getSource();
        // Not a message
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED || eventSource == null)
            return false;

        List<AccessibilityNodeInfo> nodes = eventSource.findAccessibilityNodeInfosByText(WECHAT_NOTIFICATION_TIP);
        //增加条件判断currentActivityName.contains(WECHAT_LUCKMONEY_GENERAL_ACTIVITY)
        //避免当订阅号中出现标题为“[微信红包]拜年红包”（其实并非红包）的信息时误判
        if (!nodes.isEmpty() && currentActivityName.contains(WECHAT_LUCKMONEY_GENERAL_ACTIVITY)) {
            AccessibilityNodeInfo nodeToClick = nodes.get(0);
            if (nodeToClick == null) return false;
            CharSequence contentDescription = nodeToClick.getContentDescription();
            if (contentDescription != null && !signature.getContentDescription().equals(contentDescription)) {
                nodeToClick.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                signature.setContentDescription(contentDescription.toString());
                return true;
            }
        }
        return false;
    }

    private boolean watchNotifications(AccessibilityEvent event) {
        // Not a notification
        if (event.getEventType() != AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED)
            return false;

        // Not a hongbao
        String tip = event.getText().toString();
        if (!tip.contains(WECHAT_NOTIFICATION_TIP)) return true;

        Utils.printInfo(" AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED");
        List<CharSequence> list = event.getText();
        for (CharSequence charSequence : list) { //遍历通知栏并打开通知
            if(charSequence.toString().contains(WECHAT_NOTIFICATION_TIP)){
                mStateController.executeComment(State.COMMAND_NOTIFICATION_EVENT, event);
                break;
            }
        }
        return true;
    }

    @Override
    public void onInterrupt() {
        Toast.makeText(this, "已停止抢红包功能", Toast.LENGTH_SHORT).show();
        isSuspend = true;
    }

    public AccessibilityNodeInfo findOpenButton(AccessibilityNodeInfo node) {
        if (node == null)
            return null;

        //非layout元素
        if (node.getChildCount() == 0) {
            if ("android.widget.Button".equals(node.getClassName()))
                return node;
            else
                return null;
        }

        //layout元素，遍历找button
        AccessibilityNodeInfo button;
        for (int i = 0; i < node.getChildCount(); i++) {
            button = findOpenButton(node.getChild(i));
            if (button != null)
                return button;
        }
        return null;
    }

    private void checkNodeInfo(int eventType) {
        if (this.rootNodeInfo == null) return;

        if (signature.commentString != null) {
            sendComment();
            signature.commentString = null;
        }

        /* 聊天会话窗口，遍历节点匹配“领取红包”和"查看红包" */
        AccessibilityNodeInfo node1 = this.getTheLastNode(WECHAT_VIEW_OTHERS_CH, WECHAT_VIEW_SELF_CH);
       /* AccessibilityNodeInfo node1 = (sharedPreferences.getBoolean("pref_watch_self", false)) ?
                this.getTheLastNode(WECHAT_VIEW_OTHERS_CH, WECHAT_VIEW_SELF_CH) : this.getTheLastNode(WECHAT_VIEW_OTHERS_CH);*/
        if (node1 != null &&
                (currentActivityName.contains(WECHAT_LUCKMONEY_CHATTING_ACTIVITY)
                        || currentActivityName.contains(WECHAT_LUCKMONEY_GENERAL_ACTIVITY))) {
            String excludeWords = sharedPreferences.getString("pref_watch_exclude_words", "");
            if (this.signature.generateSignature(node1, excludeWords)) {
                mLuckyMoneyReceived = true;
                mReceiveNode = node1;
                Log.d("sig", this.signature.toString());
            }
            return;
        }

        /* 戳开红包，红包还没抢完，遍历节点匹配“拆红包” */
        AccessibilityNodeInfo node2 = findOpenButton(this.rootNodeInfo);
        if (node2 != null && "android.widget.Button".equals(node2.getClassName()) && currentActivityName.contains(WECHAT_LUCKMONEY_RECEIVE_ACTIVITY)) {
            mUnpackNode = node2;
            List<AccessibilityNodeInfo> accessibilityNodeInfoList = mUnpackNode.findAccessibilityNodeInfosByViewId(UI.OPEN_LUCKY_MONEY_BUTTON_ID);
            if (!accessibilityNodeInfoList.isEmpty()){
                AccessibilityNodeInfo openNode = accessibilityNodeInfoList.get(accessibilityNodeInfoList.size()-1);
                openNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
            mUnpackCount += 1;
            return;
        }

        /* 戳开红包，红包已被抢完，遍历节点匹配“红包详情”和“手慢了” */
        boolean hasNodes = this.hasOneOfThoseNodes(
                WECHAT_BETTER_LUCK_CH, WECHAT_DETAILS_CH,
                WECHAT_BETTER_LUCK_EN, WECHAT_DETAILS_EN, WECHAT_EXPIRES_CH);
        if (mMutex && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && hasNodes
                && (currentActivityName.contains(WECHAT_LUCKMONEY_DETAIL_ACTIVITY)
                || currentActivityName.contains(WECHAT_LUCKMONEY_RECEIVE_ACTIVITY))) {
            mMutex = false;
            mLuckyMoneyPicked = false;
            mUnpackCount = 0;
            performGlobalAction(GLOBAL_ACTION_BACK);
            signature.commentString = generateCommentString();
        }
    }

    private void sendComment() {
        try {
            AccessibilityNodeInfo outNode =
                    getRootInActiveWindow().getChild(0).getChild(0);
            AccessibilityNodeInfo nodeToInput = outNode.getChild(outNode.getChildCount() - 1).getChild(0).getChild(1);

            if ("android.widget.EditText".equals(nodeToInput.getClassName())) {
                Bundle arguments = new Bundle();
                arguments.putCharSequence(AccessibilityNodeInfo
                        .ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, signature.commentString);
                nodeToInput.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
            }
        } catch (Exception e) {
            // Not supported
        }
    }


    private boolean hasOneOfThoseNodes(String... texts) {
        List<AccessibilityNodeInfo> nodes;
        for (String text : texts) {
            if (text == null) continue;

            nodes = this.rootNodeInfo.findAccessibilityNodeInfosByText(text);

            if (nodes != null && !nodes.isEmpty()) return true;
        }
        return false;
    }

    private AccessibilityNodeInfo getTheLastNode(String... texts) {
        int bottom = 0;
        AccessibilityNodeInfo lastNode = null, tempNode;
        List<AccessibilityNodeInfo> nodes;

        for (String text : texts) {
            if (text == null) continue;

            nodes = this.rootNodeInfo.findAccessibilityNodeInfosByText(text);

            if (nodes != null && !nodes.isEmpty()) {
                tempNode = nodes.get(nodes.size() - 1);
                if (tempNode == null) return null;
                Rect bounds = new Rect();
                tempNode.getBoundsInScreen(bounds);
                if (bounds.bottom > bottom) {
                    bottom = bounds.bottom;
                    lastNode = tempNode;
                    signature.others = text.equals(WECHAT_VIEW_OTHERS_CH);
                }
            }
        }
        return lastNode;
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        this.watchFlagsFromPreference();
    }

    private void watchFlagsFromPreference() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
       // sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        this.powerUtil = new PowerUtil(this);
        Boolean watchOnLockFlag = sharedPreferences.getBoolean("pref_watch_on_lock", false);
        this.powerUtil.handleWakeLock(watchOnLockFlag);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("pref_watch_on_lock")) {
            Boolean changedValue = sharedPreferences.getBoolean(key, false);
            this.powerUtil.handleWakeLock(changedValue);
        }
    }

    @Override
    public void onDestroy() {
        this.powerUtil.handleWakeLock(false);
        super.onDestroy();
        isSuspend = true;
        Toast.makeText(this, "已停止抢红包功能", Toast.LENGTH_SHORT).show();

    }

    private String generateCommentString() {
        if (!signature.others) return null;

        Boolean needComment = sharedPreferences.getBoolean("pref_comment_switch", false);
        if (!needComment) return null;

        String[] wordsArray = sharedPreferences.getString("pref_comment_words", "").split(" +");
        if (wordsArray.length == 0) return null;

        Boolean atSender = sharedPreferences.getBoolean("pref_comment_at", false);
        if (atSender) {
            return "@" + signature.sender + " " + wordsArray[(int) (Math.random() * wordsArray.length)];
        } else {
            return wordsArray[(int) (Math.random() * wordsArray.length)];
        }
    }
}
