package com.codig.CyberPotato.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Set;

/**
 * 用于注册日志监听器
 * 
 * @author codig_work@outlook.com
 * 
 */
class EventLogManager {
    //日志监听器列表
    private static Set<IEventLogListener> sEventLogListenerList = new HashSet<>();
    //日志 格式: 时间|事件等级|事件名称|事件简述|附加描述
    //错误码格式：(类名缩写)(方法编号)(方法内编号)
    private static Queue<String> sLogList = new LinkedList<>();
    //日志中事件发生时间的格式
    private static SimpleDateFormat sLogDateFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    //日志锁，同步读写操作
    private final static byte[] sLogLock = new byte[0];

    //日志监听接口
    interface IEventLogListener {
        /**
         * @param level  NORM  ALERT  ERROR
         */
        void eventLogChange(String level, String event, String cause);
    }
    //日志监听器
    public static void addEventLogListener(IEventLogListener eventLogListener){
        sEventLogListenerList.add(eventLogListener);
    }
    public static void removeEventLogListener(IEventLogListener eventLogListener){
        sEventLogListenerList.remove(eventLogListener);
    }
    public synchronized static void updateEventLogListener(String level,String event,String cause){
        synchronized(sLogLock) {
            //表示可存放256条信息
            if (sLogList.size() > 255)
                sLogList.poll();
            //添加到日志队列
            Date date = new Date(System.currentTimeMillis());
            String time = sLogDateFormat.format(date);
            sLogList.offer(time+"|"+level + "|" + event + "|" + cause);
        }
        for (IEventLogListener ell : sEventLogListenerList) {
            ell.eventLogChange(level, event, cause);
        }
    }
    //取日志最新的几行，正序，最新的在数组末
    public static List<String> getLastLog(int row){
        //加同步锁主要是防止在日志列表初始化阶段列表被更新
        synchronized(sLogLock) {
            List<String> lastLog = new LinkedList<>();
            int ln= sLogList.size();
            int start=ln-row;
            if(start<0) start=0;
            lastLog.addAll(((List) sLogList).subList(start,ln));
            return lastLog;
        }
    }

    //获取日志列表
    public static Queue<String> getLogList(){
        return sLogList;
    }
}
