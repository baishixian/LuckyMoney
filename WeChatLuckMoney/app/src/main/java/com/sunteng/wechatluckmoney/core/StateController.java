package com.sunteng.wechatluckmoney.core;

import android.accessibilityservice.AccessibilityService;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import com.sunteng.wechatluckmoney.FloadingWindow.FloatWindowManager;
import com.sunteng.wechatluckmoney.LuckyActivity;
import com.sunteng.wechatluckmoney.TransparentActivity;
import com.sunteng.wechatluckmoney.UI;
import com.sunteng.wechatluckmoney.UnlockScreenService;
import com.sunteng.wechatluckmoney.Utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * WeChatLuckMoney Created by baishixian on 2016/12/9.
 */

public class StateController {

    State mIdleState = new IdleState(this);
    State mFetchingState = new FetchingState(this);
    State mFetchedState= new FetchedState(this);
    State mOpeningState = new OpeningState(this);
    State mOpenedState = new OpenedState(this);
    State mSuspendState = new SuspendState(this);

    State mCurState = mIdleState;

    /**
     * 待抢的红包队列
     */
    private LinkedList<AccessibilityNodeInfo> mHongBaoNodes = new LinkedList<>();

    /**
     * 已获取的红包列表
     */
    private List<String> mfetchedHongBaoNodes = new ArrayList<>();


    AccessibilityService mLuckyService;
    private boolean isAutoBackWeechat = false;
    private Handler mHandler;

    private static StateController mStateController = new StateController();

    private Context mContext;

    public static StateController getInstance(){
        if (mStateController == null){
            mStateController = new StateController();
        }
        return mStateController;
    }

    public void setHandler(Handler handler){
        this.mHandler = handler;
    }

    public void setAccessibilityService(AccessibilityService accessibilityService){
        this.mLuckyService = accessibilityService;
    }

    private StateController() {
    }

    public void changeState(State state) {
        mCurState = state;
    }

    public void executeComment(byte command, Object object) {
        if(mCurState != null) {
            Utils.printInfo("executeCommand " +"state - " + mCurState + " command " + command);
            mCurState.executeCommand(command, object);
        }
    }

    public void handleNotificationHongBao(AccessibilityEvent event) {

        Utils.printInfo("openNotification");

        if(event== null) {
            return;
        }

        if(event.getParcelableData() == null) {
            return;
        }

        try {
            if(Utils.isScreenLock(mLuckyService.getApplicationContext()) || !Utils.isScreenOn(mLuckyService.getApplicationContext())) {
                Intent intent = new Intent(mLuckyService, UnlockScreenService.class);
                Utils.printInfo("will be unlock screen");
                mLuckyService.startService(intent);
            }
            handleNotification(event);
        }catch (Exception e){
            Utils.printErrorInfo(e.getMessage());
        }
    }


