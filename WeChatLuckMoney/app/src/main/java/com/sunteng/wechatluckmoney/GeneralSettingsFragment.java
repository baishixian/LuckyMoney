package com.sunteng.wechatluckmoney;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.widget.Toast;

/**
 * Created by baishixian on 12/08/16.
 */
public class GeneralSettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.general_preferences);
        setPrefListeners();
    }

    private void setPrefListeners() {
        // Check for updates
        Preference updatePref = findPreference("pref_etc_check_update");
        updatePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                UpdateAppTask updateAppTask = new UpdateAppTask(getActivity().getApplicationContext());
                updateAppTask.setUpdateListener(new UpdateAppTask.UpdateListener() {
                    @Override
                    public void onUpdate(String updateUrl, String fileName) {

                        final Context context = GeneralSettingsFragment.this.getActivity();

                        String updateDirPath = Utils.getUpdateDirPath(context);
                        String filePath = updateDirPath +  fileName;
                        if (Utils.fileExist(filePath)){
                            Utils.installApk(context,filePath);
                            return;
                        }

                        // Give up on the fucking DownloadManager. The downloaded apk got renamed and unable to install. Fuck.
                        SystemDownloadTask systemDownloadTask = new SystemDownloadTask(context, updateUrl, fileName);
                        systemDownloadTask.setDownloadListener(new SystemDownloadTask.DownloadListener() {
                            @Override
                            public void onSuccess(String fileName) {
                                Utils.installApk(context,Utils.getUpdateDirPath(context) + fileName);
                            }

                            @Override
                            public void onFail() {
                                Toast.makeText(context, "下载新版本WeChatLuckMoney失败", Toast.LENGTH_SHORT).show();
                            }
                        });
                        systemDownloadTask.doExecute();
                    }
                });
                updateAppTask.update();
                return false;
            }
        });

        // Open issue
        Preference issuePref = findPreference("pref_etc_issue");
        issuePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/baishixian"));
                browserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(browserIntent);
                return false;
            }
        });

        Preference excludeWordsPref = findPreference("pref_watch_exclude_words");
        String summary = getResources().getString(R.string.pref_watch_exclude_words_summary);
        String value = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("pref_watch_exclude_words", "");
        if (value.length() > 0) excludeWordsPref.setSummary(summary + ":" + value);

        excludeWordsPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                String summary = getResources().getString(R.string.pref_watch_exclude_words_summary);
                if (o != null && o.toString().length() > 0) {
                    preference.setSummary(summary + ":" + o.toString());
                } else {
                    preference.setSummary(summary);
                }
                return true;
            }
        });
    }
}
