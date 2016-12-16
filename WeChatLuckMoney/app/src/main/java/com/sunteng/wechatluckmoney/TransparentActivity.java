package com.sunteng.wechatluckmoney;

import android.app.Activity;
import android.os.Bundle;

public class TransparentActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transparent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.finish();
    }
}
