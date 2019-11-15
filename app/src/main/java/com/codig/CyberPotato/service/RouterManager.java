package com.codig.CyberPotato.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 
 * 管理文件和路径哈希之间的映射，动态修改fileList.html文件
 * 
 * @author codig_work@outlook.com
 *
 */
class RouterManager
{
	//实例
	private static RouterManager sInstance;
	//路由文件路径
	private File mRouterFile;
	//对外发布显示的文件列表
	private File mFileListHtml;
	//周期性更新路由表的线程
	private static Thread sUpdateThread;

	// 内存中的路由表，文件路径前缀00为有效路由，01失效，10隐藏且有效，11隐藏且失效
	private static Map<String, String> sRouterList = new DynamicalMap<>();
	//文件锁，向外部暴露，需要进行读写时都需要加锁
	//fileList.html文件的锁
	public final static byte[] fileListHtmlLock = new byte[0];
	//route文件的锁
	public final static byte[] routerFileLock = new byte[0];

	//单例
	private RouterManager(){
		if(sInstance!=null)
			throw new RuntimeException("This class can't be reflected");
	}

	public static RouterManager getInstance() throws RuntimeException{
		if(sInstance==null){
			synchronized(ReceiveRecordManager.class){
				if(sInstance==null){
					sInstance=new RouterManager();
				}
			}
		}
		return sInstance;
	}

	//启动更新线程
	void startUpdateThread(File routerFile, File fileListHtml){
		mRouterFile=routerFile;
		mFileListHtml=fileListHtml;
		initFile();

		if(sUpdateThread == null) {
			sUpdateThread = new RouterValidityRealtimeUpdateThread();
			sUpdateThread.setDaemon(true);
		}
		if(!sUpdateThread.isAlive())
			sUpdateThread.start();
	}

	private void initFile(){
		try {
			if (!mRouterFile.isFile())
			{
				mRouterFile.createNewFile();
			}
			// 将route文件中的路由表项读入fileList
			BufferedReader br = new BufferedReader(new FileReader(mRouterFile));
			String line;
			while ((line = br.readLine()) != null)
			{
				sRouterList.put(line.split("[|]")[0], line.split("[|]")[1]);
			}
			br.close();

			//如果不存在fileList.html文件则创建
			if (!mFileListHtml.isFile())
			{
				mFileListHtml.createNewFile();
				writeFileListToCommunicator();
			}

			//更新到route和fileList.html文件
			routerValidityUpdate();
		}
		catch (IOException ignored) { }
	}

