package com.codig.CyberPotato.service;

import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;

/**
 * 为了尽可能减少复用成本，自己实现了线程池
 * @author codig_work@outlook.com
 *
 */
public class ServerThreadPool
{
	//最大空闲线程数，考虑到手机性能，一般不能太大
	private int mIdleThreadsSize=3;
	//存放空闲线程//TODO 如果mIdleThreadsSize太大，则需要一个remove(Object)性能高的队列，或许可以重写LinkedHashSet实现Queue
	private Queue<ReusableServerThread> mIdleThreadsList = new LinkedList<>();
    //空闲线程存活时间
    private long mKeepAliveTime=30000;
	
    //处理socket，不需要同步锁
	void execute(Socket serverAccept, DeviceInfo di, String docRoot, String downloadDir) {
		ReusableServerThread t = mIdleThreadsList.poll();
        if(t==null){
            t=new ReusableServerThread(serverAccept,di,docRoot,downloadDir);
            t.setDaemon(true);
            t.start();
        }
        else {
        	//参数重装填
        	t.refactor(serverAccept,di,docRoot,downloadDir);
        	//解除线程阻塞
        	t.restart();
        }

//        //TODO 测试用
//        String time = (new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())).format(new java.util.Date(System.currentTimeMillis()));
//        System.out.println(time+" 分配 "+t+" 处理：IP:"+di.IP+" permit:"+di.permit);
	}
	
	//将当前线程加入空闲线程队列，成功返回true
    private boolean addToIdleThreadsList(ReusableServerThread t) {
		//添加到空闲线程，保留一定的线程在空闲线程列表中，以备复用
    	boolean res=false;
    	synchronized (this)
		{
    		if(mIdleThreadsList.size() < mIdleThreadsSize) {
    			res = mIdleThreadsList.offer(t);
    		}
		}
		return res;
    }
    
    //将当前线程从空闲线程队列中移除
    private void removeFromIdleThreadsList(ReusableServerThread t) {
    	synchronized (this) {
    		mIdleThreadsList.remove(t);
    	}
    }
    
    /**
     * 可复用线程
     */
    private class ReusableServerThread extends ServerThread
    {
    	//判断是否超时导致的解锁
    	private boolean mIsTimeout=true;
    	
    	ReusableServerThread(Socket serverAccept, DeviceInfo deviceInfo, String docRoot, String downloadDir)
    	{
    		super(serverAccept, deviceInfo, docRoot, downloadDir);
    	}

    	@Override
    	public void run() {
    		for(;;) {
    			//执行父类方法
    			super.run();
    			//线程复用
    			boolean res=addToIdleThreadsList(this);
            	if(res) {
            		//阻塞，等待线程被复用
            		try
					{
//	            		System.out.println(this+" wait");//TODO 测试用
	        			synchronized(this) {
	        				this.wait(mKeepAliveTime);
	                	}
//        				System.out.println(this+" notify");//TODO 测试用
						if(mIsTimeout) {
//	        				System.out.println(this+" destroy");//TODO 测试用
							removeFromIdleThreadsList(this);
							break;
						}
	            		mIsTimeout=true;
//	            		System.out.println(this+" restart");//TODO 测试用
					}
					catch (InterruptedException ignored) { }
            	}
            	else {
            		break;
            	}
    		}
    	}
        
        //线程复用时，用来唤醒线程
        public void restart() {
    		mIsTimeout=false;
        	synchronized(this) {
        		this.notify();
        	}
        }
        
    }

    @Deprecated
    Queue getIdleThreadsList() {
    	return mIdleThreadsList;
    }
}
