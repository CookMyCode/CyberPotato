package com.codig.CyberPotato.service;

import java.io.IOException;
import java.net.Socket;

/**
 * 对设备进行管理
 * 
 * @author codig_work@outlook.com
 * 
 */
class DevicesManger extends AbstractDevicesManager{

    //单例引用
    private static DevicesManger sInstance;
    //周期性更新设备列表的线程
    private static Thread sUpdateThread;
    //路径
    private String mDocRoot;
    private String mDownloadDir;

    //单例
    private DevicesManger(){
        if(sInstance!=null)
            throw new RuntimeException("This class can't be reflected");
    }

    public static DevicesManger getInstance() throws RuntimeException{
        if(sInstance==null){
            synchronized(Firewall.class){
                if(sInstance==null){
                    sInstance=new DevicesManger();
                }
            }
        }
        return sInstance;
    }

    //处理设备的请求
    boolean acceptConn(Socket socket) {
        //获取设备信息
        String ip = socket.getRemoteSocketAddress().toString().substring(1).split(":")[0];
        DeviceInfo di = getDeviceInfo(ip);
        if(di==null){
            return false;
        }
        //更新最新的套接字
        di.lastSocket = socket;
        
        //将分配一个线程处理本次请求，开启防火墙的状态下线程会被阻塞
        ThreadListManger.add(di,socket,mDocRoot,mDownloadDir);
        return true;
    }

    //生成设备信息
    private DeviceInfo getDeviceInfo(String ip){
        //向防火墙获取证书
        DeviceInfo di=Firewall.getDevicePermit(ip);
        if(di == null){
            return null;
        }
        //更新deviceInfo的时间戳
        di.timeStamp=System.currentTimeMillis();
        //设备第一次访问
        if(di.permit!=null && di.permit==false){
            di.IP=ip;//第一次访问时记录ip
        	//无防火墙模式
        	if(!Firewall.isActive()) {
	        	di.permit=true;
	            di.connStatus=true;
	            //生成设备名
	            di.deviceName=createDeviceName(ip.hashCode());
	            //添加到连接列表
	            putDeviceToDevicesList(ip, di);
	            //日志
	            String cause="info:\n["+di.deviceName+":"+di.IP+"]";
	            updateEventLogListener("NORM","access is granted|Permit",cause);
        	}
        }
        return di;
    }

    //启动更新线程
    void startUpdateThread(String docRoot, String downloadDir){
        mDocRoot=docRoot;
        mDownloadDir=downloadDir;
        if(sUpdateThread == null) {
            sUpdateThread = new DevicesListUpdateThread();
            sUpdateThread.setDaemon(true);
        }
        if(!sUpdateThread.isAlive())
            sUpdateThread.start();
    }

    //更新root路径，在切换root路径时使用
    void setDocRoot(String docRoot){
        mDocRoot=docRoot;
    }

    //断开该设备的连接
    void ceaseConn(DeviceInfo di){
        DeviceInfo deviceInfo=getDeviceFromDevicesList(di.IP);
        if(deviceInfo==null){
            //日志
            updateEventLogListener("ALERT","no such device|failed","错误码:D0001");
            return;
        }
        destroyAllThread(deviceInfo);
        deviceInfo.connStatus=false;
        removeDeviceFromDevicesList(deviceInfo.IP);
        //日志
        String cause="info:\n["+di.deviceName+":"+di.IP+"]";
        updateEventLogListener("NORM","disconnection|cease",cause);
    }

    //允许该设备连接
    void permitConn(DeviceInfo di){
        DeviceInfo deviceInfo=getDeviceFromConnRequestList(di.IP);
        if(deviceInfo==null){
            //日志
            updateEventLogListener("ALERT","no such device|failed","错误码:D0101");
            return;
        }
        try {
            //检查连接是否仍然可用
            deviceInfo.lastSocket.sendUrgentData(0xFF);
            deviceInfo.timeStamp=System.currentTimeMillis();//防止此时刚好处于检查时间戳操作
            deviceInfo.connStatus=true;
            //生成设备名
            deviceInfo.deviceName=createDeviceName(di.IP.hashCode());
            putDeviceToDevicesList(deviceInfo.IP, deviceInfo);
            removeDeviceFromConnRequestList(deviceInfo.IP);
            Firewall.unlock(deviceInfo);
            //日志
            String cause="info:\n["+deviceInfo.deviceName+":"+deviceInfo.IP+"]";
            updateEventLogListener("NORM","access is granted|granted",cause);
        } catch (IOException e) {
            try {
                if(deviceInfo.lastSocket!=null) deviceInfo.lastSocket.close();
            } catch (IOException ignored) {
            }
            deviceInfo.connRequestLock.notify();
            //日志
            updateEventLogListener("ALERT","request timeout|timeout","错误码:D0102");
        }
    }

