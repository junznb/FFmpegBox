package com.ffmpegbox.controller;

import com.ffmpegbox.utils.*;
import io.github.palexdev.materialfx.controls.*;
import javafx.scene.layout.AnchorPane;
import org.apache.tika.Tika;
import java.io.File;
import java.io.IOException;
import com.ffmpegbox.model.FileType;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;

import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import com.ffmpegbox.model.FileTask;
import com.ffmpegbox.model.UserSettings;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.util.*;



import javafx.scene.input.KeyCode;

public class MainController {

    private String ffmpegPath = "ffmpeg";
    private final Map<String, FileTask> fileTaskMap = new HashMap<>();
    private final Tika tika = new Tika();
    private final Map<String, String> codecDisplayToValue = new HashMap<>();
    private final Map<String, String> codecValueToDisplay = new HashMap<>();
    public static ExecutorService executor;

    //MFX
    @FXML private MFXButton handleButton;
    @FXML private MFXTextField maxThreadsField;
    @FXML private MFXTextField ffmpegVersionField;
    @FXML private MFXToggleButton textWatermarkToggle;
    @FXML private MFXTextField textWatermarkContent;
    @FXML private MFXTextField textWatermarkSize;
    @FXML private MFXFilterComboBox<String> textWatermarkPosition;
    @FXML private MFXToggleButton clipToggle;
    @FXML private MFXTextField startTimeField;
    @FXML private MFXTextField endTimeField;
    @FXML private MFXToggleButton avSeparationToggle;
    @FXML private MFXFilterComboBox<String> saveModeBox;
    @FXML private MFXToggleButton useCrfToggle;
    @FXML private MFXSlider crfSlider;
    @FXML private MFXTextField crfTextField;
    @FXML private MFXFilterComboBox<String> formatBox_MFX_file;
    @FXML private MFXFilterComboBox<String> audioCodecBox;
    @FXML private MFXTextField outputDirField;
    @FXML private MFXTextField resolutionField;
    @FXML private MFXTextField bitrateField;
    @FXML private MFXTextField ffmpegPathField;
    @FXML private MFXFilterComboBox<String> videoCodecBox;
    @FXML private MFXFilterComboBox<String> audioBitrateBox;
    @FXML private MFXFilterComboBox<String> videoBitrateBox;
    @FXML private AnchorPane rootPane;

    //+++++++++
    @FXML private TextField inputField;
    @FXML private TextArea logArea;
    @FXML private TextArea commandPreviewArea;
    @FXML private ListView<HBox> fileListView;



    /**
     * 初始化界面元素和事件监听器
     */
    @FXML
    public void initialize() {

        initLoadSettings();
        initCheckffmpeg();
        initExecutor(2);



        
        initTextWatermarkControls();
        initClipControls();
        initAVSplitControls();
        initCRFControls();
        initVideoTap();
        initAudioTap();
        initListSelectionListener();
        initDragAndDrop();
        initFormatBox();
        initTextFields();

        initPreviewListener();


    }


    // 选择输入文件
    @FXML
    private void handleChooseInput() {
        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showOpenDialog(null);
        if (file != null) inputField.setText(file.getAbsolutePath());
    }

    // 选择输出目录
    @FXML
    private void handleChooseOutputDir() {
        DirectoryChooser dirChooser = new DirectoryChooser();
        File dir = dirChooser.showDialog(null);
        if (dir != null) outputDirField.setText(dir.getAbsolutePath());
    }

