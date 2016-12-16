package com.sunteng.wechatluckmoney.core;

import android.view.accessibility.AccessibilityEvent;

/**
 * 初始化状态，等待被激活抢红包任务
 * WeChatLuckMoney Created by baishixian on 2016/12/9.
 */

public class IdleState implements State {

    private StateController mStateController = null;

    public IdleState(StateController stateController) {
        mStateController = stateController;
    }

    @Override
    public String toString() {
        return "IdleState";
    }

    @Override
    public void executeCommand(byte comment, Object object) {
        switch (comment){
            case State.COMMAND_FINDING_NOTIFICATION_HONGBAO: // 查找通知栏红包消息
                mStateController.changeState(mStateController.mFetchingState);
                if (object != null && object instanceof AccessibilityEvent) {
                    mStateController.handleNotificationHongBao((AccessibilityEvent)object); // 从消息栏跳转到微信聊天界面
                }
                break;
            case State.COMMAND_FINDING_CHAT_LIST_HONGBAO: // 查找消息列表界面红包消息（微信不支持了）
              /*  mStateController.changeState(mStateController.mFetchingState);
                mStateController.findChatListHongBao();*/
                break;
            case State.COMMAND_FINDING_CHATTING_WINDOW_HONGBAO: // 查找聊天界面红包
                mStateController.changeState(mStateController.mFetchingState); // 切换到查找红包状态
                mStateController.findChattingWindowHongBao();
                break;
            case State.COMMAND_EXIT_SERVICE:
                mStateController.exitService();
                break;
            default:break;
        }
    }
}
