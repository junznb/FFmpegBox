package com.ffmpegbox;

import io.github.palexdev.materialfx.theming.JavaFXThemes;
import io.github.palexdev.materialfx.theming.MaterialFXStylesheets;
import io.github.palexdev.materialfx.theming.UserAgentBuilder;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import com.ffmpegbox.controller.MainController;


public class MainApp extends Application {

    public static void main(String[] args) {
        // 1️⃣ 注册 Modena (JavaFX 默认) 主题，以保证与系统默认风格兼容
        // 2️⃣ 再注册 MaterialFX 整套样式
        //    forAssemble(true) 会自动打包并解析内部资源（CSS、图标等）
        UserAgentBuilder.builder()
                .themes(JavaFXThemes.MODENA)
                .themes(MaterialFXStylesheets.forAssemble(true))
                .build()
                .setGlobal();  // 全局生效

        launch(args);
    }

    @Override
    public void stop() {
        if (MainController.executor != null && !MainController.executor.isShutdown()) {
            MainController.executor.shutdown();
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/MainView.fxml"));
        AnchorPane root = loader.load();

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);


        primaryStage.setTitle("FFmpegBox");
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/icon.png")));
        primaryStage.setResizable(false);
        primaryStage.show();
    }


}
