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
import android.widget.ListView;
import android.widget.TextView;

import com.codig.CyberPotato.service.DeviceInfo;
import com.codig.CyberPotato.service.DynamicalMap;
import com.codig.CyberPotato.service.HttpServer;
import com.codig.CyberPotato.widget.AbstractGenericAdapter;

import java.util.Map;

public class MessageActivity extends AppCompatActivity {

    //来自MainActivity的服务器对象引用
    private HttpServer httpServer=MainActivity.httpServer;
    //获取请求连接列表引用
    private Map<String, DeviceInfo> connReqList=httpServer.getConnRequestList();
    //滚动视图适配器
    private ListViewAdapter connRequestListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);
        Window window = this.getWindow();
        window.setGravity(Gravity.TOP);//置顶

        //实例化适配器
        connRequestListAdapter=new ListViewAdapter(this,connReqList);
        //装载适配器
        ListView scrollView=findViewById(R.id.message_ScrollView);
        scrollView.setAdapter(connRequestListAdapter);
        //注册设备列表监听器
        httpServer.addConnRequestListListener(connRequestListListener);
        //注册关闭按钮
        findViewById(R.id.message_CloseButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
    @Override
    public void onDestroy(){
        MainActivity.reminder=null;
        httpServer.removeConnRequestListListener(connRequestListListener);
        connRequestListUpdateHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
    //设备连接情况监听器
    private DynamicalMap.DynamicalMapListener connRequestListListener=new DynamicalMap.DynamicalMapListener(){
        @Override
        public void dynamicalMapChange(String opt,Object key)
        {
            Message msg = new Message();
//            msg.obj=connReqList.get(key);
            connRequestListUpdateHandler.sendMessage(msg);
        }
    };
    //消息句柄
    private Handler connRequestListUpdateHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            connRequestListAdapter.notifyDataSetChanged();
            return false;
        }
    });
    //为设备按钮设置监听器
    private View.OnClickListener onClickListener=new View.OnClickListener(){
        @Override
        public void onClick(View v)
        {
            Intent intent=new Intent(MessageActivity.this,AccessActivity.class);
            //将获取的DeviceInfo对象传递给下一个activity或对话框
            Bundle devInfo=new Bundle();
            devInfo.putSerializable("devInfo",(DeviceInfo)v.getTag());
            intent.putExtras(devInfo);
            startActivity(intent);
        }
    };

    private class ListViewAdapter extends AbstractGenericAdapter<Map> {

        //ConnRequestListAdapter
        private int message_MailButton=R.id.message_MailButton;
        private int message_IP=R.id.message_IP;

        ListViewAdapter(Context context, Map list){
            super(context, list, R.layout.activity_message_connreqlist_item);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder,int position){
            // 取出对象
            DeviceInfo di=(DeviceInfo)getItem(position);

            viewHolder.getView(message_MailButton).setOnClickListener(onClickListener);
            viewHolder.getView(message_MailButton).setTag(di);
            ((TextView)viewHolder.getView(message_IP)).setText(di.IP);
        }
    }
}
