package com.codig.CyberPotato;

import android.Manifest;
import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.codig.CyberPotato.service.DynamicalMap;
import com.codig.CyberPotato.service.HttpServer;
import com.codig.CyberPotato.service.HttpService;
import com.codig.CyberPotato.utils.CommonUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    //服务器类，使用静态变量存放引用
    public static HttpServer httpServer;
    //消息提醒，控制消息图标闪动  null：可以执行动画  false：用户正处于MessageActivity界面  true:动画已经在执行，不允许再次执行动画
    public static Boolean reminder=null;

    private CyberPotato thisApplication;

    private TextView statusView;
    private ImageButton messageButton;
    private TextView log1;
    private TextView log2;
    private TextView log3;
    private TextView log4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);//去掉对话框标题
        setContentView(R.layout.activity_main);
        thisApplication=(CyberPotato) getApplication();

        statusView=findViewById(R.id.StatusView);//状态视图

        View btn;
        //监听设置按钮
        btn=findViewById(R.id.ButtonSetting);
        btn.setOnClickListener(this);
        //监听设备按钮
        btn=findViewById(R.id.ButtonDevices);
        btn.setOnClickListener(this);
        //监听储存按钮
        btn=findViewById(R.id.ButtonStorage);
        btn.setOnClickListener(this);
        //监听日志按钮
        btn=findViewById(R.id.ButtonEventLog);
        btn.setOnClickListener(this);
        //监听记录按钮
        messageButton=findViewById(R.id.ButtonMessage);
        messageButton.setOnClickListener(this);

        log1=findViewById(R.id.Log_1);
        log2=findViewById(R.id.Log_2);
        log3=findViewById(R.id.Log_3);
        log4=findViewById(R.id.Log_4);
//        startService(new Intent(this, HttpService.class));
        //启动服务器线程，内部集成了界面相关的监听器
        askPermission(MainActivity.this,START_SERVER,Manifest.permission.INTERNET);
        //抽取apk文件供客户端用户下载
        extractApk();
    }

    //抽取apk文件，文件一般在/data/app/，可能需要访问安装列表权限，但目前未发现需要授权
    void extractApk(){
        File file = new File(thisApplication.getFilesDir() + File.separator + "CyberPotato.apk");
        if (file.isFile())
            return;
        //获取apk路径
        String packetName=thisApplication.getPackageName();
        File apkFile=null;
        List<PackageInfo> packageInfoList = getPackageManager().getInstalledPackages(0);
        for (PackageInfo packageInfo:packageInfoList){
            if(packageInfo.applicationInfo.sourceDir.contains(packetName)){
                apkFile=new File(packageInfo.applicationInfo.sourceDir);
                break;
            }
        }
        //复制到app的data路径下
        if(apkFile!=null) {
            try {
                //输入流
                InputStream is = new FileInputStream(apkFile);
                byte[] read = new byte[is.available()];
                is.read(read);
                is.close();
                //输出流
                OutputStream os = new FileOutputStream(file);
                os.write(read);
                os.close();
            } catch (IOException ignored) { }
        }
    }

    @Override
    public void onClick(View v){
        switch (v.getId()){
            case R.id.ButtonSetting:
                startActivity(new Intent(this,SettingActivity.class));
                break;
            case R.id.ButtonDevices:
                startActivity(new Intent(this,DevicesActivity.class));
                break;
            case R.id.ButtonStorage:
                startActivity(new Intent(this,StorageActivity.class));
                break;
            case R.id.ButtonMessage:
                reminder=false;
                startActivity(new Intent(this,MessageActivity.class));
                break;
            case R.id.ButtonEventLog:
                startActivity(new Intent(this,EventLogActivity.class));
                break;
            default:
                break;
        }
    }

