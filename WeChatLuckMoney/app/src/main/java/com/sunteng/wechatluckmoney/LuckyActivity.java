package com.sunteng.wechatluckmoney;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class LuckyActivity extends AppCompatActivity implements AccessibilityManager.AccessibilityStateChangeListener {

    //自动抢红包开关切换按钮
    private TextView luckyStatusText;
    private ImageView luckyStatusIcon;

    RelativeLayout layout;

    //辅助功能服务 AccessibilityService 管理
    private AccessibilityManager accessibilityManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_luckly);

        luckyStatusText = (TextView) findViewById(R.id.layout_control_accessibility_text);
        luckyStatusIcon = (ImageView) findViewById(R.id.layout_control_accessibility_icon);

        findViewById(R.id.layout_github).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openGitHub();
            }
        });

        findViewById(R.id.layout_control_settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openSettings();
            }
        });

        findViewById(R.id.layout_control_accessibility).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openAccessibility();
            }
        });

        layout = (RelativeLayout)findViewById(R.id.activity_luckly);

        handleMaterialStatusBar();

        explicitlyLoadPreferences();

        //监听AccessibilityService 变化
        accessibilityManager = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        accessibilityManager.addAccessibilityStateChangeListener(this);
        updateLuckyServiceStatus();
    }

    private void explicitlyLoadPreferences() {
        PreferenceManager.setDefaultValues(this, R.xml.general_preferences, false);
    }

    public void openGitHub() {
        // Give up on the fucking DownloadManager. The downloaded apk got renamed and unable to install. Fuck.
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/baishixian"));
        browserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(browserIntent);
    }


    public void openSettings() {
        Intent settingsIntent = new Intent(this, SettingsActivity.class);
        settingsIntent.putExtra("title", "偏好设置");
        settingsIntent.putExtra("frag_id", "GeneralSettingsFragment");
        startActivity(settingsIntent);
    }

    public void openAccessibility() {
        try {
            Toast.makeText(this, "微信红包" + luckyStatusText.getText(), Toast.LENGTH_SHORT).show();
            Intent accessibleIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(accessibleIntent);
        } catch (Exception e) {
            Toast.makeText(this, "遇到一些问题,请手动打开系统设置>无障碍服务>微信红包(ฅ´ω`ฅ)", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }

    }

    /**
     * 适配沉浸状态栏
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void handleMaterialStatusBar() {
        // Not supported in APK level lower than 21
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            return;
        }

        Window window = this.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(0xffE46C62);

    }

    /**
     * 更新当前 LuckyHelperService 显示状态
     */
    private void updateLuckyServiceStatus() {
        //isLuckyHelperServiceEnabled()
        if (isAccessibilitySettingsOn(LuckyActivity.this)) {
            luckyStatusText.setText(R.string.service_off);
            luckyStatusIcon.setBackgroundResource(R.mipmap.ic_stop);
        } else {
            luckyStatusText.setText(R.string.service_on);
            luckyStatusIcon.setBackgroundResource(R.mipmap.ic_start);
        }
    }

    /**
     * 获取 LuckyHelperService 是否启用状态
     *
     * @return
     */
    private boolean isLuckyHelperServiceEnabled() {
        List<AccessibilityServiceInfo> accessibilityServices =
                accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC);
        for (AccessibilityServiceInfo info : accessibilityServices) {
            if (info.getId().equals(getPackageName() + "/.LuckyService")) {
                return true;
            }
        }
        return false;
    }

    // To check if service is enabled
    private boolean isAccessibilitySettingsOn(Context mContext) {
        int accessibilityEnabled = 0;
        final String service = getPackageName() + "/" + LuckyService.class.getCanonicalName();
       // final String service = getPackageName() + "/" + LuckyService.class.getCanonicalName();
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    mContext.getApplicationContext().getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
            Utils.printInfo("accessibilityEnabled = " + accessibilityEnabled);
        } catch (Settings.SettingNotFoundException e) {
            Utils.printInfo("Error finding setting, default accessibility to not found: "
                    + e.getMessage());
        }
        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');

        if (accessibilityEnabled == 1) {
            Utils.printInfo("***ACCESSIBILITY IS ENABLED*** -----------------");
            String settingValue = Settings.Secure.getString(
                    mContext.getApplicationContext().getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue);
                while (mStringColonSplitter.hasNext()) {
                    String accessibilityService = mStringColonSplitter.next();

                    Utils.printInfo( "-------------- > accessibilityService :: " + accessibilityService + " " + service);
                    if (accessibilityService.equalsIgnoreCase(service)) {
                        Utils.printInfo("We've found the correct setting - accessibility is switched on!");
                        return true;
                    }
                }
            }
        } else {
            Utils.printInfo("***ACCESSIBILITY IS DISABLED***");
        }

        return false;
    }


    @Override
    public void onAccessibilityStateChanged(boolean enabled) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateLuckyServiceStatus();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
       // Check for update when WIFI is connected or on first time.
        if (Utils.isFirstStartUp && Utils.isWifi(this) && UpdateAppTask.taskCount == 0){
            Utils.isFirstStartUp = false;
            UpdateAppTask updateAppTask = new UpdateAppTask(this);
            updateAppTask.setUpdateListener(new UpdateAppTask.UpdateListener() {
                @Override
                public void onUpdate(final String updateUrl, final String fileName) {
                    LuckyActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showUpdateInfo(updateUrl,fileName);
                        }
                    });
                }
            });
            updateAppTask.update();
        }
    }

    @Override
    protected void onDestroy() {
        //移除监听服务
        Utils.printInfo("onDestroy 移除监听服务");
       // accessibilityManager.removeAccessibilityStateChangeListener(this);
        super.onDestroy();
    }

    private void showUpdateInfo(final String updateUrl, final String fileName){
        Snackbar.make(layout , "检测到有新版本程序，是否下载更新？", Snackbar.LENGTH_LONG).setAction("确定", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String updateDirPath = Utils.getUpdateDirPath(LuckyActivity.this);
                String filePath = updateDirPath + fileName;
                if (Utils.fileExist(filePath)){
                    Utils.installApk(LuckyActivity.this,filePath);
                    return;
                }

                Utils.printInfo("showUpdateInfo updateUrl: " + updateUrl);

                // Give up on the fucking DownloadManager. The downloaded apk got renamed and unable to install. Fuck.
                SystemDownloadTask systemDownloadTask = new SystemDownloadTask(LuckyActivity.this, updateUrl, fileName);
                systemDownloadTask.setDownloadListener(new SystemDownloadTask.DownloadListener() {
                    @Override
                    public void onSuccess(String fileName) {
                        Utils.installApk(LuckyActivity.this, Utils.getUpdateDirPath(LuckyActivity.this) +fileName);
                    }

                    @Override
                    public void onFail() {
                        Toast.makeText(LuckyActivity.this, "下载新版本WeChatLuckMoney失败", Toast.LENGTH_SHORT).show();
                    }
                });
                systemDownloadTask.doExecute();
            }
        }).show();
    }
}
