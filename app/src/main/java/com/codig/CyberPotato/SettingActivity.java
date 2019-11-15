package com.codig.CyberPotato;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.InputFilter;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.codig.CyberPotato.service.HttpServer;
import com.codig.CyberPotato.utils.PositiveNumberMaxInputFilter;

public class SettingActivity extends AppCompatActivity{

    private ImageButton saveButtonMask;

    private EditText editPort;
    private EditText editReceive;
    private EditText editSend;
    private ImageButton firewallModify;
    private ImageButton keepAliveModify;
    private ImageButton exportRootModify;

    private InputMethodManager imm;

    //来自MainActivity的服务器对象引用
    private HttpServer httpServer=MainActivity.httpServer;
    //每次保存实时更新，与服务器同步
    private String strPort;
    private String strRecv;
    private String strSend;
    private boolean uncheckMode;
    private boolean keepAlive;
    private boolean exportRoot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        Window window = this.getWindow();
        window.setGravity(Gravity.TOP);//置顶

        //赋值版本号
        ((TextView)findViewById(R.id.setting_Version)).setText("Ver:"+CyberPotato.version);

        //键盘
        imm = (InputMethodManager)this.getSystemService(Context.INPUT_METHOD_SERVICE);
        //实例化
        editPort=findViewById(R.id.setting_PortContent);
        editReceive=findViewById(R.id.setting_ReceiveContent);
        editSend=findViewById(R.id.setting_SendContent);
        firewallModify=findViewById(R.id.setting_FirewallModify);
        keepAliveModify=findViewById(R.id.setting_KeepAliveModify);
        exportRootModify=findViewById(R.id.setting_ExportRootModify);
        //实例化按钮
        saveButtonMask=findViewById(R.id.setting_SaveButtonMask);

        //获取来自服务器的参数
        strPort=String.valueOf(httpServer.getPortFromSettings());
        strRecv=httpServer.getRecvChunkMaxSize();
        strSend=httpServer.getSendChunkMaxSize();
        uncheckMode=httpServer.getFirewall();
        keepAlive=httpServer.getKeepAlive();
        exportRoot=httpServer.getExportRoot();
        //初始化到UI
        editPort.setText(strPort);
        editReceive.setText(strRecv+"MB");
        editSend.setText(strSend+"MB");
        if(uncheckMode)
            firewallModify.setImageResource(R.drawable.boolean_true);
        else
            firewallModify.setImageResource(R.drawable.boolean_false);
        if(keepAlive)
            keepAliveModify.setImageResource(R.drawable.boolean_true);
        else
            keepAliveModify.setImageResource(R.drawable.boolean_false);
        if(exportRoot)
            exportRootModify.setImageResource(R.drawable.boolean_true);
        else
            exportRootModify.setImageResource(R.drawable.boolean_false);
        firewallModify.setTag(uncheckMode);
        keepAliveModify.setTag(keepAlive);
        exportRootModify.setTag(exportRoot);

        //设置所有输入框不可聚焦
        editPort.setFocusable(false);
        editPort.setFocusableInTouchMode(false);
        editReceive.setFocusable(false);
        editReceive.setFocusableInTouchMode(false);
        editSend.setFocusable(false);
        editSend.setFocusableInTouchMode(false);
        //设置过滤器
        editPort.setFilters(new InputFilter[] {new PositiveNumberMaxInputFilter(0,65535,null)});
        editReceive.setFilters(new InputFilter[] {new PositiveNumberMaxInputFilter(1,99.9,"MB")});
        editSend.setFilters(new InputFilter[] {new PositiveNumberMaxInputFilter(1,99.9,"MB")});

