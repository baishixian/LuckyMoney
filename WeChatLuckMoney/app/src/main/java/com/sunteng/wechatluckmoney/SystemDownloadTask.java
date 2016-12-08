package com.sunteng.wechatluckmoney;

import android.annotation.TargetApi;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;

/**
 * 使用system下载器执行下载任务，调用
 * Created by baishixian on 2016/1/21.
 */
public class SystemDownloadTask {

    String uri;

    String fileName;

    boolean isSuccess = false;

    Context context;

    long downloadId = -1;

    public SystemDownloadTask(Context context, String uri, String fileName) {
        this.uri = uri;
        this.fileName = fileName;
        this.context = context;
    }

    protected void onPostExecute(Boolean isSuccess) {
        this.isSuccess = isSuccess;
        if (mDownloadListener != null){
            if (isSuccess){
                mDownloadListener.onSuccess(fileName);
            }else{
                mDownloadListener.onFail();
            }
        }
    }

    protected void doBackground() {
        if (context != null) {
            Uri downloadUri = Uri.parse(uri);
            DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            Utils.printInfo("doBackground downloadUri: " + downloadUri);
            DownloadManager.Request request = new DownloadManager.Request(downloadUri);
            request.setDestinationInExternalFilesDir(context, "Downloads", fileName); // 用于设置下载文件的存放路径和文件名称
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI);//设置下载时的网络类型，默认任何网络都可以下载，提供的网络常量有：NETWORK_BLUETOOTH、NETWORK_MOBILE、NETWORK_WIFI
//            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);     //用于设置下载时时候在状态栏显示通知信息
            request.setTitle("新版本WeChatLuckyMoney"); //设置Notification的title信息
            request.setDescription(fileName + " 下载中"); //设置Notification的message信息
            request.setMimeType("application/vnd.android.package-archive");
            DownloadCompleteReceiver completeReceiver = new DownloadCompleteReceiver();
            DownloadClickReceiver clickReceiver = new DownloadClickReceiver();
            context.registerReceiver(completeReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
            context.registerReceiver(clickReceiver, new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED));
            Utils.printInfo("Download new version apk file " + uri);
            downloadId = manager.enqueue(request);
        }
    }

    public long getDownloadId() {
        return downloadId;
    }


    class DownloadCompleteReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            Utils.printInfo("DownloadCompleteReceiver " + id);
            if (id == downloadId){
                queryDownloadStatus(downloadId);
            }
        }
    }

    class DownloadClickReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Utils.printInfo("DownloadClickReceiver");
            if (intent.getAction() == DownloadManager.ACTION_NOTIFICATION_CLICKED) {
                if(context != null){
                    context.startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public boolean queryDownloadStatus(long downloadId) {
        DownloadManager.Query myDownloadQuery = new DownloadManager.Query();
        myDownloadQuery.setFilterById(downloadId);
        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        Cursor cursor = manager.query(myDownloadQuery);
        if (cursor != null && cursor.moveToFirst()) {
            int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
            switch (status) {
                case DownloadManager.STATUS_SUCCESSFUL:
                    Utils.printInfo("Download file STATUS_SUCCESSFUL");
                    if (cursor.moveToFirst()) {
                        cursor.close();
                        onPostExecute(true);
                    }
                    break;
                case DownloadManager.STATUS_FAILED:
                    //清除已下载的内容
                    Utils.printInfo("Download file STATUS_FAILED");
                    onPostExecute(false);
                    manager.remove(downloadId);
                    break;
                case DownloadManager.STATUS_RUNNING:
                    Utils.printInfo("Download file STATUS_RUNNING");
                    return true;
                case DownloadManager.STATUS_PAUSED:
                    Utils.printInfo("Download file STATUS_PAUSED");
                    break;
                case DownloadManager.STATUS_PENDING:
                    Utils.printInfo("Download file STATUS_PENDING");
                    break;
                default:
                    break;
            }
        }else{
            Utils.printInfo("Download task cancel");
            onPostExecute(false);
        }
        return false;
    }

    public String getUrl() {
        return this.uri;
    }

    public Boolean isSuccess() {
        return this.isSuccess;
    }

    public void doExecute() {
        doBackground();
    }

    public String getFileName() {
        return fileName;
    }

    DownloadListener mDownloadListener;

    public void setDownloadListener(DownloadListener downloadListener){
        this.mDownloadListener = downloadListener;
    }

    interface DownloadListener{
        void onSuccess(String fileName);
        void onFail();
    }
}
