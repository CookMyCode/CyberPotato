package com.codig.CyberPotato;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;

import com.codig.CyberPotato.service.DynamicalMap;
import com.codig.CyberPotato.service.HttpServer;

import java.io.File;
import java.util.Map;

public class PublicFileInfoActivity extends AppCompatActivity {

    private TextView fileNameTextView;
    private TextView fileStatusTextView;
    private TextView filePathTextView;
    private ImageButton openDirButtonMask;
    private ImageButton publishButtonMask;
    private ImageButton hideButtonMask;
    private ImageButton removeButton;
    private ImageButton removeButtonMask;

    private String fileHashCode;
    private String filePath;
    private boolean removeFileBoolean=false;

    private HttpServer httpServer=MainActivity.httpServer;
    private Map<String,String> fileList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_storage_public_file);
        Window window = this.getWindow();
        window.setGravity(Gravity.TOP);//置顶

        fileStatusTextView=findViewById(R.id.storage_public_file_Status);
        fileNameTextView=findViewById(R.id.storage_public_file_FileName);
        filePathTextView=findViewById(R.id.storage_public_file_PathContent);
        openDirButtonMask=findViewById(R.id.storage_public_file_OpenDirButtonMask);
        publishButtonMask=findViewById(R.id.storage_public_file_PublishButtonMask);
        hideButtonMask=findViewById(R.id.storage_public_file_HideButtonMask);
        removeButtonMask=findViewById(R.id.storage_public_file_RemoveButtonMask);
        removeButton=findViewById(R.id.storage_public_file_RemoveButton);

        fileList=httpServer.getFileList();
        httpServer.addFileListListener(fileListListener);

        //取出文件信息
        String fileInfo=(String)getIntent().getSerializableExtra("fileInfo");
        fileHashCode=String.valueOf(new File(fileInfo).hashCode());
        String router=fileList.get(fileHashCode);
        //初始化UI
        if(router!=null) {
            filePath = router.substring(2);
            String status = router.substring(0, 2);
            String fileName = new File(filePath).getName();
            fileNameTextView.setText("Name: " + fileName);
            filePathTextView.setText(filePath);
            if (status.charAt(1) == '1') {
                //文件失效
                fileStatusTextView.setText("Status: invalid");
                openDirButtonMask.setVisibility(View.VISIBLE);
                publishButtonMask.setVisibility(View.VISIBLE);
                hideButtonMask.setVisibility(View.VISIBLE);
            } else if (status.charAt(0) == '1') {
                //文件隐藏
                fileStatusTextView.setText("Status: hidden");
                openDirButtonMask.setVisibility(View.GONE);
                publishButtonMask.setVisibility(View.GONE);
                hideButtonMask.setVisibility(View.VISIBLE);
            } else {
                //文件有效且公开
                fileStatusTextView.setText("Status: public");
                openDirButtonMask.setVisibility(View.GONE);
                publishButtonMask.setVisibility(View.VISIBLE);
                hideButtonMask.setVisibility(View.GONE);
            }
        }
        else {
            fileNameTextView.setText("Name: --");
            filePathTextView.setText("--");
            fileStatusTextView.setText("Status: --");
            openDirButtonMask.setVisibility(View.VISIBLE);
            publishButtonMask.setVisibility(View.VISIBLE);
            hideButtonMask.setVisibility(View.VISIBLE);
            removeButtonMask.setVisibility(View.VISIBLE);
        }

        //按钮监听
        findViewById(R.id.storage_public_file_OpenDirButton).setOnClickListener(onClickListener);
        findViewById(R.id.storage_public_file_PublishButton).setOnClickListener(onClickListener);
        findViewById(R.id.storage_public_file_HideButton).setOnClickListener(onClickListener);
        removeButton.setOnClickListener(onClickListener);

        //注册关闭按钮
        findViewById(R.id.storage_public_file_CloseButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
    @Override
    public void onDestroy(){
        if(removeFileBoolean==true) httpServer.deleteRouter(filePath);
        httpServer.removeFileListListener(fileListListener);
        filesListUpdateHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private View.OnClickListener onClickListener=new View.OnClickListener(){
        @Override
        public void onClick(View v){
            switch (v.getId()){
                case R.id.storage_public_file_OpenDirButtonMask:
                    //TODO
                    break;
                case R.id.storage_public_file_PublishButton:
                    httpServer.publishRouter(filePath);
                    break;
                case R.id.storage_public_file_HideButton:
                    httpServer.hideRouter(filePath);
                    break;
                case R.id.storage_public_file_RemoveButton:
                    if(v.getTag().equals("remove")) {
                        v.setTag("undo");
                        removeFileBoolean=true;
                        removeButton.setImageResource(R.drawable.undo_file_button);
                        fileNameTextView.setText("Name: --");
                        filePathTextView.setText("--");
                        fileStatusTextView.setText("Status: --");
                        openDirButtonMask.setVisibility(View.VISIBLE);
                        publishButtonMask.setVisibility(View.VISIBLE);
                        hideButtonMask.setVisibility(View.VISIBLE);
                    }
                    else{
                        v.setTag("remove");
                        removeFileBoolean=false;
                        removeButton.setImageResource(R.drawable.remove_file_button);
                        String router=fileList.get(fileHashCode);
                        if(router!=null) {
                            filePath = router.substring(2);
                            String fileName = new File(filePath).getName();
                            fileNameTextView.setText("Name: " + fileName);
                            filePathTextView.setText(filePath);
                            String status = router.substring(0, 2);
                            if (status.charAt(1) == '1') {
                                //文件失效
                                fileStatusTextView.setText("Status: invalid");
                                openDirButtonMask.setVisibility(View.VISIBLE);
                                publishButtonMask.setVisibility(View.VISIBLE);
                                hideButtonMask.setVisibility(View.VISIBLE);
                            } else if (status.charAt(0) == '1') {
                                //文件隐藏
                                fileStatusTextView.setText("Status: hidden");
                                openDirButtonMask.setVisibility(View.GONE);
                                publishButtonMask.setVisibility(View.GONE);
                                hideButtonMask.setVisibility(View.VISIBLE);
                            } else {
                                //文件有效且公开
                                fileStatusTextView.setText("Status: public");
                                openDirButtonMask.setVisibility(View.GONE);
                                publishButtonMask.setVisibility(View.VISIBLE);
                                hideButtonMask.setVisibility(View.GONE);
                            }
                        }
                        else {
                            fileNameTextView.setText("Name: --");
                            filePathTextView.setText("--");
                            fileStatusTextView.setText("Status: --");
                            openDirButtonMask.setVisibility(View.VISIBLE);
                            publishButtonMask.setVisibility(View.VISIBLE);
                            hideButtonMask.setVisibility(View.VISIBLE);
                            removeButtonMask.setVisibility(View.VISIBLE);
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private DynamicalMap.DynamicalMapListener fileListListener=new DynamicalMap.DynamicalMapListener(){
        @Override
        public void dynamicalMapChange(String opt,Object key) {
            filesListUpdateHandler.sendMessage(new Message());
        }
    };
    private Handler filesListUpdateHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            //更新UI
            String router=fileList.get(fileHashCode);
            if(router!=null) {
                String status = router.substring(0, 2);
                if (status.charAt(1) == '1') {
                    //文件失效
                    fileStatusTextView.setText("Status: invalid");
                    openDirButtonMask.setVisibility(View.VISIBLE);
                    publishButtonMask.setVisibility(View.VISIBLE);
                    hideButtonMask.setVisibility(View.VISIBLE);
                } else if (status.charAt(0) == '1') {
                    //文件隐藏
                    fileStatusTextView.setText("Status: hidden");
                    openDirButtonMask.setVisibility(View.GONE);
                    publishButtonMask.setVisibility(View.GONE);
                    hideButtonMask.setVisibility(View.VISIBLE);
                } else {
                    //文件有效且公开
                    fileStatusTextView.setText("Status: public");
                    openDirButtonMask.setVisibility(View.GONE);
                    publishButtonMask.setVisibility(View.VISIBLE);
                    hideButtonMask.setVisibility(View.GONE);
                }
            }
            else {
                fileNameTextView.setText("Name: --");
                filePathTextView.setText("--");
                fileStatusTextView.setText("Status: --");
                openDirButtonMask.setVisibility(View.VISIBLE);
                publishButtonMask.setVisibility(View.VISIBLE);
                hideButtonMask.setVisibility(View.VISIBLE);
                removeButtonMask.setVisibility(View.VISIBLE);
            }
            return false;
        }
    });
}
