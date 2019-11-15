package com.codig.CyberPotato.service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URLEncoder;

/**
 * 处理每一个请求
 * 
 * @author codig_work@outlook.com
 * 
 */
public class ServerThread extends Thread implements IServerThread
{
    // 存放accept到的用户套接字
    private Socket mSocket;
    //来自客户的输入流
    private BufferedInputStream mBuffInputStream;
    // 向客户端发送的字节输出流
    private BufferedOutputStream mBuffOutputStream;
    // 向客户端发送信息的字符输出流
    private PrintWriter mPrintWriter;
    // 服务器绝对路径文件String对象
    private String mDocRootString;
    // 服务器绝对路径文件对象
    private File mDocRoot;
    //设备信息
    private DeviceInfo mDeviceInfo;
    //接收文件的存放目录
    private String mDownloadDir;

    //get
    Socket getSocket(){
        return mSocket;
    }

    // 构造函数
    ServerThread(Socket serverAccept, DeviceInfo deviceInfo, String docRoot, String downloadDir){
        mDeviceInfo =deviceInfo;
        mSocket = serverAccept;
        mDownloadDir = downloadDir;
        mDocRootString = docRoot;
    }

    //装填新的参数
    public void refactor(Socket serverAccept, DeviceInfo deviceInfo, String docRoot, String downloadDir){
        //重新初始化
        mDeviceInfo =deviceInfo;
        mSocket = serverAccept;
        mDownloadDir = downloadDir;
        mDocRootString = docRoot;
    }

    //销毁线程
    @Override
    public void destroyCurrentThread()
    {
        try
        {
            if(mPrintWriter !=null){
                mPrintWriter.close();
                mPrintWriter=null;
            }
            if(mBuffInputStream !=null){
                mBuffInputStream.close();
                mBuffInputStream=null;
            }
            //先关闭socket再释放锁，防止释放锁之后数据流入客户端
            if(mSocket !=null){
                mSocket.close();
                mSocket=null;
            }
        }
        catch (IOException ignored) {}

        //释放设备的请求连接锁，移除线程由线程结尾进行操作，因为如果调用者使用foreach会抛出异常
        if(mDeviceInfo !=null)
        {
        	Firewall.unlock(mDeviceInfo);
        }

        try {
            this.destroy();
        }
        catch(NoSuchMethodError ignored){}
        catch (UnsupportedOperationException ignored){}
        catch (Exception ignored){}
    }

