package com.ffmpegbox.utils;

import com.ffmpegbox.model.FileType;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandBuilder {

    // —— 以下字段要一一声明 ——
    private String    ffmpegPath = "ffmpeg";
    private File      inputFile;
    private FileType  fileType;
    private String    format;
    private String    resolution;
    private String    bitrate;
    private String    videoCodec;
    private String    audioCodec;
    private String    outputDir;
    private String videoBitrate;
    private String audioBitrate;
    private boolean useCrf = false;
    private int crfValue = 23;
    private String avSeparationMode = null;
    private String startTime;
    private String endTime;
    private String textWatermarkContent;
    private String textWatermarkSize;
    private String textWatermarkPosition;


    // —— 对应的 setter ——
    public CommandBuilder setFfmpegPath(String p) {
        this.ffmpegPath = p;
        return this;
    }

    public CommandBuilder setInputFile(File f) {
        this.inputFile = f;
        return this;
    }

    public CommandBuilder setFileType(FileType t) {
        this.fileType = t;
        return this;
    }

    public CommandBuilder setFormat(String f) {
        this.format = f;
        return this;
    }

    public CommandBuilder setResolution(String r) {
        this.resolution = r;
        return this;
    }

    public CommandBuilder setBitrate(String b) {
        this.bitrate = b;
        return this;
    }

    public CommandBuilder setVideoCodec(String v) {
        this.videoCodec = v;
        return this;
    }

    public CommandBuilder setAudioCodec(String a) {
        this.audioCodec = a;
        return this;
    }

    public CommandBuilder setOutputDir(String o) {
        this.outputDir = o;
        return this;
    }

    public CommandBuilder setVideoBitrate(String vb) {
        this.videoBitrate = vb;
        return this;
    }

    public CommandBuilder setAudioBitrate(String ab) {
        this.audioBitrate = ab;
        return this;
    }

    public CommandBuilder setUseCrf(boolean useCrf) {
        this.useCrf = useCrf;
        return this;
    }

    public CommandBuilder setCrfValue(int crfValue) {
        this.crfValue = crfValue;
        return this;
    }

    public CommandBuilder setAVSeparationMode(String mode) {
        this.avSeparationMode = mode;
        return this;
    }

    public CommandBuilder setStartTime(String s) {
        this.startTime = s;
        return this;
    }

    public CommandBuilder setEndTime(String e) {
        this.endTime = e;
        return this;
    }

    public CommandBuilder setTextWatermarkContent(String t) {
        this.textWatermarkContent = t;
        return this;
    }

    public CommandBuilder setTextWatermarkSize(String s) {
        this.textWatermarkSize = s;
        return this;
    }

    public CommandBuilder setTextWatermarkPosition(String p) {
        this.textWatermarkPosition = p;
        return this;
    }

    /**
     * 根据 FileType 严格分流：
     *  - VIDEO: 加 -c:v/-s/-b:v，不加音频参数
     *  - AUDIO: 加 -c:a，不加视频参数
     *  - OTHER: 两者都不加
     */
    public List<String> build() {
        List<String> cmd = new ArrayList<>();

        boolean isAudioOnly = "只保存音频".equals(avSeparationMode);
        boolean isVideoOnly = "只保存视频".equals(avSeparationMode);

        // 实际使用的格式（优先覆盖 UI）
        String actualFormat = format;
        if (isAudioOnly) actualFormat = "mp3";
        if (isVideoOnly) actualFormat = "mp4";

        // 1) ffmpeg 路径 + 覆盖开关
        cmd.add(ffmpegPath);
        cmd.add("-y");

        // 2) 剪辑参数（放在输入文件前）
        if (startTime != null && !startTime.isBlank()) {
            cmd.addAll(Arrays.asList("-ss", startTime));
        }

        // 3) 输入文件
        cmd.addAll(Arrays.asList("-i", inputFile.getAbsolutePath()));

        if (endTime != null && !endTime.isBlank()) {
            cmd.addAll(Arrays.asList("-to", endTime));
        }

        // 4) 添加编码参数（按类型区分）
        if (fileType == FileType.VIDEO && !isAudioOnly) {
            if (videoCodec != null && !videoCodec.isBlank()) {
                cmd.addAll(Arrays.asList("-c:v", videoCodec));
            }

            if (resolution != null && !resolution.isBlank()) {
                cmd.addAll(Arrays.asList("-s", resolution));
            }

            if (useCrf) {
                cmd.addAll(Arrays.asList("-crf", String.valueOf(crfValue)));
            }
            if (textWatermarkContent != null && !textWatermarkContent.isBlank()) {
                String posExpr;
                switch (textWatermarkPosition) {
                    case "左上": posExpr = "x=10:y=10"; break;
                    case "右上": posExpr = "x=w-tw-10:y=10"; break;
                    case "右下": posExpr = "x=w-tw-10:y=h-th-10"; break;
                    case "左下": posExpr = "x=10:y=h-th-10"; break;
                    default: posExpr = "x=10:y=10";
                }

                String fontSize = (textWatermarkSize != null && !textWatermarkSize.isBlank()) ? textWatermarkSize : "24";
                String drawtext = String.format(
                        "drawtext=fontfile='C\\:/Windows/Fonts/simhei.ttf':text='%s':fontsize=%s:fontcolor=white:%s",
                        textWatermarkContent, fontSize, posExpr);

                cmd.addAll(Arrays.asList("-vf", drawtext));
            }

            if (avSeparationMode != null) {
                switch (avSeparationMode) {
                    case "只保存音频":
                        cmd.add("-vn");
                        break;
                    case "只保存视频":
                        cmd.add("-an");
                        break;
                    case "音频 + 视频":
                        // 不加任何禁用流参数
                        break;
                }

            } else if (videoBitrate != null
                    && !videoBitrate.isBlank()
                    && !"原始码率".equals(videoBitrate)) {
                cmd.addAll(Arrays.asList("-b:v", videoBitrate));
            }
        } else if (fileType == FileType.AUDIO) {
            if (audioCodec != null && !audioCodec.isBlank()) {
                cmd.addAll(Arrays.asList("-c:a", audioCodec));
            }

            if (audioBitrate != null
                    && !audioBitrate.isBlank()
                    && !"原始码率".equals(audioBitrate)) {
                cmd.addAll(Arrays.asList("-b:a", audioBitrate));
            }
        }

        // 5) 强制格式（使用 actualFormat）
        if (actualFormat != null && !actualFormat.isBlank()) {
            cmd.addAll(Arrays.asList("-f", actualFormat));
        }

        // 6) 输出路径（加后缀）
        String base = inputFile.getName().replaceFirst("\\.[^.]+$", "");
        String suffix = "";

        if (isAudioOnly) {
            suffix = "_audio";
        } else if (isVideoOnly) {
            suffix = "_video";
        }

        String out = outputDir + File.separator + base + suffix + "." + actualFormat;
        cmd.add(out);

        return cmd;
    }

    public List<List<String>> buildDualOutputIfNeeded() {
        if ("音频 + 视频".equals(avSeparationMode)) {
            // 拷贝两份分别设置模式
            CommandBuilder videoBuilder = this.copy().setAVSeparationMode("只保存视频");
            CommandBuilder audioBuilder = this.copy().setAVSeparationMode("只保存音频");
            return List.of(videoBuilder.build(), audioBuilder.build());
        } else {
            return List.of(this.build());
        }
    }

    public CommandBuilder copy() {
        CommandBuilder cb = new CommandBuilder();
        cb.ffmpegPath = this.ffmpegPath;
        cb.inputFile = this.inputFile;
        cb.fileType = this.fileType;
        cb.format = this.format;
        cb.resolution = this.resolution;
        cb.bitrate = this.bitrate;
        cb.videoCodec = this.videoCodec;
        cb.audioCodec = this.audioCodec;
        cb.outputDir = this.outputDir;
        cb.videoBitrate = this.videoBitrate;
        cb.audioBitrate = this.audioBitrate;
        cb.useCrf = this.useCrf;
        cb.crfValue = this.crfValue;
        cb.avSeparationMode = this.avSeparationMode;
        return cb;
    }
}
