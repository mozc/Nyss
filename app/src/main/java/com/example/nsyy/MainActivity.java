package com.example.nsyy;

import static com.example.nsyy.code_scan.common.CodeScanCommon.*;

import android.Manifest;
import android.app.ActivityManager;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.ValueCallback;
import android.widget.Toast;

import android.app.AlertDialog;
import android.content.DialogInterface;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.nsyy.alarm.LongRunningService;
import com.example.nsyy.code_scan.CommonActivity;
import com.example.nsyy.code_scan.DefinedActivity;
import com.example.nsyy.config.MySharedPreferences;
import com.example.nsyy.message.FileHelper;
import com.example.nsyy.service.NsServerService;
import com.example.nsyy.service.NsyyServerBroadcastReceiver;
import com.example.nsyy.utils.AppVersionUtil;
import com.example.nsyy.utils.BlueToothUtil;
import com.example.nsyy.utils.LocationUtil;
import com.example.nsyy.utils.NotificationUtil;
import com.example.nsyy.utils.PermissionUtil;

import com.huawei.hms.hmsscankit.ScanUtil;
import com.huawei.hms.ml.scan.HmsScan;
import com.huawei.hms.ml.scan.HmsScanAnalyzerOptions;

import java.io.File;
import java.util.Base64;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    public static final String TAG = "Nsyy";

    private static String LOAD_RUL = "";

    private WebView webView;
