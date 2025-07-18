package com.ffmpegbox.utils;

import java.io.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FFmpegController {

    private double totalDuration = 60; // 初始默认值，会被真实值替换
    private Consumer<Double> updateProgressCallback;

    public interface LogListener {
        void onLog(String line);
    }

    private LogListener logListener;

    public FFmpegController(LogListener logListener) {
        this.logListener = logListener;
    }

    public void setUpdateProgressCallback(Consumer<Double> callback) {
        this.updateProgressCallback = callback;
    }


    private void readProcessOutput(InputStream inputStream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            Pattern durationPattern = Pattern.compile("Duration: (\\d+):(\\d+):(\\d+\\.\\d+)");
            Pattern timePattern = Pattern.compile("time=(\\d+):(\\d+):(\\d+\\.\\d+)");

            while ((line = reader.readLine()) != null) {
                log(line);

                // 提取总时长
                Matcher durationMatcher = durationPattern.matcher(line);
                if (durationMatcher.find()) {
                    double hours = Double.parseDouble(durationMatcher.group(1));
                    double minutes = Double.parseDouble(durationMatcher.group(2));
                    double seconds = Double.parseDouble(durationMatcher.group(3));
                    totalDuration = hours * 3600 + minutes * 60 + seconds;
                }

                // 提取当前进度
                Matcher matcher = timePattern.matcher(line);
                if (matcher.find()) {
                    double hours = Double.parseDouble(matcher.group(1));
                    double minutes = Double.parseDouble(matcher.group(2));
                    double seconds = Double.parseDouble(matcher.group(3));
                    double currentSeconds = hours * 3600 + minutes * 60 + seconds;

                    if (totalDuration > 0 && updateProgressCallback != null) {
                        double progress = currentSeconds / totalDuration;
                        updateProgressCallback.accept(Math.min(progress, 1.0)); // 限制最大值为 1.0（100%）
                    }
                }
            }
        } catch (IOException e) {
            log("读取输出失败：" + e.getMessage());
        }finally {
            if (updateProgressCallback != null) {
                updateProgressCallback.accept(1.0); // 💡 保底设置为 100%
            }
        }

    }

    public void runCommand(List<String> command) {
        log("生成命令：");
        log(String.join(" ", command));
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 复用已有的输出读取线程
//            new Thread(() -> readProcessOutput(process.getInputStream())).start();
            readProcessOutput(process.getInputStream());
        } catch (IOException e) {
            log("执行失败：" + e.getMessage());
        }
    }

    private void log(String message) {
        if (logListener != null) {
            logListener.onLog(message);
        }
    }
}
