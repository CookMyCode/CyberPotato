package com.codig.CyberPotato;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.codig.CyberPotato.service.DeviceInfo;
import com.codig.CyberPotato.service.HttpServer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AccessActivity extends AppCompatActivity {

    private DeviceInfo di;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_access);
        Window window = this.getWindow();
        window.setGravity(Gravity.TOP);//置顶
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.y = 50;//主界面高度
        window.setAttributes(lp);


        //获取来自DevicesActivity的数据，给控件赋值
        di=(DeviceInfo)getIntent().getSerializableExtra("devInfo");
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        Date date = new Date(di.timeStamp);
        String time = simpleDateFormat.format(date);
        ((TextView)findViewById(R.id.message_access_Time)).setText("Time: "+time);
        ((TextView)findViewById(R.id.message_access_IP)).setText("IP: "+di.IP);
        //注册关闭按钮
        findViewById(R.id.message_access_CloseButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        final HttpServer httpServer=MainActivity.httpServer;
        //注册拒绝连接按钮
        findViewById(R.id.message_access_RefuseButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                httpServer.refuseConn(di);
                finish();
            }
        });
        //注册允许连接按钮
        findViewById(R.id.message_access_PermitButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                httpServer.permitConn(di);
                finish();
            }
        });
    }
}
