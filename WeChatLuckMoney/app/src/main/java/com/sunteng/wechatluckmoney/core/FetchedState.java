package com.sunteng.wechatluckmoney.core;

import com.sunteng.wechatluckmoney.Utils;

/**
 * 红包读取完成状态（屏幕上的红包都已加入待抢队列）
 * WeChatLuckMoney Created by baishixian on 2016/12/9.
 */

public class FetchedState implements State {

    private StateController mStateController = null;

    public FetchedState(StateController stateController) {
        mStateController = stateController;
    }

    @Override
    public String toString() {
        return "FetchedState";
    }

    @Override
    public void executeCommand(byte comment, Object object) {
        switch (comment){
            case State.COMMAND_OPEN_HONGBAO:
                Utils.printInfo("FetchedState COMMAND_OPEN_HONGBAO");
                mStateController.changeState(mStateController.mOpeningState);
                mStateController.openHongbaoNodes();
                break;
            default:break;
        }
    }
}
