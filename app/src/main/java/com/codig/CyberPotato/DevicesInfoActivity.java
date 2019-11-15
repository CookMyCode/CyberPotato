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

import com.codig.CyberPotato.service.DeviceInfo;
import com.codig.CyberPotato.service.DynamicalMap;
import com.codig.CyberPotato.service.HttpServer;

public class DevicesInfoActivity extends AppCompatActivity {

    private HttpServer httpServer=MainActivity.httpServer;
    private DeviceInfo di;

    private ImageButton disconnectButtonMask;
    private TextView deviceName;
    private TextView ip;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devices_devinfo);
        Window window = this.getWindow();
        window.setGravity(Gravity.TOP);//置顶
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.y = 250;//主界面高度
        window.setAttributes(lp);

        //获取控件
        deviceName=findViewById(R.id.devices_devinfo_DeviceName);
        ip=findViewById(R.id.devices_devinfo_IP);
        disconnectButtonMask=findViewById(R.id.devices_devinfo_DisconnectButtonMask);
        //获取来自DevicesActivity的数据，给控件赋值
        di=(DeviceInfo)getIntent().getSerializableExtra("devInfo");
        deviceName.setText("Name:"+di.deviceName);
        ip.setText("IP:"+di.IP);


        //注册设备列表监听器
        httpServer.addDevicesStatusListener(devicesStatusListener);

        //注册断开连接按钮
        findViewById(R.id.devices_devinfo_DisconnectButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //将用户从内存中的允许连接表中移除
                httpServer.ceaseConn(di);
                finish();
            }
        });
        //注册关闭按钮
        findViewById(R.id.devices_devinfo_CloseButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
    @Override
    public void onDestroy(){
        //卸载监听器
        httpServer.removeDevicesStatusListener(devicesStatusListener);
        devicesListUpdateHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
    //设备连接情况监听器
    private DynamicalMap.DynamicalMapListener devicesStatusListener=new DynamicalMap.DynamicalMapListener(){
        @Override
        public void dynamicalMapChange(String opt,Object key)
        {
            if(di.IP.equals(key)&&opt.equals("remove")) {
                Message msg = new Message();
                devicesListUpdateHandler.sendMessage(msg);
            }
        }
    };
    //消息句柄
    private Handler devicesListUpdateHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            deviceName.setText("Name:--");
            ip.setText("IP:--.--.--.--");
            disconnectButtonMask.setVisibility(View.VISIBLE);
            return false;
        }
    });
}
