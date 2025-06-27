package com.ffmpegbox.controller;

import io.github.palexdev.materialfx.controls.MFXFilterComboBox;
import io.github.palexdev.materialfx.controls.MFXTextField;
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
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import com.ffmpegbox.model.FileTask;
import com.ffmpegbox.model.UserSettings;
import com.ffmpegbox.utils.CommandBuilder;
import com.ffmpegbox.utils.FFmpegController;
import com.ffmpegbox.utils.SettingsManager;
import java.util.HashMap;
import java.util.Map;
import java.io.File;
import javafx.scene.input.KeyCode;
import io.github.palexdev.materialfx.controls.MFXProgressBar;

public class MainController {

    private String ffmpegPath = "ffmpeg";
    private final Map<String, FileTask> fileTaskMap = new HashMap<>();


    //MFX
    @FXML private MFXFilterComboBox<String> formatBox_MFX_file;
    @FXML private MFXTextField outputDirField;
    @FXML private MFXTextField resolutionField;
    @FXML private MFXTextField bitrateField;
    @FXML private MFXTextField ffmpegPathField;
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

        setupDragAndDrop();
        fileListView.setPlaceholder(new Label("将文件拖入此区域"));
        // 初始化格式下拉框和默认值
        formatBox_MFX_file.getItems().addAll("mp4", "avi", "mkv", "mov");
        formatBox_MFX_file.setValue("mp4");



        resolutionField.setText("1920x1080");
        bitrateField.setText("800k");

        // 加载历史设置
        UserSettings settings = SettingsManager.loadSettings();
        if (settings != null) {
            if (settings.getFfmpegPath() != null) {
                ffmpegPath = settings.getFfmpegPath();
                ffmpegPathField.setText(ffmpegPath);
            }
            if (settings.getResolution() != null) resolutionField.setText(settings.getResolution());
            if (settings.getBitrate() != null) bitrateField.setText(settings.getBitrate());
            if (settings.getFormat() != null) formatBox_MFX_file.setValue(settings.getFormat());


//            if (settings.getLastInput() != null) inputField.setText(settings.getLastInput());
            if (settings.getLastOutputDir() != null) outputDirField.setText(settings.getLastOutputDir());
        }

        // 输入变更自动刷新命令预览
        ChangeListener<Object> listener = (obs, oldVal, newVal) -> updateCommandPreview();
        inputField.textProperty().addListener(listener);
        outputDirField.textProperty().addListener(listener);
        resolutionField.textProperty().addListener(listener);
        bitrateField.textProperty().addListener(listener);
        ffmpegPathField.textProperty().addListener(listener);
        formatBox_MFX_file.valueProperty().addListener(listener);


        updateCommandPreview();

        // 监听列表选择变化，更新参数输入框
        fileListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                Label label = (Label) newVal.getChildren().get(0); // 获取文件名
                String filename = label.getText();
                FileTask task = fileTaskMap.get(filename);
                if (task != null) {
                    formatBox_MFX_file.setValue(task.getFormat());

                    resolutionField.setText(task.getResolution());
                    bitrateField.setText(task.getBitrate());

                    // 更新隐藏的 inputField 以支持命令预览
                    inputField.setText(task.getFile().getAbsolutePath());
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
        String outputDir = outputDirField.getText();

        for (var entry : fileTaskMap.entrySet()) {
            FileTask task = entry.getValue();
            File file = task.getFile();
            ProgressBar progressBar = task.getProgressBar();

            String format = task.getFormat();
            String resolution = task.getResolution();
            String bitrate = task.getBitrate();

            if (format == null || resolution == null || bitrate == null || outputDir.isEmpty()) {
                logArea.appendText("跳过文件 " + file.getName() + "：参数不完整\n");
                continue;
            }

            String baseName = file.getName();
            int dotIndex = baseName.lastIndexOf('.');
            if (dotIndex > 0) baseName = baseName.substring(0, dotIndex);

            String outputPath = outputDir + File.separator + baseName + "." + format;

            CommandBuilder builder = new CommandBuilder()
                    .setFfmpegPath(ffmpegPath)
                    .setInputPath(file.getAbsolutePath())
                    .setOutputPath(outputPath)
                    .setFormat(format)
                    .setResolution(resolution)
                    .setBitrate(bitrate);

            FFmpegController controller = new FFmpegController(msg ->
                    Platform.runLater(() -> logArea.appendText(msg + "\n")));

            MFXProgressBar taskBar = (MFXProgressBar) task.getProgressBar();
// …
            controller.setUpdateProgressCallback(progress ->
                    Platform.runLater(() -> smoothProgress(taskBar, progress))
            );

            controller.convertVideo(
                    builder.getInputPath(),
                    builder.getOutputPath(),
                    builder.getFormat(),
                    builder.getResolution(),
                    builder.getBitrate(),
                    builder.getFfmpegPath()
            );

            logArea.appendText("开始转换：" + file.getName() + "\n");
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
        String inputPath = inputField.getText();
        String outputDir = outputDirField.getText();
        String format = formatBox_MFX_file.getValue();

        String resolution = resolutionField.getText();
        String bitrate = bitrateField.getText();
        String path = ffmpegPathField.getText().isEmpty() ? "ffmpeg" : ffmpegPathField.getText();

        if (inputPath.isEmpty() || outputDir.isEmpty() || format == null) {
            commandPreviewArea.setText("");
            return;
        }

        File inputFile = new File(inputPath);
        String baseName = inputFile.getName();
        int dotIndex = baseName.lastIndexOf('.');
        if (dotIndex > 0) baseName = baseName.substring(0, dotIndex);

        String outputPath = outputDir + File.separator + baseName + "." + format;

        CommandBuilder builder = new CommandBuilder()
                .setFfmpegPath(path)
                .setInputPath(inputPath)
                .setOutputPath(outputPath)
                .setFormat(format)
                .setResolution(resolution)
                .setBitrate(bitrate);

        commandPreviewArea.setStyle(
                "-fx-font-family: 'Courier New'; -fx-font-size: 13px; " +
                        "-fx-control-inner-background: #f8f8f8; -fx-border-color: #dcdcdc; " +
                        "-fx-border-radius: 5; -fx-background-radius: 5;");
        commandPreviewArea.setText(String.join(" ", builder.build()));
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

        SettingsManager.saveSettings(settings);
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
                    FileTask task = new FileTask(file, fileProgress);
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
                    }

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
}