    @Override
    public void run()
    {
        try
        {
            // 获得绝对路径
            mDocRoot = new File(mDocRootString).getCanonicalFile();
            //来自客户的输入流
            mBuffInputStream =new BufferedInputStream(mSocket.getInputStream());
            // 返回给客户端的输出字节流，用于更快速地传送文件
            mBuffOutputStream = new BufferedOutputStream(mSocket.getOutputStream());
            // 输出字符流
            mPrintWriter = new PrintWriter(new OutputStreamWriter(mBuffOutputStream));
            
        	//添加到相应设备的线程列表
            ThreadListManger.addCurrentThread(mDeviceInfo);
        	
        	//解析报文，获取相关的参数，包括限制用户访问内容
            HttpRequestMessage requestMessage = new HttpRequestMessage(mBuffInputStream, mDownloadDir);
            //处理请求行
            requestMessage.resolveRequestLine();

//            System.out.println(currentThread()+"申请 | "+requestMessage.getMethod()+" "+requestMessage.getURI());//TODO 测试用

            //防火墙，根据di中的许可证对请求行做出相应的修改过滤
            Firewall.requestFilter(mDeviceInfo,requestMessage);
            //处理body部分
            requestMessage.resolveParamsOrFile();

            mSocket.shutdownInput();// 通知客户端输入流关闭

            //获取接收文件的路径
            String receiveFilePath=requestMessage.getReceiveFilePath();
            if(receiveFilePath!=null) {
                if(receiveFilePath.startsWith(mDownloadDir)) {
                    ReceiveRecordManager rrm=ReceiveRecordManager.getInstance();
                    //以下载目录开头说明接收成功
                    rrm.addRouter(receiveFilePath);
                    //日志
                    String cause="from: ["+ mDeviceInfo.IP+"]\nfile: "+receiveFilePath;
                    updateEventLogListener("NORM","receive file|receive",cause);
                }
                else {
                    //日志
                    String cause="来自["+ mDeviceInfo.IP+"]的文件接收失败，可能由网络连接中断引起\nfile: "+receiveFilePath;
                    updateEventLogListener("ALERT","failed to receive|failed",cause);
                }
            }

            //防火墙，阻塞，等待用户许可设备接入
            if(Firewall.isActive())
                Firewall.getInstance().blockAccess(mDeviceInfo,requestMessage);

//            System.out.println(currentThread()+"修改 | "+requestMessage.getMethod()+" "+requestMessage.getURI());//TODO 测试用
            
            //对客户端发出响应报文
            String method = requestMessage.getMethod();
            if (method != null)
            {
                // 分析用户http请求报文参数,根据get和post做出不同的响应
                if ("GET".equals(method)||"HEAD".equals(method))
                {
                    //响应get请求
                    if(mSocket!=null && !mSocket.isClosed())
                        responseGET(requestMessage);
                }
                else if ("POST".equals(method))
                {
                    mPrintWriter.println("HTTP/1.0 200 OK");
                    mPrintWriter.println();
                    // responsePOST(parmList,fileList);
                }
                else
                {
                    if("403".equals(method)){
                        //禁止访问
                        mPrintWriter.println("HTTP/1.0 403 Forbidden");
                        mPrintWriter.println();
                        mPrintWriter.flush();
                    }
                    else{
                        // 不支持的服务
                        mPrintWriter.println("HTTP/1.0 501 Not Implemented");
                        mPrintWriter.println();
                    }
                }
                mPrintWriter.flush();
                //mBuffOutputStream.flush();
            }
            // 关闭套接字
            if(mPrintWriter !=null)
            	mPrintWriter.close();
            if(mBuffInputStream !=null)
            	mBuffInputStream.close();
            if(mSocket!=null)
            	mSocket.close();
        }
        catch (Exception e)
        {
//        	System.out.println("exception:"+Thread.currentThread());//TODO 测试用
//        	e.printStackTrace();//TODO 测试用
            //移除线程
            ThreadListManger.removeCurrentThread(mDeviceInfo);
            try
            {
                if(mPrintWriter !=null) mPrintWriter.close();
                if(mBuffInputStream !=null) mBuffInputStream.close();
                if(mSocket !=null) mSocket.close();
            }
            catch (IOException ignored){ }

            if(mDeviceInfo !=null)
            {
                synchronized (mDeviceInfo.connRequestLock)
                {
                    mDeviceInfo.connRequestLock.notify();
                }
            }
            //日志
            String cause= mDeviceInfo.IP+" 连接失效，连接可能已被对方关闭，请尝试重新开启浏览器并请求网页";
            updateEventLogListener("ALERT","connection unavailable|failed",cause);
        }
        setNull();
    }
    
    /**
     * 将所有参数置null
     */
    private void setNull() {
		mDeviceInfo = null;
        mDownloadDir = null;
        mDocRoot = null;
    	mSocket = null;
    	mBuffInputStream = null;
    	mBuffOutputStream = null;
    	mPrintWriter = null;
    }

