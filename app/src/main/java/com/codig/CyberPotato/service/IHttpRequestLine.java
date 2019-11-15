package com.codig.CyberPotato.service;

/**
 * 操作报文请求行
 * 
 * @author codig_work@outlook.com
 *
 */
public interface IHttpRequestLine {
    // 设置请求报文的方法，设置403代表拒绝访问，设置空值代表不支持服务，设置null代表直接退出线程
    void setMethod(String method);

    // 返回请求报文的方法
    String getMethod();

    // 设置获取的资源地址
    void setURI(String uri);

    // 客户请求获取的资源地址
    String getURI();
}
