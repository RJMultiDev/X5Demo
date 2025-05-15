package rj.browser;


import android.app.Application;
import android.content.Intent;
import android.util.Log;
import com.tencent.smtt.sdk.QbSdk;
import com.tencent.smtt.sdk.QbSdk.PreInitCallback;
import com.tencent.smtt.sdk.TbsListener;
import com.tencent.smtt.sdk.WebView;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import android.content.Context;
import android.content.res.AssetManager;


public class DemoApplication extends Application {


    private static final String TAG = "DemoApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        
        /* 设置允许移动网络下进行内核下载。默认不下载，会导致部分一直用移动网络的用户无法使用x5内核 */
        QbSdk.setDownloadWithoutWifi(true);

        QbSdk.setCoreMinVersion(46290);
        /* SDK内核初始化周期回调，包括 下载、安装、加载 */

        QbSdk.setTbsListener(new TbsListener() {

            /**
             * @param stateCode 用户可处理错误码请参考{@link com.tencent.smtt.sdk.TbsCommonCode}
             */
            @Override
            public void onDownloadFinish(int stateCode) {
                Log.i(TAG, "onDownloadFinished: " + stateCode);
            }

            /**
             * @param stateCode 用户可处理错误码请参考{@link com.tencent.smtt.sdk.TbsCommonCode}
             */
            @Override
            public void onInstallFinish(int stateCode) {
                Log.i(TAG, "onInstallFinished: " + stateCode);
            }

            /**
             * 首次安装应用，会触发内核下载，此时会有内核下载的进度回调。
             * @param progress 0 - 100
             */
            @Override
            public void onDownloadProgress(int progress) {
                Log.i(TAG, "Core Downloading: " + progress);
            }
        });
        File internalStorage = this.getFilesDir();
        Context appContext = getApplicationContext();
        String path = internalStorage.getAbsolutePath();
        /*DownloadUtils.builder()
        .setContext(this)
        .setLister(new IDownloadlister() {
            @Override
            public void success(Uri uri) {
                QbSdk.installLocalTbsCore(this, 46904, "/sdcard/Downloads/tbs_core_046904_20231225151606_nolog_fs_obfs_armeabi_release.tbs");
            }
        })
        .download();*/
        //if (!QbSdk.isTbsCoreInited()) {
            if (QbSdk.getTbsVersion(appContext) <= 0) {
                copyAssetsToSDCard(this, "tbs", path + "/");
                QbSdk.installLocalTbsCore(this, 46904, path + "/tbs_core_046904_20231225151606_nolog_fs_obfs_armeabi_release.tbs");

            }
       // }
        
        /* 此过程包括X5内核的下载、预初始化，接入方不需要接管处理x5的初始化流程，希望无感接入 */
        
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

}
