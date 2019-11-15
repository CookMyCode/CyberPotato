package com.codig.CyberPotato.deprecated;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.codig.CyberPotato.R;
import com.codig.CyberPotato.service.DeviceInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DevicesListAdapter extends BaseAdapter {

    private Map<String, DeviceInfo> devicesList;
    private LayoutInflater mInflater;//布局装载器对象
    private View.OnClickListener onClickListener;

    public DevicesListAdapter(Context context, Map<String,DeviceInfo> devicesList,View.OnClickListener onClickListener){
        mInflater = LayoutInflater.from(context);
        this.devicesList=devicesList;
        this.onClickListener=onClickListener;
    }
    @Override
    public int getCount()
    {
        return devicesList.size();
    }
    @Override
    public Object getItem(int position)
    {
        List<DeviceInfo> valuesList = new ArrayList(devicesList.values());
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
            convertView = mInflater.inflate(R.layout.activity_devices_devlist_item,null);
            //将视图对象引用集合保存到中布局的Tag中
            viewHolder = new ViewHolder();
            viewHolder.decorativeView = convertView.findViewById(R.id.devices_Line);
            viewHolder.deviceButton = convertView.findViewById(R.id.devices_DeviceButton);
            viewHolder.deviceName = convertView.findViewById(R.id.devices_DeviceName);
            convertView.setTag(viewHolder);
        }
        else{
            //如果缓存池中有对应的view缓存，则直接通过getTag取出viewHolder
            viewHolder = (ViewHolder) convertView.getTag();
        }
        // 取出对象
        DeviceInfo di=(DeviceInfo)getItem(position);
        // 适配器有缓存机制，数据有可能属于前一个被删除的Item的，每次取出都需要重新赋值
        if(position==0)
            viewHolder.decorativeView.setImageResource(R.drawable.devices_first_line);// 设置装饰控件的图片
        else if(devicesList.size()-1==position)
            viewHolder.decorativeView.setImageResource(R.drawable.devices_last_line);
        else
            viewHolder.decorativeView.setImageResource(R.drawable.devices_middle_line);

        viewHolder.deviceButton.setOnClickListener(onClickListener);
        viewHolder.deviceButton.setTag(di);
        viewHolder.deviceName.setText(di.deviceName);

        return convertView;
    }
    private class ViewHolder{
        ImageView decorativeView;
        ImageButton deviceButton;
        TextView deviceName;
    }
}
