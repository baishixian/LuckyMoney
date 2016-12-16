package com.sunteng.wechatluckmoney.core;

/**
 * WeChatLuckMoney Created by baishixian on 2016/12/9.
 */

public interface State {

    // 通知栏
    String NOTIFICATION = "notification";

    // 聊天列表
    String CHAT_LIST = "chaList";

    // 正在聊天的窗口
    String CHATTING_WINDOW = "chattingWindow";

    // 查找屏幕上通知栏的红包（查找）
    byte COMMAND_FINDING_NOTIFICATION_HONGBAO = 1;

    // 查找屏幕上聊天列表的红包（查找） 微信不支持
    byte COMMAND_FINDING_CHAT_LIST_HONGBAO = 2;

    // 查找屏幕上聊天界面的红包（查找）
    byte COMMAND_FINDING_CHATTING_WINDOW_HONGBAO = 3;

    // 查找到红包（匹配成功）
    byte COMMAND_FINDED_HONGBAO = 4;

    // 没查找到红包（匹配失败）
    byte COMMAND_NOT_FINDED_HONGBAO = 5;

    // 打开红包（打开红包后有三种情况：1.等待拆 2.已经拆过的 3.来晚了）
    byte COMMAND_OPEN_HONGBAO = 6;

    // 拆红包事件
    byte COMMAND_FIRE_HONGBAO = 7;

    // 结束本次抢红包流程，返回退出红包界面事件
    byte COMMAND_BACK_FINISH = 8;

    // 暂停抢红包服务事件
    byte COMMAND_STOP_SERVICE = 9;

    // 退出抢红包服务事件
    byte COMMAND_EXIT_SERVICE = 10;

    // 开启抢红包服务事件
    byte COMMAND_START_SERVICE = 11;

    byte COMMAND_FIRE_HONGBAO_DONE = 12;


    void executeCommand(byte comment, Object object);
}