    //不允许该设备连接
    void refuseConn(DeviceInfo di){
        DeviceInfo deviceInfo=getDeviceFromConnRequestList(di.IP);
        if(deviceInfo==null){
            //日志
            updateEventLogListener("ALERT","no such device|failed","错误码:D0201，请求连接可能已超时");
            return;
        }
        removeDeviceFromConnRequestList(deviceInfo.IP);
        Firewall.unlock(deviceInfo);
    }

    //销毁所有设备的线程池
    void destroyAllConn(){
        clearDevicesList();
        clearConnRequestList();
    }

    //根据IP字符串的hashcode生成设备名
    private String createDeviceName(int n) {
        long l= Integer.MAX_VALUE + 1L + n;
        char []b = {'0','1','2','3','4','5','6','7'
                ,'8','9','A','B','C','D','E'
                ,'F','G','H','I','J','K','L'
                ,'M','N','O','P','Q','R','S'
                ,'T','U','V','W','X','Y','Z'};
        StringBuilder sb = new StringBuilder();
        while(l != 0){
            sb = sb.append(b[(int)(l%36)]);
            l = l/36;
        }
        while(sb.length()<7){
            sb=sb.append("0");
        }
        return sb.reverse().toString();
    }


    /**
     * 新线程，维护设备连接表，将连接超时的设备从表中去除
     */
    private class DevicesListUpdateThread extends Thread
    {
        public void run()
        {
            for(;;)
            {
                try
                {
                    sleep(3000);
                }
                catch (InterruptedException ignored) { }
                long st=System.currentTimeMillis();
                //遍历设备表，去除连接超时的设备//TODO 需要改为同步，修改DynamicalMap
                for(DeviceInfo di:getValuesFromDevicesList())
                {
                    if(st-di.timeStamp>4000)
                    {
                        di.connStatus=false;
                        destroyAllThread(di);
                        removeDeviceFromDevicesList(di.IP);
                        //日志
                        String cause="info:\n["+di.deviceName+":"+di.IP+"]";
                        updateEventLogListener("NORM","access timeout|timeout",cause);
                    }
                }
            }
        }
    }

    private void updateEventLogListener(String level,String event,String cause){
        EventLogManager.updateEventLogListener(level,event,cause);
    }

    //注册连接请求队列监听器
    void addConnRequestListListener(DynamicalMap.DynamicalMapListener connRequestListListener){
        ((DynamicalMap<?, ?>)getConnRequestList()).addDynamicalMapListener(connRequestListListener);
    }
    //卸载连接请求队列监听器
    void removeConnRequestListListener(DynamicalMap.DynamicalMapListener connRequestListListener){
        ((DynamicalMap<?, ?>)getConnRequestList()).removeDynamicalMapListener(connRequestListListener);
    }


    // 注册设备连接情况监听器
    void addDevicesStatusListener(DynamicalMap.DynamicalMapListener devicesStatusListener){
        ((DynamicalMap<?, ?>)getDevicesList()).addDynamicalMapListener(devicesStatusListener);
    }
    // 卸载设备连接情况监听器
    void removeDevicesStatusListener(DynamicalMap.DynamicalMapListener devicesStatusListener){
        ((DynamicalMap<?, ?>)getDevicesList()).removeDynamicalMapListener(devicesStatusListener);
    }
    

//    @Deprecated //TODO 测试用
//    void showAllDeviceInfo() {
//    	System.out.println("****空闲线程****");
//    	java.util.Iterator<Thread> i=ThreadListManger.getIdleThreadsList().iterator();
//    	while(i.hasNext())
//        {
//        	System.out.print(i.next()+"|");
//        }
//    	System.out.println();
//    	System.out.println("****连接列表****");
//    	for(DeviceInfo di:getValuesFromDevicesList())
//        {
//        	System.out.println("设备："+di.IP);
//        	System.out.println("活跃线程：");
//            for (Thread t : di.threadsList) {
//            	System.out.print(t+"|");
//            }
//            System.out.println();
//        }
//    	System.out.println("****请求队列****");
//    	for(DeviceInfo di:getConnRequestList().values())
//        {
//        	System.out.print("设备："+di.IP);
//        	System.out.print(" 活跃线程：");
//            for (Thread t : di.threadsList) {
//            	System.out.print(t+"|");
//            }
//            System.out.println();
//        }
//    }
}
