package com.embedsky.hicamera.bean;

import java.util.ArrayList;
import java.util.List;

public class DownLoadTasks {
    public static final List<DownloadTask> downloadTasks = new ArrayList<DownloadTask>();

    public static void addTask(long startTime, long endTime){
        for(MyCamera myCamera : HiDataValue.CameraList){
            downloadTasks.add(new DownloadTask(startTime, endTime, myCamera.getUid()));
        }
    }
}
