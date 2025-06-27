package com.ffmpegbox.model;

import javafx.scene.control.ProgressBar;

import java.io.File;

public class FileTask {
    private final File file;
    private final ProgressBar progressBar;
    private String format = "mp4";
    private String resolution = "1920x1080";
    private String bitrate = "800k";

    public FileTask(File file, ProgressBar progressBar) {
        this.file = file;
        this.progressBar = progressBar;
    }

    public File getFile() {
        return file;
    }

    public ProgressBar getProgressBar() {
        return progressBar;
    }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public String getResolution() { return resolution; }

    public void setResolution(String resolution) { this.resolution = resolution; }

    public String getBitrate() { return bitrate; }

    public void setBitrate(String bitrate) { this.bitrate = bitrate; }
}
