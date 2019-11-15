package com.codig.CyberPotato.widget;

import android.content.Context;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.List;
import java.util.Map;

public abstract class AbstractGenericAdapter<T> extends BaseAdapter {
    private T mList;
    private LayoutInflater mInflater;
    private int mItemLayoutId;

    public AbstractGenericAdapter(Context context, T list, int itemLayoutId){
        mInflater = LayoutInflater.from(context);
        mList=list;
        mItemLayoutId=itemLayoutId;
    }

    @Override
    public int getCount() {
        if(mList instanceof Map)
            return ((Map) mList).size();
        else if(mList instanceof List)
            return ((List) mList).size();
        else
            return 0;
    }

    @Override
    public Object getItem(int position) {
        if(mList instanceof Map)
            return ((Map) mList).values().toArray()[position];
        else if(mList instanceof List)
            return ((List) mList).get(position);
        else
            return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    //对item进行相应的参数渲染
    public abstract void onBindViewHolder(ViewHolder viewHolder,int position);

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            //将xml转化为View
            convertView = mInflater.inflate(mItemLayoutId,null);
            //将视图对象引用集合保存到中布局的Tag中
            viewHolder = new ViewHolder(convertView);
            onBindViewHolder(viewHolder,position);
            convertView.setTag(viewHolder);
        }
        else{
            //如果缓存池中有对应的view缓存，则直接通过getTag取出viewHolder
            viewHolder = (ViewHolder) convertView.getTag();
            onBindViewHolder(viewHolder,position);
        }
        return convertView;
    }

    protected class ViewHolder{
        private SparseArray<View> mIdMap;
        private View mConvertView;

        ViewHolder(View convertView){
            mConvertView=convertView;
            mIdMap=new SparseArray<>();
        }

        public View getView(int id){
            View v=mIdMap.get(id);
            if(v==null) {
                View tmp=mConvertView.findViewById(id);
                mIdMap.put(id,tmp);
                return tmp;
            }
            return v;
        }
    }
}
