package com.codig.CyberPotato.utils;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommonUtils {

    //将内嵌文件导出
    public static void exportResourceFromRaw(Context context,int rawId,String path)
    {
        try {
            //输入流，注意：文件大小不能超过int大小，不能导出大文件
            InputStream is = context.getApplicationContext().getResources().openRawResource(rawId);
            byte[] read=new byte[is.available()];
            is.read(read);
            is.close();
            //输出流
            File file  = new File(path);
            OutputStream os=new FileOutputStream(file);
            os.write(read);
            os.close();
        } catch (IOException ignored) { }
    }

    //获取本机所有IP
    public static String getAllAddress(Context context)
    {
        String ap="--.--.--.--";//热点的地址
        String wifi="--.--.--.--";//WiFi的地址
        String isp="--.--.--.--";//网络运营商分配的地址
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        //wifi地址
        if (wifiManager.isWifiEnabled()) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int ipAddress = wifiInfo.getIpAddress();
            wifi=(ipAddress & 0xFF) + "." +
                    ((ipAddress >> 8) & 0xFF) + "." +
                    ((ipAddress >> 16) & 0xFF) + "." +
                    (ipAddress >> 24 & 0xFF);
        }

        //AP地址
        try
        {
            Method method = wifiManager.getClass().getMethod("isWifiApEnabled");
            if((Boolean) method.invoke(wifiManager)){
                ap="192.168.43.1";
            }
        }
        catch (NoSuchMethodException ignored){ }
        catch (IllegalAccessException ignored){ }
        catch (InvocationTargetException ignored){ }

        //运营商分配的地址
        try {
            String ipv4;
            ArrayList<NetworkInterface> nilist = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface ni: nilist)
            {
                ArrayList<InetAddress>  ialist = Collections.list(ni.getInetAddresses());
                for (InetAddress address: ialist){
                    if (!address.isLoopbackAddress() && !address.isLinkLocalAddress() && address instanceof Inet4Address)
                    {
                        ipv4=address.getHostAddress();
                        if(!ipv4.equals(ap)&&!ipv4.equals(wifi))
                            isp=ipv4;
                    }
                }
            }
        } catch (SocketException ignored) { }
        return ap+"|"+wifi+"|"+isp;
    }

    /**
     * 通过uri获取真实路径
     */
    @SuppressLint("NewApi")
    public static String getPath(final Context context, final Uri uri) {
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if ("com.android.externalstorage.documents".equals(uri.getAuthority())) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String volumeUuid = split[0];
                final String storagePath=getExternalStoragePathByUuid(context,volumeUuid);
                if(storagePath!=null)
                    return storagePath + "/" + split[1];
                else
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
            }
            // DownloadsProvider
            else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                final String id = DocumentsContract.getDocumentId(uri);
                if(id.startsWith("raw")) {
                    return id.split(":")[1];
                }

                //部分系统支持本方案
                String[] contentUriPrefixesToTry = new String[]{
                        "content://downloads/public_downloads",
                        "content://downloads/my_downloads",
                        "content://downloads/all_downloads"
                };
                for (String contentUriPrefix : contentUriPrefixesToTry) {
                    Uri contentUri = ContentUris.withAppendedId(Uri.parse(contentUriPrefix), Long.valueOf(id));
                    try {
                        String path = getDataColumn(context, contentUri, null, null);
                        if (path != null) {
                            return path;
                        }
                    } catch (Exception ignored) {}
                }

                //如果上面的方案不支持则进行文件查找
                //如果文件同名不同后缀且最近修改时间相同，则只会取出第一个找到的文件
                //如果文件树过于复杂，查找带来的延迟可能会使程序因太久没响应而被系统杀死
                //获取文件信息
                String fileName=null;
                long lastModified=0;
                Cursor cursor;
                cursor = context.getContentResolver().query(uri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int column_index = cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME);
                    fileName= cursor.getString(column_index);
                    column_index = cursor.getColumnIndexOrThrow("last_modified");
                    lastModified= cursor.getLong(column_index);
                }
                if (cursor != null)
                    cursor.close();
                //去除文件名中不能包含的字符，用下划线替换
                fileName=fileName.replaceAll("[\\\\/|<>*:?\"]","_")+".";
                //获取download目录路径
                String path=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
                //系统文件的修改时间会舍弃毫秒部分，需要去除
                List<File> files =searchFile(path, fileName,lastModified/1000*1000);
                return files.get(0).getAbsolutePath();

            }
            // MediaProvider
            else if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    //通过sd卡的uuid获取sd卡路径
    private static String getExternalStoragePathByUuid(Context context,String uuid){
        StorageManager storageManager=(StorageManager)context.getApplicationContext().getSystemService(Context.STORAGE_SERVICE);
        String path=null;
        try {
            Method method=StorageManager.class.getMethod("getVolumeList");
            method.setAccessible(true);
            Object[] volumeList=(Object[])method.invoke(storageManager);
            if(volumeList!=null){
                for(int i=0;i<volumeList.length;i++){
                    String status=(String) volumeList[i].getClass().getMethod("getState").invoke(volumeList[i]);
                    String volumeUuid=(String) volumeList[i].getClass().getMethod("getUuid").invoke(volumeList[i]);
                    if(volumeUuid!=null&&volumeUuid.equals(uuid)&&status.equals("mounted")){
                        path=(String) volumeList[i].getClass().getMethod("getPath").invoke(volumeList[i]);
                        break;
                    }
                }
            }
        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
        } catch (IllegalAccessException e) {
//            e.printStackTrace();
        } catch (InvocationTargetException e) {
//            e.printStackTrace();
        }
        return path;
    }

    /**
     * 根据文件名和最后修改时间查找文件绝对路径
     */
    private static List<File> searchFile(String path, final String fileName, final long lastModified) {
        File folder=new File(path);
        final List<File> result = new ArrayList<>();
        File[] subFolders = folder.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                if (file.isDirectory()) {
                    return true;
                }
                else if (file.getName().startsWith(fileName)&&(file.lastModified()==lastModified)) {
                    result.add(file);
                }
                return false;
            }
        });
        if(result.size()!=0)
            return result;
        if (subFolders != null) {
            for (File file : subFolders) {
                // 递归
                result.addAll(searchFile(file.getPath(), fileName,lastModified));
            }
        }
        return result;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {

        Cursor cursor;
        final String column = "_data";
        final String[] projection = {column};

        cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
        if (cursor != null && cursor.moveToFirst()) {
            final int column_index = cursor.getColumnIndexOrThrow(column);
            return cursor.getString(column_index);
        }
        if (cursor != null)
            cursor.close();
        return null;
    }
}