//    private SwipeRefreshLayout swipeRefreshLayout;

    private final BroadcastReceiver noticeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String targetPage = intent.getStringExtra("target_page");
            // 在 WebView 中加载目标页面
            webView.loadUrl(targetPage);
        }
    };

    private final NsyyServerBroadcastReceiver nsyyServerBroadcastReceiver =
            new NsyyServerBroadcastReceiver(new NsyyServerBroadcastReceiver.ServerStateListener() {
                @Override
                public void onStart(String hostAddress) {
                    Log.d(TAG, "Nsyy 服务器已经启动，地址为：" + hostAddress);
                }

                @Override
                public void onStop() {
                    Log.d(TAG, "Nsyy 服务器已经停止");
                }

                @Override
                public void onError(String error) {
                    super.onError(error);
                    Log.e(TAG, error);
                    Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AppVersionUtil.getInstance().init(this);

        MySharedPreferences.init(this);
        // 初始化 WebView
        webView = findViewById(R.id.webView);
        String loadUrl = MySharedPreferences.getSharedPreferences().getString("load_url", "");
        if (!loadUrl.isEmpty() && !loadUrl.equals("")) {
            LOAD_RUL = loadUrl;
            initView();
        } else {
            // Ask the user to choose a website
            showWebsiteChooserDialog();
        }

        // 启动定时任务 每十分钟打印一次时间
        Intent intent = new Intent(this, LongRunningService.class);
        startService(intent);

        // 检查权限: 这里需要开启位置权限 & 位置服务 TODO 其他权限
        PermissionUtil.checkLocationPermission(this);

        // 启动 web server
        registerReceiver(nsyyServerBroadcastReceiver, new IntentFilter("NsyyServerBroadcastReceiver"));
        startService(new Intent(this, NsServerService.class));

        // 消息通知
        PermissionUtil.checkNotification(this);
        NotificationUtil.getInstance().setContext(this);
        NotificationUtil.getInstance().initNotificationChannel();

        FileHelper.getInstance().setContext(this);

        // 检查是否开启位置服务
        LocationUtil.getInstance().setContext(this);
        LocationUtil.getInstance().initGPS();

        // 检查是否开启蓝牙权限 & 初始化
        PermissionUtil.checkBlueToothPermission(this);
        BlueToothUtil.getInstance().init(this);

        // 注册广播接收器
        registerReceiver(noticeReceiver, new IntentFilter("LOAD_TARGET_PAGE"));

    }


    private void showWebsiteChooserDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择要访问的网站：")
                .setItems(R.array.website_choices, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Load the selected website
                        loadWebsite(which);
                    }
                });

        // Create and show the alert dialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void loadWebsite(int choice) {
        // Array of websites
        String[] websites = getResources().getStringArray(R.array.websites);

        if (choice >= 0 && choice < websites.length) {
            String selectedWebsite = websites[choice];
            LOAD_RUL = selectedWebsite;
            initView();
        } else {
            LOAD_RUL = "http://oa.nsyy.com.cn:6060";
            initView();
        }
        SharedPreferences.Editor editor = MySharedPreferences.getSharedPreferences().edit();
        editor.putString("load_url", LOAD_RUL);
        editor.apply();
    }

    private void initView() {
//        swipeRefreshLayout = findViewById(R.id.refreshLayout);
//        // 配置 SwipeRefreshLayout
//        swipeRefreshLayout.setOnRefreshListener(null);
//        // 隐藏加载图标
//        swipeRefreshLayout.setRefreshing(false);
//        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
//            @Override
//            public void onRefresh() {
//                // 检查 WebView 是否为空
//                if (webView == null) {
//                    swipeRefreshLayout.setRefreshing(false);
//                    return;
//                }
//                // 在 UI 线程上执行 WebView 刷新
//                new Handler().post(new Runnable() {
//                    @Override
//                    public void run() {
//                        webView.reload();
//                    }
//                });
//            }
//        });

        if (webView == null) {
            webView = findViewById(R.id.webView);
        }

        // Enable Javascript
        WebSettings webSettings = webView.getSettings();
        webSettings.setDomStorageEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        // 设置允许JS弹窗
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        // 设置 WebView 允许执行 JavaScript 脚本
        webSettings.setJavaScriptEnabled(true);

        // Add the JavaScriptInterface to the WebView
        webView.addJavascriptInterface(this, "AndroidInterface");

        // 确保跳转到另一个网页时仍然在当前 WebView 中显示,而不是调用浏览器打开
        webView.setWebViewClient(new WebViewClient() {
//            public void onPageFinished(WebView view, String url) {
//                swipeRefreshLayout.setRefreshing(false);
//            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                // 在这里根据需要判断是否要拦截请求  返回 true 表示拦截请求，返回 false 表示不拦截请求

                String fileName = "";
                // 邮件附件下载
                if (url.contains("gyl/workstation/mail/download_attachment")) {

                    // 找到最后一个 '/' 的索引
                    int lastIndex = url.lastIndexOf('/');
                    // 截取最后一个 '/' 后面的部分
                    String base64String = url.substring(lastIndex + 1);
                    base64String = base64String.replace("&", "/");
                    String param = base64decode(base64String);
                    param = param.replace(" ", "");
                    String[] params = param.split("#");

                    if (params.length != 3) {
                        return false;
                    }

                    url = params[1];
                    String date = params[2];
                    date = date.replace(" ", "");
                    date = date.replace(":", "");
                    date = date.replace("-", "");

                    String originalFileName = params[0];
                    int dotIndex = originalFileName.lastIndexOf(".");
                    fileName = originalFileName.substring(0, dotIndex);
                    String extension = originalFileName.substring(dotIndex);

                    fileName = fileName + "-" +date + extension;
                } else if (url.contains("att_download?save_path=")) {
                    // app 安装包下载
                    String base64String = url.substring(url.indexOf("save_path=") + "save_path=".length());
                    fileName = base64decode(base64String);
                } else {
                    return false;
                }

                // 请求存储权限
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_FILE_PERMISSION_CODE);
                } else {
                    // 下载文件
                    startDownload(url, fileName);
                }

                return true;
            }
        });

        // 重写 javascript 的 alert 和 confirm 函数,弹窗界面更美观。
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
                AlertDialog.Builder b = new AlertDialog.Builder(MainActivity.this);
                b.setTitle("Alert");
                b.setMessage(message);
                b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        result.confirm();
                    }
                });
                b.setCancelable(false);
                b.create().show();
                return true;
            }

            //设置响应js 的Confirm()函数
            @Override
            public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
                AlertDialog.Builder b = new AlertDialog.Builder(MainActivity.this);
                b.setTitle("Confirm");
                b.setMessage(message);
                b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        result.confirm();
                    }
                });
                b.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        result.cancel();
                    }
                });
                b.create().show();
                return true;
            }
        });

        // 加载 南石OA
        webView.loadUrl(LOAD_RUL);
    }

    protected String base64decode(String encodedString) {
        // 解码 Base64 编码的字符串
        byte[] decodedBytes = new byte[0];
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            decodedBytes = Base64.getDecoder().decode(encodedString);
        }

        // 将字节数组转换为字符串
        String decodedString = new String(decodedBytes);
        return decodedString;
    }

    private void startDownload(String url, String fileName) {

        // 获取下载目录
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        // 形成完整的文件地址
        File file = new File(downloadsDir, fileName);
        // 判断文件是否存在
        if (file.exists()) {
            // 在此处处理下载完成后 跳转到文件管理器查看下载的文件
            openDownloadFile(fileName);
            return;
        }

        // 获取文件扩展名
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setDescription("Downloading file...");
        request.setTitle(fileName);
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
        DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        long downloadId = dm.enqueue(request);

        // 下载完成广播接收器
        String finalFileName = fileName;
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (id == downloadId) {
                    openDownloadFile(finalFileName);
                }
            }
        };

        // 注册广播接收器
        registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    private void openDownloadFile(String fileName) {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);

        Uri uri = FileProvider.getUriForFile(webView.getContext(), "com.example.nsyy.fileprovider", file);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        if (fileName.toLowerCase().endsWith("png") || fileName.toLowerCase().endsWith("jpg") || fileName.toLowerCase().endsWith("jpeg")
                || fileName.toLowerCase().endsWith("gif") || fileName.toLowerCase().endsWith("webp")) {
            intent.setDataAndType(uri, "image/*");
        } else if (fileName.toLowerCase().endsWith("pdf")) {
            intent.setDataAndType(uri, "application/pdf");
        } else if (fileName.toLowerCase().endsWith(".docx") || fileName.toLowerCase().endsWith(".doc")) {
            intent.setDataAndType(uri, "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        } else if (fileName.toLowerCase().endsWith(".xlsx") || fileName.toLowerCase().endsWith("xls")) {
            intent.setDataAndType(uri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        } else if (fileName.toLowerCase().endsWith(".apk")) {
            // 打开安装包
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            //intent.setDataAndType(uri, "application/vnd.android.package-archive");
            // 设置 Intent 的 flags 为 FLAG_ACTIVITY_NEW_TASK，表示新建一个任务进行安装
            //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        } else {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri downloadUri = Uri.parse(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath());
            intent.setDataAndType(downloadUri, "*/*");// 设置要显示的文件类型为所有类型
        }
        webView.getContext().startActivity(intent);
    }

    // 接管返回按键的响应
    @Override
    public void onBackPressed() {
        // 如果 WebView 可以返回，则返回上一页
        if (webView.canGoBack()) {
            webView.goBack();
            return;
        }
        FileHelper.RUN_IN_BACKGROUND = true;
        // 这里返回后台运行，而不是直接杀死
        moveTaskToBack(false);
//        // 否则退出应用程序
//        super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        System.out.println("===> webview resume");
        FileHelper.RUN_IN_BACKGROUND = false;

        if (isAppInBackground()) {
            bringWebViewActivityToFront();
        }

        webView.onResume();
    }

    private boolean isAppInBackground() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
        if (!tasks.isEmpty()) {
            ComponentName topActivity = tasks.get(0).topActivity;
            return !topActivity.getPackageName().equals(getPackageName());
        }
        return false;
    }

    private void bringWebViewActivityToFront() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        System.out.println("===> webview 暂停");
        FileHelper.RUN_IN_BACKGROUND = true;
        webView.onPause();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        webView.destroy();
        FileHelper.RUN_IN_BACKGROUND = true;
        unregisterReceiver(nsyyServerBroadcastReceiver);
        stopService(new Intent(this, NsServerService.class));//停止服务

        // 注销广播接收器
        unregisterReceiver(noticeReceiver);
    }


    @JavascriptInterface
    public void scanCode(){
        // 多种模式可选： https://developer.huawei.com/consumer/cn/doc/development/HMSCore-Guides/android-overview-0000001050282308
//        loadScanKitBtnClick();
        newViewBtnClick();
//        multiProcessorSynBtnClick();
//        multiProcessorAsynBtnClick();
    }

    // 接入华为统一扫码功能：https://developer.huawei.com/consumer/cn/doc/development/HMSCore-Guides/android-dev-process-0000001050043953

    /**
     * Call the default view.
     */
    public void loadScanKitBtnClick() {
        requestPermission(CAMERA_REQ_CODE, DECODE);
    }

    /**
     * Call the customized view.
     */
    public void newViewBtnClick() {
//        requestPermission(DEFINED_CODE, DECODE);
        // CAMERA_REQ_CODE为用户自定义，用于接收权限校验结果的请求码
        this.requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE}, DEFINED_CODE);
    }

    /**
     * Call the MultiProcessor API in synchronous mode.
     */
    public void multiProcessorSynBtnClick() {
//        requestPermission(MULTIPROCESSOR_SYN_CODE, DECODE);
        this.requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE}, MULTIPROCESSOR_SYN_CODE);

    }

    /**
     * Call the MultiProcessor API in asynchronous mode.
     */
    public void multiProcessorAsynBtnClick() {
        requestPermission(MULTIPROCESSOR_ASYN_CODE, DECODE);
    }

    /**
     * Apply for permissions.
     */
    private void requestPermission(int requestCode, int mode) {
        if (mode == DECODE) {
            decodePermission(requestCode);
        } else if (mode == GENERATE) {
            // generatePermission(requestCode);
        }
    }

    /**
     * Apply for permissions.
     */
    private void decodePermission(int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES},
                    requestCode);
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE},
                    requestCode);
        }
    }

    /**
     * Call back the permission application result. If the permission application is successful, the barcode scanning view will be displayed.
     * @param requestCode Permission application code.
     * @param permissions Permission array.
     * @param grantResults: Permission application result array.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (permissions == null || grantResults == null) {
            return;
        }

        if (grantResults.length < 2 || grantResults[0] != PackageManager.PERMISSION_GRANTED || grantResults[1] != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        //Default View Mode
        if (requestCode == CAMERA_REQ_CODE) {
            ScanUtil.startScan(this, REQUEST_CODE_SCAN_ONE, new HmsScanAnalyzerOptions.Creator().create());
        }

        //Customized View Mode
        if (requestCode == DEFINED_CODE) {
            Intent intent = new Intent(this, DefinedActivity.class);
            try {
                this.startActivityForResult(intent, REQUEST_CODE_DEFINE);
            } catch (Exception e) {
                System.out.println("未成功打开扫码页面，请检查");
                e.printStackTrace();
                // Handle the exception
            }
        }

        //Multiprocessor Synchronous Mode
        if (requestCode == MULTIPROCESSOR_SYN_CODE) {
            Intent intent = new Intent(this, CommonActivity.class);
            intent.putExtra(DECODE_MODE, MULTIPROCESSOR_SYN_CODE);

            try {
                this.startActivityForResult(intent, REQUEST_CODE_SCAN_MULTI);
            } catch (Exception e) {
                e.printStackTrace();
                // Handle the exception
            }
        }
        //Multiprocessor Asynchronous Mode
        if (requestCode == MULTIPROCESSOR_ASYN_CODE) {
            Intent intent = new Intent(this, CommonActivity.class);
            intent.putExtra(DECODE_MODE, MULTIPROCESSOR_ASYN_CODE);

            try {
                this.startActivityForResult(intent, REQUEST_CODE_SCAN_MULTI);
            } catch (Exception e) {
                e.printStackTrace();
                // Handle the exception
            }
        }

        if (requestCode == REQUEST_FILE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 用户授予了存储权限，开始下载
                Toast.makeText(MainActivity.this, "存储权限已获取，请重新点击下载", Toast.LENGTH_SHORT).show();

            } else {
                // 用户拒绝了存储权限，显示提示信息
                Toast.makeText(MainActivity.this, "没有存储权限，无法下载文件", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Event for receiving the activity result.
     *
     * @param requestCode Request code.
     * @param resultCode Result code.
     * @param data        Result.
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) {
            return;
        }
        //Default View
        if (requestCode == REQUEST_CODE_SCAN_ONE) {
            HmsScan obj = data.getParcelableExtra(ScanUtil.RESULT);
            if (obj != null) {
                Toast.makeText(this,obj.originalValue,Toast.LENGTH_SHORT).show();
            }
            //MultiProcessor & Bitmap
        } else if (requestCode == REQUEST_CODE_SCAN_MULTI) {
            Parcelable[] obj = data.getParcelableArrayExtra(CommonActivity.SCAN_RESULT);
            if (obj != null && obj.length > 0) {
                //Get one result.
                if (obj.length == 1) {
                    if (obj[0] != null && !TextUtils.isEmpty(((HmsScan) obj[0]).getOriginalValue())) {
                        Toast.makeText(this,((HmsScan) obj[0]).originalValue,Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this,obj[0].describeContents(),Toast.LENGTH_SHORT).show();
                }
            }
            //Customized View
        } else if (requestCode == REQUEST_CODE_DEFINE) {
            HmsScan obj = data.getParcelableExtra(DefinedActivity.SCAN_RESULT);
            if (obj != null) {
                String retValue = obj.originalValue;
                Toast.makeText(this, retValue, Toast.LENGTH_SHORT).show();

                try {
                    String js = "javascript:receiveScanResult('" + retValue + "')";
                    System.out.println("开始执行 JS 方法：" + js);

                    webView.evaluateJavascript(js, new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String s) {
                            //将button显示的文字改成JS返回的字符串
                            System.out.println("成功接收到扫码返回值：" + s);
                        }
                    });

                    //webView.loadUrl("javascript:handleScanResult('" + retValue + "')");
                } catch (Exception e) {
                    System.out.println("未成功调用 JS 方法 handleScanResult");
                    e.printStackTrace();
                    // Handle the exception
                }
            }
        }
    }


}