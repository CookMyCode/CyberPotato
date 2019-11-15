package com.codig.CyberPotato;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;

import com.codig.CyberPotato.utils.CommonUtils;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public class CyberPotato extends Application {

    // 内部储存根目录
    public String internalStorageDocRoot;
    //外部储存发布目录
    public String externalStorageDocRoot;
    //当前版本号
    public static String version;

    @Override
    public void onCreate() {
        super.onCreate();
        registerBroadcast();//注册广播监听
        internalStorageDocRoot=getApplicationContext().getFilesDir() + File.separator +"HTTP";
        externalStorageDocRoot=Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator +"HTTP";
        try {
            //获取当前版本号并赋值
            version=getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            version="unknown";
        }
        //检查版本和HTTP文件夹是否一致，用于版本升级时
        checkVersionAndUpdate(version);
    }

    /**
     * 检查配置文件中的版本号，版本号和当前不一致，删除相关数据文件重新导出
     */
    private void checkVersionAndUpdate(String currVer){
        //shareReferences存放版本号
        SharedPreferences sharedPreferences= getSharedPreferences("version",Context.MODE_PRIVATE);
        String ver=sharedPreferences.getString("version","undefined");
        //不存在字段或版本号不一致，更换新的版本号并删除HTTP文件
        if("undefined".equals(ver)||!currVer.equals(ver)){
            SharedPreferences.Editor editor= sharedPreferences.edit();
            editor.putString("version",currVer);
            editor.apply();
            deleteInternalStorageDocRoot();//删除数据文件
        }
    }

    /**
     * 删除内部储存的HTTP文件夹
     */
    private void deleteInternalStorageDocRoot(){
        File iDir = new File(internalStorageDocRoot);
        if (iDir.isDirectory()) {
            String[] fileList = iDir.list();
            for (String fileName : fileList) {
                File iFile = new File(internalStorageDocRoot + File.separator + fileName);
                if (iFile.isFile()) iFile.delete();
            }
            iDir.delete();
        }
    }

    /**
     * 生成发布根目录
     * @param exportRoot true:导出根目录到外部储存  false:生成根目录到内部储存
     */
    public void createDocRoot (boolean exportRoot) {
        String path=exportRoot?externalStorageDocRoot:internalStorageDocRoot;
        File file = new File(path);
        if (!file.isDirectory()) {
            file.mkdirs();
            CommonUtils.exportResourceFromRaw(this,R.raw.toaster,path+ File.separator +"toaster.html");
            CommonUtils.exportResourceFromRaw(this,R.raw.authentication,path+ File.separator +"authentication.html");
            CommonUtils.exportResourceFromRaw(this,R.raw.heartbeat,path+ File.separator +"heartBeat.js");
        }
    }

    //监听器列表
    private Set<CyberPotato.NetworkStatusListener> networkStatusListenerList=new HashSet<>();
    //通过广播监听网络状态变化
    private void registerBroadcast() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        filter.addAction("android.net.wifi.STATE_CHANGE");
        filter.addAction("android.net.wifi.WIFI_STATE_CHANGED");//TODO 尝试删除
        filter.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");
        getApplicationContext().registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                try {
                    ConnectivityManager connectionManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                    Method getMobileDataEnabledMethod = connectionManager.getClass().getDeclaredMethod("getMobileDataEnabled");
                    getMobileDataEnabledMethod.setAccessible(true);
                    if((Boolean) getMobileDataEnabledMethod.invoke(connectionManager)){
                        //连接到移动网络
                        updateServerStatusListener("mobile");
                    }
                    else{
                        String action = intent.getAction();
                        if("android.net.wifi.WIFI_AP_STATE_CHANGED".equals(action)){
                            //便携式热点的状态为：10---正在关闭；11---已关闭；12---正在开启；13---已开启
                            int state = intent.getIntExtra("wifi_state",  0);
                            if(state == 13){
                                //开启了热点
                                updateServerStatusListener("ap");
                            }
                            else if(state == 11){

                                NetworkInfo.State wifiState = connectionManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
                                if(wifiState != NetworkInfo.State.CONNECTED){
                                    //热点关闭且wifi和移动网络都关闭
                                    updateServerStatusListener("none");
                                }
                            }
                        }
                        else if(WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)){
                            NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                            if(NetworkInfo.State.CONNECTED.equals(info.getState())){
                                //连接到wifi局域网
                                updateServerStatusListener("wifi");
                            }
                        }
                        else{
                            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                            Method getWifiApStateMethod = wifiManager.getClass().getDeclaredMethod("getWifiApState");
                            getWifiApStateMethod.setAccessible(true);
                            int apState = (int) getWifiApStateMethod.invoke(wifiManager);
                            NetworkInfo.State wifiState = connectionManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
                            if(apState!=13&&wifiState != NetworkInfo.State.CONNECTED){
                                //所有网络都关闭
                                updateServerStatusListener("none");
                            }
                        }
                    }
                } catch (Exception ignored) { }
            }
        }, filter);
    }

    //网络状态变化监听器
    public interface NetworkStatusListener{
        void networkStatusChange(String status);
    }
    //注册器
    public void addNetworkStatusListener(CyberPotato.NetworkStatusListener networkStatusListener){
        networkStatusListenerList.add(networkStatusListener);
    }
    //监听器卸载
    public void removeNetworkStatusListener(CyberPotato.NetworkStatusListener networkStatusListener){
        networkStatusListenerList.remove(networkStatusListener);
    }

    //更新状态
    private void updateServerStatusListener(String status){
        for (CyberPotato.NetworkStatusListener ssl:networkStatusListenerList) {
            ssl.networkStatusChange(status);
        }
    }
}
