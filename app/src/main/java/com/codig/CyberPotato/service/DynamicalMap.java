package com.codig.CyberPotato.service;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

/**
 * 
 * 带有监听器的Map
 * 
 * @author codig_work@outlook.com
 * 
 */
public class DynamicalMap<K,V> extends LinkedHashMap<K,V> {

	private static final long serialVersionUID = 1L;
	private Set<DynamicalMapListener> listenersList=new HashSet<>();

    @Override
    public V put(K key,V value){
        V res=super.put(key,value);
        if(!value.equals(res))
            updateServerStatusListener("put",key);
        return res;
    }

    @Override
    public V remove(Object key){
        V res=super.remove(key);
        updateServerStatusListener("remove",key);
        return res;
    }

    @Override
    public void clear(){
        super.clear();
        updateServerStatusListener("clear",null);
    }

    //服务器状态监听器接口
    public interface DynamicalMapListener {
        void dynamicalMapChange(String opt, Object key);
    }
    //服务器状态监听器注册
    public void addDynamicalMapListener(DynamicalMapListener dynamicalMapListenerListener){
        listenersList.add(dynamicalMapListenerListener);
    }
    //服务器状态监听器卸载
    public void removeDynamicalMapListener(DynamicalMapListener dynamicalMapListenerListener){
        listenersList.remove(dynamicalMapListenerListener);
    }
    //将数据发送给所有服务器状态监听器
    private void updateServerStatusListener(String opt,Object key){
        for (DynamicalMapListener dm:listenersList) {
            dm.dynamicalMapChange(opt,key);
        }
    }
}
