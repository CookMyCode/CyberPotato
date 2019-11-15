package com.codig.CyberPotato;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.codig.CyberPotato.service.DynamicalMap;
import com.codig.CyberPotato.service.HttpServer;
import com.codig.CyberPotato.utils.CommonUtils;
import com.codig.CyberPotato.widget.AbstractGenericAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StorageActivity extends AppCompatActivity {

    private ListViewAdapter fileListAdapter;
    private ListViewAdapter receiveFileListAdapter;
    private HttpServer httpServer=MainActivity.httpServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_storage);
        Window window = this.getWindow();
        window.setGravity(Gravity.TOP);//置顶

        //实例化控件
        final ListView publicScrollView=findViewById(R.id.storage_public_ScrollView);
        final ListView receiveScrollView=findViewById(R.id.storage_receive_ScrollView);
        final ImageButton selectFileButton=findViewById(R.id.storage_public_SelectFile);
        final ImageButton changeFileListButton=findViewById(R.id.storage_public_ChangeFileListButton);
        //获取引用
        Map<String,String> fileList=httpServer.getFileList();
        Map<String,String> receiveFileList=httpServer.getReceiveFileList();
        //文件列表监听器
        httpServer.addFileListListener(fileListListener);
        httpServer.addReceiveFileListListener(receiveFileListListener);
        //实例化适配器
        fileListAdapter=new ListViewAdapter(this,fileList,R.drawable.file_icon_public,publicFileOnClickListener);
        receiveFileListAdapter=new ListViewAdapter(this,receiveFileList,R.drawable.file_icon_receive,receiveFileOnClickListener);
        //装载适配器
        publicScrollView.setAdapter(fileListAdapter);
        receiveScrollView.setAdapter(receiveFileListAdapter);

        //注册添加文件按钮
        findViewById(R.id.storage_public_SelectFile).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");//无类型限制
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);//多选
                //intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);//单选
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, 1);
            }
        });
        //注册切换文件列表按钮
        changeFileListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(v.getTag().equals("receiveFileListButton")){
                    v.setTag("publicFileListButton");
                    selectFileButton.setVisibility(View.GONE);
                    publicScrollView.setVisibility(View.GONE);
                    receiveScrollView.setVisibility(View.VISIBLE);
                    changeFileListButton.setImageResource(R.drawable.public_file_list_button);
                }
                else {
                    v.setTag("receiveFileListButton");
                    selectFileButton.setVisibility(View.VISIBLE);
                    publicScrollView.setVisibility(View.VISIBLE);
                    receiveScrollView.setVisibility(View.GONE);
                    changeFileListButton.setImageResource(R.drawable.receive_file_list_button);
                }
            }
        });
        //注册关闭按钮
        findViewById(R.id.storage_CloseButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
    @Override
    public void onDestroy(){
        httpServer.removeFileListListener(fileListListener);
        httpServer.removeReceiveFileListListener(receiveFileListListener);

        fileListUpdateHandler.removeCallbacksAndMessages(null);
        receiveFileListUpdateHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (data.getData() != null) {
                //文件单选
                try {
                    Uri uri = data.getData();
                    String path = CommonUtils.getPath(getApplicationContext(),uri);
                    httpServer.addRouter(path);
                } catch (Exception e) { }
            }
            else{
                //文件多选
                ClipData clipData = data.getClipData();
                if (clipData != null) {
                    List<String> routers=new ArrayList<>();
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        ClipData.Item item = clipData.getItemAt(i);
                        Uri uri = item.getUri();
                        String path = CommonUtils.getPath(getApplicationContext(),uri);
                        routers.add(path);
                    }
                    httpServer.addRouter(routers);
                }
            }

        }
    }
    private View.OnClickListener publicFileOnClickListener=new View.OnClickListener(){
        @Override
        public void onClick(View v)
        {
            Intent intent = new Intent(StorageActivity.this, PublicFileInfoActivity.class);
            Bundle fileInfo=new Bundle();
            fileInfo.putSerializable("fileInfo",(String)v.getTag());
            intent.putExtras(fileInfo);
            startActivity(intent);
        }
    };
    private View.OnClickListener receiveFileOnClickListener=new View.OnClickListener(){
        @Override
        public void onClick(View v)
        {
            Intent intent = new Intent(StorageActivity.this, ReceiveFileInfoActivity.class);
            Bundle fileInfo=new Bundle();
            fileInfo.putSerializable("fileInfo",(String)v.getTag());
            intent.putExtras(fileInfo);
            startActivity(intent);
        }
    };
    private DynamicalMap.DynamicalMapListener fileListListener=new DynamicalMap.DynamicalMapListener(){
        @Override
        public void dynamicalMapChange(String opt,Object key) {
            Message msg = new Message();
            fileListUpdateHandler.sendMessage(msg);
        }
    };
    private Handler fileListUpdateHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            fileListAdapter.notifyDataSetChanged();
            return false;
        }
    });
    private DynamicalMap.DynamicalMapListener receiveFileListListener=new DynamicalMap.DynamicalMapListener(){
        @Override
        public void dynamicalMapChange(String opt,Object key) {
            Message msg = new Message();
            receiveFileListUpdateHandler.sendMessage(msg);
        }
    };
    private Handler receiveFileListUpdateHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            receiveFileListAdapter.notifyDataSetChanged();
            return false;
        }
    });

    private class ListViewAdapter extends AbstractGenericAdapter<Map> {

        //FilesListAdapter
        private Context mContext;

        private int mItemButtonId=R.id.storage_file_list_item_ItemButton;
        private int mDecorativeViewId=R.id.storage_file_list_item_DecorativeFileIcon;
        private int mFileNameId=R.id.storage_file_list_item_FileName;

        private View mItemButton;
        private ImageView mDecorativeView;
        private TextView mFileName;

        private int mIconNormal;
        private int mIconHidden=R.drawable.file_icon_hidden;
        private int mIconInvalid=R.drawable.file_icon_invalid;

        private View.OnClickListener mOnClickListener;

        ListViewAdapter(Context context, Map list, int iconNormal, View.OnClickListener onClickListener){
            super(context, list, R.layout.activity_storage_file_list_item);
            mContext=context;
            mIconNormal=iconNormal;
            mOnClickListener=onClickListener;
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder,int position){
            // 取出对象
            String router=(String)getItem(position);
            String filePath=router.substring(2);
            String status=router.substring(0,2);
            String fileName=new File(filePath).getName();

            mItemButton=viewHolder.getView(mItemButtonId);
            mDecorativeView=(ImageView)viewHolder.getView(mDecorativeViewId);
            mFileName=(TextView)viewHolder.getView(mFileNameId);

            mItemButton.setOnClickListener(mOnClickListener);
            mItemButton.setTag(filePath);
            if(status.charAt(1)=='1'){
                //文件失效
                mDecorativeView.setImageResource(mIconInvalid);
                mFileName.setTextColor(mContext.getResources().getColor(R.color.colorDim));
            }
            else if(status.charAt(0)=='1'){
                //文件隐藏
                mDecorativeView.setImageResource(mIconHidden);
                mFileName.setTextColor(mContext.getResources().getColor(R.color.colorDim));
            }
            else{
                //文件有效且公开
                mDecorativeView.setImageResource(mIconNormal);
                mFileName.setTextColor(mContext.getResources().getColor(R.color.colorHighLight));
            }
            mFileName.setText(fileName);
        }
    }
}