    /**
     * 处理通知栏信息
     * 如果是微信红包的提示信息,则模拟点击
     * @param event
     */
    public void handleNotification(AccessibilityEvent event) {
        List<CharSequence> texts = event.getText();
        if (!texts.isEmpty()) {
            for (CharSequence text : texts) {
                String content = text.toString();
                //如果微信红包的提示信息,则模拟点击进入相应的聊天窗口
                if (content.contains("[微信红包]")) {
                    if (event.getParcelableData() != null && event.getParcelableData() instanceof Notification) {
                        Notification notification = (Notification) event.getParcelableData();
                        PendingIntent pendingIntent = notification.contentIntent;
                        try {
                            pendingIntent.send(); //打开消息意图，跳转到聊天界面
                            return;
                        } catch (PendingIntent.CanceledException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    public void findChatListHongBao() {
        findChattingWindowHongBao();
    }

    public void findChattingWindowHongBao() {
        if (mHandler == null) {
            Utils.printErrorInfo("findChattingWindowHongBao mHandler is null");
            return;
        }

        mHandler.removeCallbacks(findChattingWindowsHongBaoRunnable);
        mHandler.post(findChattingWindowsHongBaoRunnable);

        if(isAutoBackWeechat) { // 抢完自动回到主界面
            mHandler.removeCallbacks(backMain);
            mHandler.postDelayed(backMain, 3000);
        }
    }

    private Runnable backMain = new Runnable() {
        @Override
        public void run() {
            backHome();
            Toast.makeText(mLuckyService,"将微信退到后台，可以在红包助手设置", Toast.LENGTH_SHORT).show();
        }
    };

    /**
     * 返回
     */
    private void backCurrentWindow(){
        mLuckyService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
        Utils.printInfo("backCurrentWindow GLOBAL_ACTION_BACK ");
    }


    private void backHome(){
        mLuckyService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
        Utils.printInfo("backCurrentWindow GLOBAL_ACTION_HOME ");

    }

    public void clear(){
        mfetchedHongBaoNodes.clear();
        mHongBaoNodes.clear();
        Utils.printInfo("mfetchedHongBaoNodes.clear() ");

    }


    Runnable findChattingWindowsHongBaoRunnable = new Runnable() {
        @Override
        public void run() {
            Utils.printInfo("findChattingWindowsHongBaoRunnable 领取红包");
            findHongbaoNodes("领取红包");
            findHongbaoNodes("查看红包");

            if (!mHongBaoNodes.isEmpty()){
                Utils.printInfo("findChattingWindowsHongBaoRunnable mHongBaoNodes not Empty");
                executeComment(State.COMMAND_FINDED_HONGBAO, null);
            }else {
                executeComment(State.COMMAND_NOT_FINDED_HONGBAO, null);
            }
        }
    };

    /**
     * 根据红包关键字内容查找红包节点
     * @param key
     */
    public void findHongbaoNodes(String key) {

        if (mLuckyService == null){
            return;
        }

        AccessibilityNodeInfo nodeInfo = mLuckyService.getRootInActiveWindow();
        if (nodeInfo == null) {
            Utils.printInfo("findHongbaoNode getRootInActiveWindow is null");
            return;
        }

        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText(key);
        /*for (AccessibilityNodeInfo nodepar : list) {
            if(nodepar != null) {
                Utils.printInfo("findHongbaoNode " + key + " " + nodepar.getText());
                nodepar = nodepar.getParent(); // 红包节点
                if(nodepar != null) {
                    Utils.printInfo("找到红包加入HongBaoNodes " + nodepar.getText());
                    if (!mfetchedHongBaoNodes.contains(getHongbaoHash(nodepar))){
                        Utils.printInfo("add HongBaoNodes " + nodepar.getText());
                        mHongBaoNodes.offer(nodepar); // 插入队尾
                    }else {
                        Utils.printInfo("已经存在开启过的红包 " + nodepar.getText());
                    }
                }
            }
        }*/
        if (list.isEmpty()){
            return;
        }
        AccessibilityNodeInfo nodepar =  list.get(list.size() - 1);
        if(nodepar != null) {
            Utils.printInfo("找到红包 " + key + " " + nodepar.getText());
            nodepar = nodepar.getParent(); // 红包节点
            if(nodepar != null) {
                if (!mfetchedHongBaoNodes.contains(getHongbaoHash(nodepar))){
                    Utils.printInfo("找到红包加入HongBaoNodes " + nodepar.getText());
                    mHongBaoNodes.offer(nodepar); // 插入队尾
                }else {
                    Utils.printInfo("已经存在开启过的红包 " + nodepar.getText());
                }
            }
        }
    }

    public boolean isHongbaoNodesAvailable(){
        return !mHongBaoNodes.isEmpty();
    }

    Runnable openHongbaoNodesRunnable = new Runnable() {
        @Override
        public void run() {
            if (mHongBaoNodes != null && !mHongBaoNodes.isEmpty()){

                AccessibilityNodeInfo nodeInfo;
                while ((nodeInfo = mHongBaoNodes.poll()) != null) {
                    openHongbao(nodeInfo);
                    break;
                }
            }
        }
    };

    public void openHongbaoNodes(){
        if (mHandler != null) {
            mHandler.post(openHongbaoNodesRunnable);
        }else{
            Utils.printErrorInfo("openHongbaoNodes mHandler is null");
        }

    }


    /**
     * 如果戳开红包但还未领取
     * <p/>
     * 第一种情况，当界面上出现“过期”(红包超过有效时间)、
     * “手慢了”(红包发完但没抢到)或“红包详情”(已经抢到)时，
     * 直接返回聊天界面
     * <p/>
     * 第二种情况，界面上出现“拆红包”时
     * 点击该节点，并将阶段标记为OPENED_STAGE
     * <p/>
     * 第三种情况，以上节点没有出现，
     * 说明窗体可能还在加载中，维持当前状态，TTL增加，返回重试
     *
     * @param nodeInfo 当前窗体的节点信息
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void openHongbao(AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo == null) {
            return;
        }
        Utils.printInfo("戳开红包 openHongbao ACTION_CLICK");
        // 戳开红包
        mfetchedHongBaoNodes.add(getHongbaoHash(nodeInfo));
        nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK); // 触发新的窗体事件
    }

    public void fireHongbao(AccessibilityNodeInfo source){
        AccessibilityNodeInfo nodeInfo;
        nodeInfo = mLuckyService.getRootInActiveWindow();

        if (nodeInfo == null){
            Utils.printInfo("fireHongbao nodeInfo is null");
            return;
        }

        /* 戳开红包，红包还没抢完，遍历节点匹配id（没有拆红包文字可以匹配) */
        List<AccessibilityNodeInfo> successNoticeNodes = nodeInfo.findAccessibilityNodeInfosByViewId(UI.OPEN_LUCKY_MONEY_BUTTON_ID);
        if (!successNoticeNodes.isEmpty()) {
            AccessibilityNodeInfo openNode = successNoticeNodes.get(0);
            Utils.printInfo("fireHongbao success 抢到红包 " + openNode.getViewIdResourceName());
            openNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }else{
            /* 戳开红包，红包已被抢完，遍历节点匹配“红包详情”、“手慢了”和“过期” */
            Utils.printInfo("fireHongbao 戳开红包，红包已被抢完");
           /* List<AccessibilityNodeInfo> failureNoticeNodes = new ArrayList<>();
            failureNoticeNodes.addAll(nodeInfo.findAccessibilityNodeInfosByText("红包详情"));
            failureNoticeNodes.addAll(nodeInfo.findAccessibilityNodeInfosByText("手慢了，红包派完了"));
            failureNoticeNodes.addAll(nodeInfo.findAccessibilityNodeInfosByText("该红包已超过24小时。如已领取，可在“我的红包”中查看"));
            if (!failureNoticeNodes.isEmpty()) {
                Utils.printInfo("fireHongbao fail");
            }*/
        }
        executeComment(State.COMMAND_FIRE_HONGBAO_DONE, null);
    }


    public void goBackChat() {
        if (mLuckyService != null){
            changeState(mIdleState);
            mLuckyService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
        }
    }

    public void exitService() {
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    public void handlerRealyLuckyMoneyReceiveUI(AccessibilityEvent event) {
        if (mHandler == null) {
            Utils.printErrorInfo("handlerRealyLuckyMoneyReceiveUI mHandler is null");
            return;
        }
        handlerRealyLuckyMoneyReceiveUI();
       /* mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Utils.printInfo("checkRealyLuckyMoneyReceiveTask find");
                AccessibilityNodeInfo rootInActiveWindow  = mLuckyService.getRootInActiveWindow();
                if (rootInActiveWindow != null) {
                    List<AccessibilityNodeInfo> failureNoticeNodes = new ArrayList<>();
                    failureNoticeNodes.addAll(rootInActiveWindow.findAccessibilityNodeInfosByViewId(UI.OPEN_LUCKY_MONEY_BUTTON_ID));
                    failureNoticeNodes.addAll(rootInActiveWindow.findAccessibilityNodeInfosByText("红包"));
                    failureNoticeNodes.addAll(rootInActiveWindow.findAccessibilityNodeInfosByText("红包详情"));
                    failureNoticeNodes.addAll(rootInActiveWindow.findAccessibilityNodeInfosByText("手慢了，红包派完了"));
                    failureNoticeNodes.addAll(rootInActiveWindow.findAccessibilityNodeInfosByText("看看大家手气"));
                    failureNoticeNodes.addAll(rootInActiveWindow.findAccessibilityNodeInfosByText("该红包已超过24小时。如已领取，可在“我的红包”中查看"));
                    if (!failureNoticeNodes.isEmpty()) {
                        Utils.printInfo("find is realyLuckyMoneyReceiveUI");
                        mStateController.changeState(mStateController.mOpeningState);
                        mStateController.executeComment(State.COMMAND_FIRE_HONGBAO, rootInActiveWindow);
                    }else{
                        goBackChat();
                        Utils.printInfo("not find is realyLuckyMoneyReceiveUI");
                    }
                }else{
                    mLuckyService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS);
                    mLuckyService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                    Utils.printInfo("checkRealyLuckyMoneyReceiveTask getRootInActiveWindow is null");
                }
            }
        },3000);*/
    }


    Timer timer;
    TimerTask checkRealyLuckyMoneyReceiveTask;


    public static final int MAX_TTL = 10;

    private void handlerRealyLuckyMoneyReceiveUI() {
        Utils.printInfo("handlerRealyLuckyMoneyReceiveUI");
        timer = new Timer();
        final int[] tryCount = {0};
        checkRealyLuckyMoneyReceiveTask = new TimerTask(){

            public void run(){
              //  FloatWindowManager.setFloatWindowViewEnable(false);
                Utils.printInfo("checkRealyLuckyMoneyReceiveTask find");
                AccessibilityNodeInfo rootInActiveWindow  = mLuckyService.getRootInActiveWindow();
                if (rootInActiveWindow != null) {
                    List<AccessibilityNodeInfo> failureNoticeNodes = new ArrayList<>();
                    failureNoticeNodes.addAll(rootInActiveWindow.findAccessibilityNodeInfosByViewId(UI.OPEN_LUCKY_MONEY_BUTTON_ID));
                    failureNoticeNodes.addAll(rootInActiveWindow.findAccessibilityNodeInfosByText("红包"));
                    failureNoticeNodes.addAll(rootInActiveWindow.findAccessibilityNodeInfosByText("红包详情"));
                    failureNoticeNodes.addAll(rootInActiveWindow.findAccessibilityNodeInfosByText("手慢了，红包派完了"));
                    failureNoticeNodes.addAll(rootInActiveWindow.findAccessibilityNodeInfosByText("看看大家手气"));
                    failureNoticeNodes.addAll(rootInActiveWindow.findAccessibilityNodeInfosByText("该红包已超过24小时。如已领取，可在“我的红包”中查看"));
                    if (!failureNoticeNodes.isEmpty()) {
                        timer.cancel();
                        Utils.printInfo("find is realyLuckyMoneyReceiveUI");
                        mStateController.changeState(mStateController.mOpeningState);
                        mStateController.executeComment(State.COMMAND_FIRE_HONGBAO, rootInActiveWindow);
                    }else{
                        Utils.printInfo("not find is realyLuckyMoneyReceiveUI");
                    }
                }else{
                    //FloatWindowManager.setFloatWindowViewEnable(true);
                    mLuckyService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS);
                  //  mLuckyService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS);
                    Utils.printInfo("checkRealyLuckyMoneyReceiveTask getRootInActiveWindow is null");
                    /*mLuckyService.startActivity(new Intent(mLuckyService, TransparentActivity.class));*/
                }
                if (tryCount[0]++ >= MAX_TTL){
                   // FloatWindowManager.setFloatWindowViewEnable(false);
                    timer.cancel();
                    goBackChat();
                }
            }

        };
        timer.schedule(checkRealyLuckyMoneyReceiveTask, 500, 500);// 延时减少过多的重复判断
    }


    /**
     * 获取节点对象唯一的id，通过正则表达式匹配
     * AccessibilityNodeInfo@后的十六进制数字
     *
     * @param node AccessibilityNodeInfo对象
     * @return id字符串
     */
    private String getNodeId(AccessibilityNodeInfo node) {
        /* 用正则表达式匹配节点Object */
        Pattern objHashPattern = Pattern.compile("(?<=@)[0-9|a-z]+(?=;)");
        Matcher objHashMatcher = objHashPattern.matcher(node.toString());

        // AccessibilityNodeInfo必然有且只有一次匹配，因此不再作判断
        objHashMatcher.find();

        return objHashMatcher.group(0);
    }

    /**
     * 将节点对象的id和红包上的内容合并
     * 用于表示一个唯一的红包
     *
     * @param node 任意对象
     * @return 红包标识字符串
     */
    private String getHongbaoHash(AccessibilityNodeInfo node) {
        /* 获取红包上的文本 */
        String content;
        try {
            AccessibilityNodeInfo i = node.getParent().getChild(0);
            content = i.getText().toString();
        } catch (NullPointerException npr) {
            return null;
        }

        return content + "@" + getNodeId(node);
    }

}
