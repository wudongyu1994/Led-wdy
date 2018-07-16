package cn.assassing.camtest.bean;

import java.util.concurrent.LinkedBlockingQueue;

public class DownLoadTasks {
//    public static final List<DownloadTask> downloadTasks = new ArrayList<>();
    public static final LinkedBlockingQueue<DownloadTask> downloadTasks = new LinkedBlockingQueue<>();

    public static void addTask(long startTime, long endTime){
        for(MyCamera myCamera : HiDataValue.CameraList){
//            downloadTasks.add(new DownloadTask(startTime, endTime, myCamera.getUid()));
            try {
                downloadTasks.put(new DownloadTask(startTime, endTime, myCamera.getUid()));
            }catch(Exception e){e.printStackTrace();}
        }
    }
}