    /**
     * 响应get请求
     */
    private int responseGET(HttpRequestMessage requestMessage)
    {
        String uri = requestMessage.getURI();
        //用户请求download，将哈希码映射到真实路径
        String downloadFile=null;
        if("download".equals(requestMessage.getServletType()))
        {
            //读取内存中的映射，获取映射的路径，如果存在则修改uri，其他操作交由responseGET处理
            downloadFile = RouterManager.getInstance().getFilePathByHash(requestMessage.getParm("file"));
            if(downloadFile!=null)
            {
                uri=downloadFile;
            }
            else{
                //使用204报文是为了防止页面跳转，但是一些浏览器并不支持
                mPrintWriter.println("HTTP/1.0 204 No Content");
                mPrintWriter.println("Content-Length: 0");
                mPrintWriter.println();
                mPrintWriter.flush();
                return -1;
            }
        }
        //System.out.println(uri);
        File file=new File(uri);
        //如果请求的文件是绝对路径，说明requestMessage.uri被赋值为路由表中的项目，略过构成绝对路径的过程
        if(downloadFile==null)
        {
            String fPath;
            if (uri.startsWith("/") || uri.startsWith("\\"))
                fPath = this.mDocRoot + uri;
            else
                fPath = this.mDocRoot + File.separator + uri;
            // 规范化绝对路径
            try
            {
                file = new File(fPath).getCanonicalFile();
            }
            catch (IOException ignored) {}
            // 只有fDocRoot目录下的文件才可以访问
            if (!file.getAbsolutePath().startsWith(this.mDocRoot.getAbsolutePath()))
            {
                mPrintWriter.println("HTTP/1.0 403 Forbidden");
                mPrintWriter.println();
                mPrintWriter.flush();
                return -1;
            }
        }

        // 文件不存在
        if (!file.exists())
        {
            //使用204报文是为了防止页面跳转，但是一些浏览器并不支持
            mPrintWriter.println("HTTP/1.0 204 No Content");
            mPrintWriter.println("Content-Length: 0");
            mPrintWriter.println();
            mPrintWriter.flush();
            return -1;
        }
        // 文件无法读取
        else if (!file.canRead())
        {
            mPrintWriter.println("HTTP/1.0 403 Forbidden");
            mPrintWriter.println();
            mPrintWriter.flush();
            return -1;
        }
        // 请求的是目录
        else if (file.isDirectory())
        {
            // sendDir(mBuffOutputStream,mPrintWriter,file,uri);
            mPrintWriter.println("HTTP/1.0 403 Forbidden");
            mPrintWriter.println();
            mPrintWriter.flush();
            return -1;
        }
        //HEAD请求报文，仅测试文件有效性
        else if (file.exists()&&"HEAD".equals(requestMessage.getMethod()))
        {
            mPrintWriter.println("HTTP/1.0 200 OK");
            mPrintWriter.println();
            mPrintWriter.flush();
            return -1;
        }

        // 传送文件
        String[] rangeArr=requestMessage.getRange();//文件字节流范围数组
        String range=null;
        byte[] data = new byte[HttpServer.sendChunkMaxSize];// 数据分片
        int sendTimes=1;//发送次数，取决于请求报文的range个数
        if(rangeArr!=null)
        {
            sendTimes=rangeArr.length;
        }
        for(int i=0;i<sendTimes;i++)
        {
        	//取字节区间
            if(rangeArr!=null)
            {
                range=rangeArr[i];
            }
            //发送文件
            if("fileList.html".equals(file.getName()))
            {
                //如果请求fileList.html，必须等待文件写完
                synchronized(RouterManager.fileListHtmlLock)
                {
                    sendFile(file, range, data);
                }
            }
            else
            {
                sendFile(file, range, data);
            }
        }
        return 0;
    }

