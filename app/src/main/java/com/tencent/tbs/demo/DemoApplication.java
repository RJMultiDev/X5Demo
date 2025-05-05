package com.tencent.tbs.demo;


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
        /* [new] 独立Web进程演示 */
        if (!startX5WebProcessPreinitService()) {
            return;
        }

        /* 设置允许移动网络下进行内核下载。默认不下载，会导致部分一直用移动网络的用户无法使用x5内核 */
        QbSdk.setDownloadWithoutWifi(true);

        QbSdk.setCoreMinVersion(QbSdk.CORE_VER_ENABLE_202112);
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
        String path = internalStorage.getAbsolutePath();
        if (!QbSdk.isTbsCoreInited()) {
            if (QbSdk.getTbsVersion(context) <= 0) {
                copyAssetsToSDCard(this, "tbs", path + "/tbs");
                QbSdk.installLocalTbsCore(this, 46904, path + "/tbs" + "/tbs_core_046904_20231225151606_nolog_fs_obfs_armeabi_release.tbs");
            }
        }
        
        /* 此过程包括X5内核的下载、预初始化，接入方不需要接管处理x5的初始化流程，希望无感接入 */
        QbSdk.initX5Environment(this, new PreInitCallback() {
            @Override
            public void onCoreInitFinished() {
                // 内核初始化完成，可能为系统内核，也可能为系统内核
            }

            /**
             * 预初始化结束
             * 由于X5内核体积较大，需要依赖wifi网络下发，所以当内核不存在的时候，默认会回调false，此时将会使用系统内核代替
             * 内核下发请求发起有24小时间隔，卸载重装、调整系统时间24小时后都可重置
             * 调试阶段建议通过 WebView 访问 debugtbs.qq.com -> 安装线上内核 解决
             * @param isX5 是否使用X5内核
             */
            @Override
            public void onViewInitFinished(boolean isX5) {
                Log.i(TAG, "onViewInitFinished: " + isX5);
                // hint: you can use QbSdk.getX5CoreLoadHelp(context) anytime to get help.
            }
        });
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


    /**
     * 启动X5 独立Web进程的预加载服务。优点：
     * 1、后台启动，用户无感进程切换
     * 2、启动进程服务后，有X5内核时，X5预加载内核
     * 3、Web进程Crash时，不会使得整个应用进程crash掉
     * 4、隔离主进程的内存，降低网页导致的App OOM概率。
     *
     * 缺点：
     * 进程的创建占用手机整体的内存，demo 约为 150 MB
     */
    private boolean startX5WebProcessPreinitService() {
        String currentProcessName = QbSdk.getCurrentProcessName(this);
        // 设置多进程数据目录隔离，不设置的话系统内核多个进程使用WebView会crash，X5下可能ANR
        WebView.setDataDirectorySuffix(QbSdk.getCurrentProcessName(this));
        Log.i(TAG, currentProcessName);
        if (currentProcessName.equals(this.getPackageName())) {
            this.startService(new Intent(this, com.tencent.tbs.demo.utils.X5ProcessInitService.class));
            return true;
        }
        return false;
    }

}