///**
// * 根据文件名和最后修改时间多线程快速查找文件路径
// */
//class GetAbsolutePathByFileName extends Thread {
//    private static String sFileName;
//    private static long sLast_modified;
//    private static byte[] sLock = null;
//    private static File sFilePath = null;
//
//    private File folder;
//
//    public GetAbsolutePathByFileName(File folder, String fileName, long last_modified, byte[] lock) {
//        this.folder = folder;
//        GetAbsolutePathByFileName.sFileName = fileName + ".";
//        GetAbsolutePathByFileName.sLast_modified = last_modified;
//        GetAbsolutePathByFileName.sLock = lock;
//    }
//
//    private GetAbsolutePathByFileName(File folder) {
//        this.folder = folder;
//    }
//
//    public static File getResult() {
//        return sFilePath;
//    }
//
//    @Override
//    public void run() {
//        if (sFilePath != null) {
//            //System.out.println(Thread.currentThread()+" end");
//            return;
//        }
//        //System.out.println(Thread.currentThread()+" start");
//        File[] subFolders = folder.listFiles(new FileFilter() {
//            @Override
//            public boolean accept(File file) {
//                if (file.isDirectory()) {
//                    return true;
//                }
//                if (file.getName().startsWith(sFileName) && file.lastModified() == sLast_modified) {
//                    sFilePath = file;
//                    synchronized (sLock) {
//                        sLock.notify();
//                    }
//                    return true;
//                }
//                return false;
//            }
//        });
//        if (subFolders != null) {
//            for (File file : subFolders) {
//                if (sFilePath != null)
//                    break;
//                if (file.isFile()) {
//                    sFilePath = file;
//                    synchronized (sLock) {
//                        sLock.notify();
//                    }
//                    break;
//                } else {
//                    Thread th = new GetAbsolutePathByFileName(file);
//                    th.setDaemon(true);
//                    th.start();
//                }
//            }
//        }
//        //System.out.println(Thread.currentThread()+" end");
//    }
//}
