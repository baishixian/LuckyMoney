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
            case State.COMMAND_NOTIFICATION_EVENT:
                mStateController.changeState(mStateController.mFetchingState);
                if (object != null && object instanceof AccessibilityEvent) {
                    mStateController.findNotificationHongBao((AccessibilityEvent)object);
                }
                break;
            case State.COMMAND_FINDING_CHAT_LIST_HONGBAO:
                mStateController.changeState(mStateController.mFetchingState);
                mStateController.findChatListHongBao();
                break;
            case State.COMMAND_FINDING_CHATTING_WINDOW_HONGBAO:
                mStateController.changeState(mStateController.mFetchingState);
                if (object != null && object instanceof AccessibilityEvent) {
                    mStateController.findChattingWindowHongBao();
                }
                break;
            case State.COMMAND_EXIT_SERVICE:
                mStateController.exitService();
                break;
            default:break;
        }
    }
}
