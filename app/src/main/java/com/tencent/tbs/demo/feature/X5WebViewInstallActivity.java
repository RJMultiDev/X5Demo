package com.tencent.tbs.demo.feature;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import com.tencent.smtt.export.external.extension.proxy.ProxyWebViewClientExtension;
import com.tencent.smtt.export.external.interfaces.ISelectionInterface;
import com.tencent.smtt.sdk.QbSdk;
import com.tencent.tbs.demo.R;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import android.content.Context;
import android.content.res.AssetManager;

public class X5WebViewInstallActivity extends BaseWebViewActivity {

    private static final String M_TAG = "X5WebViewInstallActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        this.setTAG(M_TAG);
        super.onCreate(savedInstanceState);
        File internalStorage = this.getFilesDir();
        String path = internalStorage.getAbsolutePath();
        copyAssetsToSDCard(this, "tbs", path + "/tbs");
        //if (!QbSdk.isTbsCoreInited()) {
            //if (!QbSdk.isTbsCoreInstalled()) {
                QbSdk.installLocalTbsCore(this, 46904, path + "/tbs" + "/tbs_core_046904_20231225151606_nolog_fs_obfs_armeabi_release.tbs");
            //}
        //}
        startDefinedUrl();
    }

    public static void copyAssetsToSDCard(Context context, String sourceFolder, String destinationFolder) {
        AssetManager assetManager = context.getAssets();
        String[] files;
        try {
            // 获取assets文件夹下的所有文件和子文件夹
            files = assetManager.list(sourceFolder);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
 
        // 创建目标文件夹
        File destFolder = new File(destinationFolder);
        if (!destFolder.exists()) {
            destFolder.mkdirs();
        }
 
        for (String filename : files) {
            InputStream in = null;
            OutputStream out = null;
            try {
                // 从assets中打开文件
                in = assetManager.open(sourceFolder + "/" + filename);
                // 指定输出目标文件
                File outFile = new File(destinationFolder, filename);
                out = new FileOutputStream(outFile);
                // 将文件内容复制到目标文件
                Log.i("Qbsdk","copy 开始");
                copyFile(in, out);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

     private static void copyFile(InputStream in, OutputStream out) throws IOException {
        try {
            byte[] buffer = new byte[1024];
            int read;
            Log.i("Qbsdk","copy 进行中...");
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            Log.i("Qbsdk","copy 文件成功");
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("Qbsdk","copy 文件失败"+e.getMessage());
        }
 
    }

    @Override
    protected void initWebView() {
        super.initWebView();
        Toast.makeText(this, mWebView.getIsX5Core() ?
                "X5内核: " + QbSdk.getTbsVersion(this) : "SDK系统内核" , Toast.LENGTH_SHORT).show();
    }

    private void startDefinedUrl() {
        Intent intent = getIntent();
        if (intent != null) {
            String url = intent.getStringExtra("url");
            if (mWebView != null) {
                mWebView.loadUrl(url);
            }
        } else {
            Log.i(M_TAG, "Intent is null");
        }
    }
}
