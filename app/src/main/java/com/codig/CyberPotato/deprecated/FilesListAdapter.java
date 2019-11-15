package com.codig.CyberPotato.deprecated;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.codig.CyberPotato.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FilesListAdapter extends BaseAdapter {

    private Map<String,String> fileList;
    private Context context;
    private LayoutInflater mInflater;//布局装载器对象
    private View.OnClickListener onClickListener;
    private int iconNormal;
    private int iconHidden;
    private int iconInvalid;

    public FilesListAdapter(Context context, Map<String,String> fileList,int iconNormal, View.OnClickListener onClickListener){
        this.context=context;
        mInflater = LayoutInflater.from(context);
        this.fileList=fileList;
        this.onClickListener=onClickListener;
        this.iconNormal=iconNormal;
        this.iconHidden= R.drawable.file_icon_hidden;
        this.iconInvalid=R.drawable.file_icon_invalid;
    }
    @Override
    public int getCount()
    {
        return fileList.size();
    }
    @Override
    public Object getItem(int position)
    {
        List<String> valuesList = new ArrayList(fileList.values());
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
            convertView = mInflater.inflate(R.layout.activity_storage_file_list_item,null);
            //将视图对象引用集合保存到中布局的Tag中
            viewHolder = new ViewHolder();
            viewHolder.itemButton = convertView.findViewById(R.id.storage_file_list_item_ItemButton);
            viewHolder.DecorativeView = convertView.findViewById(R.id.storage_file_list_item_DecorativeFileIcon);
            viewHolder.fileName = convertView.findViewById(R.id.storage_file_list_item_FileName);
            convertView.setTag(viewHolder);
        }
        else{
            //如果缓存池中有对应的view缓存，则直接通过getTag取出viewHolder
            viewHolder = (ViewHolder) convertView.getTag();
        }
        // 取出对象
        String router=(String)getItem(position);
        String filePath=router.substring(2);
        String status=router.substring(0,2);
        String fileName=new File(filePath).getName();

        viewHolder.itemButton.setOnClickListener(onClickListener);
        viewHolder.itemButton.setTag(filePath);

        if(status.charAt(1)=='1'){
            //文件失效
            viewHolder.DecorativeView.setImageResource(iconInvalid);
            viewHolder.fileName.setTextColor(context.getResources().getColor(R.color.colorDim));
        }
        else if(status.charAt(0)=='1'){
            //文件隐藏
            viewHolder.DecorativeView.setImageResource(iconHidden);
            viewHolder.fileName.setTextColor(context.getResources().getColor(R.color.colorDim));
        }
        else{
            //文件有效且公开
            viewHolder.DecorativeView.setImageResource(iconNormal);
            viewHolder.fileName.setTextColor(context.getResources().getColor(R.color.colorHighLight));
        }
        viewHolder.fileName.setText(fileName);

        return convertView;
    }
    private class ViewHolder{
        public ImageView itemButton;
        public ImageView DecorativeView;
        public TextView fileName;
    }
}
