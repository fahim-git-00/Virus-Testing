package com.security.lab;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity {
    static {
        System.loadLibrary("securitylab");
    }

    private native String nativeStatus();

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        TextView view = new TextView(this);
        view.setPadding(32, 48, 32, 32);
        view.setText("Android Security Lab\n\n" + nativeStatus());
        setContentView(view);
    }
}