    // 选择 FFmpeg 执行文件
    @FXML
    private void handleChooseFFmpeg() {
        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            ffmpegPath = file.getAbsolutePath();
            ffmpegPathField.setText(ffmpegPath);
            saveSettings();
        }
    }

    // 执行视频转换
    @FXML
    private void handleConvert() {
        handleButton.setDisable(true);
        String outDir = outputDirField.getText();
        if (outDir == null || outDir.isBlank()) {
            logArea.appendText("请先选择输出目录\n");
            return;
        }
        // 👇 1. 获取用户设定的最大线程数
        int maxThreads = 1;
        try {
            maxThreads = Integer.parseInt(maxThreadsField.getText());
            if (maxThreads <= 0) maxThreads = 1; // 防止非法值
        } catch (NumberFormatException e) {
            maxThreads = 1;
        }

        for (FileTask task : fileTaskMap.values()) {
            // 1) 跳过参数不全的任务
            if (task.getFormat() == null
                    || task.getResolution() == null
                    || task.getBitrate() == null) {
                logArea.appendText("跳过文件 " + task.getFile().getName() + "：参数不完整\n");
                continue;
            }

            // 2) 用同一套 Builder 生成命令

            CommandBuilder builder = new CommandBuilder()
                    .setFfmpegPath(ffmpegPathField.getText().isBlank() ? "ffmpeg" : ffmpegPathField.getText())
                    .setInputFile(task.getFile())
                    .setFileType(task.getType())
                    .setFormat(task.getFormat())
                    .setResolution(task.getResolution())
                    .setBitrate(task.getBitrate())
                    .setVideoCodec(codecDisplayToValue.get(videoCodecBox.getValue()))
                    .setAudioCodec(audioCodecBox.getValue())
                    .setOutputDir(outputDirField.getText())
                    .setVideoBitrate(videoBitrateBox.getValue())
                    .setAudioBitrate(audioBitrateBox.getValue())
                    .setUseCrf(useCrfToggle.isSelected())
                    .setCrfValue((int) crfSlider.getValue())
                    .setAVSeparationMode(avSeparationToggle.isSelected() ? saveModeBox.getValue() : null)
                    .setStartTime(startTimeField.getText())
                    .setEndTime(endTimeField.getText())
                    .setTextWatermarkContent(textWatermarkToggle.isSelected() ? textWatermarkContent.getText() : null)
                    .setTextWatermarkSize(textWatermarkToggle.isSelected() ? textWatermarkSize.getText() : null)
                    .setTextWatermarkPosition(textWatermarkToggle.isSelected() ? textWatermarkPosition.getValue() : null);


//            List<String> cmd = buildCommandForTask(task);
            List<List<String>> cmds = builder.buildDualOutputIfNeeded();

            // ← 一次性拿到完整命令

//            logArea.appendText("开始转换：" + task.getFile().getName() + "\n");
//            logArea.appendText("命令： " + String.join(" ", cmd) + "\n");

//            // 3) 执行并绑定进度
//            FFmpegController controller = new FFmpegController(msg ->
//                    Platform.runLater(() -> logArea.appendText(msg + "\n"))
//            );
//            // 假设你把 FileTask.progressBar 定义成 MFXProgressBar
//            MFXProgressBar bar = (MFXProgressBar) task.getProgressBar();
//            controller.setUpdateProgressCallback(p ->
//                    Platform.runLater(() -> smoothProgress(bar, p))
//            );

            // 4) 真正启动命令 —— 请在你的 FFmpegController 中添加 runCommand(List<String>)：
//            controller.runCommand(cmd);
//            for (List<String> cmd : cmds) {
//                logArea.appendText("开始转换：" + task.getFile().getName() + "\n");
//                logArea.appendText("命令： " + String.join(" ", cmd) + "\n");
//
//                FFmpegController controller = new FFmpegController(msg ->
//                        Platform.runLater(() -> logArea.appendText(msg + "\n")));
//
//                MFXProgressBar bar = (MFXProgressBar) task.getProgressBar();
//                controller.setUpdateProgressCallback(p ->
//                        Platform.runLater(() -> smoothProgress(bar, p)));
//
//                controller.runCommand(cmd);
//            }
            if (executor != null && !executor.isShutdown()) {
                executor.shutdownNow(); // 重启前先关闭旧线程池
            }
            executor = Executors.newFixedThreadPool(maxThreads);

//            for (List<String> cmd : cmds) {
//                executor.submit(() -> {
//                    Platform.runLater(() -> {
//                        logArea.appendText("开始转换：" + task.getFile().getName() + "\n");
//                        logArea.appendText("命令： " + String.join(" ", cmd) + "\n");
//                    });
//
//                    FFmpegController controller = new FFmpegController(msg ->
//                            Platform.runLater(() -> logArea.appendText(msg + "\n"))
//                    );
//
//                    MFXProgressBar bar = (MFXProgressBar) task.getProgressBar();
//                    controller.setUpdateProgressCallback(p ->
//                            Platform.runLater(() -> smoothProgress(bar, p))
//                    );
//
//                    controller.runCommand(cmd);
//                });
//            }
            for (List<String> cmd : cmds) {
                // 创建新的 task 实例用于闭包绑定
                FileTask currentTask = task;

                executor.submit(() -> {
                    Platform.runLater(() -> {
                        logArea.appendText("开始转换：" + currentTask.getFile().getName() + "\n");
                        logArea.appendText("命令： " + String.join(" ", cmd) + "\n");
                    });

                    // 每个任务单独控制器实例
                    FFmpegController controller = new FFmpegController(msg ->
                            Platform.runLater(() -> logArea.appendText("[" + currentTask.getFile().getName() + "] " + msg + "\n"))
                    );

                    // 获取当前任务的进度条并绑定更新
                    MFXProgressBar bar = (MFXProgressBar) currentTask.getProgressBar();
                    controller.setUpdateProgressCallback(p ->
                            Platform.runLater(() -> smoothProgress(bar, p))
                    );

                    controller.runCommand(cmd);
                });
            }

        }
    }

    // 清空日志
    @FXML
    private void handleClearLog() {
        logArea.clear();
    }

    // 复制命令行到剪贴板
    @FXML
    private void handleCopyCommand() {
        String command = commandPreviewArea.getText();
        ClipboardContent content = new ClipboardContent();
        content.putString(command);
        Clipboard.getSystemClipboard().setContent(content);
        logArea.appendText("命令已复制到剪贴板\n");
    }

    // 实时更新命令预览
    private void updateCommandPreview() {


        HBox row = fileListView.getSelectionModel().getSelectedItem();
        if (row == null) {
            commandPreviewArea.clear();
            return;
        }
        String name = ((Label)row.getChildren().get(0)).getText();
        FileTask task = fileTaskMap.get(name);
        if (task == null) {
            commandPreviewArea.clear();
            return;
        }

        CommandBuilder builder = new CommandBuilder()
                .setFfmpegPath(ffmpegPathField.getText().isBlank() ? "ffmpeg" : ffmpegPathField.getText())
                .setInputFile(task.getFile())
                .setFileType(task.getType())
                .setFormat(task.getFormat())
                .setResolution(task.getResolution())
                .setBitrate(task.getBitrate())
                .setVideoCodec(codecDisplayToValue.get(videoCodecBox.getValue()))
                .setAudioCodec(audioCodecBox.getValue())
                .setOutputDir(outputDirField.getText())
                .setVideoBitrate(videoBitrateBox.getValue())
                .setAudioBitrate(audioBitrateBox.getValue())
                .setUseCrf(useCrfToggle.isSelected())
                .setCrfValue((int) crfSlider.getValue())
                .setAVSeparationMode(avSeparationToggle.isSelected() ? saveModeBox.getValue() : null)
                .setStartTime(startTimeField.getText())
                .setEndTime(endTimeField.getText())
                .setTextWatermarkContent(textWatermarkToggle.isSelected() ? textWatermarkContent.getText() : null)
                .setTextWatermarkSize(textWatermarkToggle.isSelected() ? textWatermarkSize.getText() : null)
                .setTextWatermarkPosition(textWatermarkToggle.isSelected() ? textWatermarkPosition.getValue() : null);

        List<List<String>> cmds = builder.buildDualOutputIfNeeded();

        String previewText = cmds.stream()
                .map(cmd -> String.join(" ", cmd))
                .reduce((a, b) -> a + "\n\n" + b)  // 两条命令中间空一行
                .orElse("");

        commandPreviewArea.setText(previewText);
    }


    private void updateControlsForTask(FileTask task) {

//        根据文件类型禁用控件

        boolean isAudio = task.getType() == FileType.AUDIO;
        boolean isVideo = task.getType() == FileType.VIDEO;

        // 视频相关
        videoCodecBox.setDisable(!isVideo);
        resolutionField.setDisable(!isVideo);
        bitrateField.setDisable(!isVideo);
        videoBitrateBox.setDisable(!isVideo);
        useCrfToggle.setDisable(!isVideo);
        crfSlider.setDisable(!isVideo);

        // 音频相关
        audioCodecBox.setDisable(!isAudio);
        audioBitrateBox.setDisable(!isAudio);

        // 如果有音频码率、采样率控件，也在这里启/禁
        // audioBitrateField.setDisable(!isAudio);
        // audioSampleRateField.setDisable(!isAudio);
    }

    // 保存用户设置
    private void saveSettings() {
        UserSettings settings = new UserSettings();
        settings.setFfmpegPath(ffmpegPathField.getText());
        settings.setResolution(resolutionField.getText());
        settings.setBitrate(bitrateField.getText());
        settings.setFormat(formatBox_MFX_file.getValue());
//        settings.setLastInput(inputField.getText());
        settings.setLastOutputDir(outputDirField.getText());
        settings.setVideoCodec(codecDisplayToValue.get(videoCodecBox.getValue()));
        try {
            int threads = Integer.parseInt(maxThreadsField.getText());
            settings.setMaxThreads(threads);
            initExecutor(threads);  // 可选，立即刷新线程池大小
        } catch (NumberFormatException e) {
            logArea.appendText("最大并发数格式错误，已使用默认值 2\n");
            settings.setMaxThreads(2);
            initExecutor(2);
        }
        SettingsManager.saveSettings(settings);
    }


    private void initExecutor(int maxThreads) {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();  // 停止旧线程池
        }

        executor = Executors.newFixedThreadPool(Math.max(1, maxThreads));
        logArea.appendText("线程池已初始化，最大并发数: " + maxThreads + "\n");
    }

    private void setupDragAndDrop() {
        fileListView.setOnDragOver(event -> {
            if (event.getGestureSource() != fileListView && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        fileListView.setOnDragDropped(event -> {
            var db = event.getDragboard();
            boolean success = false;

            if (db.hasFiles()) {
                success = true;

                for (File file : db.getFiles()) {
                    String filename = file.getName();
                    if (fileTaskMap.containsKey(filename)) continue;

                    // 文件名标签
                    Label nameLabel = new Label(filename);
                    nameLabel.setPrefWidth(400);

                    // 用 MFXProgressBar 替换原生 ProgressBar
                    MFXProgressBar fileProgress = new MFXProgressBar();

                    fileProgress.setProgress(0);
                    fileProgress.setPrefWidth(150);
                    fileProgress.getStyleClass().add("file-task-progress");  // 可选，自定义 class

                    // 一行 HBox
                    HBox row = new HBox(10, nameLabel, fileProgress);
                    row.setStyle("-fx-padding: 5; -fx-alignment: center-left;");
                    fileListView.getItems().add(row);

                    // 创建并存储任务
                    FileType type = detectType(file);
                    FileTask task = new FileTask(file, fileProgress, type);
                    task.setFormat(formatBox_MFX_file.getValue());
                    task.setResolution(resolutionField.getText());
                    task.setBitrate(bitrateField.getText());
                    fileTaskMap.put(filename, task);

                    // 如果这是第一个文件，选中它以便右侧预览
                    if (fileListView.getItems().size() == 1) {
                        inputField.setText(file.getAbsolutePath());
                        updateSelectedFileTask(t -> {
                            t.setFormat(task.getFormat());
                            t.setResolution(task.getResolution());
                            t.setBitrate(task.getBitrate());
                        });
                        updateControlsForTask(task);
                        updateCommandPreview();
                    }
                    fileListView.getSelectionModel().select(row);

                    // 同步更新当前选中任务
                    updateSelectedFileTask(t -> {
                        t.setFormat(task.getFormat());
                        t.setResolution(task.getResolution());
                        t.setBitrate(task.getBitrate());
                    });
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
        // Delete 键删除
        fileListView.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DELETE) {
                handleDeleteSelected();
            }
        });
    }


    private void updateSelectedFileTask(java.util.function.Consumer<FileTask> updater) {
        HBox selected = fileListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Label label = (Label) selected.getChildren().get(0);
        FileTask task = fileTaskMap.get(label.getText());
        if (task != null) updater.accept(task);
    }


    @FXML
    private void handleDeleteSelected() {
        HBox selected = fileListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Label nameLabel = (Label) selected.getChildren().get(0); // 第一个是文件名
        String filename = nameLabel.getText();

        fileTaskMap.remove(filename);              // 移除任务记录
        fileListView.getItems().remove(selected);  // 移除 UI 项目

        // 如果被删的是当前选中的那个文件，需要清空 inputField
        if (inputField.getText().endsWith(filename)) {
            inputField.clear();
            formatBox_MFX_file.setValue("mp4");
            resolutionField.setText("1920x1080");
            bitrateField.setText("800k");
        }
    }

    private void smoothProgress(MFXProgressBar bar, double target) {
        // 停掉上一次的动画（如果有的话）
        Object previous = bar.getProperties().get("smoothTimeline");
        if (previous instanceof Timeline) {
            ((Timeline) previous).stop();
        }

        // 从当前进度开始
        double start = bar.getProgress();

        Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(bar.progressProperty(), start)
                ),
                new KeyFrame(Duration.millis(300),  // 300ms 过渡，也可以调长短
                        new KeyValue(bar.progressProperty(), target)
                )
        );
        tl.play();

        // 把它存到进度条属性里，下次好停掉
        bar.getProperties().put("smoothTimeline", tl);
    }

    private void initDragAndDrop() {
        setupDragAndDrop();
        fileListView.setPlaceholder(new Label("将文件拖入此区域"));
    }

    private void initFormatBox() {
        formatBox_MFX_file.getItems().addAll("mp4", "avi", "mkv", "mov");
        formatBox_MFX_file.setValue("mp4");
    }

    private void initTextFields() {
        resolutionField.setText("1920x1080");
        bitrateField.setText("800k");
        // 其余 MFXTextField 默认值……
    }

    private void initLoadSettings() {
        UserSettings settings = SettingsManager.loadSettings();
        if (settings != null) {
            if (settings.getFfmpegPath() != null) {
                ffmpegPath = settings.getFfmpegPath();
                ffmpegPathField.setText(ffmpegPath);
            }
            if (settings.getMaxThreads() > 0) {
                maxThreadsField.setText(String.valueOf(settings.getMaxThreads()));
            }
            if (settings.getResolution() != null) resolutionField.setText(settings.getResolution());
            if (settings.getBitrate() != null) bitrateField.setText(settings.getBitrate());
            if (settings.getFormat() != null) formatBox_MFX_file.setValue(settings.getFormat());

            // 设置编码器显示值（使用字典转换）
            if (settings.getVideoCodec() != null) {
                videoCodecBox.setValue(codecValueToDisplay.getOrDefault(settings.getVideoCodec(), "H.264 (软件编码)"));
            }

            if (settings.getLastOutputDir() != null) outputDirField.setText(settings.getLastOutputDir());
        }
    }


    private void initPreviewListener() {
        // 输入变更自动刷新命令预览
        ChangeListener<Object> listener = (obs, oldVal, newVal) -> updateCommandPreview();
        inputField.textProperty().addListener(listener);
        outputDirField.textProperty().addListener(listener);
        resolutionField.textProperty().addListener(listener);
        bitrateField.textProperty().addListener(listener);
        ffmpegPathField.textProperty().addListener(listener);
        formatBox_MFX_file.valueProperty().addListener(listener);
        videoCodecBox.valueProperty().addListener(listener);
        audioCodecBox.valueProperty().addListener(listener);
        resolutionField.textProperty().addListener(listener);
        bitrateField.textProperty().addListener(listener);
        videoBitrateBox.valueProperty().addListener(listener);
        audioBitrateBox.valueProperty().addListener(listener);
    }

    private void initListSelectionListener() {
        // 监听列表选择变化，更新参数输入框
        fileListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                Label label = (Label) newVal.getChildren().get(0); // 获取文件名
                String filename = label.getText();
                FileTask task = fileTaskMap.get(filename);
                if (task != null) {
                    System.out.println("选中的文件类型: " + task.getType());
                    formatBox_MFX_file.setValue(task.getFormat());
                    resolutionField.setText(task.getResolution());
                    bitrateField.setText(task.getBitrate());
                    // 更新隐藏的 inputField 以支持命令预览
                    inputField.setText(task.getFile().getAbsolutePath());
                    updateControlsForTask(task);
                    updateCommandPreview();
                }
            }
        });

