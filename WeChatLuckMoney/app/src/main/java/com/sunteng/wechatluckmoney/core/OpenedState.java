package com.sunteng.wechatluckmoney.core;

/**
 * 红包已经拆开状态 （红包详情页面）
 * WeChatLuckMoney Created by baishixian on 2016/12/9.
 */

public class OpenedState implements State {

    private StateController mStateController = null;

    public OpenedState(StateController stateController) {
        mStateController = stateController;
    }

    @Override
    public String toString() {
        return "OpenedState";
    }


    @Override
    public void executeCommand(byte comment, Object object) {
        switch (comment){
            case State.COMMAND_FIRE_HONGBAO_DONE:

                if (mStateController.isHongbaoNodesAvailable()){
                    mStateController.changeState(mStateController.mFetchedState);
                    mStateController.executeComment(State.COMMAND_OPEN_HONGBAO, null);
                }else {
                    mStateController.changeState(mStateController.mIdleState);
                }
                mStateController.goBackChat();
                break;
            case COMMAND_BACK_FINISH:
                mStateController.changeState(mStateController.mIdleState);
                mStateController.goBackChat();
                break;
            default:break;
        }
    }
}
