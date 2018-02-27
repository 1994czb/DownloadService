package com.example.zym.downloadservice;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import java.io.File;

public class DownloadService extends Service {

    private DownloadTask downloadTask;
    private String downloadUrl;
    //创建一个DownloadListenter匿名实例
    private DownloadListenter listenter = new DownloadListenter() {
        @Override
        public void onProgress(int progress) {
            //构建一个用于显示下载进度的通知
            getNotificationManager().notify(1, getNotification("Downloading...", progress));
        }

        @Override
        public void onSuccess() {
            downloadTask = null;
            //下载成功时将前台服务通知关闭（下载进度的通知）
            stopForeground(true);
            //并创建一个下载成功的通知
            getNotificationManager().notify(1, getNotification("Download Success", -1));
            Toast.makeText(DownloadService.this, "Download Success", Toast.LENGTH_LONG).show();

        }

        @Override
        public void onFailed() {

            downloadTask = null;
            //下载失败时将前台服务通知关闭(下载进度的通知)
            stopForeground(true);
            //，并创建一个下载失败的通知
            getNotificationManager().notify(1, getNotification("Download Failed", -1));
            Toast.makeText(DownloadService.this, "Download Failed", Toast.LENGTH_LONG).show();

        }

        @Override
        public void onPaused() {

            downloadTask = null;
            Toast.makeText(DownloadService.this, "Pause", Toast.LENGTH_LONG).show();


        }

        @Override
        public void onCanceled() {

            downloadTask = null;
            Toast.makeText(DownloadService.this, "Cancel", Toast.LENGTH_LONG).show();


        }
    };


    private DownloadBinder mBinder = new DownloadBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    class DownloadBinder extends Binder {

        public void startDownoad(String url){
            if (downloadUrl==null){
                downloadUrl=url;
                //创建一个DownloadTask实例
                downloadTask = new DownloadTask(listenter);
                downloadTask.execute(downloadUrl);
                //创建一个正在下载的通知
                startForeground(1,getNotification("Downloading...",0));
                Toast.makeText(DownloadService.this, "Downloading...", Toast.LENGTH_LONG).show();

            }
        }

        public void pauseDownload(){
            if (downloadTask!=null){
                downloadTask.pauseDownload();
            }
        }

        public void cancelDownload(){
            if (downloadTask!=null){
                downloadTask.cancelDownload();
            }else {
                if (downloadTask!=null){
                    //取消下载时需要将文件删除
                    String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
                    String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
                    File file = new File(directory + fileName);
                    if (file.exists()){
                        file.delete();
                    }
                    //关闭正在下载的通知
                    getNotificationManager().cancel(1);
                    Toast.makeText(DownloadService.this, "Canceled", Toast.LENGTH_LONG).show();


                }
            }
        }

    }

    private NotificationManager getNotificationManager() {
        return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    private Notification getNotification(String title, int progress) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        builder.setContentIntent(pi);
        builder.setContentTitle(title);
        if (progress > 0) {
            //当progress大于等于0时才需要显示下载进度
            builder.setContentText(progress + "%");
            //第一个参数：传入通知的最大进度；第二个参数：传入当前进度；第三个参数：是否使用模糊进度条
            builder.setProgress(100, progress, false);
        }
        return builder.build();
    }
}