    /**
     * 将请求的文件发送
     * @param file 发送的文件
     * @param range 发送文件的范围
     * @param data tcp报文发送的最大数据块
     * @return
     * @throws IOException
     */
    private int sendFile(File file,String range,byte[] data)
    {
        //需要读取的文件流大小
        long contentLength=file.length();
        //文件字符流
        BufferedInputStream bis = null;
        try
        {
            bis = new BufferedInputStream(new FileInputStream(file.getAbsolutePath()));
            //如果请求报文指定了range，获取要发送的文件流范围并初始化文件流指针、文件流大小
            if(range!=null)
            {
                long needToSkipSize;//需要跳过的字节数
                long actualSkipSize;//实际跳过的字节数
                long rangeBegin;
                long rangeEnd;
                String[] rangeUnit=range.split("-",-1);
                if("".equals(rangeUnit[0]))
                {
                    rangeBegin=contentLength-Integer.valueOf(rangeUnit[1]).longValue();//开始读取的位置
                    needToSkipSize=rangeBegin;//需要跳过的字节数，即开始读取的位置，初始值和上面的变量相同
                    rangeEnd=contentLength-1;//range无标明开始位置时，结束位置为文件最末尾
                    //设置文件流指针
                    while(needToSkipSize!=0)
                    {
                        actualSkipSize=bis.skip(needToSkipSize);
                        needToSkipSize-=actualSkipSize;
                    }
                    //contentLength=Integer.valueOf(rangeUnit[1]).longValue();//body的大小
                }
                else
                {
                    rangeBegin=Integer.valueOf(rangeUnit[0]).longValue();//开始读取的位置
                    needToSkipSize=rangeBegin;
                    while(needToSkipSize!=0)
                    {
                        actualSkipSize=bis.skip(needToSkipSize);
                        needToSkipSize-=actualSkipSize;
                    }
                    //针对range结束位置是否为空值计算body大小和结束位置
                    if("".equals(rangeUnit[1]))
                    {
                        rangeEnd=contentLength-1;//无标明结束位置则为文件末尾
                    }
                    else
                    {
                        rangeEnd=Integer.valueOf(rangeUnit[1]).longValue();
                        //部分厂商忽视标准，将末尾本应为空值的range设置为0，遇到这种软件直接拒绝
                        if(rangeEnd<rangeBegin)
                        {
                            bis.close();
                            return -1;
                        }
                    }
                }
                contentLength=rangeEnd-rangeBegin+1;
                //206报文
                mPrintWriter.println("HTTP/1.0 206 Partial Content");
                String fileType=file.getName().substring(file.getName().lastIndexOf("."));
                if(!".html".equals(fileType)&&!".js".equals(fileType))//后缀不是html则表示需要下载的文件
                {
                    //mPrintWriter.println("Content-Type: text/plain");
                    mPrintWriter.println("Content-Disposition: attachment;filename="+ URLEncoder.encode(file.getName(),"utf-8").replaceAll("\\+", "%20"));
                }
                mPrintWriter.println("Accept-Ranges: bytes");
                mPrintWriter.println("Content-Length: "+contentLength);
                mPrintWriter.println("Content-Range: bytes "+rangeBegin+"-"+rangeEnd+"/"+file.length());
                mPrintWriter.println();
                mPrintWriter.flush();
            }
            else
            {
                //200报文
                mPrintWriter.println("HTTP/1.0 200 OK");
                String fileType=file.getName().substring(file.getName().lastIndexOf("."));
                if(!".html".equals(fileType)&&!".js".equals(fileType))//后缀不是html则表示需要下载的文件
                {
                    //mPrintWriter.println("Content-Type: text/plain");
                    mPrintWriter.println("Content-Disposition: attachment;filename="+URLEncoder.encode(file.getName(),"utf-8").replaceAll("\\+", "%20"));
                }
                mPrintWriter.println("Content-Length: "+contentLength);
                mPrintWriter.println();
                mPrintWriter.flush();
            }

            //将文件写入输出流
            int readSize;
            mBuffOutputStream.flush();
            long totalReadByteSize=0;//已经读取的字节流大小
            while (totalReadByteSize != contentLength)
            {
                //读入文件流
                readSize = bis.read(data);
                //如果请求的数据流大小不超过data，则进行截取
                if(totalReadByteSize+readSize>contentLength)
                    readSize=(int) (contentLength-totalReadByteSize);
                //文件流写入输出流
                mBuffOutputStream.write(data, 0, readSize);
                mBuffOutputStream.flush();
                totalReadByteSize+=readSize;
            }
            mBuffOutputStream.flush();
            bis.close();
            //不记录传输网页文件
            if(!file.getName().endsWith(".html")&&!file.getName().endsWith(".js")) {
                //日志
                String cause = "to: [" + mDeviceInfo.IP + "]\nfile: " + file.getAbsolutePath();
                updateEventLogListener("NORM", "send file|send", cause);
            }
        }
        catch (FileNotFoundException e)
        {
            try
            {
                if(bis!=null)
                    bis.close();
            }
            catch (IOException ignored){}
            //日志
            updateEventLogListener("ALERT","file unavailable|failed","错误码: S0010 "+file.getAbsolutePath());
        }
        catch (IOException e)
        {
            try
            {
                bis.close();
            }
            catch (IOException ignored) {}
            //日志
            String cause= mDeviceInfo.IP+" 连接失效，连接可能已被对方关闭，请尝试重新开启浏览器并请求网页";
            updateEventLogListener("ALERT","connection unavailable|failed",cause);
        }
        return 0;
    }

    private void updateEventLogListener(String level,String event,String cause){
        EventLogManager.updateEventLogListener(level,event,cause);
    }
}
