package com.codig.CyberPotato.service;

/**
 * 定义处理请求的线程的方法
 * @author codig_work@outlook.com
 *
 */
public interface IServerThread
{
    //设置守护线程
    void setDaemon(boolean on);
    //启动线程
    void start();
	//销毁线程
    void destroyCurrentThread();
}
