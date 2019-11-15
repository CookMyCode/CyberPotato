package com.codig.CyberPotato;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.codig.CyberPotato.service.DeviceInfo;
import com.codig.CyberPotato.service.DynamicalMap;
import com.codig.CyberPotato.service.HttpServer;
import com.codig.CyberPotato.widget.AbstractGenericAdapter;

import java.util.Map;

public class DevicesActivity extends AppCompatActivity {

    private Map<String, DeviceInfo> devicesList=null;
    //来自MainActivity的服务器对象引用
    private HttpServer httpServer=MainActivity.httpServer;
    //滚动视图适配器
    private ListViewAdapter devicesListAdapter;

    //装饰控件
    ImageView decorativeLine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devices);
        Window window = this.getWindow();
        window.setGravity(Gravity.TOP);//置顶

        //获取设备列表引用
        devicesList=httpServer.getDevicesList();
        //实例化适配器
        devicesListAdapter=new ListViewAdapter(this,devicesList);
        //装载适配器
        ListView scrollView=findViewById(R.id.devices_ScrollView);
        scrollView.setAdapter(devicesListAdapter);
        //注册设备列表监听器
        httpServer.addDevicesStatusListener(devicesStatusListener);

        //装饰控件
        decorativeLine=findViewById(R.id.devices_DecorativeLine);
        //初始化
        if(devicesList.size()==0){
            decorativeLine.setVisibility(View.VISIBLE);
        }
        else{
            if(decorativeLine.getVisibility()==View.VISIBLE)
                decorativeLine.setVisibility(View.GONE);
        }

        //服务器按钮监听
        findViewById(R.id.devices_DeviceServer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(DevicesActivity.this,ServerInfoActivity.class);
                startActivity(intent);
            }
        });
        //注册关闭按钮
        findViewById(R.id.devices_CloseButton).setOnClickListener(new View.OnClickListener() {
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
            Message msg = new Message();
//            msg.obj=devicesList.get(key);
            devicesListUpdateHandler.sendMessage(msg);
        }
    };
    //消息句柄
    private Handler devicesListUpdateHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            devicesListAdapter.notifyDataSetChanged();
            if(devicesList.size()==0){
                decorativeLine.setVisibility(View.VISIBLE);
            }
            else{
                if(decorativeLine.getVisibility()==View.VISIBLE)
                    decorativeLine.setVisibility(View.GONE);
            }
            return false;
        }
    });
    //为设备按钮设置监听器
    private View.OnClickListener onClickListener=new View.OnClickListener(){
        @Override
        public void onClick(View v)
        {
            Intent intent=new Intent(DevicesActivity.this,DevicesInfoActivity.class);
            //将获取的DeviceInfo对象传递给下一个activity或对话框
            Bundle devInfo=new Bundle();
            devInfo.putSerializable("devInfo",(DeviceInfo)v.getTag());
            intent.putExtras(devInfo);
            startActivity(intent);
        }
    };

    private class ListViewAdapter extends AbstractGenericAdapter<Map> {

        //DevicesListAdapter
        private int devices_Line=R.id.devices_Line;
        private int devices_DeviceButton=R.id.devices_DeviceButton;
        private int devices_DeviceName=R.id.devices_DeviceName;

        private int devices_first_line=R.drawable.devices_first_line;
        private int devices_last_line=R.drawable.devices_last_line;
        private int devices_middle_line=R.drawable.devices_middle_line;

        ListViewAdapter(Context context, Map list){
            super(context, list, R.layout.activity_devices_devlist_item);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder,int position){
            // 取出对象
            DeviceInfo di=(DeviceInfo)getItem(position);

            // 适配器有缓存机制，数据有可能属于前一个被删除的Item的，每次取出都需要重新赋值
            if(position==0)
                ((ImageView)viewHolder.getView(devices_Line)).setImageResource(devices_first_line);// 设置装饰控件的图片
            else if(devicesList.size()-1==position)
                ((ImageView)viewHolder.getView(devices_Line)).setImageResource(devices_last_line);
            else
                ((ImageView)viewHolder.getView(devices_Line)).setImageResource(devices_middle_line);

            viewHolder.getView(devices_DeviceButton).setOnClickListener(onClickListener);
            viewHolder.getView(devices_DeviceButton).setTag(di);
            ((TextView)viewHolder.getView(devices_DeviceName)).setText(di.deviceName);
        }
    }
}
