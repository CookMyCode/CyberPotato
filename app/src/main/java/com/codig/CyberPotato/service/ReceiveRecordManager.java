package com.codig.CyberPotato.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

/**
 * 记录下载的文件
 * 
 * @author codig_work@outlook.com
 *
 */
class ReceiveRecordManager {

    //单例引用
    private static ReceiveRecordManager sInstance;
    // 记录接收的文件路径//TODO 加同步锁
    private static Map<String, String> sReceiveFileList=new DynamicalMap<>();
    //记录接收文件的日志文件
    private File mReceiveRecordFile;
    //更新线程
    private Thread mUpdateThread;

    private ReceiveRecordManager(){
        if(sInstance!=null)
            throw new RuntimeException("This class can't be reflected");
    }

    public static ReceiveRecordManager getInstance() throws RuntimeException{
        if(sInstance==null){
            synchronized(ReceiveRecordManager.class){
                if(sInstance==null){
                    sInstance=new ReceiveRecordManager();
                }
            }
        }
        return sInstance;
    }

    //更新到receiveRecord文件
    private int writeListToFile(File file)
    {
        synchronized(ReceiveRecordManager.class)
        {
            try
            {
                // 将fileList写入route
                BufferedWriter bw = new BufferedWriter(new FileWriter(file));
                for (Map.Entry<String, String> entry : sReceiveFileList.entrySet())
                {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    bw.write(key + "|" + value + "\r\n");
                }
                bw.close();
            }
            catch (IOException ignored){}
            return 0;
        }
    }

    //加入路由条目//TODO sReceiveFileList应该改为同步类
    Object addRouter(String receiveFilePath){
        String res = sReceiveFileList.put(String.valueOf(new File(receiveFilePath).hashCode()), "00" + receiveFilePath);
        writeListToFile(mReceiveRecordFile);
        return res;
    }

    //删除接收的文件
    public void deleteFile(String filePath){
        if(filePath==null) return;
        File f=new File(filePath);
        String fileHashCode=String.valueOf(f.hashCode());
        if(f.isFile())
        {
            f.delete();
            //日志
            updateEventLogListener("NORM","delete file|delete",filePath);
        }
        sReceiveFileList.remove(fileHashCode);
        writeListToFile(mReceiveRecordFile);
    }

    //启动更新线程
    void startUpdateThread(File file){
        mReceiveRecordFile=file;
        if(mUpdateThread == null) {
            mUpdateThread = new ReceiveFileListValidityRealtimeUpdateThread();
            mUpdateThread.setDaemon(true);
        }
        if(!mUpdateThread.isAlive())
            mUpdateThread.start();
    }

    //get
    public Map<String,String> getReceiveFileList(){
        return sReceiveFileList;
    }

    /**
     * 用于实时检查接收文件列表的可用性并更新
     */
    private class ReceiveFileListValidityRealtimeUpdateThread extends Thread
    {
        public void run()
        {
            //从文件取出列表
            try {
                if (!mReceiveRecordFile.isFile())
                {
                    mReceiveRecordFile.createNewFile();
                }
                BufferedReader br = new BufferedReader(new FileReader(mReceiveRecordFile));
                String line;
                while ((line = br.readLine()) != null)
                {
                    sReceiveFileList.put(line.split("[|]")[0], line.split("[|]")[1]);
                }
                br.close();
            } catch (IOException e) {
                //日志
                updateEventLogListener("ERROR","no such file|failed","错误码:S0009");
            }

            for(;;)
            {
                for (Map.Entry<String, String> entry : sReceiveFileList.entrySet())
                {
                    String res;
                    String key = entry.getKey();
                    String filePath = entry.getValue().substring(2);//去掉两个状态码
                    if(new File(filePath).isFile()) {
                        res=sReceiveFileList.put(key, "00" + filePath);// 失效的路径生效
                        if(!("00" + filePath).equals(res))
                            writeListToFile(mReceiveRecordFile);
                    }
                    else {
                        res=sReceiveFileList.put(key, "01" + filePath);// 文件路径失效
                        if(!("00" + filePath).equals(res))
                            writeListToFile(mReceiveRecordFile);
                    }
                }

                try
                {
                    sleep(3000);
                }
                catch (InterruptedException ignored){}
            }
        }
    }

    private void updateEventLogListener(String level,String event,String cause){
        EventLogManager.updateEventLogListener(level,event,cause);
    }

    //注册接收文件列表监听器
    void addReceiveFileListListener(DynamicalMap.DynamicalMapListener receiveFileListListener){
        ((DynamicalMap)sReceiveFileList).addDynamicalMapListener(receiveFileListListener);
    }
    //卸载接收文件列表监听器
    void removeReceiveFileListListener(DynamicalMap.DynamicalMapListener receiveFileListListener){
        ((DynamicalMap)sReceiveFileList).removeDynamicalMapListener(receiveFileListListener);
    }
}
