package com.codig.CyberPotato.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * 
 * 主线程
 * 
 * @author codig_work@outlook.com
 * 
 */
public class HttpServer extends Thread
{
	// 软件版本号
	private String version;
	// 内部储存发布根目录
	private String internalStorageDocRoot;
	// 外部储存发布根目录
	private String externalStorageDocRoot;
	// 真正使用的根目录，默认值为内部储存根目录
	private String mDocRoot;
	//路由文件的目录
	private File routerFile;
	//对外发布显示的文件列表，html文件
	private File fileListHtml;
	// 接收文件存放的目录
	private String mDownloadDir;
	//服务器当前状态，true为启动或正在启动，false为已停止或正在停止，null为正在启动
	private Boolean serverStatus=false;
	//服务器状态锁
	final private byte[] serverStatusLock = new byte[0];
	//记录接收文件的日志文件
	private File receiveRecordFile;
	//配置文件
	private File configFile;
	// 服务端套接字
	private ServerSocket serverSocket;
	//路由文件管理，用于哈希码和文件路径的转换
	private RouterManager routerManager;
	//设备管理
	private DevicesManger devicesManger;
	//接收文件管理
	private ReceiveRecordManager receiveRecordManager;
    //服务器状态监听器列表
    private Set<ServerStatusListener> serverStatusListenerList=new HashSet<>();
	//服务终止时改变
	private boolean onServiceDestroy=false;

	/**
	 * 用户可修改的配置
	 */
	// 默认端口
	private int port = 8080;
	//每次向输出流发送最大字节数
	static int sendChunkMaxSize=10000000;
	//每次于输入流接收最大字节数
	static int recvChunkMaxSize=10000000;
    //是否开启防火墙，检查用户请求
	private Boolean firewall=false;
	//是否开启后台保活
	private Boolean keepAlive=true;
	//TODO 请求连接的超时时间
//	private int requestTimeout=0;
	//标记是否使用外部储存根目录
	private Boolean exportRoot=false;
	
	// 构造函数
	public HttpServer(String iDocRoot,String eDocRoot,String appDir,String downloadDir,String version)
	{
		this.version=version;//版本
		mDownloadDir =downloadDir;//存放下载文件的目录
		this.internalStorageDocRoot=iDocRoot;//内部存储的路径
		this.externalStorageDocRoot=eDocRoot;//外部存储的路径
		mDocRoot =internalStorageDocRoot;//默认发布文件夹的路径
		this.routerFile =new File(appDir+File.separator +"router");//路由文件，存放文件和路径哈希的映射
		this.fileListHtml = new File(mDocRoot +File.separator+"fileList.html");//对外发布的html文件，动态更新
		this.receiveRecordFile=new File(appDir+File.separator +"receiveRecord");//记录接收的文件
		this.configFile=new File(appDir+File.separator+"config");//配置文件路径
	}

