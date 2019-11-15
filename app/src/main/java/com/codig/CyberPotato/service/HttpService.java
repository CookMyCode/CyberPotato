package com.codig.CyberPotato.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;

import com.codig.CyberPotato.R;

public class HttpService extends Service {

    private HttpServer httpServer;
    private HttpServiceBinder binder=new HttpServiceBinder();
    private static MediaPlayer mMediaPlayer;//空音频保活
    private static Context applicationContext;

    //Binder类用于存放服务器对象
    public class HttpServiceBinder extends Binder {
        public HttpServer getHttpServer(){
            return httpServer;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        applicationContext=getApplicationContext();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    //将binder传递给前台
    @Override
    public IBinder onBind(Intent intent) {
        String iDocRoot=intent.getStringExtra("iDocRoot");
        String eDocRoot=intent.getStringExtra("eDocRoot");
        String appDir=intent.getStringExtra("appDir");
        String downloadDir=intent.getStringExtra("sDownloadDir");
        //启动服务器线程
        httpServer = new HttpServer(iDocRoot,eDocRoot,appDir,downloadDir,"2.0"){
            //重写后台保活方法，解耦服务器
            @Override
            protected void setHttpServerKeepAlive(boolean status){
                HttpService.setHttpServerKeepAlive(status);
            }
        };
        httpServer.setDaemon(true);
        httpServer.start();

        //开启前台提示
        //针对8.1及以上版本的Bad notification for startForeground异常
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            //注册通道
            String channelId = "codig.cyberpotato.channel";
            String channelName = "CyberPotatoStartForeground";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
            //前台提示
            Notification notification = new Notification.Builder(this)
                    .setContentTitle("CyberPotato提醒")
                    .setContentText("后台正在运行http服务器")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setChannelId(channelId)
                    .build();
            startForeground(100,notification);
        }
        else{
            startForeground(100, new Notification());
        }

        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        httpServer.exitProcess();
        stopForeground(true);
        setHttpServerKeepAlive(false);
        super.onDestroy();
    }

    //服务保活
    static void setHttpServerKeepAlive(boolean status){
        if(status){
            if(mMediaPlayer==null) {
                mMediaPlayer = MediaPlayer.create(applicationContext, R.raw.silent);
                mMediaPlayer.setLooping(true);
                mMediaPlayer.start();
            }
        }
        else{
            if(mMediaPlayer!=null) {
                mMediaPlayer.stop();
                mMediaPlayer.release();
                mMediaPlayer=null;
            }
        }
    }
}
