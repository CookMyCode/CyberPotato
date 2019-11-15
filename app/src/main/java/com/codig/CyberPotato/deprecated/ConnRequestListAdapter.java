package com.codig.CyberPotato.deprecated;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.codig.CyberPotato.R;
import com.codig.CyberPotato.service.DeviceInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConnRequestListAdapter extends BaseAdapter {

    private Map<String, DeviceInfo> connRequestList;
    private LayoutInflater mInflater;//布局装载器对象
    private View.OnClickListener onClickListener;

    public ConnRequestListAdapter(Context context, Map<String,DeviceInfo> connRequestList, View.OnClickListener onClickListener){
        mInflater = LayoutInflater.from(context);
        this.connRequestList=connRequestList;
        this.onClickListener=onClickListener;
    }
    @Override
    public int getCount()
    {
        return connRequestList.size();
    }
    @Override
    public Object getItem(int position)
    {
        List<DeviceInfo> valuesList = new ArrayList(connRequestList.values());
        return valuesList.get(position);
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
            convertView = mInflater.inflate(R.layout.activity_message_connreqlist_item,null);
            //将视图对象引用集合保存到中布局的Tag中
            viewHolder = new ViewHolder();
            viewHolder.mailButton = convertView.findViewById(R.id.message_MailButton);
            viewHolder.decorativeText = convertView.findViewById(R.id.message_DecorativeText);
            viewHolder.ip = convertView.findViewById(R.id.message_IP);
            convertView.setTag(viewHolder);
        }
        else{
            //如果缓存池中有对应的view缓存，则直接通过getTag取出viewHolder
            viewHolder = (ViewHolder) convertView.getTag();
        }
        // 取出对象
        DeviceInfo di=(DeviceInfo)getItem(position);

        viewHolder.mailButton.setOnClickListener(onClickListener);
        viewHolder.mailButton.setTag(di);
        viewHolder.ip.setText(di.IP);

        return convertView;
    }
    private class ViewHolder{
        public ImageView mailButton;
        public TextView decorativeText;
        public TextView ip;
    }
}
