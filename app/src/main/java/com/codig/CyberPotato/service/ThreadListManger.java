package com.codig.CyberPotato.service;

import java.net.Socket;
import java.util.HashSet;

/**
 * 对对应设备的线程进行管理
 * 
 * @author codig_work@outlook.com
 * 
 */
class ThreadListManger {

    //线程池
	private static ServerThreadPool sServerThreadPool=new ServerThreadPool();
	
    //分配线程处理请求，方法只在主线程中调用，不需要进行同步
    static void add(DeviceInfo di, Socket socket, String docRoot, String downloadDir){
        //初始化
        if(di.threadsList==null)
            di.threadsList=new HashSet<>();
        //线程池
        sServerThreadPool.execute(socket,di,docRoot,downloadDir);
    }

    //请求处理开始，添加
    static void addCurrentThread(DeviceInfo di){
        Thread currentThread = Thread.currentThread();
        //线程添加到活跃列表说明
        synchronized (di.threadsListLock) {
        	di.threadsList.add(currentThread);
        }
    }
    
    //请求处理完成，移除
    static void removeCurrentThread(DeviceInfo di){
        Thread currentThread = Thread.currentThread();
        //从活跃列表中移除
        //加锁，防止出现在用户重复申请时，新线程在遍历释放旧线程出现的ConcurrentModificationException
        synchronized (di.threadsListLock) {
        	di.threadsList.remove(currentThread);
        }
    }

    //销毁某个设备的其他线程，保留自身线程
    static void destroyOtherThread(DeviceInfo di){
        Thread currentThread = Thread.currentThread();
        //加锁，防止出现在用户重复申请时，新线程在遍历释放旧线程出现的ConcurrentModificationException
        synchronized (di.threadsListLock) {
            for (Thread t : di.threadsList) {
                if (t != currentThread) {
                    ((IServerThread)t).destroyCurrentThread();
                }
            }
        }
    }

    //销毁某个设备的所有线程
    static void destroyAllThread(DeviceInfo di){
        //加锁，防止出现在用户重复申请时，新线程在遍历释放旧线程出现的ConcurrentModificationException
        synchronized (di.threadsListLock) {
	        for (Thread t : di.threadsList) {
	            ((IServerThread)t).destroyCurrentThread();
	        }
	        di.threadsList.clear();
        }
    }
    

//    @Deprecated //TODO 测试用
//    static java.util.Queue getIdleThreadsList() {
//    	return sServerThreadPool.getIdleThreadsList();
//    }
}
