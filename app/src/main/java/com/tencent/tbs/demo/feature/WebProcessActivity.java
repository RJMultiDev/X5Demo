package com.tencent.tbs.demo.feature;

import android.os.Bundle;
import android.util.Log;
import androidx.annotation.Nullable;

public class WebProcessActivity extends X5WebViewActivity {
    private static final String TAG = "WebViewActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "process create: " + android.os.Process.myPid());
    }
}