	@Override
	public void run()
	{
		updateEventLogListener("NORM","initializing server|initialize",null);
		//从配置文件获取设置参数
		readSettingsFromConfigFile();
		//初始化路由文件，传入router和fileList文件，启动路由实时更新
		routerManager = RouterManager.getInstance();
		routerManager.startUpdateThread(routerFile,fileListHtml);
		//启动设备列表更新线程
		devicesManger = DevicesManger.getInstance();
		devicesManger.startUpdateThread(mDocRoot, mDownloadDir);
		//启动接收文件列表更新线程
		receiveRecordManager = ReceiveRecordManager.getInstance();
		receiveRecordManager.startUpdateThread(receiveRecordFile);
		
		// 实例化套接字
		while(true)
		{
			//从配置文件更新设置参数
			readSettingsFromConfigFile();
			//保活后台
			setHttpServerKeepAlive(keepAlive);
			//使用外部储存根目录根目录
			if(exportRoot) {
				//重新设置fileList.html的路径
				fileListHtml = new File(mDocRoot + File.separator + "fileList.html");
				routerManager.setFileListHtmlPath(fileListHtml);
				//更新root路径
				devicesManger.setDocRoot(mDocRoot);
				//将内部储存root目录复制到外部储存
				exportRoot(externalStorageDocRoot,internalStorageDocRoot);
			}
			//设置防火墙
			Firewall.setFireWallState(firewall);

			//初始化套接字
			try {
				this.serverSocket = new ServerSocket(this.port);
				serverStatus=true;
				//更新服务器状态监听器
				updateServerStatusListener(true);
				//更新日志
				String cause;
				cause=firewall?"+ firewall on\n":"- firewall off\n";
				cause+=keepAlive?"+ keep daemon alive\n":"- daemon keeper down\n";
				cause+=exportRoot?"- external root":"+ internal root";
				updateEventLogListener("NORM","server is running|running",cause);
			} catch (Exception e) {
				//端口被占用
				port=(port<65534)?port+1:1025;
				writeSettingsToConfigFile();
				//更新日志
				String cause="端口可能已被占用,已自动更改为"+port+",三秒后自动重启服务器.";
				updateEventLogListener("ERROR","failed to start server|failed",cause);
				try {
					sleep(3000);
				} catch (InterruptedException ignored) { }
				continue;
			}

			// 多线程响应多用户的请求
			Socket serverAccept;
			while (serverStatus)
			{
				try {
					//监听阻塞
					serverAccept = serverSocket.accept();
					//超时时间
//					serverAccept.setSoTimeout(requestTimeout);
				} catch (IOException e) {
					updateEventLogListener("ERROR","failed to start server|failed","错误码:M0001，请重新启动服务器");
					break;
				}

				//关闭服务器
				if(!serverStatus) break;

				//分配线程处理请求
				boolean res = devicesManger.acceptConn(serverAccept);
				if(!res){
					//日志
					updateEventLogListener("ERROR","no such device|failed","错误码:M0002，请重新启动服务器");
					break;
				}

			}

			if(!serverSocket.isClosed())
			{
				try {
					serverSocket.close();
				} catch (IOException ignored) { }
			}

			//销毁所有连接
			devicesManger.destroyAllConn();
			//服务器状态监听器
			serverStatus=false;
			updateServerStatusListener(false);
			updateEventLogListener("NORM","server is down|closed",null);
			//完全退出程序
			if(onServiceDestroy)
			{
				break;
			}

			//阻塞，等待重新开启
			synchronized (serverStatusLock) {
				try {
					serverStatusLock.wait();
				} catch (InterruptedException ignored) { }
			}
		}
	}

	//后台保活，被调用时需重写
	protected void setHttpServerKeepAlive(boolean status){ }

    //set
	public void setPort(int port)
    {
    	if(port<1024) port=1025;
		this.port=port;
    }
	public void setSendChunkMaxSize(double test)
    {
        int i =(int)(test*1024*1024);
        if(i!=sendChunkMaxSize)
            sendChunkMaxSize=i;
    }
	public void setRecvChunkMaxSize(double test)
    {
        int i =(int)(test*1024*1024);
        if(i!=recvChunkMaxSize)
            recvChunkMaxSize=i;
    }
	public void setFirewall(boolean status){
		this.firewall=status;
	}
	public void setKeepAlive(boolean status){
		this.keepAlive=status;
	}
	public void setExportRoot(boolean status){
		this.exportRoot=status;
	}

    // get
	public int getPort()
    {
        return serverSocket.getLocalPort();
    }
	public int getPortFromSettings()
	{
		return this.port;
	}
	public String getIP() {
		return serverSocket.getLocalSocketAddress().toString().substring(1).split(":")[0];
	}
	public String getSendChunkMaxSize() {
        return String.format(Locale.getDefault(),"%.1f", (double)sendChunkMaxSize/1024.0/1024.0);
    }
	public String getRecvChunkMaxSize() {
        return String.format(Locale.getDefault(),"%.1f", (double)recvChunkMaxSize/1024.0/1024.0);
    }
	public boolean getFirewall(){
		return firewall;
	}
	public boolean getKeepAlive(){
		return keepAlive;
	}
	public boolean getExportRoot(){
		return exportRoot;
	}
	public Boolean getServerStatus(){
		return serverStatus;
	}
	public Queue<String> getLog(){
		return EventLogManager.getLogList();
	}
	public Map<String, DeviceInfo> getDevicesList()
    {
        return devicesManger.getDevicesList();
    }
	public Map<String, DeviceInfo> getConnRequestList()
	{
		return devicesManger.getConnRequestList();
	}
	public Map<String,String>getFileList(){
		return routerManager.getRouterList();
	}
	public Map<String,String>getReceiveFileList(){
		return receiveRecordManager.getReceiveFileList();
	}

