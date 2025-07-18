package com.ffmpegbox.model;

public class UserSettings {
    private String ffmpegPath;
    private String resolution;
    private String bitrate;
    private String format;
    private String lastOutputDir;
    private String videoCodec;  // 添加视频编码器字段
    private int maxThreads = 3; // 默认值

    public int getMaxThreads() {
        return maxThreads;
    }

    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }
    // Getter 和 Setter
    public String getFfmpegPath() {
        return ffmpegPath;
    }

    public void setFfmpegPath(String ffmpegPath) {
        this.ffmpegPath = ffmpegPath;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public String getBitrate() {
        return bitrate;
    }

    public void setBitrate(String bitrate) {
        this.bitrate = bitrate;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getLastOutputDir() {
        return lastOutputDir;
    }

    public void setLastOutputDir(String lastOutputDir) {
        this.lastOutputDir = lastOutputDir;
    }

    public String getVideoCodec() {
        return videoCodec;
    }

    public void setVideoCodec(String videoCodec) {
        this.videoCodec = videoCodec;
    }
}
