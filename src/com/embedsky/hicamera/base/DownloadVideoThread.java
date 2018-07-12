package com.embedsky.hicamera.base;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;

import com.hichip.callback.ICameraDownloadCallback;
import com.hichip.callback.ICameraIOSessionCallback;
import com.hichip.content.HiChipDefines;
import com.hichip.control.HiCamera;

import java.io.File;
import java.util.List;

import com.embedsky.hicamera.bean.DownLoadTasks;
import com.embedsky.hicamera.bean.DownloadTask;
import com.embedsky.hicamera.bean.MyCamera;
import com.embedsky.hicamera.bean.MyFileInfo;

public class DownloadVideoThread extends Thread implements ICameraIOSessionCallback, ICameraDownloadCallback {

    private int TaskIndex;
    private int status;
    private DownloadTask downLoadTask;
    private MyCamera myCamera;
    private boolean downloadFlag;
    private Handler outHandler;

    private static int STATUS_INIT = 0;
    private static int STATUS_RUNNING = 1;
    private static int STATUS_QUERYING = 2;
    private static int STATUS_QUERYEND = 3;
    private static int STATUS_DOWNLOADING = 4;
    private static int STATUS_DOWNLOADED = 5;
    private static int STATUS_EXCEPTION = 6;

    public DownloadVideoThread(Handler outHandler) {
        TaskIndex = 0;
        status = STATUS_INIT;
        downloadFlag = false;
        this.outHandler = outHandler;
    }

    private void registe(){
        myCamera.registerIOSessionListener(this);
        myCamera.registerDownloadListener(this);
    }
    private void unregiste(){
        myCamera.unregisterIOSessionListener(this);
        myCamera.unregisterDownloadListener(this);
    }

