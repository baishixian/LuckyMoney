package com.sunteng.wechatluckmoney.core;

import android.accessibilityservice.AccessibilityService;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import com.sunteng.wechatluckmoney.UI;
import com.sunteng.wechatluckmoney.UnlockScreenService;
import com.sunteng.wechatluckmoney.Utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
     * 待抢的红包列表
     */
    private List<AccessibilityNodeInfo> mHongBaoNodes = new ArrayList<>();

    /**
     * 已获取的红包列表
     */
    private List<AccessibilityNodeInfo> mfetchedHongBaoNodes = new ArrayList<>();


    final AccessibilityService mLuckyService;
    private boolean isAutoBackWeechat = false;
    private Handler handler;

    public StateController(AccessibilityService luckyService) {
        mLuckyService = luckyService;
        handler = new Handler(mLuckyService.getMainLooper());
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

    public void findNotificationHongBao(AccessibilityEvent event) {

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
                            pendingIntent.send();
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
        changeState(mFetchingState);
        handler.removeCallbacks(findHongBaoRunnable);
        handler.post(findHongBaoRunnable);
        /*if(isAutoBackWeechat) {
            handler.removeCallbacks(backMain);
            handler.postDelayed(backMain, 3000);
        }*/
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
        Utils.printInfo("mfetchedHongBaoNodes.clear() ");

    }


    Runnable findHongBaoRunnable = new Runnable() {

        @Override
        public void run() {
            Utils.printInfo("findHongBaoRunnable 领取红包");
            findHongbaoNode("领取红包");
            findHongbaoNode("查看红包");
            findHongbaoNode("[微信红包]");
            executeComment(State.COMMAND_FINDED_HONGBAO, null);
        }
    };

    public void findHongbaoNode(String key) {

        if (mLuckyService == null){
            return;
        }

        AccessibilityNodeInfo nodeInfo = mLuckyService.getRootInActiveWindow();
        if (nodeInfo == null) {
            Utils.printInfo("getRootInActiveWindow is null");
            return;
        }

        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText(key);
        for (AccessibilityNodeInfo nodepar : list) {
            if(nodepar != null) {
                nodepar = nodepar.getParent();
                if(nodepar != null) {
                    Utils.printInfo("findHongbaoNode " + key + " " + nodepar.getText());
                    if (!mfetchedHongBaoNodes.contains(nodepar)){
                        mHongBaoNodes.add(nodepar);
                    }
                }
            }
        }
    }

    public void openHongbaoNodes(){
        if (mHongBaoNodes != null && !mHongBaoNodes.isEmpty()){
            Iterator<AccessibilityNodeInfo> infoIterator = mHongBaoNodes.iterator();
            while (infoIterator.hasNext()){
                AccessibilityNodeInfo node = infoIterator.next();
                openHongbao(node);
            }
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
        Utils.printInfo("openHongbao ACTION_CLICK ");
        // 戳开红包
        nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);

        mfetchedHongBaoNodes.add(nodeInfo);
    }

    public void fireHongbao(){
        AccessibilityNodeInfo nodeInfo = mLuckyService.getRootInActiveWindow();

        if (nodeInfo == null){
            return;
        }

        Utils.printInfo("fireHongbao " + nodeInfo.getViewIdResourceName());


        /* 戳开红包，红包还没抢完，遍历节点匹配 */
        List<AccessibilityNodeInfo> successNoticeNodes = nodeInfo.findAccessibilityNodeInfosByViewId(UI.OPEN_LUCKY_MONEY_BUTTON_ID);
        if (!successNoticeNodes.isEmpty()) {
            AccessibilityNodeInfo openNode = successNoticeNodes.get(0);
            Utils.printInfo("fireHongbao " + openNode.getViewIdResourceName());
            openNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }

        /* 戳开红包，红包已被抢完，遍历节点匹配“红包详情”、“手慢了”和“过期” */
        List<AccessibilityNodeInfo> failureNoticeNodes = new ArrayList<>();
        failureNoticeNodes.addAll(nodeInfo.findAccessibilityNodeInfosByText("红包详情"));
        failureNoticeNodes.addAll(nodeInfo.findAccessibilityNodeInfosByText("手慢了"));
        failureNoticeNodes.addAll(nodeInfo.findAccessibilityNodeInfosByText("过期"));
        if (!failureNoticeNodes.isEmpty()) {
            executeComment(State.COMMAND_BACK_FINISH, null);
            return ;
        }
    }


    public void goBackChat() {
        if (mLuckyService != null){
            mLuckyService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
        }
    }

    public void exitService() {
        android.os.Process.killProcess(android.os.Process.myPid());
    }
}
