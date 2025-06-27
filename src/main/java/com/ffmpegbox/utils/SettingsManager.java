package com.ffmpegbox.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ffmpegbox.model.UserSettings;

import java.io.File;
import java.io.IOException;

public class SettingsManager {

    private static final String SETTINGS_FILE = "user-settings.json";
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void saveSettings(UserSettings settings) {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(SETTINGS_FILE), settings);
        } catch (IOException e) {
            System.err.println("保存设置失败：" + e.getMessage());
        }
    }

    public static UserSettings loadSettings() {
        File file = new File(SETTINGS_FILE);
        if (file.exists()) {
            try {
                return mapper.readValue(file, UserSettings.class);
            } catch (IOException e) {
                System.err.println("读取设置失败：" + e.getMessage());
            }
        }
        return new UserSettings(); // 返回默认值
    }
}
