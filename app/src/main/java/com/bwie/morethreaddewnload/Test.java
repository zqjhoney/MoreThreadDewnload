package com.bwie.morethreaddewnload;

import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by 张乔君 on 2017/11/22.
 */

public class Test {
    private static int threadCount=3;
    private static int activeThread;
    public static void main(String[] args) throws Exception {
        String path="http://120.27.23.105/version/baidu.apk";
        URL url=new URL(path);
        HttpURLConnection conn= (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5000);
        conn.setRequestMethod("GET");
        if(conn.getResponseCode()==200){//判断编码

            int length=conn.getContentLength();
            RandomAccessFile file=new RandomAccessFile("baidu.apk","rw");
            file.setLength(length);
            file.close();//

            //假设有3个线程去下载资源
            int blocksize=length/threadCount;
            for (int threadid = 1; threadid <=threadCount ; threadid++) {
                //开始位置
                int startIndex=blocksize*(threadid-1);
                int endIndex=blocksize*threadid-1;//结束位置
                if(threadid==threadCount){//最后一个
                    endIndex=length;
                }

                System.out.println("线程【" + threadid + "】开始下载：" + startIndex + "---->" + endIndex);

                new DownLoadThread(path,threadid,startIndex,endIndex).start();//创建子线程
                activeThread++;
                System.out.println("当前线程的个数是："+threadid);

            }
        }

    }

    public static class DownLoadThread extends Thread{

        private String path;
        private int start;
        private  int end;
        private int threadId;
        public DownLoadThread(String path, int threadId, int start,
                              int end) {
            this.path = path;
            this.threadId = threadId;
            this.start = start;
            this.end = end;
        }

        @Override
        public void run() {
            super.run();
            try {
                     File file=new File(threadId+".txt");
                    if(file.exists()){//判断文件是否存在
                        FileInputStream fis = new FileInputStream(file);
                        byte[] temp = new byte[1024];
                        int length = fis.read(temp);
                        //读取到已经下载的位置
                        int downloadNewIndex = Integer.parseInt(new String(temp, 0, length));
                        //设置重新开始下载的开始位置
                        start = downloadNewIndex;
                        fis.close();
                        //显示真实下载数据的区间
                        System.out.println("线程【" + threadId + "】真实开始下载数据区间：" + start + "---->" + end);

                     }//否则


                 URL url=new URL(path);
                    HttpURLConnection conn= (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(5000);
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Range","bytes="+start+"-"+end);//设置线程下载文件的范围
                int code=conn.getResponseCode();
                if(206==code){
                    InputStream inputStream = conn.getInputStream();
                    RandomAccessFile accessFile=new RandomAccessFile("baidu.apk","rw");
                    accessFile.seek(start);//从开始位置写入数据
                    int len=0;
                    //开始写数据
                    byte[] bytes=new byte[1024];

                    int totle=0;
                    while(((len=inputStream.read(bytes)) != -1)){
                        RandomAccessFile threadidfile=new RandomAccessFile(threadId+"xx"+".txt","rw");
                        accessFile.write(bytes,0,len);
                        totle+=len;//计算总长度
                        threadidfile.write((start+totle+"").getBytes());//将下载到呢，保存下来
                        threadidfile.close();

                    }
                    //关闭其他资源
                    inputStream.close();
                    accessFile.close();
                    System.out.println("线程【" + threadId + "】下载完毕");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }finally {
                //活动的线程数减少
                activeThread--;
                if (activeThread == 0) {
                    for (int i = 1; i <= threadCount; i++) {
                        File tempFile = new File(i + ".txt");
                        tempFile.delete();
                    }
                    System.out.println("下载完毕，已清除全部临时文件");
                }
            }

        }
    }
}