// 下方参数变动时同步更新 com.ffmpegbox.model.FileTask
        formatBox_MFX_file.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateSelectedFileTask(task -> task.setFormat(newVal));
        });

        resolutionField.textProperty().addListener((obs, oldVal, newVal) -> {
            updateSelectedFileTask(task -> task.setResolution(newVal));
        });
        bitrateField.textProperty().addListener((obs, oldVal, newVal) -> {
            updateSelectedFileTask(task -> task.setBitrate(newVal));
        });

        fileListView.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DELETE) {
                handleDeleteSelected(); // 按下 Delete 键时调用删除
            }
        });
    }

//    private void initVideoTap() {
//        // 常用视频编码器列表
//        videoCodecBox.getItems().addAll(
//                "libx264",    // H.264 软件编码
//                "libx265",    // H.265/HEVC 软件编码
//                "h264_nvenc", // NVIDIA 硬件加速
//                "hevc_qsv",   // Intel QSV 硬件加速
//                "libvpx-vp9",        // VP9
//                "av1"         // AV1
//        );
//        // 默认使用 H.264
//        videoCodecBox.setValue("libx264");
//
//        videoBitrateBox.getItems().addAll(
//                "原始码率", "600k"   // <-- 表示使用源文件的码率
//                ,"800k", "1200","1500k","1600k", "2000k","2400k", "3000k", "5000k"
//                );
//        videoBitrateBox.setValue("原始码率");
//
//    }
private void initVideoTap() {
    // 中文显示 → FFmpeg 参数
    codecDisplayToValue.put("H.264 (软件编码)", "libx264");
    codecDisplayToValue.put("H.265 / HEVC (软件编码)", "libx265");
    codecDisplayToValue.put("H.264 (NVIDIA 硬件加速)", "h264_nvenc");
    codecDisplayToValue.put("H.265 (Intel QSV 硬件加速)", "hevc_qsv");
    codecDisplayToValue.put("VP9", "libvpx-vp9");
    codecDisplayToValue.put("AV1", "av1");

    // FFmpeg 参数 → 中文显示（反向）
    for (Map.Entry<String, String> entry : codecDisplayToValue.entrySet()) {
        codecValueToDisplay.put(entry.getValue(), entry.getKey());
    }

    // 设置 UI 下拉选项为中文显示
    videoCodecBox.getItems().addAll(codecDisplayToValue.keySet());
    videoCodecBox.setValue("H.264 (软件编码)");

    // 码率选择不变
    videoBitrateBox.getItems().addAll(
            "原始码率", "600k", "800k", "1200", "1500k", "1600k", "2000k", "2400k", "3000k", "5000k"
    );
    videoBitrateBox.setValue("原始码率");
}

    private FileType detectType(File file) {
        String name = file.getName();
        int idx = name.lastIndexOf('.');
        String ext = (idx > 0 ? name.substring(idx + 1).toLowerCase() : "");

        // —— 1. 扩展名优先判断 ——
        switch (ext) {
            case "mp4": case "mkv": case "avi": case "mov": case "flv":
                return FileType.VIDEO;
            case "mp3": case "wav": case "aac": case "flac": case "ogg":
                return FileType.AUDIO;
        }

        // —— 2. 扩展名不在列表，再用 Tika 嗅探 ——
        try {
            String mime = tika.detect(file);      // e.g. "audio/mpeg", "video/mp4"
            if (mime != null) {
                if (mime.startsWith("video/")) return FileType.VIDEO;
                if (mime.startsWith("audio/")) return FileType.AUDIO;
            }
        } catch (IOException ignored) { }

        // —— 3. 都不符合，则当 OTHER ——
        return FileType.OTHER;
    }


    private void initAudioTap() {
        /**
         * 音频菜单界面初始化
         *
         *
         * **/

        audioCodecBox.getItems().addAll(
                "aac",           // 默认 AAC
                "libmp3lame",    // MP3
                "libopus",       // Opus
                "ac3",           // AC-3
                "flac"           // FLAC 无损
        );
        audioCodecBox.setValue("aac");

        audioBitrateBox.getItems().addAll(
                "原始码率",
                "64k", "96k", "128k", "192k", "256k", "320k"
        );
        audioBitrateBox.setValue("原始码率");
    }

    private void initCRFControls() {
        // 默认值为 23
        crfSlider.setValue(23);
        crfTextField.setText("23");

        // 滑块 → 文本框
        crfSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int crf = newVal.intValue();
            crfTextField.setText(String.valueOf(crf));
            updateCommandPreview(); // 可选：同步更新预览
        });

        // 文本框 → 滑块
        crfTextField.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                int val = Integer.parseInt(newVal);
                if (val >= 0 && val <= 51) {
                    crfSlider.setValue(val);
                    updateCommandPreview(); // 可选：同步更新预览
                }
            } catch (NumberFormatException ignored) {
                // 非法输入不处理
            }
        });
        videoCodecBox.valueProperty().addListener((obs, oldVal, newCodec) -> {
            boolean supportsCrf = newCodec != null &&  (newCodec.contains("libx264") || newCodec.contains("libx265") || newCodec.contains("libvpx-vp9"));
            useCrfToggle.setDisable(!supportsCrf);

            if (!supportsCrf) {
                useCrfToggle.setSelected(false);      // 自动取消勾选
                crfSlider.setDisable(true);           // 同时禁用滑块和文本
                crfTextField.setDisable(true);
                bitrateField.setDisable(false);       // 启用码率设置
                videoBitrateBox.setDisable(false);
            }
        });
        // 监听 Toggle 开关状态
        useCrfToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
            boolean useCrf = newVal;
            crfSlider.setDisable(!useCrf);
            crfTextField.setDisable(!useCrf);
            bitrateField.setDisable(useCrf);
            videoBitrateBox.setDisable(useCrf);
            updateCommandPreview();
        });
    }
    private void initClipControls() {
        clipToggle.selectedProperty().addListener((obs, oldVal, isSelected) -> {
            startTimeField.setDisable(!isSelected);
            endTimeField.setDisable(!isSelected);
            updateCommandPreview();  // 勾选变化也可以触发命令预览更新
        });

        // 初始化默认禁用
        startTimeField.setDisable(true);
        endTimeField.setDisable(true);
    }

    private void initCheckffmpeg() {
        if (FFmpegChecker.isFFmpegInSystemPath()) {
            ffmpegPathField.setText("ffmpeg"); // 系统可识别
            ffmpegVersionField.setText(FFmpegChecker.getFFmpegVersion("ffmpeg"));
        } else if (FFmpegChecker.isValidFFmpegPath(ffmpegPath)) {
            ffmpegPathField.setText(ffmpegPath);
            ffmpegVersionField.setText(FFmpegChecker.getFFmpegVersion(ffmpegPath));
        } else {
            ffmpegVersionField.setText("未检测到");
            DialogUtils.showMaterialError(rootPane, "未检测到 FFmpeg", "请检查是否安装FFmpeg或者将其添加至环境变量或者在设置手动选择 FFmpeg 可执行文件");
        }

    }
    private void initTextWatermarkControls() {
        textWatermarkPosition.getItems().addAll("左上", "右上", "右下", "左下");
        textWatermarkPosition.setValue("右下");

        textWatermarkToggle.selectedProperty().addListener((obs, oldVal, isSelected) -> {
            textWatermarkContent.setDisable(!isSelected);
            textWatermarkSize.setDisable(!isSelected);
            textWatermarkPosition.setDisable(!isSelected);
            updateCommandPreview();
        });

        textWatermarkContent.setDisable(true);
        textWatermarkSize.setDisable(true);
        textWatermarkPosition.setDisable(true);
    }
    private void initAVSplitControls() {

        saveModeBox.getItems().addAll(
                "音频 + 视频",
                "只保存音频",
                "只保存视频"
        );
        saveModeBox.setValue("音频 + 视频");  // 可默认值
        // 启用/禁用保存模式 ComboBox
        avSeparationToggle.selectedProperty().addListener((obs, oldVal, isSelected) -> {
            saveModeBox.setDisable(!isSelected);
            updateCommandPreview();
        });
        saveModeBox.valueProperty().addListener((obs, oldVal, newVal) -> updateCommandPreview());
    }
    /**
     * 根据 FileTask 及当前 UI 状态，生成对应的 ffmpeg 命令列表
     */
    private List<String> buildCommandForTask(FileTask task) {
        return new CommandBuilder()
                .setFfmpegPath(ffmpegPathField.getText().isBlank() ? "ffmpeg" : ffmpegPathField.getText())
                .setInputFile(task.getFile())
                .setFileType(task.getType())
                .setFormat(task.getFormat())
                .setResolution(task.getResolution())
                .setBitrate(task.getBitrate())
                .setVideoCodec(videoCodecBox.getValue())
                .setAudioCodec(audioCodecBox.getValue())
                .setOutputDir(outputDirField.getText())
                .setVideoBitrate(videoBitrateBox.getValue())
                .setAudioBitrate(audioBitrateBox.getValue())
                .setUseCrf(useCrfToggle.isSelected())
                .setCrfValue((int) crfSlider.getValue())
                .setAVSeparationMode(avSeparationToggle.isSelected() ? saveModeBox.getValue() : null)
                .build();
    }
}
