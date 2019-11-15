package com.codig.CyberPotato.service;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

/**
 * 将http请求报文进行拆解
 * 
 * @author codig_work@outlook.com
 * 
 *
 */
class HttpRequestMessage implements IHttpRequestLine
{
	// 报文方法
	private String mMethod;
	// 请求的uri
	private String mUri;
	//uri中的参数，未经过处理
	private String mRawUriParams;
	//请求的文件range，浏览器使用1.1协议的异常情况下会出现数组
	private String[] mRangeArr;
	//文件分隔符
	private String mFileBoundary=null;
	//客户端请求的servlet，目前只有download，用于下载
	private String mServlet;
	// 存放get或post参数键值对
	private Map<String, String> mParamList;
	//接收到的文件绝对路径
	private String mReceiveFilePath;
	//存放接收文件的文件夹
	private String mDownloadDir;
	//接收的数据流
	private WeakReference<BufferedReader> mWeakReferenceHttpMessage;

	HttpRequestMessage( BufferedInputStream rawHttpMessageStream,String downloadDir) throws UnsupportedEncodingException
	{
		BufferedReader httpMessage = new BufferedReader(new InputStreamReader(rawHttpMessageStream, "ISO-8859-1"));
		mWeakReferenceHttpMessage = new WeakReference<>(httpMessage);
		mDownloadDir = downloadDir;
	}
	
	/**
	 * 解析起始行
	 */
	public boolean resolveRequestLine() throws IOException {
		BufferedReader httpMessage=mWeakReferenceHttpMessage.get();
		if(httpMessage==null){
			return false;
		}
		String requestLine;
		// 获取请求行，确认请求报类型和uri
		//readLine不进行非null检查，客户端发送无内容数据会产生nullPoint异常
		if((requestLine = httpMessage.readLine())!=null)
		{
			String[] requestLineParm=requestLine.split(" ");
			mMethod = requestLineParm[0];
			if(requestLineParm.length>1)
				mUri = URLDecoder.decode(requestLineParm[1], "UTF-8");
		}
		else{
			//返回false的情况下仍然要使用请求行，外层调用需要检查mMethod和mUri是否为null
			return false;
		}

		//起始行格式出错
		if(mMethod.isEmpty()||mUri.isEmpty()){
			return false;
		}
		//分离get参数和请求地址
		String[] pathAndParams=mUri.split("[?]");
		mUri=pathAndParams[0];
		if(pathAndParams.length>1)
			mRawUriParams=pathAndParams[1];
		return true;
	}

