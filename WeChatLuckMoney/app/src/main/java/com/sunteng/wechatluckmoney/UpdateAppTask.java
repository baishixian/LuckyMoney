package com.sunteng.wechatluckmoney;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * WeChatLuckMoney Created by baishixian on 2016/12/7.
 */
public class UpdateAppTask extends AsyncTask<String, String, String> {

    private final Context context;

    public static int taskCount = 0;
    public static final String updateUrl = "https://api.github.com/repos/baishixian/Android-Training/releases/latest";
    private String latestVersion;
    private UpdateListener mUpdateListener;

    public UpdateAppTask(Context context) {
        super();
        this.context = context;
    }

    @Override
    protected String doInBackground(String... uris) {
        URL url;
        HttpURLConnection connection;
        taskCount++;
        try {
            url = new URL(uris[0]);
            connection = (HttpURLConnection)url.openConnection();
            connection.setInstanceFollowRedirects(true);
            connection.setConnectTimeout(30 * 1000);
            connection.setReadTimeout(30 * 1000);
            connection.setDoOutput(false);
            connection.setDoInput(true);
            connection.setRequestMethod("GET");
            connection.connect();

            Utils.printInfo( "check app update info！");
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK){
                Utils.printErrorInfo("errorResponseCode = " + connection.getResponseCode());
            }

            //------------字符流读取服务端返回的数据------------
            InputStream in = connection.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String str;
            StringBuffer buffer = new StringBuffer();
            while((str = br.readLine())!=null){//BufferedReader特有功能，一次读取一行数据
                buffer.append(str);
            }
            in.close();
            br.close();
            String response = buffer.toString();
            return parseJson(response);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String parseJson(String response) throws JSONException, PackageManager.NameNotFoundException, IOException {
        JSONObject release = new JSONObject(response);

        // Get app current version
        PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        String version = pInfo.versionName;

        latestVersion = release.getString("tag_name");
        boolean isPreRelease = release.getBoolean("prerelease");

        if (!isPreRelease && version.compareToIgnoreCase(latestVersion) >= 0) {
            // Your version is latest.
            Utils.printInfo("app don't need update.");
            return null;
        }else{
            // Need update.
            String downloadUrl = release.getJSONArray("assets").getJSONObject(0).getString("browser_download_url");
            return downloadUrl;
        }
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        if (!TextUtils.isEmpty(result)){
            if (mUpdateListener != null){
                mUpdateListener.onUpdate(result, "WeChatLuckyMoney-" + latestVersion + ".apk");
            }
            taskCount = 0;
        }
         //   Utils.installApk(context, context.getExternalCacheDir().getAbsolutePath() + File.separator + "LuckyMoneyHelper-" + latestVersion + ".apk");
    }

    public void update() {
        execute(updateUrl);
    }

    interface UpdateListener{
        void onUpdate(String updateUrl, String fileName);
    }

    public void setUpdateListener(UpdateListener updateListener){
        this.mUpdateListener = updateListener;
    }
}
