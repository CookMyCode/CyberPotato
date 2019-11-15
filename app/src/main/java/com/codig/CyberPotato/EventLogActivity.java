package com.codig.CyberPotato;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;

import com.codig.CyberPotato.service.HttpServer;
import com.codig.CyberPotato.widget.AbstractGenericAdapter;

import java.util.List;

public class EventLogActivity extends AppCompatActivity {

    private List<String> logList = null;
    //来自MainActivity的服务器对象引用
    private HttpServer httpServer=MainActivity.httpServer;
    //适配器
    private ListViewAdapter logListAdapter;
    //标识ListView是否置底
    private boolean atListViewBottom=true;

    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eventlog);
        Window window = this.getWindow();
        window.setGravity(Gravity.TOP);//置顶

        logList =(List)httpServer.getLog();
        logListAdapter=new ListViewAdapter(this, logList);
        listView=findViewById(R.id.eventLog_ListView);
        listView.setAdapter(logListAdapter);
        listView.setOnScrollListener(onScrollListener);
        httpServer.addEventLogListener(eventLogListener);
        listView.setSelection(logList.size()-1);//置底显示

        //注册关闭按钮
        findViewById(R.id.eventLog_CloseButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
    @Override
    public void onDestroy(){
        //卸载监听器
        httpServer.removeEventLogListener(eventLogListener);
        updateEventLog.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
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
            logListAdapter.notifyDataSetChanged();
            if(atListViewBottom)
                listView.setSelection(logList.size()-1);
            return false;
        }
    });
    //监听是否到达日志列表的低端，如果在低端则有新日志时将滚动至底
    private AbsListView.OnScrollListener onScrollListener= new AbsListView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(AbsListView absListView, int i) { }
        @Override
        public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            if(firstVisibleItem + visibleItemCount == totalItemCount){
                //日志条目不足以填充整个ListView的情况
                if(firstVisibleItem==0){
                    atListViewBottom = true;
                    return;
                }

                View lastView = listView.getChildAt(listView.getChildCount() - 1);
                //getBottom获取控件底部到父元素顶部的像素
                if(lastView != null && lastView.getBottom() == listView.getHeight()) {
                    atListViewBottom = true;
                    return;
                }
            }
            atListViewBottom=false;
        }
    };

    private class ListViewAdapter extends AbstractGenericAdapter<List> {

//        LogListAdapter
        private int eventLog_log_item_Time=R.id.eventLog_log_item_Time;
        private int eventLog_log_item_Level=R.id.eventLog_log_item_Level;
        private int eventLog_log_item_Event=R.id.eventLog_log_item_Event;
        private int eventLog_log_item_Cause=R.id.eventLog_log_item_Cause;

        ListViewAdapter(Context context, List list){
            super(context, list, R.layout.activity_eventlog_log_item);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder,int position){
            // 取出对象
            String singleLog=(String)getItem(position);
            String[] contentArr=singleLog.split("[|]",5);
            String time=contentArr[0];
            String level=contentArr[1];
            String event=contentArr[2];
            String cause=contentArr[4];
            // 适配器有缓存机制，数据有可能属于前一个被删除的Item的，每次取出都需要重新赋值
            ((TextView)viewHolder.getView(eventLog_log_item_Time)).setText(time);
            ((TextView)viewHolder.getView(eventLog_log_item_Level)).setText("["+level+"]");
            ((TextView)viewHolder.getView(eventLog_log_item_Event)).setText(event);
            ((TextView)viewHolder.getView(eventLog_log_item_Cause)).setText(cause+"\n");
        }
    }
}