        //监听关闭按钮
        findViewById(R.id.setting_CloseButton).setOnClickListener(onClickListener);
        //监听保存按钮
        findViewById(R.id.setting_SaveButton).setOnClickListener(onClickListener);
        //监听修改按钮
        ImageButton modifyButton;
        modifyButton = findViewById(R.id.setting_PortModify);
        modifyButton.setOnClickListener(onClickListener);
        modifyButton =findViewById(R.id.setting_ReceiveModify);
        modifyButton.setOnClickListener(onClickListener);
        modifyButton =findViewById(R.id.setting_SendModify);
        modifyButton.setOnClickListener(onClickListener);
        modifyButton =findViewById(R.id.setting_VersionInfo);
        modifyButton.setOnClickListener(onClickListener);
        firewallModify.setOnClickListener(onClickListener);
        keepAliveModify.setOnClickListener(onClickListener);
        exportRootModify.setOnClickListener(onClickListener);

        //监听键盘输入
        editPort.setOnKeyListener(onKeyListener);
        editReceive.setOnKeyListener(onKeyListener);
        editSend.setOnKeyListener(onKeyListener);

    }
    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String editPortContent = editPort.getText().toString();
            String editReceiveContent = editReceive.getText().toString();
            String editSendContent = editSend.getText().toString();
            //初始化EditText的编辑状态和内容
            if(v.getId()!=R.id.setting_PortModify) {
                if(editPortContent.equals("")||editPortContent.equals("0"))
                    editPort.setText(strPort);
                editPort.setFocusable(false);
                editPort.setFocusableInTouchMode(false);
            }
            if(v.getId()!=R.id.setting_ReceiveModify) {
                String num=editReceiveContent.split("MB")[0];
                if(num.equals(""))
                    editReceive.setText(strRecv+"MB");
                else if(!editReceiveContent.contains("MB"))
                    editReceive.setText(editReceiveContent+"MB");
                if(num.endsWith("."))
                    editReceive.setText(num+"0"+"MB");
                editReceive.setFocusable(false);
                editReceive.setFocusableInTouchMode(false);
            }
            if(v.getId()!=R.id.setting_SendModify) {
                String num=editSendContent.split("MB")[0];
                if(num.equals(""))
                    editSend.setText(strSend+"MB");
                else if(!editSendContent.contains("MB"))
                    editSend.setText(editSendContent+"MB");
                if(num.endsWith("."))
                    editSend.setText(num+"0"+"MB");
                editSend.setFocusable(false);
                editSend.setFocusableInTouchMode(false);
            }

            switch (v.getId()) {
                case R.id.setting_CloseButton:
                        finish();
                    break;
                case R.id.setting_PortModify:
                        //光标置尾
                        editPort.setSelection(editPortContent.length());
                        //打开软键盘
                        imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
                        //取消保存按钮蒙版
                        saveButtonMask.setVisibility(View.GONE);
                        //聚焦
                        editPort.setFocusableInTouchMode(true);
                        editPort.setFocusable(true);
                        editPort.requestFocus();
                    break;
                case R.id.setting_ReceiveModify:
                        //去掉MB字符，只显示小数
                        editReceive.setText(editReceiveContent.split("MB")[0]);
                        //光标置尾
                        editReceive.setSelection(editReceiveContent.split("MB")[0].length());
                        //打开软键盘
                        imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
                        //取消保存按钮蒙版
                        saveButtonMask.setVisibility(View.GONE);
                        //聚焦
                        editReceive.setFocusableInTouchMode(true);
                        editReceive.setFocusable(true);
                        editReceive.requestFocus();
                    break;
                case R.id.setting_SendModify:
                        //去掉MB字符，只显示小数
                        editSend.setText(editSendContent.split("MB")[0]);
                        //光标置尾
                        editSend.setSelection(editSendContent.split("MB")[0].length());
                        //打开软键盘
                        imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
                        //取消保存按钮蒙版
                        saveButtonMask.setVisibility(View.GONE);
                        //聚焦
                        editSend.setFocusableInTouchMode(true);
                        editSend.setFocusable(true);
                        editSend.requestFocus();
                    break;
                case R.id.setting_FirewallModify:
                case R.id.setting_KeepAliveModify:
                case R.id.setting_ExportRootModify:
                    if((boolean)v.getTag()){
                        ((ImageView)v).setImageResource(R.drawable.boolean_false);
                        v.setTag(false);
                    }
                    else{
                        ((ImageView)v).setImageResource(R.drawable.boolean_true);
                        v.setTag(true);
                    }
                    //关闭软键盘
                    if(imm.isActive())
                    {
                        imm.hideSoftInputFromWindow(v.getApplicationWindowToken(), 0);
                    }
                    //取消保存按钮蒙版
                    saveButtonMask.setVisibility(View.GONE);
                    break;
                case R.id.setting_VersionInfo:
                        //版本号，开发者信息等相关声明
                        startActivity(new Intent(SettingActivity.this,VersionActivity.class));
                    break;
                case R.id.setting_SaveButton:
                        //加上蒙版
                        saveButtonMask.setVisibility(View.VISIBLE);
                        //输入空内容时
                        editPortContent = editPort.getText().toString();
                        if(editPortContent.equals("")) {
                            editPortContent = strPort;
                        }
                        editReceiveContent = editReceive.getText().toString().split("MB")[0];
                        if(editReceiveContent.equals("")) {
                            editReceiveContent = strRecv;
                        }
                        editSendContent = editSend.getText().toString().split("MB")[0];
                        if(editSendContent.equals("")) {
                            editSendContent = strSend;
                        }

                        //通过来自MainActivity的引用更新到服务器内存并写入配置文件
                        httpServer.setPort(Integer.valueOf(editPortContent));
                        httpServer.setRecvChunkMaxSize(Double.parseDouble(editReceiveContent));
                        httpServer.setSendChunkMaxSize(Double.parseDouble(editSendContent));
                        httpServer.setFirewall((boolean)firewallModify.getTag());
                        httpServer.setKeepAlive((boolean)keepAliveModify.getTag());
                        httpServer.setExportRoot((boolean)exportRootModify.getTag());
                        httpServer.writeSettingsToConfigFile();
                        //更新本地内存
                        strPort=String.valueOf(httpServer.getPortFromSettings());
                        strRecv=httpServer.getRecvChunkMaxSize();
                        strSend=httpServer.getSendChunkMaxSize();
                        uncheckMode=httpServer.getFirewall();
                        keepAlive=httpServer.getKeepAlive();
                        exportRoot=httpServer.getExportRoot();
                        //更新UI
                        editPort.setText(strPort);
                        editReceive.setText(strRecv+"MB");
                        editSend.setText(strSend+"MB");
                    break;
                default:
                    break;
            }
        }
    };
    //监听EditText的键盘事件，遇到确定键完成编辑
    private View.OnKeyListener onKeyListener = new View.OnKeyListener() {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                //隐藏键盘
                //InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if(imm.isActive())
                {
                    imm.hideSoftInputFromWindow(v.getApplicationWindowToken(), 0);
                }

                EditText et=(EditText)v;
                String textContent=et.getText().toString();
                //如果为空还原原来的值
                if(textContent.equals("")||textContent.equals("0"))
                {
                    if(v.getId()==R.id.setting_PortContent)
                        textContent=strPort;
                    else if(v.getId()==R.id.setting_ReceiveContent)
                        textContent=strRecv;
                    else if(v.getId()==R.id.setting_SendContent)
                        textContent=strSend;
                }
                //结尾为小数点需要加0
                if(textContent.endsWith("."))
                    textContent=textContent + "0";
                //修改内容，末尾添加MB字符串
                if(v.getId()==R.id.setting_ReceiveContent||v.getId()==R.id.setting_SendContent) {
                    if(!textContent.contains("MB"))
                        textContent=textContent + "MB";
                }
                et.setText(textContent);
                //禁用编辑
                et.setFocusable(false);
                et.setFocusableInTouchMode(false);
                return true;
            }
            return false;
        }
    };
}
