package com.codig.CyberPotato.service;

import java.util.Collection;
import java.util.Map;

/**
 * 提供操作请求队列和连接列表的方法
 * 
 * @author codig_work@outlook.com
 * 
 */
abstract class AbstractDevicesManager{

    //TODO 需要修改为同步方法
    //连接设备列表
    private static Map<String, DeviceInfo> sDevicesList =new DynamicalMap<>();
    //连接请求队列
    private static Map<String,DeviceInfo> sConnRequestList =new DynamicalMap<>();

    //销毁某个设备线程池中的其他线程，保留自身线程
    void destroyOtherThread(DeviceInfo di){
        ThreadListManger.destroyOtherThread(di);
    }

    //销毁某个设备的所有线程
    void destroyAllThread(DeviceInfo di){
        ThreadListManger.destroyAllThread(di);
    }

    //清空列表
    void clearDevicesList(){
        for(DeviceInfo di:sDevicesList.values())
        {
            destroyAllThread(di);
        }
        sDevicesList.clear();
    }
    void clearConnRequestList(){
        for(DeviceInfo di:sConnRequestList.values())
        {
            destroyAllThread(di);
        }
        sConnRequestList.clear();
    }

    //将当前线程从当前设备活跃线程线程中移除
    void removeCurrentThreadFromThreadList(DeviceInfo di){
        ThreadListManger.removeCurrentThread(di);
    }

    //设备是否在连接设备列表里
    static boolean deviceInDevicesList(Object key){
        return sDevicesList.containsKey(key);
    }

    //设备是否在请求队列里
//    static boolean deviceInConnRequestList(Object key){
//        return sConnRequestList.containsKey(key);
//    }

    //从请求队列里移除本设备
    Object removeDeviceFromConnRequestList(Object key){
        return sConnRequestList.remove(key);
    }
    //从连接列表里移除本设备
    Object removeDeviceFromDevicesList(Object key){
        return sDevicesList.remove(key);
    }
    //从请求队列里获取设备
    static DeviceInfo getDeviceFromConnRequestList(Object key){
        return sConnRequestList.get(key);
    }
    //从连接列表里获取设备
    static DeviceInfo getDeviceFromDevicesList(Object key){
        return sDevicesList.get(key);
    }
    //从请求队列里获取设备
    static DeviceInfo putDeviceToConnRequestList(Object key,DeviceInfo di){
        return sConnRequestList.put((String)key,di);
    }
    //从连接列表里获取设备
    DeviceInfo putDeviceToDevicesList(Object key,DeviceInfo di){
        return sDevicesList.put((String)key,di);
    }

    //获取DevicesList所有值
    Collection<DeviceInfo> getValuesFromDevicesList(){
        return sDevicesList.values();
    }

    //get
    Map<String, DeviceInfo> getDevicesList()
    {
        return sDevicesList;
    }
    Map<String, DeviceInfo> getConnRequestList()
    {
        return sConnRequestList;
    }
}
