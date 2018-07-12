package com.embedsky.hicamera.bean;

import com.hichip.content.HiChipDefines;

public class MyFileInfo {
    private boolean isDownloaded;
    private HiChipDefines.HI_P2P_FILE_INFO file_info;
    private String FileName;

    public MyFileInfo() {
        this.isDownloaded = false;
        this.file_info = null;
        this.FileName = null;
    }

    public MyFileInfo(boolean isDownloaded, HiChipDefines.HI_P2P_FILE_INFO file_info, String fileName) {
        this.isDownloaded = isDownloaded;
        this.file_info = file_info;
        this.FileName = fileName;
    }

    public boolean isDownloaded() {
        return isDownloaded;
    }

    public void setDownloaded(boolean downloaded) {
        isDownloaded = downloaded;
    }

    public HiChipDefines.HI_P2P_FILE_INFO getFile_info() {
        return file_info;
    }

    public void setFile_info(HiChipDefines.HI_P2P_FILE_INFO file_info) {
        this.file_info = file_info;
    }

    public String getFileName() {
        return FileName;
    }

    public void setFileName(String fileName) {
        FileName = fileName;
    }

    @Override
    public String toString() {
        return "MyFileInfo{" +
                "isDownloaded=" + isDownloaded +
                ", file_info=" + file_info +
                ", FileName='" + FileName + '\'' +
                '}';
    }
}
