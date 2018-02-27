package com.example.zym.downloadservice;

import android.os.AsyncTask;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Copyright (c) 18/2/26 by  
 *
 * @author lixin
 * @version V1.0
 * @ClassName: ${FILE_NAME}
 * @Description: ${todo}
 * @Date 18/2/26 15:45
 **/
//// // TODO: 18/2/26 实现下载文件的逻辑
public class DownloadTask extends AsyncTask<String, Integer, Integer> {
    public static final int TYPE_SUCCESS = 0;
    public static final int TYPE_FAILED = 1;
    public static final int TYPE_PAUSED = 2;
    public static final int TYPE_CANCELED = 3;

    private DownloadListenter listenter;
    private boolean isCanceled = false;
    private boolean isPaused = false;
    private int lastProgress;

    //将下载的状态通过listenter进行回调
    public DownloadTask(DownloadListenter listenter) {
        this.listenter = listenter;
    }

    //在后台执行具体的下载逻辑
    @Override
    protected Integer doInBackground(String... params) {
        InputStream is = null;
        RandomAccessFile saveFile = null;
        File file = null;

        try {
            long downloadedLength = 0;//记录已下载的文件长度
            String dowanloadUrl = params[0];
            String fileName = dowanloadUrl.substring(dowanloadUrl.lastIndexOf("/"));
            //将文件下载到指定目录下
            String directory = Environment.getExternalStoragePublicDirectory
                    (Environment.DIRECTORY_DOWNLOADS).getPath();
            file = new File(directory + fileName);
            //判断当前目录下是否存在要下载的文件，如果存在则读取已下载的字节数
            if (file.exists()) {
                downloadedLength = file.length();
            }

            //调用getContentLength方法获取待下载文件的总长度
            long contentLength = getContentLength(dowanloadUrl);
            //如果文件总长度为0，说明文件有问题，返回TYPE_FAILED
            if (contentLength == 0) {
                return TYPE_FAILED;

            //如果文件总长度等于已下载文件的长度
            } else if (contentLength == downloadedLength) {
                //已下载字节与文件总字节相等，说明已经下载完成了
                return TYPE_SUCCESS;
            }

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    //断点下载，指定从哪个字节开始下载
                    .addHeader("RANGE", "bytes=" + downloadedLength + "-")
                    .url(dowanloadUrl)
                    .build();
            Response response = client.newCall(request).execute();
            if (response != null) {
                is = response.body().byteStream();
                saveFile = new RandomAccessFile(file, "rw");
                saveFile.seek(downloadedLength);//跳过已下载的字节
                byte[] b = new byte[1024];
                int total = 0;
                int len;
                while ((len = is.read(b)) != -1) {
                    if (isCanceled) {
                        return TYPE_CANCELED;
                    } else if (isPaused) {
                        return TYPE_PAUSED;
                    } else {
                        total += len;
                        saveFile.write(b, 0, len);
                        //计算已下载的百分比
                        int progress = (int) ((total + downloadedLength) * 100 / contentLength);
                        publishProgress(progress);
                    }
                }
                response.body().close();
                return TYPE_SUCCESS;
            }


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (saveFile != null) {
                    saveFile.close();
                }
                if (isCanceled && file != null) {
                    file.delete();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return TYPE_FAILED;
    }

    //在界面上更新当前的下载进度
    @Override
    protected void onProgressUpdate(Integer... values) {
        //从参数中获取到当前的下载进度
        int progress = values[0];
        //当前下载进度与上次下载进度进行对比，如果有变化就调用DownloadListenter类的onProgress方法来通知下载进度更新
        if (progress > lastProgress) {
            listenter.onProgress(progress);
            lastProgress = progress;
        }
    }

    //通知最终的下载结果
    @Override
    protected void onPostExecute(Integer status) {
        switch (status) {
            case TYPE_SUCCESS:
                listenter.onSuccess();
                break;
            case TYPE_FAILED:
                listenter.onFailed();
                break;
            case TYPE_PAUSED:
                listenter.onPaused();
                break;
            case TYPE_CANCELED:
                listenter.onCanceled();
                break;
        }
    }

    public void pauseDownload() {
        isPaused = true;
    }

    public void cancelDownload() {
        isCanceled = true;
    }

    private long getContentLength(String downloadUrl) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(downloadUrl)
                .build();

        Response response = client.newCall(request).execute();
        if (response != null && response.isSuccessful()) {
            long contentLength = response.body().contentLength();
            response.close();
            return contentLength;
        }

        return 0;
    }

}
