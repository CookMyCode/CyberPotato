package com.codig.CyberPotato.deprecated;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.codig.CyberPotato.R;

import java.util.List;

public class LogListAdapter extends BaseAdapter {

    private List<String> log;
    private LayoutInflater mInflater;//布局装载器对象

    public LogListAdapter(Context context, List<String> log){
        mInflater = LayoutInflater.from(context);
        this.log=log;
    }
    @Override
    public int getCount()
    {
        return log.size();
    }
    @Override
    public Object getItem(int position)
    {
        return log.get(position);
    }
    @Override
    public long getItemId(int position) {
        return position;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            //将xml转化为View
            convertView = mInflater.inflate(R.layout.activity_eventlog_log_item,null);
            //将视图对象引用集合保存到中布局的Tag中
            viewHolder = new ViewHolder();
            viewHolder.time = convertView.findViewById(R.id.eventLog_log_item_Time);
            viewHolder.level = convertView.findViewById(R.id.eventLog_log_item_Level);
            viewHolder.event = convertView.findViewById(R.id.eventLog_log_item_Event);
            viewHolder.cause = convertView.findViewById(R.id.eventLog_log_item_Cause);
            convertView.setTag(viewHolder);
        }
        else{
            //如果缓存池中有对应的view缓存，则直接通过getTag取出viewHolder
            viewHolder = (ViewHolder) convertView.getTag();
        }
        // 取出对象
        String singleLog=(String)getItem(position);
        String[] contentArr=singleLog.split("[|]",5);
        String time=contentArr[0];
        String level=contentArr[1];
        String event=contentArr[2];
        String cause=contentArr[4];
        // 适配器有缓存机制，数据有可能属于前一个被删除的Item的，每次取出都需要重新赋值
        viewHolder.time.setText(time);
        viewHolder.level.setText("["+level+"]");
        viewHolder.event.setText(event);
        viewHolder.cause.setText(cause+"\n");

        return convertView;
    }
    private class ViewHolder{
        TextView time;
        TextView level;
        TextView event;
        TextView cause;
    }
}
