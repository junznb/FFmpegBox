package com.ffmpegbox.utils;

import java.util.ArrayList;
import java.util.List;

public class CommandBuilder {

    private String inputPath;
    private String outputPath;
    private String format;
    private String resolution;
    private String bitrate;
    private String ffmpegPath = "ffmpeg";

    public CommandBuilder setFfmpegPath(String ffmpegPath) {
        this.ffmpegPath = ffmpegPath;
        return this;
    }

    public CommandBuilder setInputPath(String inputPath) {
        this.inputPath = inputPath;
        return this;
    }

    public CommandBuilder setOutputPath(String outputPath) {
        this.outputPath = outputPath;
        return this;
    }

    public CommandBuilder setFormat(String format) {
        this.format = format;
        return this;
    }

    public CommandBuilder setResolution(String resolution) {
        this.resolution = resolution;
        return this;
    }

    public CommandBuilder setBitrate(String bitrate) {
        this.bitrate = bitrate;
        return this;
    }

    public String getInputPath() {
        return inputPath;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public String getFormat() {
        return format;
    }

    public String getResolution() {
        return resolution;
    }

    public String getBitrate() {
        return bitrate;
    }

    public String getFfmpegPath() {
        return ffmpegPath;
    }

    public List<String> build() {
        List<String> command = new ArrayList<>();

        command.add(ffmpegPath);
        command.add("-y"); // 自动覆盖输出文件
        command.add("-i");
        command.add(inputPath);

        if (resolution != null && !resolution.trim().isEmpty()) {
            command.add("-s");
            command.add(resolution.trim());
        }

        if (bitrate != null && !bitrate.trim().isEmpty()) {
            command.add("-b:v");
            command.add(bitrate.trim());
        }

        String fullOutputPath = ensureExtension(outputPath, format);
        command.add(fullOutputPath);

        return command;
    }

    private String ensureExtension(String path, String format) {
        if (path.toLowerCase().endsWith("." + format.toLowerCase())) {
            return path;
        } else {
            return path + "." + format;
        }
    }
}
