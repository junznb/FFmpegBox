//package com.ffmpegbox.model;
//
//
//import javafx.scene.control.ProgressBar;
//import com.ffmpegbox.model.FileType;
//import java.io.File;
//
//
//
//
//public class FileTask {
//    private final File file;
//    private final ProgressBar progressBar;
//    private final FileType type;
//    private String format = "mp4";
//    private String resolution = "1920x1080";
//    private String bitrate = "800k";
//
//    public FileTask(File file, ProgressBar progressBar, FileType type) {
//        this.file = file;
//        this.progressBar = progressBar;
//        this.type = type;
//    }
//
//    public FileType getType() { return type; }
//    public File getFile() {
//        return file;
//    }
//    public ProgressBar getProgressBar() {
//        return progressBar;
//    }
//    public String getFormat() { return format; }
//    public void setFormat(String format) { this.format = format; }
//
//    public String getResolution() { return resolution; }
//
//    public void setResolution(String resolution) { this.resolution = resolution; }
//
//    public String getBitrate() { return bitrate; }
//
//    public void setBitrate(String bitrate) { this.bitrate = bitrate; }
//}

package com.ffmpegbox.model;

import com.ffmpegbox.utils.FFmpegController;
import javafx.application.Platform;
import javafx.scene.control.ProgressBar;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

public class FileTask implements Runnable {
    private final File file;
    private final ProgressBar progressBar;
    private final FileType type;

    private String format = "mp4";
    private String resolution = "1920x1080";
    private String bitrate = "800k";

    private List<String> command;
    private Consumer<String> logCallback;
    private Consumer<Double> progressCallback;

    public FileTask(File file, ProgressBar progressBar, FileType type) {
        this.file = file;
        this.progressBar = progressBar;
        this.type = type;
    }

    // 🔧 执行前设置
    public void setCommand(List<String> command) {
        this.command = command;
    }

    public void setLogCallback(Consumer<String> logCallback) {
        this.logCallback = logCallback;
    }

    public void setProgressCallback(Consumer<Double> progressCallback) {
        this.progressCallback = progressCallback;
    }

    @Override
    public void run() {
        if (command == null) {
            if (logCallback != null) {
                logCallback.accept("未设置命令，跳过任务: " + file.getName());
            }
            return;
        }

        FFmpegController controller = new FFmpegController(msg ->
                Platform.runLater(() -> {
                    if (logCallback != null) logCallback.accept(msg);
                })
        );

        controller.setUpdateProgressCallback(p ->
                Platform.runLater(() -> {
                    if (progressCallback != null) progressCallback.accept(p);
                })
        );

        controller.runCommand(command);
    }

    // 现有字段 Getter/Setter 保留
    public File getFile() {
        return file;
    }

    public ProgressBar getProgressBar() {
        return progressBar;
    }

    public FileType getType() {
        return type;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
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
}

