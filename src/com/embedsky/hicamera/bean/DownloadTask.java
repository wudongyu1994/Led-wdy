package com.embedsky.hicamera.bean;

import android.os.Environment;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.embedsky.hicamera.base.HiTools;
import com.embedsky.hicamera.base.LogUtil;

public class DownloadTask {
    private long startTime;
    private long endTime;
    private long warnTime;
    private String cameraUID;
    private List<MyFileInfo> file_infos;
    private String savePath;

    public DownloadTask(long startTime, long endTime, String cameraUID) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.warnTime = (startTime + endTime) / 2;
        this.cameraUID = cameraUID;
        this.file_infos = new ArrayList<MyFileInfo>();
        if (HiTools.isSDCardValid()) {
            String warnTimeStr = getTimeStr(warnTime);
            this.savePath = HiDataValue.ONLINE_VIDEO_PATH + File.separator + warnTimeStr + File.separator;
            File dirFile = new File(savePath);
            if (!dirFile.exists() || !dirFile.isDirectory()) {
                dirFile.mkdirs();
            }
        } else {
            LogUtil.d("no sdcard");
            this.savePath = null;
        }
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public String getCameraUID() {
        return cameraUID;
    }

    public void setCameraUID(String cameraUID) {
        this.cameraUID = cameraUID;
    }

    public String getSavePath() {
        return savePath;
    }

    public void setSavePath(String savePath) {
        this.savePath = savePath;
    }

    public long getWarnTime() {
        return warnTime;
    }

    public void setWarnTime(long warnTime) {
        this.warnTime = warnTime;
    }

    public List<MyFileInfo> getFile_infos() {
        return file_infos;
    }

    //    public void setFile_infos(List<MyFileInfo> file_infos) {
//        this.file_infos = file_infos;
//    }

    private String getTimeStr(long timestamp) {
        SimpleDateFormat sf2 = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA);
        return sf2.format(timestamp);
    }

    @Override
    public String toString() {
        return "DownloadTask{" +
                "startTime=" + startTime +
                ", endTime=" + endTime +
                ", warnTime=" + warnTime +
                ", cameraUID='" + cameraUID + '\'' +
                ", file_infos=" + file_infos +
                ", savePath='" + savePath + '\'' +
                '}';
    }
}
