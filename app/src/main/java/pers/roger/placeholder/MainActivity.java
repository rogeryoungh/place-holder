package pers.roger.placeholder;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.android.apksig.ApkSigner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import pers.roger.placeholder.axml.GenerateAXML;

public class MainActivity extends AppCompatActivity {
    String inputApkName = "hello.nosign.apk";
    String keyFileName = "test.bks";
    String outputApkName = "hello.sign.apk";
    String dexName = "classes.dex";
    String xmlName = "AndroidManifest.xml";
    String keyAlias = "mykey";
    String password = "123456";

    EditText name_text, pakage_text;

    byte[] data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        name_text = findViewById(R.id.app_name);
        pakage_text = findViewById(R.id.package_name);
    }

    private void prepare(String fileName, int id) {
        data = new byte[8192];
        int len;
        //获取文件中的内容
        try (InputStream inputStream = getResources().openRawResource(id)) {
            len = inputStream.read(data);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        try (FileOutputStream fos = this.openFileOutput(fileName, Context.MODE_PRIVATE)) {
            fos.write(data, 0, len);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Toast.makeText(this, "Prepare Success: " + fileName + " (" + len + " byte)",
        //         Toast.LENGTH_SHORT).show();
    }

    private void generateAXML(String appName, String packageName) {
        GenerateAXML g = new GenerateAXML(appName, packageName);
        data = g.generate();

        try (FileOutputStream fos = this.openFileOutput(xmlName, Context.MODE_PRIVATE)) {
            fos.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Toast.makeText(this, "Generate Success: " + xmlName + "(" + data.length + " byte)",
        //        Toast.LENGTH_SHORT).show();
    }

    private void generateAPK() {
        int len = 0;
        int sum = 0;

        try (FileOutputStream fos = this.openFileOutput(inputApkName, Context.MODE_PRIVATE)) {
            ZipOutputStream stream = new ZipOutputStream(fos);
            ZipEntry zipEntry = new ZipEntry(xmlName);
            stream.putNextEntry(zipEntry);

            try (FileInputStream in = this.openFileInput(xmlName)) {
                sum += len = in.read(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
            stream.write(data, 0, len);
            stream.closeEntry();

            zipEntry = new ZipEntry(dexName);
            stream.putNextEntry(zipEntry);
            try (FileInputStream in = this.openFileInput(dexName)) {
                sum += len = in.read(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
            stream.write(data, 0, len);
            stream.closeEntry();

            stream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        // Toast.makeText(this, "Generate Success: " + inputApkName + "(" + sum + " byte)", Toast.LENGTH_SHORT).show();
    }

    private void signedAPK() {
        File outputApk = new File(this.getFilesDir(), outputApkName);
        File inputApk = new File(this.getFilesDir(), inputApkName);
        File keystore = new File(this.getFilesDir(), keyFileName);
        char[] pwd = password.toCharArray();

        String ksType = KeyStore.getDefaultType();

        try {
            KeyStore ks = KeyStore.getInstance(ksType);

            try (FileInputStream in = new FileInputStream(keystore)) {
                ks.load(in, pwd);
            }

            PrivateKey key = (PrivateKey) ks.getKey(keyAlias, pwd);

            Certificate[] certChain = ks.getCertificateChain(keyAlias);

            List<X509Certificate> certs = new ArrayList<>(certChain.length);
            for (Certificate certificate : certChain) {
                certs.add((X509Certificate) certificate);
            }


            ApkSigner.SignerConfig.Builder builder
                    = new ApkSigner.SignerConfig.Builder("mykey", key, certs);

            List<ApkSigner.SignerConfig> signerConfigs = new ArrayList<>(1);
            signerConfigs.add(builder.build());

            ApkSigner apkSigner = new com.android.apksig.ApkSigner.Builder(signerConfigs)
                    .setInputApk(inputApk).setOutputApk(outputApk)
                    .setOtherSignersSignaturesPreserved(false)
                    .setV1SigningEnabled(true)
                    .setV2SigningEnabled(true)
                    .setV3SigningEnabled(true)
                    .setDebuggableApkPermitted(true)
                    .setSigningCertificateLineage(null)
                    .build();


            apkSigner.sign();
            // Toast.makeText(this, "Sign Success: " + outputApkName, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void generateAPP(String appName, String packageName) {
        prepare(dexName, R.raw.classes);
        prepare(keyFileName, R.raw.test);
        generateAXML(appName, packageName);
        generateAPK();
        signedAPK();
    }

    public void share(View view) {
        File file = new File(this.getFilesDir(), outputApkName);

        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
        Intent share = new Intent(Intent.ACTION_SEND);
        share.putExtra(Intent.EXTRA_STREAM, uri);
        share.setType("*/*"); // 此处可发送多种文件
        share.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(share, "分享文件"));

    }

    public void gen(View view) {
        String name = name_text.getText().toString();
        String pak = pakage_text.getText().toString();
        generateAPP(name, pak);
    }

    public void install(View view) {
        File file = new File(this.getFilesDir(), outputApkName);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri uri;
        if (Build.VERSION.SDK_INT >= 24) {
            uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            uri = Uri.fromFile(file);
        }
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        startActivity(intent);
    }

    public void list(View view) {
        Intent intent = new Intent(MainActivity.this, ListActivity.class);
        startActivity(intent);
    }
}