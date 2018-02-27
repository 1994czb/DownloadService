package com.example.zym.downloadservice;

/**
 * Copyright (c) 18/2/26 by  
 *
 * @author lixin
 * @version V1.0
 * @ClassName: ${FILE_NAME}
 * @Description: ${todo}
 * @Date 18/2/26 15:11
 **/
//// // TODO: 18/2/26 定义一个回调接口，用于对下载过程中的各种状态进行监听和回调
public interface DownloadListenter {
    //下载进度
    void onProgress(int progress);
    //成功
    void onSuccess();
    //失败
    void onFailed();
    //暂停
    void onPaused();
    //取消下载
    void onCanceled();

}