	/**
	 * 默认使用构造方法获得的锁
	 */
	private int writeFileListToCommunicator()
	{
		synchronized(fileListHtmlLock)
		{
			try
			{
				// 将fileList写入fileList.html
				BufferedWriter bw = new BufferedWriter(new FileWriter(mFileListHtml));
				bw.write("<html>\r\n");
				bw.write("<head>\r\n");
				bw.write("<meta http-equiv=\"Content-Type\" content=\"text/html;charset=UTF-8\">\r\n");
				bw.write("<link rel=\"icon\" href=\"data:image/ico;base64,aWNv\">\r\n");
				bw.write("</head>\r\n");
				bw.write("<body>\r\n");
				for (Entry<String, String> entry : sRouterList.entrySet())
				{
					String key = entry.getKey();
					String value = entry.getValue();
					//检查状态前缀是否开启了隐藏
					if(value.charAt(0)!='1')
					{
						//检查状态前缀是否为失效文件
						if(value.charAt(1)=='1')
						{
							key="death";
						}
						File file = new File(value.substring(2));
						bw.write("<input type=\"hidden\" name=\"" + file.getName() + "\" id=\"" + key + "\" value=\"" + file.length() + " Bytes\">\r\n");
					}
				}
				//这段脚本用于发送信息给heartBeat.js，表示请求文件列表未被阻塞
				bw.write("<script type=\"text/javascript\">");
				bw.write("setTimeout(function(){window.parent.worker.postMessage(false)},10);");
				bw.write("</script>\r\n");
				bw.write("</body>\r\n");
				bw.write("</html>\r\n");
				bw.close();
	
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			return 0;
		}
	}

	/**
	 * 默认使用构造方法获得的锁
	 */
	private int writeFileListToRouteFile()
	{
		synchronized(routerFileLock)
		{
			try
			{
				// 将fileList写入route
				BufferedWriter bw = new BufferedWriter(new FileWriter(mRouterFile));
				for (Entry<String, String> entry : sRouterList.entrySet())
				{
					String key = entry.getKey();
					String value = entry.getValue();
					bw.write(key + "|" + value + "\r\n");
				}
				bw.close();
	
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			return 0;
		}
	}

	/**
	 * 更新内存中的路由表项
	 * 
	 * @param cmd
	 *            ：del(删除) add(添加) death(失效) revive(生效) hid(隐藏) pub(公布)
	 * @param router
	 *            ：当cmd为add时为文件路径，其他情况为文件路径哈希码
	 * @return -1:指令无效  -2:文件路径无效 -3:路由已存在  -4:路由不存在  -5:路由无改动
	 */
	private int updateFileList(String cmd, String router)
	{
		File f=new File(router);
		String fileHashCode=String.valueOf(f.hashCode());
		if (cmd.equals("add"))
		{
			if(!f.isFile())
			{
				return -2;
			}
			if(!sRouterList.containsKey(fileHashCode))
				sRouterList.put(fileHashCode, "00"+router);
			else
				return -3;
		}
		else if (cmd.equals("del"))
		{
			if(sRouterList.remove(fileHashCode)==null)
				return -4;
		}
		else
		{
			String filePath= sRouterList.get(fileHashCode);
			if(filePath==null) return -4;
			//用于修改文件路径前缀状态码。00有效，01无效，10隐藏，11无效且隐藏
			StringBuilder alterStatus = new StringBuilder(filePath);

			if (cmd.equals("death"))
			{
				if(alterStatus.charAt(1)=='1')
					return -3;
				// 使表项失效
				alterStatus.setCharAt(1, '1');
				String res= sRouterList.put(fileHashCode, alterStatus.toString());
				if(res.equals(alterStatus.toString()))
					return -5;
			}
			else if (cmd.equals("revive"))
			{
				if(alterStatus.charAt(1)=='0')
					return -3;
				// 使表项生效
				alterStatus.setCharAt(1, '0');
				String res= sRouterList.put(fileHashCode, alterStatus.toString());
				if(res.equals(alterStatus.toString()))
					return -5;
			}
			else if (cmd.equals("hid"))
			{
				if(alterStatus.charAt(0)=='1')
					return -3;
				// 隐藏表项
				alterStatus.setCharAt(0, '1');
				sRouterList.put(fileHashCode, alterStatus.toString());
			}
			else if (cmd.equals("pub"))
			{
				if(alterStatus.charAt(0)=='0')
					return -3;
				// 公布表项
				alterStatus.setCharAt(0, '0');
				sRouterList.put(String.valueOf(f.hashCode()), alterStatus.toString());
			}
			else
			{
				return -1;
			}
		}
		return 0;
	}
	
	
	String getFilePathByHash(String hash)
	{
		//哈希值为0，说明获取的是CyberPotato akp
		if(hash.equals("0")){
			String fPath= mRouterFile.getParent()+File.separator+"CyberPotato.apk";
			File f=new File(fPath);
			if(f.isFile())
				return fPath;
			else
				return null;
		}

		String filePath= sRouterList.get(hash);
		File f;
		if(filePath==null||filePath.charAt(0)=='1')
		{
			return null;
		}
		else
		{
			f=new File(filePath.substring(2));
			if(!f.isFile())
			{
				//如果路径无效说明路由已经失效，更新路由表和路由文件
				if(updateFileList("death",String.valueOf(f.hashCode()))==0)
				{
					writeFileListToCommunicator();
					writeFileListToRouteFile();
				}
				return null;
			}
			else
			{
				//检查文件之前是否被标记失效，如果文件存在再次生效
				if(updateFileList("revive",String.valueOf(f.hashCode()))==0)
				{
					writeFileListToCommunicator();
					writeFileListToRouteFile();
				}
				return filePath.substring(2);
			}
		}
	}
	
	//遍历内存中的fileList，检查fileList中路由的有效性，并更新至route和fileList.html文件
	private int routerValidityUpdate()
	{
		int res;
		int update=-1;
		for (Entry<String, String> entry : sRouterList.entrySet())
		{
			String key = entry.getKey();
			String filePath = entry.getValue().substring(2);//去掉两个状态码
			if(new File(filePath).isFile())
				res = updateFileList("revive", key);// 失效的路径生效
			else
				res = updateFileList("death", key);// 文件路径失效
			if(res==0)
				update=0;
		}
		if(update==0)
		{
			writeFileListToCommunicator();
			writeFileListToRouteFile();
		}
		return update;
	}

	//更改网页发布根目录
	void setFileListHtmlPath(File fileListHtml){
		synchronized(fileListHtmlLock) {
			mFileListHtml = fileListHtml;
			//如果不存在fileList.html文件则创建
			if (!mFileListHtml.isFile())
			{
				try {
					mFileListHtml.createNewFile();
					writeFileListToCommunicator();
				} catch (IOException ignored) {}
			}
		}
	}

	//删除路由
	public int deleteRouter(String router){
		int re= updateFileList("del", router);
		if (re == 0)
		{
			writeFileListToRouteFile();
			writeFileListToCommunicator();
			//更新日志
			updateEventLogListener("NORM","delete router|delete",router);
		}
		return re;
	}
	//添加路由
	public int addRouter(String filePath){
		if(filePath==null) return -6;
		int re= updateFileList("add", filePath);
		if (re == 0)
		{
			writeFileListToRouteFile();
			writeFileListToCommunicator();
			//日志
			String cause=filePath;
			updateEventLogListener("NORM","add router|add",cause);
		}
		return re;
	}
	//添加多路由
	public int addRouter(List<String> filePathList){
		if(filePathList==null) return -6;
		int re=-1;
		for(String router:filePathList) {
			re = updateFileList("add", router);
			if(re==0){
				//日志
				String cause=router;
				updateEventLogListener("NORM","add router|add",cause);
			}
		}
		if (re == 0)
		{
			writeFileListToRouteFile();
			writeFileListToCommunicator();
		}
		return re;
	}
	//隐藏路由
	public int hideRouter(String router){
		int re= updateFileList("hid", router);
		if (re == 0)
		{
			writeFileListToRouteFile();
			writeFileListToCommunicator();
			//日志
			updateEventLogListener("NORM","hide router|hide",router);
		}
		return re;
	}
	//发布路由
	public int publishRouter(String router){
		int re= updateFileList("pub", router);
		if (re == 0)
		{
			writeFileListToRouteFile();
			writeFileListToCommunicator();
			//日志
			updateEventLogListener("NORM","publish router|publish",router);
		}
		return re;
	}

	/**
	 * 用于实时检查路由项的可用性并更新
	 */
	private class RouterValidityRealtimeUpdateThread extends Thread
	{
		public void run()
		{
			for(;;)
			{
				routerValidityUpdate();
				try
				{
					sleep(3000);
				}
				catch (InterruptedException ignored) { }
			}
		}
	}

	private void updateEventLogListener(String level,String event,String cause){
		EventLogManager.updateEventLogListener(level,event,cause);
	}

	//get
	Map<String,String> getRouterList(){
		return sRouterList;
	}

	//注册发布文件列表监听器
	void addFileListListener(DynamicalMap.DynamicalMapListener fileListListener){
		((DynamicalMap)sRouterList).addDynamicalMapListener(fileListListener);
	}
	//卸载发布文件列表监听器
	void removeFileListListener(DynamicalMap.DynamicalMapListener fileListListener){
		((DynamicalMap)sRouterList).removeDynamicalMapListener(fileListListener);
	}
}
