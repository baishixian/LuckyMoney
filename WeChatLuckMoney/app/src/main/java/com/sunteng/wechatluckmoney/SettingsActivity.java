package com.sunteng.wechatluckmoney;

import android.annotation.TargetApi;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

/**
 * Created by baishixian on 12/08/16.
 */
public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        loadUI();
        prepareSettings();
    }

    private void prepareSettings() {
        String fragId;
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            fragId = bundle.getString("frag_id");
        } else {
            fragId = "GeneralSettingsFragment";
        }

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        if ("GeneralSettingsFragment".equals(fragId)) {
            fragmentTransaction.replace(R.id.preferences_fragment, new GeneralSettingsFragment());
        } else if ("CommentSettingsFragment".equals(fragId)) {
            fragmentTransaction.replace(R.id.preferences_fragment, new CommentSettingsFragment());
        }
        fragmentTransaction.commit();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void loadUI() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) return;

        Window window = this.getWindow();

        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        window.setStatusBarColor(0xffE46C62);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public void performBack(View view) {
        super.onBackPressed();
    }

    public void enterAccessibilityPage() {
        Intent mAccessibleIntent =
                new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(mAccessibleIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        //当点击不同的menu item 是执行不同的操作
        switch (id) {
            case R.id.action_refresh:
                enterAccessibilityPage();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
