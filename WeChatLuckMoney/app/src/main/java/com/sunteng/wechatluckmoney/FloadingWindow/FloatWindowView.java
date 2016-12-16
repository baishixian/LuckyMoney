package com.sunteng.wechatluckmoney.FloadingWindow;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.sunteng.wechatluckmoney.R;

/**
 * Created by baishixian on 2016/7/20.
 */
public class FloatWindowView extends LinearLayout{

    public static int viewWidth = 30;
    public static int viewHeight = 30;

    public FloatWindowView(final Context context) {
        super(context);
       // LayoutInflater.from(context).inflate(R.layout.layout_float_controller, this);
        LayoutInflater mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mInflater.inflate(R.layout.activity_transparent, this);
    }
}
