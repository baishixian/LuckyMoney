package com.sunteng.wechatluckmoney.core;

/**
 * 正在拆红包状态
 * WeChatLuckMoney Created by baishixian on 2016/12/9.
 */

public class OpeningState implements State {

    private StateController mStateController = null;

    public OpeningState(StateController stateController) {
        mStateController = stateController;
    }

    @Override
    public String toString() {
        return "OpeningState";
    }

    @Override
    public void executeCommand(byte comment, Object object) {
        switch (comment){
            case State.COMMAND_FIRE_HONGBAO:
                mStateController.fireHongbao();
                break;
            case State.COMMAND_BACK_FINISH:
                mStateController.goBackChat();
            default:break;
        }
    }
}
