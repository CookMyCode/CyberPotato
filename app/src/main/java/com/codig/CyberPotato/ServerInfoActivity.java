package com.codig.CyberPotato;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import com.codig.CyberPotato.service.HttpServer;
import com.codig.CyberPotato.utils.CommonUtils;

public class ServerInfoActivity extends AppCompatActivity {

    private CyberPotato thisApplication;

    private ImageButton serverStatusChangeButtonMask;
    private ImageButton serverStatusChangeButton;
    private TextView serverStatusTextView;
    private TextView serverPortTextView;
    private TextView serverAPTextView;
    private TextView serverWifiTextView;
    private TextView serverMobileTextView;

    //来自MainActivity的服务器对象引用
    private HttpServer httpServer=MainActivity.httpServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devices_svrinfo);
        Window window = this.getWindow();
        window.setGravity(Gravity.TOP);//置顶
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.y = 50;//主界面高度
        window.setAttributes(lp);

        thisApplication=(CyberPotato) getApplication();

        //实例化
        serverStatusChangeButtonMask=findViewById(R.id.devices_svrinfo_ServerStatusChangeButtonMask);
        serverStatusChangeButton=findViewById(R.id.devices_svrinfo_ServerStatusChangeButton);
        serverStatusTextView=findViewById(R.id.devices_svrinfo_ServerStatus);
        serverPortTextView=findViewById(R.id.devices_svrinfo_Port);
        serverAPTextView=findViewById(R.id.devices_svrinfo_APContent);
        serverWifiTextView=findViewById(R.id.devices_svrinfo_WIFIContent);
        serverMobileTextView=findViewById(R.id.devices_svrinfo_MobileContent);

        //注册关闭按钮
        findViewById(R.id.devices_svrinfo_CloseButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        //解除蒙版
        serverStatusChangeButtonMask.setVisibility(View.GONE);

        //网络状态监听器
        thisApplication.addNetworkStatusListener(networkStatusListener);
        //服务器状态监听器
        httpServer.addServerStatusListener(serverStatusListener);
        //获取服务器的状态
        Boolean serverStatus=httpServer.getServerStatus();
        //获取来自服务器类的数据，给控件赋值
        if(serverStatus) {
            //注册关闭服务器按钮
            serverStatusChangeButton.setOnClickListener(closeServerButtonOnClickListener);
            serverStatusChangeButton.setImageResource(R.drawable.disconnect_button);
            serverStatusTextView.setText("Status: Running");
            serverPortTextView.setText("Port: "+httpServer.getPort());
        }
        else {
            //注册开启服务器按钮
            serverStatusChangeButton.setOnClickListener(startServerButtonOnClickListener);
            serverStatusChangeButton.setImageResource(R.drawable.connect_button);
            serverStatusTextView.setText("Status: Stop");
            serverPortTextView.setText("Port: --");
        }
        String[] ipArr= CommonUtils.getAllAddress(getApplicationContext()).split("[|]");
        serverAPTextView.setText(ipArr[0]);
        serverWifiTextView.setText(ipArr[1]);
        serverMobileTextView.setText(ipArr[2]);
    }
    @Override
    public void onDestroy(){
        //卸载监听器
        httpServer.removeServerStatusListener(serverStatusListener);
        thisApplication.removeNetworkStatusListener(networkStatusListener);
        updateUI.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
    private View.OnClickListener closeServerButtonOnClickListener=new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            serverStatusChangeButton.setOnClickListener(startServerButtonOnClickListener);
            //在状态改变之前不允许再次按下按钮
            serverStatusChangeButtonMask.setVisibility(View.VISIBLE);
            //向服务器发送关闭指令
            Thread t=new CloseServerThread();
            t.setDaemon(true);
            t.start();
        }
    };
    private View.OnClickListener startServerButtonOnClickListener=new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            serverStatusChangeButton.setOnClickListener(closeServerButtonOnClickListener);
            //在状态改变之前不允许再次按下按钮
            serverStatusChangeButtonMask.setVisibility(View.VISIBLE);
            //向服务器发送开启指令
            Thread t=new StartServerThread();
            t.setDaemon(true);
            t.start();
        }
    };
    //实例化服务器状态监听器
    private HttpServer.ServerStatusListener serverStatusListener=new HttpServer.ServerStatusListener(){
        @Override
        public void serverStatusChange(Boolean status){
            //根据从监听器获取的服务器状态更新界面
            Message msg = new Message();
            msg.obj=status;
            updateUI.sendMessage(msg);
        }
    };
    //实例化网络状态监听器
    private CyberPotato.NetworkStatusListener networkStatusListener=new CyberPotato.NetworkStatusListener(){
        @Override
        public void networkStatusChange(String status){
            Message msg = new Message();
            msg.obj=httpServer.getServerStatus();
            updateUI.sendMessage(msg);
        }
    };
    private class StartServerThread extends Thread{
        @Override
        public void run(){
            //向服务器发送启动指令
            httpServer.startServer();
        }
    }
    private class CloseServerThread extends Thread{
        @Override
        public void run(){
            //向服务器发送关闭指令
            httpServer.closeServer();
        }
    }
    private Handler updateUI = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if((Boolean) msg.obj) {
                serverStatusTextView.setText("Status: Running");
                serverPortTextView.setText("Port: "+httpServer.getPort());
                serverStatusChangeButton.setImageResource(R.drawable.disconnect_button);
                serverStatusChangeButtonMask.setVisibility(View.GONE);
            }
            else{
                serverStatusTextView.setText("Status: Stop");
                serverPortTextView.setText("Port: --");
                serverStatusChangeButton.setImageResource(R.drawable.connect_button);
                serverStatusChangeButtonMask.setVisibility(View.GONE);
            }
            String[] ipArr=CommonUtils.getAllAddress(getApplicationContext()).split("[|]");
            serverAPTextView.setText(ipArr[0]);
            serverWifiTextView.setText(ipArr[1]);
            serverMobileTextView.setText(ipArr[2]);
            return false;
        }
    });
}
