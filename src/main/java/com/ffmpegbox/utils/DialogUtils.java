package com.ffmpegbox.utils;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialog;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialogBuilder;
import io.github.palexdev.materialfx.dialogs.MFXStageDialog;
import io.github.palexdev.materialfx.enums.ScrimPriority;
import io.github.palexdev.mfxresources.fonts.MFXFontIcon;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Map;

public class DialogUtils {

    public static void showMaterialError(Node owner, String title, String message) {
        Platform.runLater(() -> {
            // 创建内容区域
            MFXGenericDialog dialogContent = MFXGenericDialogBuilder.build()
                    .setHeaderText(title)
                    .setContentText(message)
                    .makeScrollable(true)
                    .get();

            dialogContent.setMaxSize(400, 200);
            dialogContent.getStyleClass().add("mfx-error-dialog");
            dialogContent.setHeaderIcon(new MFXFontIcon("fas-circle-xmark", 18));

            // 用数组包装变量引用
            final MFXStageDialog[] dialogStageRef = new MFXStageDialog[1];

            dialogContent.addActions(
                    Map.entry(new MFXButton("确定"), e -> dialogStageRef[0].close())
            );

            // 创建对话框
            Stage stage = (Stage) owner.getScene().getWindow();
            MFXStageDialog dialogStage = MFXGenericDialogBuilder.build(dialogContent)
                    .toStageDialogBuilder()
                    .initOwner(stage)
                    .initModality(Modality.APPLICATION_MODAL)
                    .setTitle("错误")
                    .setDraggable(true)
                    .setScrimOwner(true)
                    .setScrimPriority(ScrimPriority.WINDOW)
                    .setOwnerNode((Pane) owner)
                    .get();

            // 设置引用后再打开
            dialogStageRef[0] = dialogStage;
            dialogStage.showDialog();
        });
    }
}