//    final private int CREATE_EXTERNAL_STORAGE_DOC_ROOT=1;
    final private int CREATE_EXTERNAL_STORAGE_DOWNLOAD_DIR=2;
    final private int START_SERVER = 3;
    //申请权限
    public void askPermission(Activity activity, int requestCode, String permission){
        int hasPermission =ContextCompat.checkSelfPermission(thisApplication,permission);
        if(hasPermission !=PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,new String[]{permission},requestCode);
        }
        else {
            switch(requestCode){
//                case CREATE_EXTERNAL_STORAGE_DOC_ROOT:
//                    createExternalStorageDocRoot();
//                    break;
                case CREATE_EXTERNAL_STORAGE_DOWNLOAD_DIR:
                    createExternalStorageDownloadDir();
                    break;
                case START_SERVER:
                    startHttpServer();
                    break;
            }
        }
    }
    //权限申请结果的回调函数
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode) {
//            case CREATE_EXTERNAL_STORAGE_DOC_ROOT :
//                if(grantResults[0]==PackageManager.PERMISSION_GRANTED) {
//                    createExternalStorageDocRoot();
//                }else{
//                    Toast.makeText(MainActivity.this,"SD卡存储权限没有开启",Toast.LENGTH_SHORT).show();
//                }
//                break;
            case CREATE_EXTERNAL_STORAGE_DOWNLOAD_DIR:
                if(grantResults[0]==PackageManager.PERMISSION_GRANTED) {
                    createExternalStorageDownloadDir();
                }else{
                    finish();
                }
                break;
            case START_SERVER :
                if(grantResults[0]==PackageManager.PERMISSION_GRANTED) {
                    startHttpServer();
                }else{
                    finish();
                }
                break;
        }
        //super.onRequestPermissionsResult(requestCode, permissions,grantResults);
    }
    //生成外部下载文件存放目录
    private void createExternalStorageDownloadDir(){
        //生成下载文件的存放目录
        File pathCheck=new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator+"HttpDownload");
        if(!pathCheck.isDirectory())
        {
            pathCheck.mkdirs();
        }
    }
    private void startHttpServer () {
        //生成存放下载文件的目录
        askPermission(MainActivity.this,CREATE_EXTERNAL_STORAGE_DOWNLOAD_DIR,Manifest.permission.WRITE_EXTERNAL_STORAGE);
        //生成内部储存发布目录
        thisApplication.createDocRoot(false);
        //启动服务器
        String downloadDir=Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator +"HttpDownload";
        final Intent intent = new Intent(this, HttpService.class);
        intent.putExtra("iDocRoot",thisApplication.internalStorageDocRoot);
        intent.putExtra("eDocRoot",thisApplication.externalStorageDocRoot);
        intent.putExtra("appDir",this.getFilesDir().getAbsolutePath());
        intent.putExtra("downloadDir",downloadDir);
//        startService(intent);
        bindService(intent, conn, Service.BIND_AUTO_CREATE);
    }

    private ServiceConnection conn = new ServiceConnection() {
        //此方法会调用服务中的onBind方法
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            HttpService.HttpServiceBinder binder = (HttpService.HttpServiceBinder) service;
            httpServer=binder.getHttpServer();
            //设置服务器状态监听器
            httpServer.addServerStatusListener(serverStatusListener);
            //网络状态监听
            thisApplication.addNetworkStatusListener(networkStatusListener);
            //初始化状态界面
            Message msg = new Message();
            msg.obj=httpServer.getServerStatus();
            updateStatusView.sendMessage(msg);
            //日志预览设置监听器
            httpServer.addEventLogListener(eventLogListener);
            //初始化日志列表
            TextView[] textViewArr={log1,log2,log3,log4};
            List<String> lastLog=httpServer.getLastLog(4);
            int i=0;
            for(String log:lastLog){
                String[] contentArr=log.split("[|]");
                if(!"".equals(contentArr[3])) {
                    log = "[" + contentArr[0].substring(0, 5) + "]# " + contentArr[3] + "..";
                    textViewArr[i++].setText(log);
                }
            }
            //设置请求连接监听器，控制消息提醒按钮闪动
            httpServer.addConnRequestListListener(connRequestListListener);
        }
        //服务意外中断时被调用，主进程主动解绑服务时不会被调用
        @Override
        public void onServiceDisconnected(ComponentName name) {
            httpServer=null;
        }
    };

    //设备连接请求监听器
    private DynamicalMap.DynamicalMapListener connRequestListListener=new DynamicalMap.DynamicalMapListener(){
        @Override
        public void dynamicalMapChange(String opt,Object key)
        {
            //根据reminder变量为true启动动画线程
            if("put".equals(opt)&&reminder==null){
                reminder=true;
                Thread messageButtonReminderThread=new MessageButtonReminderThread();
                messageButtonReminderThread.setDaemon(true);
                messageButtonReminderThread.start();
            }
        }
    };
    //消息提醒动画线程
    class MessageButtonReminderThread extends Thread{
        @Override
        public void run(){
            Message msg;
            //reminder变量为false退出循环
            try {
                while (reminder!=null&&reminder){
                    msg = new Message();
                    msg.arg1=R.drawable.message_button_dim;
                    messageButtonSrcChange.sendMessage(msg);
                    sleep(600);
                    if(reminder==null||!reminder) break;
                    msg = new Message();
                    msg.arg1=R.drawable.message_button;
                    messageButtonSrcChange.sendMessage(msg);
                    sleep(400);
                }
            }
            catch (InterruptedException ignored) {}
            msg = new Message();
            msg.arg1=R.drawable.message_button;
            messageButtonSrcChange.sendMessage(msg);
            reminder=null;
        }
    }
    //消息句柄，变更消息按钮背景图片
    private Handler messageButtonSrcChange = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            //根据msg传来的id设置背景图片
            messageButton.setImageResource(msg.arg1);
            return false;
        }
    });
    //日志列表更新
    private HttpServer.EventLogListener eventLogListener=new HttpServer.EventLogListener(){
        @Override
        public void eventLogChange(String level,String event,String cause){
            Message msg = new Message();
            updateEventLog.sendMessage(msg);
        }
    };
    private Handler updateEventLog = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            TextView[] textViewArr={log1,log2,log3,log4};
            List<String> lastLog=httpServer.getLastLog(4);
            int i=0;
            for(String log:lastLog){
                String[] contentArr=log.split("[|]");
                if(!"".equals(contentArr[3])) {
                    log = "[" + contentArr[0].substring(0, 5) + "]# " + contentArr[3] + "..";
                    textViewArr[i++].setText(log);
                }
            }
            return false;
        }
    });

    @Override
    public void onDestroy(){
        thisApplication.removeNetworkStatusListener(networkStatusListener);
        httpServer.removeConnRequestListListener(connRequestListListener);
        httpServer.removeEventLogListener(eventLogListener);
        httpServer.removeServerStatusListener(serverStatusListener);
        messageButtonSrcChange.removeCallbacksAndMessages(null);
        updateEventLog.removeCallbacksAndMessages(null);
        updateStatusView.removeCallbacksAndMessages(null);
        unbindService(conn);
//        stopService(new Intent(this, HttpService.class));
        super.onDestroy();
    }
    private HttpServer.ServerStatusListener serverStatusListener=new HttpServer.ServerStatusListener(){
        @Override
        public void serverStatusChange(Boolean status){
            //根据从监听器获取的服务器状态更新界面
            Message msg = new Message();
            msg.obj=status;
            updateStatusView.sendMessage(msg);
        }
    };
    private Handler updateStatusView = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if((Boolean) msg.obj) {
                String[] temp= CommonUtils.getAllAddress(getApplicationContext()).split("[|]");
                for (String ip:temp) {
                    statusView.setText("IP:" + ip + "\nPORT:" + httpServer.getPort() + "\nSVR:Running");
                    if(!ip.startsWith("0")&&!ip.startsWith("--")){
                        break;
                    }
                }
            }
            else {
                statusView.setText("IP:--.--.--.--\nPORT:--\nSVR:unable");
            }
            return false;
        }
    });

    //实现网络状态监听接口
    private CyberPotato.NetworkStatusListener networkStatusListener=new CyberPotato.NetworkStatusListener(){
        @Override
        public void networkStatusChange(String status){
            Message msg = new Message();
            switch (status){
                case "none":
                    msg.obj=false;
                    updateStatusView.sendMessage(msg);
                    break;
                default:
                    msg.obj=true;
                    updateStatusView.sendMessage(msg);
                    break;
            }
        }
    };
}