	/**
	 * 获取报文头部或body的参数以及取出文件字节码
	 */
	public boolean resolveParamsOrFile() throws IOException {
		BufferedReader httpMessage=mWeakReferenceHttpMessage.get();
		if(httpMessage==null){
			return false;
		}

		// 根据请求方式不同，解析报文
		if ("GET".equals(mMethod)||"HEAD".equals(mMethod))
		{
			// 如果存在get参数则取出
			if (mRawUriParams!=null && !mRawUriParams.isEmpty())
			{
				mParamList = new HashMap<>();
				String[] paramAndVal = mRawUriParams.split("&");
				for (String aParamAndVal : paramAndVal) {
					String[] kv = aParamAndVal.split("=",2);
					if (!kv[0].isEmpty()&&!kv[1].isEmpty()) {
						mParamList.put(kv[0], kv[1]);
					}
				}
				//存在参数，则说明可能请求servlet功能
				if(!mParamList.isEmpty())
				{
					String servlet= mUri.substring(1);
					//目前只有download功能
					if("download".equals(servlet)&& mParamList.containsKey("file"))
						this.mServlet =servlet;
				}
			}

			//取出报文头的range参数，针对部分只能使用http1.1协议的浏览器的数据请求
			String line;
			int tempIndex;// 临时存放字符串中的索引
			//遍历报文头部至第一个"\r\n"
			while (!(line = httpMessage.readLine()).isEmpty())
			{
				// 查找Range字段
				tempIndex = line.indexOf("Range: bytes=");
				if (tempIndex != -1)
				{
					mRangeArr = line.substring(tempIndex + 13).split(",");
				}
			}
		}
		else if ("POST".equals(mMethod))
		{
			String line;// 临时存放
			int tempIndex;// 临时存放字符串中的索引

			// 遍历头部取出分隔符,获取到空字符即为结束
			long contentLength = 0;//body长度
			while (!(line = httpMessage.readLine()).equals(""))
			{
				// 查找分隔符
				tempIndex = line.indexOf("boundary=");
				if (tempIndex != -1)
				{
					mFileBoundary = "--" + line.substring(tempIndex + 9);// 分隔符在使用时前会加两横杆
				}
				// 查找报文长度
				tempIndex = line.indexOf("Content-Length: ");
				if (tempIndex != -1)
				{
					contentLength = Integer.valueOf(line.substring(tempIndex + 16));// body长度
				}
			}

			//获取body数据
			char[] charRawBodyChunk = new char[HttpServer.recvChunkMaxSize];// body的数据分片
			long totalReadByteSizeOfBody=0;//已经读取的字节数
			String strRawBodyChunk;// body的数据分片
			int readSize;//每次成功获取的字节数

			//取出第一个切片并递增取出的字节数
			readSize=httpMessage.read(charRawBodyChunk);
			totalReadByteSizeOfBody+=readSize;
			strRawBodyChunk = String.valueOf(charRawBodyChunk,0,readSize);//转换为String

			//无文件分隔符说明body中是参数
			if (mFileBoundary==null)
			{
				mParamList = new HashMap<>();
				//无分隔符则说明只传送参数，post参数一般不多，直接使用+拼接字符串，可放于内存中
				//如果未到末尾，循环取出剩下的切片拼接成完整的body
				while(totalReadByteSizeOfBody!=contentLength)
				{
					readSize=httpMessage.read(charRawBodyChunk);
					strRawBodyChunk += String.valueOf(charRawBodyChunk, 0, readSize);
					totalReadByteSizeOfBody+=readSize;
				}
				String[] parmArr = strRawBodyChunk.split("&");
				for (String aParmArr : parmArr) {
					tempIndex = aParmArr.indexOf("=");
					if (tempIndex != -1) {
						if (aParmArr.split("=").length != 1)
							mParamList.put(aParmArr.split("=")[0], aParmArr.split("=")[1]);
						else
							mParamList.put(aParmArr.split("=")[0], "");
					}
				}
			}
			else{
				//存在分隔符，说明可能为文件，本服务器只支持一个表单传送一个文件
				//取出参数名
				//tempIndex = strRawBodyChunk.indexOf("name=\"") + 6;
				//String name = strRawBodyChunk.substring(tempIndex, strRawBodyChunk.indexOf("\"", tempIndex));
				//取出文件名
				tempIndex = strRawBodyChunk.indexOf("filename=\"");
				String tempFilePath = strRawBodyChunk.substring(tempIndex + 10, strRawBodyChunk.indexOf("\"", tempIndex + 10));
				tempFilePath=new String(tempFilePath.getBytes("ISO-8859-1"),"utf-8");
				File tempFile = new File(tempFilePath.trim());
				//切除body中的参数部分保留文件数据
				tempIndex=strRawBodyChunk.indexOf("\r\n\r\n")+4;
				strRawBodyChunk=strRawBodyChunk.substring(tempIndex);

				//将数据流输出到文件
				String downloadDir=mDownloadDir;
				FileOutputStream fos=null;
				try {
					File pathCheck=new File(downloadDir);
					if(!pathCheck.isDirectory())
					{
						pathCheck.mkdirs();
					}
					fos = new FileOutputStream(downloadDir+File.separator+tempFile.getName());
					if(totalReadByteSizeOfBody==contentLength)
					{
						//如果已到达文件末尾
						//切除末尾的分隔符
						strRawBodyChunk = strRawBodyChunk.substring(0, strRawBodyChunk.length()-mFileBoundary.length()-6);
						fos.write(strRawBodyChunk.getBytes("ISO-8859-1"));
					}
					else
					{
						//未到达文件末尾
						//将第一个切片输出到文件
						fos.write(strRawBodyChunk.getBytes("ISO-8859-1"));
						//循环取出剩下的切片
						while(true)
						{
							readSize=httpMessage.read(charRawBodyChunk);
							totalReadByteSizeOfBody+=readSize;
							if(totalReadByteSizeOfBody!=contentLength)
							{
								strRawBodyChunk = String.valueOf(charRawBodyChunk,0, readSize);
								fos.write(strRawBodyChunk.getBytes("ISO-8859-1"));
							}
							else
							{
								//切除结尾分隔符
								strRawBodyChunk = String.valueOf(charRawBodyChunk,0, readSize-mFileBoundary.length()-6);
								fos.write(strRawBodyChunk.getBytes("ISO-8859-1"));
								break;
							}
						}
					}
					fos.close();
					//记录获取的文件放置路径
					mReceiveFilePath =downloadDir+File.separator+tempFile.getName();
				}catch(Exception e) {
					//捕获全部异常，因为substring也可能抛出异常，如果不捕获不能释放fos
					//释放输出流
					if(fos!=null) fos.close();
					//传送失败删除文件
					File file=new File(downloadDir+File.separator+tempFile.getName());
					if(file.isFile())
					{
						file.delete();
					}
					//文件获取失败，只保存文件名
					mReceiveFilePath =tempFile.getName();
					return false;
				}
			}
		}
		return true;
	}

	String getReceiveFilePath(){
		return this.mReceiveFilePath;
	}

	// 设置请求报文的方法
	public void setMethod(String method)
	{
		mMethod =method;
	}

	// 返回请求报文的方法
	public String getMethod()
	{
		return mMethod;
	}

	// 设置获取的资源地址
	public void setURI(String uri)
	{
		mUri=uri;
	}

	// 客户请求获取的资源地址
	public String getURI()
	{
		return mUri;
	}
	
	//获取请求的servlet类型
	String getServletType()
	{
		return mServlet;
	}

	//获取请求的文件字节范围
	String[] getRange()
	{
		return this.mRangeArr;
	}
	
	// get或post方法的参数，post参数有可能是文件
//	public Map<String,String> getParmList()
//	{
//		return this.mParamList;
//	}

//	public Map<String,String> getRouterList()
//	{
//		return this.fileList;
//	}
	
	//根据参数名获取参数
	String getParm(Object PramName)
	{
		return mParamList.get(PramName);
	}
}