	/**
	 * 从配置文件读出设置选项
	 */
	private void readSettingsFromConfigFile(){
		if(!configFile.exists()) {
			initConfigFile();//配置文件不存在直接恢复默认
			return;
		}
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(configFile));
			String line;
			String setting[];
			String key;
			String val;
			while ((line = br.readLine()) != null)
			{
				setting=line.split("=",2);
				key=setting[0];
				val=setting[1];
				switch (key){
					case "port":
						port=Integer.valueOf(val);
						break;
					case "sendChunkMaxSize":
						sendChunkMaxSize=Integer.valueOf(val);
						break;
					case "recvChunkMaxSize":
						sendChunkMaxSize=Integer.valueOf(val);
						break;
					case "keepAlive":
						keepAlive=Boolean.valueOf(val);
						break;
					case "firewall":
						firewall=Boolean.valueOf(val);
						break;
					case "exportRoot":
						//设置根目录
						exportRoot=Boolean.valueOf(val);
						mDocRoot =exportRoot?externalStorageDocRoot:internalStorageDocRoot;
						break;
				}
			}
			br.close();
		} catch (IOException e) {
			try {
				if(br!=null) br.close();
			} catch (IOException ignored) { }
		}
	}

	/**
	 * 配置文件和设置恢复默认
	 */
	private void initConfigFile(){
		port=8080;
		sendChunkMaxSize=10000000;
		recvChunkMaxSize=10000000;
		keepAlive=true;
		firewall=false;
		exportRoot=false;
		writeSettingsToConfigFile();
	}

	/**
	 *写入配置文件
	 */
	public void writeSettingsToConfigFile(){
		String[] settings={
				"version="+version,
				"port="+String.valueOf(port),
				"sendChunkMaxSize="+String.valueOf(sendChunkMaxSize),
				"recvChunkMaxSize="+String.valueOf(recvChunkMaxSize),
				"keepAlive="+String.valueOf(keepAlive),
				"firewall="+String.valueOf(firewall),
				"exportRoot="+String.valueOf(exportRoot)
		};

		try {
			if(!configFile.exists())
				configFile.createNewFile();
		} catch (IOException ignored) { }

		BufferedWriter bw = null;
		try {
			//将settings写入配置文件
			bw = new BufferedWriter(new FileWriter(configFile));
			for(String setting:settings){
				bw.write(setting+"\n");
			}
			bw.close();
			//更新日志
			String cause="settings will take effect the next time you restart server.";
			updateEventLogListener("NORM","update settings|update",cause);
		} catch (IOException e) {
			try {
				if(bw!=null) bw.close();
			} catch (IOException ignored) { }
			//更新日志
			updateEventLogListener("ERROR","unable to update settings|failed","错误码:M0101");
		}
	}

    //启动服务器，仅在被关闭时有效
	public void startServer()
    {
        if(!serverStatus) {
        	serverStatus = true;
            synchronized (serverStatusLock) {
            	serverStatusLock.notify();
            }
        }
    }
    //关闭服务器
	public void closeServer()
    {
		if(serverStatus) {
			serverStatus=false;
			//解除阻塞
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						new Socket("localhost", serverSocket.getLocalPort());
					} catch (IOException ignored) {}
				}
			}).start();
		}
    }
    //退出进程
	public void exitProcess()
	{
		onServiceDestroy=true;
		closeServer();
	}

	//将内部储存目录复制到外部储存
	private void exportRoot(String externalDocRoot, String internalDocRoot){
		File eDir = new File(externalDocRoot);
		if (!eDir.isDirectory()) {
			eDir.mkdirs();
		}
		String[] fileList = new File(internalDocRoot).list();
		try {
			for(String fileName:fileList) {
				File eFile = new File(externalDocRoot + File.separator+fileName);
				if(!eFile.isFile()) {
					//输入流，偷懒了，但是一般不会出错
					InputStream is = new FileInputStream(new File(internalDocRoot + File.separator + fileName));
					byte[] read = new byte[is.available()];
					is.read(read);
					is.close();
					//输出流
					OutputStream os = new FileOutputStream(eFile);
					os.write(read);
					os.close();
				}
			}
		} catch (IOException e) {
			exportRoot=false;
			mDocRoot =internalDocRoot;
		}
	}

    //断开与指定设备的连接
	public void ceaseConn(DeviceInfo di){
		devicesManger.ceaseConn(di);
	}
	//允许指定设备连接服务器
	public void permitConn(DeviceInfo di){
		devicesManger.permitConn(di);
	}
	//拒绝指定设备的连接请求
	public void refuseConn(DeviceInfo di){
		devicesManger.refuseConn(di);
	}

	//删除路由
	public void deleteRouter(String router){
		routerManager.deleteRouter(router);
	}
	//添加路由
	public void addRouter(String router){
		routerManager.addRouter(router);
	}
	//添加多路由
	public void addRouter(List<String> routers){
		routerManager.addRouter(routers);
	}
	//隐藏路由
	public void hideRouter(String router){
		routerManager.hideRouter(router);
	}
	//发布路由
	public void publishRouter(String router){
		routerManager.publishRouter(router);
	}

	//删除文件
	public void deleteReceiveFile(String filePath){
		receiveRecordManager.deleteFile(filePath);
	}

    // 注册设备连接情况监听器
	public void addDevicesStatusListener(DynamicalMap.DynamicalMapListener devicesStatusListener){
		devicesManger.addDevicesStatusListener(devicesStatusListener);
    }
	// 卸载设备连接情况监听器
	public void removeDevicesStatusListener(DynamicalMap.DynamicalMapListener devicesStatusListener){
		devicesManger.removeDevicesStatusListener(devicesStatusListener);
	}
	//注册连接请求队列监听器
	public void addConnRequestListListener(DynamicalMap.DynamicalMapListener connRequestListListener){
		devicesManger.addConnRequestListListener(connRequestListListener);
	}
	//卸载连接请求队列监听器
	public void removeConnRequestListListener(DynamicalMap.DynamicalMapListener connRequestListListener){
		devicesManger.removeConnRequestListListener(connRequestListListener);
	}

	//注册发布文件列表监听器
	public void addFileListListener(DynamicalMap.DynamicalMapListener fileListListener){
		routerManager.addFileListListener(fileListListener);
	}
	//卸载发布文件列表监听器
	public void removeFileListListener(DynamicalMap.DynamicalMapListener fileListListener){
		routerManager.removeFileListListener(fileListListener);
	}

	//注册接收文件列表监听器
	public void addReceiveFileListListener(DynamicalMap.DynamicalMapListener receiveFileListListener){
		receiveRecordManager.addReceiveFileListListener(receiveFileListListener);
	}
	//卸载接收文件列表监听器
	public void removeReceiveFileListListener(DynamicalMap.DynamicalMapListener receiveFileListListener){
		receiveRecordManager.removeReceiveFileListListener(receiveFileListListener);
	}

    //服务器状态监听器接口
	public interface ServerStatusListener {
        void serverStatusChange(Boolean serverStatus);
    }
    //服务器状态监听器注册
	public void addServerStatusListener(ServerStatusListener serverStatusListener){
        serverStatusListenerList.add(serverStatusListener);
    }
    //服务器状态监听器卸载
	public void removeServerStatusListener(ServerStatusListener serverStatusListener){
        serverStatusListenerList.remove(serverStatusListener);
    }
    //将数据发送给所有服务器状态监听器
    private void updateServerStatusListener(Boolean serverStatus){
        for (ServerStatusListener ssl:serverStatusListenerList) {
            ssl.serverStatusChange(serverStatus);
        }
    }

	//日志监听接口
	public interface EventLogListener extends EventLogManager.IEventLogListener {

	}
    //日志监听器
	public void addEventLogListener(EventLogListener eventLogListener){
		EventLogManager.addEventLogListener(eventLogListener);
	}
	public void removeEventLogListener(EventLogManager.IEventLogListener eventLogListener){
		EventLogManager.removeEventLogListener(eventLogListener);
	}
	private void updateEventLogListener(String level,String event,String cause){
		EventLogManager.updateEventLogListener(level,event,cause);
	}
	//取日志最新的几行，正序，最新的在数组末
	public List<String> getLastLog(int row){
		return EventLogManager.getLastLog(row);
	}

//	@Deprecated //TODO 测试用
//	DevicesManger getDevicesManger() {
//		return devicesManger;
//	}
//	@Deprecated
//	RouterManager getRouterManager() {
//		return routerManager;
//	}
//	@Deprecated
//	ReceiveRecordManager getReceiveRecordManager() {
//		return receiveRecordManager;
//	}
}

