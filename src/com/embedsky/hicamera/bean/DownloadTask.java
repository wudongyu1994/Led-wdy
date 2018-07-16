package cn.assassing.camtest.bean;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import cn.assassing.camtest.base.HiTools;
import cn.assassing.camtest.base.LogUtil;

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
        this.file_infos = new ArrayList<>();
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

    public boolean isFinish(){

        //首先必须包含了全部的时间
        if(isInfo()){
            //所有的file都下载了
            for(MyFileInfo myFileInfo : this.file_infos){
                File file = new File(this.savePath + myFileInfo.getFileName() + ".avi");
                File file2 = new File(this.savePath + myFileInfo.getFileName() + ".mp4");
                File file3 = new File(this.savePath + myFileInfo.getFileName() + ".h264");
                if (!(file.exists() || file2.exists() || file3.exists())) {// 文件没有下载过
                    return false;
                }
            }
            return true;
        }else{
            return false;
        }
    }

    public boolean isInfo(){
        long minTime = Long.MAX_VALUE;
        long maxTime = Long.MIN_VALUE;
        for(MyFileInfo myFileInfo : this.file_infos){
            if(myFileInfo.getFile_info().sStartTime.getTimeInMillis() < minTime)
                minTime = HiTools.getReadMillins(myFileInfo.getFile_info().sStartTime);
            if(myFileInfo.getFile_info().sEndTime.getTimeInMillis() > maxTime)
                maxTime = HiTools.getReadMillins(myFileInfo.getFile_info().sEndTime);
        }
        if(minTime <= this.startTime && maxTime >= this.endTime)
            return true;
        else
            return false;
    }
}
