package com.codig.CyberPotato.service;

import java.io.Serializable;
import java.net.Socket;
import java.util.Set;

/**
 * 设备信息
 * 
 * @author codig_work@outlook.com
 * 
 */
public class DeviceInfo implements Cloneable, Serializable {

	private static final long serialVersionUID = 1L;
	public String deviceName;
    public String IP;
    public Boolean connStatus;//暂时无用，保留
    public long timeStamp;

	public transient final byte[] connRequestLock=new byte[0];//请求连接的锁
    public transient Boolean permit;//null：等待用户授权；false：第一次请求；true：允许
	public transient final byte[] threadsListLock=new byte[0];//操作线程的锁
    public transient Set<Thread> threadsList;//存放活跃的线程
	public transient Socket lastSocket;//最近一个处理的socket，用于允许用户连接时测试连接是否可用

    //get set
    public String getDeviceName(){
        return deviceName;
    }
    public void setDeviceName(String deviceName){
        this.deviceName=deviceName;
    }
    public String getIP(){
        return IP;
    }
    public void setIP(String IP){
        this.IP=IP;
    }
    public Boolean getConnStatus(){
        return connStatus;
    }
    public void setConnStatus(Boolean connStatus){
        this.connStatus=connStatus;
    }
    public long getTimeStamp(){
        return timeStamp;
    }
    public void setTimeStamp(long timeStamp){
        this.timeStamp=timeStamp;
    }
}