    @SuppressLint("HandlerLeak")
    private final Handler inHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    };
    private final Object SynObject = new Object();
    @Override
    public void run() {
        synchronized (SynObject){
            try {
                status = STATUS_RUNNING;
                while(true){
                    if(this.isInterrupted()){
                        break;
                    }
                    if(DownLoadTasks.downloadTasks.size() > 0){
                        this.downLoadTask = DownLoadTasks.downloadTasks.get(TaskIndex);
                        LogUtil.d("now Task:"+downLoadTask);
                        boolean downloaded = true;
                        if(downLoadTask.getFile_infos().size() < 2){
                            downloaded = false;
                        }else{
                            for(MyFileInfo myFileInfo : downLoadTask.getFile_infos()){
                                if(myFileInfo.isDownloaded()){
                                    downloaded = false;
                                    break;
                                }
                            }
                        }
                        myCamera = MyCamera.getByUID(downLoadTask.getCameraUID());
                        if(!downloaded  &&
                                downLoadTask.getSavePath() != null && myCamera != null &&
                                System.currentTimeMillis() - downLoadTask.getWarnTime() > 4*60*1000){
                            registe();
                            if(downLoadTask.getFile_infos().size() < 2){//查询fileinfo
                                status = STATUS_QUERYING;
                                queryFileInfo();
                                Thread.sleep(5000);
                                status = STATUS_QUERYEND;
                            }
                            //下载视频
                            LogUtil.d("downloadVideo is called");
                            status = STATUS_DOWNLOADING;
                            if (HiTools.isSDCardValid()) {
                                List<MyFileInfo> myFileInfoList = downLoadTask.getFile_infos();
                                if(myFileInfoList.size() > 0){
                                    for (MyFileInfo myFileInfo : myFileInfoList) {
                                        LogUtil.d("In for, myfileinfo:" + myFileInfo);
                                        if(!myFileInfo.isDownloaded()){
                                            LogUtil.d("DownLoading : CamaraUID = " + downLoadTask.getCameraUID() + ", " + myFileInfo.getFile_info().sStartTime.toString() + "-" + myFileInfo.getFile_info().sEndTime.toString());
                                            File file = new File(downLoadTask.getSavePath() + myFileInfo.getFileName() + ".avi");
                                            File file2 = new File(downLoadTask.getSavePath() + myFileInfo.getFileName() + ".mp4");
                                            File file3 = new File(downLoadTask.getSavePath() + myFileInfo.getFileName() + ".h264");
                                            if (file.exists() || file2.exists() || file3.exists()) {// 文件已下载过
                                                LogUtil.d("file exists");
                                                continue;
                                            }
                                            myCamera.startDownloadRecording2(myFileInfo.getFile_info().sStartTime, downLoadTask.getSavePath(), myFileInfo.getFileName(), 2);
                                            downloadFlag = true;
                                            while(downloadFlag){}
                                        }else{
                                            LogUtil.d("file already downloaded");
                                        }
                                    }
                                }
                            } else {
                                LogUtil.d("no sdcard");
                            }
                            status = STATUS_DOWNLOADED;
                            unregiste();
                        }
                        if(++TaskIndex == DownLoadTasks.downloadTasks.size()) {
                            Thread.sleep(1000*60);
                            TaskIndex = 0;
                        }
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
                status = STATUS_EXCEPTION;
            }
        }
    }

    //发送query命令
    private void queryFileInfo() {
        LogUtil.d("queryFileInfo is called " + downLoadTask.getCameraUID() + " : " +downLoadTask.getStartTime() + " - " + downLoadTask.getEndTime());
        MyCamera myCamera = MyCamera.getByUID(downLoadTask.getCameraUID());
        if (myCamera != null) {
            LogUtil.d("queryFile command send : " + (HiTools.sdfTimeSec(downLoadTask.getStartTime()) + " - " + HiTools.sdfTimeSec(downLoadTask.getEndTime())));
            if(myCamera.getConnectState() == MyCamera.CAMERA_CONNECTION_STATE_DISCONNECTED){
                LogUtil.d("camera connect state : disconnected");
            }else if(myCamera.getConnectState() == MyCamera.CAMERA_CONNECTION_STATE_CONNECTING){
                LogUtil.d("connect state : connecting");
            }else if(myCamera.getConnectState() == MyCamera.CAMERA_CONNECTION_STATE_CONNECTED){
                LogUtil.d("connect state : connected");
            }else{
                LogUtil.d("connect state : unstate-" + myCamera.getConnectState());
            }

            if (myCamera.getCommandFunction(HiChipDefines.HI_P2P_PB_QUERY_START_NODST)) {
                myCamera.sendIOCtrl(HiChipDefines.HI_P2P_PB_QUERY_START_NODST, HiChipDefines.HI_P2P_S_PB_LIST_REQ.parseContent(0, downLoadTask.getStartTime(), downLoadTask.getEndTime(), HiChipDefines.HI_P2P_EVENT_ALL));
            } else {
                myCamera.sendIOCtrl(HiChipDefines.HI_P2P_PB_QUERY_START, HiChipDefines.HI_P2P_S_PB_LIST_REQ.parseContent(0, downLoadTask.getStartTime(), downLoadTask.getEndTime(), HiChipDefines.HI_P2P_EVENT_ALL));
            }
        }else{
            LogUtil.d("myCamera == null");
        }
    }

    //处理query命令的返回
    @Override
    public void receiveIOCtrlData(HiCamera hiCamera, int type, byte[] data, int extre) {
        LogUtil.d("RECEIVE IOCTRL" + hiCamera + ";" + type + ";" + data.toString() + ";" + extre);
        if (extre == -1) {// IO的错误码
            LogUtil.d("connect to server error");
        }else if (extre == 0) {
            if(status == STATUS_QUERYING && (type == HiChipDefines.HI_P2P_PB_QUERY_START_NODST || type == HiChipDefines.HI_P2P_PB_QUERY_START)){
                LogUtil.d("data.length = "+data.length);
                if (data.length >= 12) {
//                      byte flag = data[8];// 数据发送的结束标识符
                    int cnt = data[9]; // 当前包的文件个数
                    if (cnt > 0) {
                        for (int i = 0; i < cnt; i++) {
                            int pos = 12;
                            int size = HiChipDefines.HI_P2P_FILE_INFO.sizeof();
                            byte[] t = new byte[24];
                            System.arraycopy(data, i * size + pos, t, 0, 24);
                            HiChipDefines.HI_P2P_FILE_INFO file_info = new HiChipDefines.HI_P2P_FILE_INFO(t);
                            long duration = file_info.sEndTime.getTimeInMillis() - file_info.sStartTime.getTimeInMillis();
                            LogUtil.d("duration:"+duration);
                            if (duration <= 1000 * 1000 && duration > 0) { // 1000秒，文件录像一般为15分钟，但是有可能会长一点所有就设置为1000
                                boolean isNew = true;
                                for (MyFileInfo myFileInfo : downLoadTask.getFile_infos()) {
                                    if(myFileInfo.getFile_info().sStartTime.getTimeInMillis() == file_info.sStartTime.getTimeInMillis()){
                                        isNew = false;
                                        break;
                                    }
                                }
                                if(isNew){
                                    String fileName = myCamera.getUid() + "-" + String.valueOf(file_info.sStartTime.getTimeInMillis()) + "-" + String.valueOf(file_info.sEndTime.getTimeInMillis());
                                    MyFileInfo myFileInfo = new MyFileInfo(false, file_info, fileName);
                                    downLoadTask.getFile_infos().add(myFileInfo);
                                    LogUtil.d("add fileinfo:" + downLoadTask.getCameraUID() + "," + myFileInfo.getFile_info().sStartTime.toString() + " - " + myFileInfo.getFile_info().sEndTime.toString());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    //处理下载状态返回
    @Override
    public void callbackDownloadState(HiCamera camera, int total, int curSize, int state, String path) {
        if(status == STATUS_DOWNLOADING){
            switch (state) {
                case DOWNLOAD_STATE_START:
                    LogUtil.d("download start " + path);
                    break;
                case DOWNLOAD_STATE_DOWNLOADING:
                    float d;
                    if (total == 0) {
                        d = curSize * 100 / (1024 * 1024);
                    } else {
                        d = curSize * 100 / total;
                    }
                    if (d >= 100) {
                        d = 99;
                    }
                    int rate = (int) d;
                    String rateStr;
                    if (rate < 10) {
                        rateStr = " " + rate + "%";
                    } else {
                        rateStr = rate + "%";
                    }
                    LogUtil.d("download start " + path + " : " + rateStr);
                    break;
                case DOWNLOAD_STATE_END:
                    LogUtil.d("download finish," + path);
                    downloadFlag = false;
                    break;
                case DOWNLOAD_STATE_ERROR_PATH:
                    LogUtil.d("DOWNLOAD_STATE_ERROR_PATH," + path);
                    if(camera != null)
                        camera.stopDownloadRecording();
                    downloadFlag = false;
                    break;
                case DOWNLOAD_STATE_ERROR_DATA:
                    LogUtil.d("DOWNLOAD_STATE_ERROR_DATA," + path);
                    if(camera != null)
                        camera.stopDownloadRecording();
                    downloadFlag = false;
                    break;
            }
        }
    }

    @Override
    public void receiveSessionState(HiCamera arg0, int arg1) {

        LogUtil.d("receiveSessionState is called");
        switch (arg1) {
            case HiCamera.CAMERA_CONNECTION_STATE_DISCONNECTED:
                LogUtil.d("disconnect");
//                this.interrupt();
                break;
        }
    }
}
