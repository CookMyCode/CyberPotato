package com.codig.CyberPotato.service;

/**
 * 防火墙，对访问请求进行修改过滤
 * 
 * @author codig_work@outlook.com
 * 
 */
public class Firewall extends AbstractDevicesManager{

    //单例引用
    private static Firewall sInstance;
    //防火墙是否开启
    private static boolean isActive=false;
    //请求未处理超时时间
    private long mRequestTimeout=0;

    private Firewall(){
        if(sInstance!=null)
            throw new RuntimeException("This class can't be reflected");
    }

    public static Firewall getInstance() throws RuntimeException{
        if(!isActive){
            throw new RuntimeException("Firewall not available");
        }
        if(sInstance==null){
            synchronized(Firewall.class){
                if(sInstance==null){
                    sInstance=new Firewall();
                }
            }
        }
        return sInstance;
    }

    /**
     * 开启或关闭防火墙
     */
    static void setFireWallState(boolean state){
        isActive=state;
        if(isActive){
            getInstance();
        }
        else{
            sInstance=null;
            System.gc();
        }
    }

    /**
     * 获取防火墙状态
     */
    static boolean isActive(){
        return isActive;
    }

    /**
     * 对请求行参数进行修正过滤
     */
    static void requestFilter(DeviceInfo di, IHttpRequestLine requestLine){
    	String uri=requestLine.getURI();
    	if(uri==null)
    		return;
        uri=uri.split("[?]")[0];
        Boolean permit=di.permit;
        //限制用户访问
        if(permit==null || !permit)
        {
            //在未获取访问权限的时，用户只能申请首页和验证页
            if("\\toaster.html".equals(uri)||"/toaster.html".equals(uri))
            {
                requestLine.setMethod("GET");
                requestLine.setURI("/toaster.html");
            }
            else if("\\fileList.html".equals(uri)||"/fileList.html".equals(uri))
            {
                requestLine.setMethod("403");
                requestLine.setURI(null);
            }
            else if("\\heartBeat".equals(uri)||"/heartBeat".equals(uri))
            {
                requestLine.setMethod("403");
                requestLine.setURI(null);
            }
            else
            {
                requestLine.setMethod("GET");
                requestLine.setURI("/authentication.html");
            }
        }
        else
        {
            //设置首页快捷方式
            if("\\".equals(uri)||"/".equals(uri))
            {
                requestLine.setMethod("GET");
                requestLine.setURI("/toaster.html");
            }
        }
    }

    /**
     * 生成设备信息，分发证书  null：等待用户授权；false：第一次请求；true：允许
     */
    static DeviceInfo getDevicePermit(String ip){
        DeviceInfo deviceInfo;
        if((deviceInfo=getDeviceFromDevicesList(ip))!=null) {
            //IP在允许连接表中
            deviceInfo.timeStamp=System.currentTimeMillis();
            deviceInfo.permit=true;//更新证书
        }
        else if((deviceInfo=getDeviceFromConnRequestList(ip))!=null) {
            //IP在请求队列中
            deviceInfo.permit=null;//更新证书
        }
        else {
            //IP不在允许连接表中，也不在请求队列中，即第一次访问
            deviceInfo = new DeviceInfo();
            deviceInfo.permit = false;//更新证书
        }
        return deviceInfo;
    }
    
    /**
     * 解除阻塞
     */
    static void unlock(DeviceInfo di) {
    	synchronized (di.connRequestLock)
        {
    		di.connRequestLock.notify();
        }
    }

    /**
     * 阻塞设备访问，加入请求队列，等待用户允许或拒绝
     */
    void blockAccess(DeviceInfo di, IHttpRequestLine requestLine) throws InterruptedException {
        ServerThread currentThread = (ServerThread)Thread.currentThread();
        //用户无传送内容，未知操作
        if(requestLine.getMethod()==null)
        {
            return;
        }
        String ip = di.IP;
        if(di.permit ==null)
        {
            if("/toaster.html".equals(requestLine.getURI()))
            {
                //客户端在请求队列中仍然继续请求
                //清除之前请求的线程，设置新的线程
                //加锁，防止出现在用户重复申请时，新线程在遍历释放旧线程出现的ConcurrentModificationException
                //冲突主要在于每次线程结束都有一次remove操作
                destroyOtherThread(di);
                //日志
                String cause="["+ di.IP+"]重复请求连接，警惕来自此IP的恶意行为";
                updateEventLogListener("ALERT","request repeatedly|repeat",cause);
                synchronized (di.connRequestLock)
                {
                    //可设置超时自动拒绝
                    di.connRequestLock.wait(mRequestTimeout);
                    //线程因调用destroyCurrentThread而释放该锁时
                    if(currentThread.getSocket()==null|| currentThread.getSocket().isClosed()){
                        removeCurrentThreadFromThreadList(di);
                        requestLine.setMethod(null);//置null，因为socket已被释放
                        return;
                    }
                }
                //如果拒绝了请求，返回403报文
                if(!deviceInDevicesList(ip)) {
                    removeDeviceFromConnRequestList(ip);
                    requestLine.setMethod("403");
                    //日志
                    updateEventLogListener("NORM","access is denied|refuse", "from: "+ di.IP);
                }
            }
            else if(!"/authentication.html".equals(requestLine.getURI()))
            {
                //除了authentication.html其他文件的请求都需要经过检查
                if(!deviceInDevicesList(ip))
                    requestLine.setMethod("403");
            }
        }
        else if(!di.permit)
        {
            if("/toaster.html".equals(requestLine.getURI()))
            {
                //第一次请求
                //未获得用户允许同时请求主页的情况说明请求还未进入请求队列
                di.IP=ip;
//                di.lastSocket= currentSocket;
                di.timeStamp=System.currentTimeMillis();
                //加入请求队列
                putDeviceToConnRequestList(ip, di);
                //日志
                String cause="["+ di.IP+"]请求连接";
                updateEventLogListener("NORM","request access|request",cause);
                synchronized (di.connRequestLock)
                {
                    //可设置超时自动拒绝
                    di.connRequestLock.wait(mRequestTimeout);
                    //线程因调用destroyCurrentThread而释放该锁时
                    if(currentThread.getSocket()==null|| currentThread.getSocket().isClosed()){
                        removeCurrentThreadFromThreadList(di);
                        requestLine.setMethod(null);//置null，因为socket已被释放
                        return;
                    }
                }
                //如果拒绝了请求，返回403报文
                if(!deviceInDevicesList(ip)) {
                    removeDeviceFromConnRequestList(ip);
                    requestLine.setMethod("403");
                    //日志
                    updateEventLogListener("NORM","access is denied|denied", "from: "+ di.IP);
                }
            }
            else if(!"/authentication.html".equals(requestLine.getURI()))
            {
                //除了authentication.html其他文件的请求都需要经过检查
                if(!deviceInDevicesList(ip))
                    requestLine.setMethod("403");
            }
        }
    }

    private void updateEventLogListener(String level,String event,String cause){
        EventLogManager.updateEventLogListener(level,event,cause);
    }
}
