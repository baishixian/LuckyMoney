package com.sunteng.wechatluckmoney.core;

/**
 * 抢红包服务暂停状态
 * WeChatLuckMoney Created by baishixian on 2016/12/9.
 */

public class SuspendState implements State {

    private StateController mStateController = null;

    public SuspendState(StateController stateController) {
        mStateController = stateController;
    }

    @Override
    public String toString() {
        return "SuspendState";
    }

    @Override
    public void executeCommand(byte comment, Object object) {

    }
}
