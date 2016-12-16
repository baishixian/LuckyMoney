package com.sunteng.wechatluckmoney.core;


import com.sunteng.wechatluckmoney.Utils;

/**
 * 正在读取屏幕上的红包状态
 * WeChatLuckMoney Created by baishixian on 2016/12/9.
 */

public class FetchingState implements State {

    private StateController mStateController = null;

    public FetchingState(StateController stateController) {
        mStateController = stateController;
    }

    @Override
    public String toString() {
        return "FetchingState";
    }

    @Override
    public void executeCommand(byte comment, Object object) {
        switch (comment) {
            case State.COMMAND_FINDED_HONGBAO:
                Utils.printInfo("FetchingState 查找到红包");
                mStateController.changeState(mStateController.mFetchedState);
                mStateController.executeComment(COMMAND_OPEN_HONGBAO, null);
                break;
            case State.COMMAND_NOT_FINDED_HONGBAO:
                Utils.printInfo("FetchingState 没查找到红包");
                mStateController.changeState(mStateController.mIdleState);
                break;
            default:
                break;
        }
    }
}
