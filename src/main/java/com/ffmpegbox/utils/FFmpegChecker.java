package com.ffmpegbox.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class FFmpegChecker {

    // 检查系统环境变量是否包含 ffmpeg
    public static boolean isFFmpegInSystemPath() {
        try {
            Process process = new ProcessBuilder("ffmpeg", "-version").start();
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    // 检查指定路径的 ffmpeg 是否可用
    public static boolean isValidFFmpegPath(String ffmpegPath) {
        try {
            File file = new File(ffmpegPath);
            if (!file.exists()) return false;

            Process process = new ProcessBuilder(ffmpegPath, "-version").start();
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    // 获取版本信息（可选，用于设置界面显示）
    public static String getFFmpegVersion(String ffmpegPath) {
        try {
            Process process = new ProcessBuilder(ffmpegPath, "-version").start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            return reader.readLine(); // 返回第一行版本号
        } catch (Exception e) {
            return "无法获取版本";
        }
    }
}
