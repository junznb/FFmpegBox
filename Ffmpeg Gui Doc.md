# FFmpeg 图形化转码工具项目文档

## 1. 项目概述

在音视频处理领域，FFmpeg 被广泛应用于视频转码、格式转换、音频提取、流媒体处理等任务。FFmpeg 是一款功能强大但高度依赖命令行操作的工具，其参数复杂、学习成本高，对于非技术用户来说存在一定使用门槛。本项目正是在这种背景下提出，旨在为 FFmpeg 提供一套图形化用户界面，使用户可以通过图形界面轻松完成常见的视频处理任务，而无需掌握繁琐的命令行语法。
### 示例：主界面展示

![主界面截图](doc\images\main_view.png)

如图所示，界面采用浅蓝白主题，支持文件拖拽导入，列表中每个文件对应独立进度条与输出格式设置区域……


本项目基于 JavaFX 框架开发图形界面，并引入了 MaterialFX 组件库提升界面美观性与交互性。用户可以通过拖拽文件的方式批量导入视频，随后根据需要为每个视频单独设置输出格式、分辨率、码率等参数，也可以选择是否仅导出音频或视频流。界面还提供了可视化进度条来实时展示每个转码任务的处理进度。系统支持多任务并发处理，确保在多个视频同时转码时依然保持界面流畅。

与传统的 FFmpeg 图形界面封装工具相比，本项目更注重用户交互的灵活性与批处理能力：每个任务都被封装为独立对象，支持独立配置与管理，便于拓展更多功能如暂停、重新排队、模板复用等。同时，通过模块化设计，项目中的命令构建逻辑、进度解析、设置保存等均被抽象为独立类，具备良好的可维护性与拓展性。

技术方面，本项目使用 Java 17 作为开发语言，界面部分由 JavaFX 实现，界面样式使用自定义 CSS 统一风格，核心功能模块包括：文件导入与列表展示、参数设置与验证、命令构建与日志输出、转码控制与进度绑定、用户设置的保存与加载等。整个系统遵循 MVC 模式设计，使得界面控制逻辑与业务逻辑相互独立，便于未来的功能扩展与测试。

综上所述，本项目不仅实现了对 FFmpeg 的高效封装，也锻炼了开发者对 Java GUI 编程、多线程调度、进程控制、配置管理等多方面能力的综合应用，是一次完整的桌面应用开发实践。

项目使用的技术栈包括：

* Java 17+
* JavaFX 图形界面框架
* MaterialFX UI 组件库
* 多线程处理（ExecutorService）
* FFmpeg 命令行调用与日志解析
* 用户设置持久化（JSON 文件）

该项目不仅提升了 FFmpeg 的可用性，也加深了对 Java GUI、进程控制与并发编程的理解。

本项目已同步提交至 GitHub 远程仓库，完整源代码与文档均可在线访问：

> 📁 仓库地址：
> [https://github.com/junznb/FFmpegBox](https://github.com/junznb/FFmpegBox)

欢迎访问查看、下载或提出建议。


## 2. 系统功能模块

### 2.1 文件管理模块（File Management Module）

文件管理模块是本项目的核心起点，负责用户导入视频文件并展示于图形界面中。用户可以通过**拖拽**的方式一次性导入多个视频文件，系统会自动将其解析并添加到 `ListView` 控件中进行可视化展示。每个文件会以“文件名 + 进度条”的形式显示，并在后续转码过程中实时更新其处理状态。

#### ✦ 功能亮点：

* 支持批量拖拽导入视频文件
* 每个任务绑定独立的进度条，便于多任务监控
* 支持键盘快捷删除（Delete 键）或界面按钮移除视频项
* 所有任务通过 `FileTask` 封装，便于参数管理与状态更新

#### ✦ 主要组件与设计：

##### 拖拽区域设计

在 `MainController` 中调用 `setupDragAndDrop()` 方法，为主界面设置拖拽事件监听器。用户将文件拖入界面时，系统会检测 MIME 类型是否为视频格式（如 `.mp4`, `.mov`, `.mkv`），并自动将合法文件封装为 `FileTask` 对象。

```java
listView.setOnDragOver(event -> {
    if (event.getDragboard().hasFiles()) {
        event.acceptTransferModes(TransferMode.COPY);
    }
    event.consume();
});
```

##### `FileTask` 类封装

每个文件被封装为一个 `FileTask` 实例，包含以下信息：

* 输入文件路径（`inputFile`）
* 输出文件名与路径（可自动生成或由用户设置）
* 输出格式、分辨率、码率等参数
* 当前任务的 `ProgressBar` 控件引用
* 任务状态（未开始、转码中、已完成等）

通过这种封装方式，系统在后续模块中可以灵活访问每个任务的所有信息，实现独立转码、进度绑定、输出控制等功能。

##### 列表展示逻辑

所有 `FileTask` 被加入 `ObservableList` 数据源中，并通过 `ListView` 展示。每一行由 `HBox` 布局，左侧是文件名（`Label`），右侧是进度条（`ProgressBar`），保持美观整洁。

使用自定义样式文件 `style.css` 中的 `.list-view .progress-bar`，统一设置了进度条的宽度、圆角、颜色，使每行在视觉上具有一致性和现代感：

```css
.list-view .progress-bar {
    -fx-pref-width: 600px;
    -fx-background-radius: 4;
}
```

##### 删除功能

用户可以通过键盘的 `Delete` 键或界面按钮来删除当前选中的文件项，系统会同步将其从 `ObservableList` 中移除，并释放对应的资源，确保界面与数据一致。

#### ✦ 实现亮点与优势：

* 每个任务的 UI 元素和数据模型都被一一对应，避免混乱更新
* 利用 JavaFX 的响应式数据模型与线程机制，保证界面不卡顿
* 为后续扩展如“暂停任务”、“修改参数”、“重新排队”等操作打下基础

#### ✦ 可视化展示（示例图）：

#### 图 2-1：拖入多个视频文件后的任务列表

![文件列表展示](doc\images\file_list_demo.png)














### 2.2 参数配置模块（Parameter Configuration Module）

参数配置模块是转码功能的核心控制区域，允许用户为每个输入视频文件指定不同的输出设置，包括视频格式、分辨率、码率和音视频提取模式等。该模块支持**对每个文件单独设置参数**，并在最终构建 FFmpeg 命令时动态应用，确保输出结果最大限度满足用户的多样化需求。

#### ✦ 配置项说明：

1. **输出格式**

   * 支持常见的视频容器格式，如 MP4、MKV、AVI、MOV 等。
   * 实现方式：通过 `ComboBox<String>` 让用户选择目标格式，默认值为 MP4。
   * 在命令构建中自动使用 `-f` 或设置输出扩展名来应用格式。

2. **分辨率设置**

   * 允许用户输入目标分辨率（例如 `1920x1080`、`1280x720`）。
   * 使用 JavaFX 的 `TextField` 输入框，并配合正则校验确保格式合法。
   * 对应 FFmpeg 命令中的 `-s` 参数。

3. **码率设置**

   * 允许输入目标视频码率（如 `1000k` 表示每秒 1000kb）。
   * 若留空，则系统自动跳过 `-b:v` 参数，默认保留原始质量。
   * 这一策略避免了强制重压缩导致的视频质量损失或冗余设置。

4. **音视频提取模式**

   * 用户可选择：

     * 完整保存（默认）：输出包含音视频
     * 仅视频（去音频）
     * 仅音频（去视频）
   * 实现方式：通过枚举 `FileType`（ALL、ONLY\_VIDEO、ONLY\_AUDIO）与 `ToggleButton` 和 `ComboBox` 联动控制。
   * 最终在命令生成时动态决定是否添加 `-an` 或 `-vn` 参数。



#### ✦ 实现机制与数据结构：

参数信息与每个文件一一对应，封装在 `FileTask` 类的以下字段中：

```java
private String resolution;
private String bitrate;
private String outputFormat;
private FileType fileType; // 枚举值
```

当用户在界面上切换某项设置时，程序会自动更新对应的 `FileTask` 实例，确保后续转码任务可以获取到完整的个性化参数。



#### ✦ 命令构建逻辑：

所有参数设置最终会通过 `CommandBuilder` 类中的链式方法构建为 FFmpeg 命令。例如：

```java
CommandBuilder builder = new CommandBuilder(ffmpegPath)
    .setInput(inputFile)
    .setResolution(resolution)
    .setBitrate(bitrate)
    .setFileType(fileType)
    .setOutput(outputFile);
```

若用户未设置码率，`CommandBuilder` 会在内部逻辑中判断该字段是否为空，从而决定是否添加 `-b:v` 参数，避免无效或干扰参数传入。


#### ✦ 用户交互体验优化：

* 所有输入框均支持默认值与手动修改，避免用户误操作。
* 使用 MaterialFX 的控件提供更直观的提示文本与选择样式。
* 设置区被放置在 TabPane 中的“设置页签”中，符合用户直觉。


#### ✦ 可视化展示（示例图）：


#### 图 2-2：参数设置区域界面展示

![参数设置面板](doc\images\parameter_panel.png)



#### ✦ 模块亮点与可拓展性：

* 支持每个文件单独设置参数，便于灵活批量处理不同需求。
* 结构清晰，便于后续拓展更多参数，如帧率（-r）、裁剪（-vf crop）、添加字幕等。
* 在不破坏用户体验的前提下，最大限度地保留了 FFmpeg 的可配置性。




### 2.3 音视频分离支持（Audio/Video Extraction Support）

本模块为用户提供了对输出内容的精细控制。通过界面中的下拉选择器或切换按钮，用户可以快速指定转码时的输出类型：

* **完整输出**分离音频与视频（默认模式）
* **仅输出视频**：去除音频流，适用于静音素材剪辑
* **仅输出音频**：去除视频流，适用于音频提取场景

#### ✦ 实现方式：

项目中定义了一个 `FileType` 枚举类，包含以下三个取值：

```java
ALL, ONLY_VIDEO, ONLY_AUDIO
```

每个 `FileTask` 实例都保存了当前任务所选的 `FileType` 值。在构建命令时，系统会根据该值动态添加参数：

* `ONLY_VIDEO`：添加 `-an`（即“audio none”，移除音频）
* `ONLY_AUDIO`：添加 `-vn`（即“video none”，移除视频）
* `ALL`：执行以上两条命令

#### ✦ 输出文件名自动修改：

为了区分不同输出类型，系统还会在生成输出文件路径时自动添加后缀。例如：

* `example.mp4` → `example_audio.mp3`（仅音频）
* `example.mp4` → `example_video.mp4`（仅视频）

这个逻辑在 `CommandBuilder` 中实现，确保用户无需手动重命名，即可清晰区分生成结果，避免文件覆盖。

#### ✦ 使用场景示例：

* 将 MP4 视频转换为 MP3 音频文件
* 从视频中剥离音轨，提取纯图像素材
* 保留完整视频流并另存一份音频备份


非常好！你当前展示的 `build()` 方法比先前描述更完整，支持了**剪辑（-ss/-to）**、**水印**、**CRF压缩**、**音视频分离**、**编码器选择**、**码率设置**、**输出路径与格式控制**等多种功能。


### 2.4 FFmpeg 命令构建模块（FFmpeg Command Builder）

为了生成结构清晰、参数灵活的 FFmpeg 命令，本项目设计了 `CommandBuilder` 类，通过面向对象封装各种参数，并集中生成完整命令列表。其核心方法 `build()` 将所有设置项有序转换为 FFmpeg 命令行参数，支持复杂转码需求。

#### ✦ 功能覆盖：

该模块支持如下功能参数的构建：

| 功能      | FFmpeg 参数示例                 |
| ------- | --------------------------- |
| 输入文件    | `-i input.mp4`              |
| 起止时间剪辑  | `-ss 00:01:00 -to 00:03:00` |
| 分辨率设置   | `-s 1280x720`               |
| 视频码率    | `-b:v 1000k`                |
| 音频码率    | `-b:a 192k`                 |
| 视频编码器   | `-c:v libx264`              |
| 音频编码器   | `-c:a aac`                  |
| CRF压缩质量 | `-crf 23`                   |
| 添加水印文字  | `-vf drawtext=...`          |
| 音视频分离模式 | `-an`（去音）或 `-vn`（去视频）       |
| 强制输出格式  | `-f mp4`、`-f mp3` 等         |
| 输出文件命名  | 自动添加 `_audio` / `_video` 后缀 |


#### ✦ 实现细节与逻辑顺序：

`build()` 方法中，命令按以下顺序组织，确保参数在 FFmpeg 中解析正确：

1. **基础配置**：

   * 添加 `ffmpeg` 执行路径与 `-y` 覆盖开关。
2. **剪辑参数**：

   * 如果用户设置了 `startTime` 或 `endTime`，则添加 `-ss` 和 `-to` 参数。
3. **输入文件**：

   * 使用 `-i` 加上绝对路径。
4. **视频转码参数**：

   * 视频编码器（`-c:v`）、分辨率（`-s`）、码率（`-b:v`）、CRF（`-crf`）。
5. **水印文字绘制**：

   * 使用 `drawtext` 滤镜构造 `-vf` 参数，包括字体、位置、字号、文字内容。
6. **音视频分离控制**：

   * 根据用户选择添加 `-vn`（仅音频）或 `-an`（仅视频）。
7. **音频转码参数**：

   * 音频编码器（`-c:a`）、码率（`-b:a`）。
8. **强制容器格式**：

   * 使用 `-f` 参数强制设定输出文件格式。
9. **输出路径构造**：

   * 输出文件名自动加后缀（如 `_audio.mp3` 或 `_video.mp4`）并拼接输出目录。


#### ✦ 关键逻辑示意：

```java
if (isAudioOnly) {
    cmd.add("-vn"); // 禁用视频流
} else if (isVideoOnly) {
    cmd.add("-an"); // 禁用音频流
}

if (useCrf) {
    cmd.addAll(Arrays.asList("-crf", String.valueOf(crfValue)));
}

if (textWatermarkContent != null) {
    cmd.addAll(Arrays.asList("-vf", "drawtext=...")); // 水印构造
}
```

命令末尾的输出路径也会自动根据类型拼接后缀：

```java
String suffix = isAudioOnly ? "_audio" : isVideoOnly ? "_video" : "";
String out = outputDir + File.separator + base + suffix + "." + actualFormat;
cmd.add(out);
```


#### ✦ 命令预览部分

为了增强用户的可操作性与可视反馈，项目在界面中提供了“命令预览”功能区域。当用户在界面中设置好输入文件、输出参数、编码选项、水印内容等信息后，系统会**实时将这些配置转化为一条完整的 FFmpeg 命令字符串**并显示在预览框中，用户可一键复制或检查命令是否符合预期。

该功能的实现逻辑基于 `CommandBuilder.build()` 方法内部的参数拼接机制。命令预览区域使用 `TextArea` 控件承载命令文本，并禁用编辑操作，确保其只作为输出窗口使用。

例如，用户设置如下参数：

* 输入文件：`test.mp4`
* 输出格式：mp4
* 分辨率：1280x720
* 视频码率：1000k
* 音视频分离：仅音频
* 输出文件夹：`./output/`

系统自动预览命令为：

```bash
ffmpeg -y -i test.mp4 -vn -s 1280x720 -b:v 1000k -f mp3 ./output/test_audio.mp3
```

该预览功能对于**进阶用户检查参数、复制到命令行调试**非常有用，同时也提升了整个系统的透明性与专业性。
#### 图 2-3：参数设置区域界面展示

![命令预览](doc\images\CommandPreview.png)






#### ✦ 模块优势：

* **高度解耦**：所有命令参数构建集中在 `CommandBuilder` 内，UI 无需关心细节。
* **配置灵活**：任意参数缺失则自动跳过，不影响整体命令合法性。
* **可拓展性强**：轻松支持新增参数，如帧率（-r）、字幕、滤镜等。


### 2.5 转码执行模块

使用 `FFmpegController` 启动命令行进程，并解析 FFmpeg 的标准输出：

* 实时读取进度行（正则提取 `frame=`, `time=`, `speed=` 等字段）
* 将输出进度更新到 JavaFX 线程（使用 `Platform.runLater`）
* 多任务并发处理，避免阻塞界面

任务调度使用线程池（`ExecutorService`），每个 `FileTask` 独立执行。

### 2.6 用户设置模块

FFmpeg 可执行文件路径等用户设置通过 `SettingsManager` 和 `UserSettings` 实现保存。

* 设置以 JSON 文件格式存储
* 用户每次打开程序自动加载上次设置
* 使用 `FileChooser` 选择可执行文件路径

### 2.7 UI 美化与 MaterialFX 使用

界面采用 MaterialFX 提供的控件：

* `MFXButton`、`MFXTextField`、`MFXFilterComboBox`
* 统一风格、圆角样式、悬浮效果

CSS 文件 `style.css` 实现了完整的浅蓝白主题配色：

* 白色背景、天蓝按钮
* ListView 条纹边框
* Tab 标签高亮动画
* 自定义进度条长度与样式

整体界面简洁、现代，增强用户体验。

## 3. 关键类说明

| 类名                 | 职责                                        |
| ------------------ | ----------------------------------------- |
| `MainController`   | 主界面控制器，负责绑定事件、更新进度、管理任务                   |
| `CommandBuilder`   | 构建 FFmpeg 命令字符串，提供链式调用接口                  |
| `FFmpegController` | 负责执行命令、读取进度、处理日志                          |
| `FileTask`         | 每个视频文件对应的任务实体，含参数、进度等信息                   |
| `SettingsManager`  | 设置管理器，负责保存与读取配置文件                         |
| `UserSettings`     | 用户设置数据类，包括 FFmpeg 路径等                     |
| `DialogUtils`      | 弹窗工具类，提供错误提示等通用界面反馈                       |
| `FileType`         | 枚举类型，表示三种导出模式：ALL、ONLY\_VIDEO、ONLY\_AUDIO |

## 4. 部分核心代码解析

### 4.1 拖拽导入文件并创建任务

```java
listView.setOnDragDropped(event -> {
    List<File> files = ...;
    for (File file : files) {
        FileTask task = new FileTask(file);
        taskList.add(task);
    }
    listView.setItems(taskList);
});
```

### 4.2 创建转码任务并执行

```java
Runnable task = () -> {
    String command = CommandBuilder.of(file).build();
    FFmpegController.execute(command, progressBar);
};
executor.submit(task);
```

### 4.3 解析进度并更新 UI

```java
Pattern pattern = Pattern.compile("frame=.*time=(.*?) ");
while ((line = reader.readLine()) != null) {
    Matcher matcher = pattern.matcher(line);
    if (matcher.find()) {
        double percent = ...;
        Platform.runLater(() -> progressBar.setProgress(percent));
    }
}
```

## 5. 技术难点与解决方案

* **多线程 UI 更新冲突**：JavaFX UI 只能在主线程更新，使用 `Platform.runLater` 包装更新逻辑。
* **命令构建灵活性不足**：设计 `CommandBuilder` 链式接口替代字符串拼接。
* **多文件同时转码**：使用线程池调度，避免界面阻塞。
* **用户配置保存**：借助 `Gson` 序列化配置为 JSON，保存用户偏好设置。




## 6. 总结与展望

本项目以 FFmpeg 为核心引擎，围绕“批量视频转码”这一高频应用场景，构建了一个功能完整、界面美观、交互友好的图形化工具。用户可以通过拖拽文件、设置参数、点击运行等直观操作，完成传统上需手动输入复杂命令的任务，从而大大降低了 FFmpeg 的使用门槛。

项目基于 Java 17 编写，前端使用 JavaFX + MaterialFX 构建现代化用户界面，后端则通过自定义的 `CommandBuilder` 和 `FFmpegController` 实现命令构建、进度监控、任务调度等逻辑，体现出良好的代码组织能力与模块化设计理念。在文件管理、参数配置、音视频分离、命令预览、用户设置保存等功能中，均做到了高内聚、低耦合，方便维护与拓展。

此外，本项目也充分考虑了用户体验与可视化效果，包括：

* 样式统一的 MaterialFX UI 与浅蓝白主题；
* 实时更新的任务进度条与日志输出窗口；
* 每个文件任务均可单独配置参数并独立执行；
* 支持命令预览，提升对高级用户的透明度。

通过本次开发，作者对 Java GUI 编程、线程池处理、进程通信、JSON 配置读写等方面的技术有了更深入的理解，同时也锻炼了从需求分析、架构设计到编码调试、界面美化等完整的项目实现流程。


### ✦ 后续优化展望

尽管项目已具备基本功能，但在用户需求多样化、专业性增强等方面仍有较大提升空间。未来可以从以下几个方向进行改进与拓展：

1. **视频截图与片段裁剪功能**

   * 提供可视化时间轴，允许用户选择某段时间进行截图或剪辑。
   * 使用 FFmpeg 的 `-ss`、`-to`、`-frames:v 1` 参数实现精确控制。

2. **FFprobe 信息展示**

   * 在导入文件后，自动调用 FFprobe 提取媒体元数据（时长、分辨率、编码格式等），并展示在界面上，辅助用户设置参数。

3. **转码任务中断 / 暂停 / 取消控制**

   * 引入更复杂的线程管理，允许用户在任务运行时点击“暂停”、“取消”按钮。
   * 结合 `Process.destroy()` 或信号控制机制实现终止。

4. **参数模板保存与复用**

   * 允许用户将当前的转码参数保存为模板，供下次批量应用到多个文件，提高效率。

5. **界面国际化（i18n）**

   * 支持语言切换（中文/英文），适配不同用户群体。
   * 将界面文本提取为 `ResourceBundle` 配置，支持动态翻译。

6. **高级滤镜与功能拓展**

   * 添加字幕、水印图片、背景音乐等复合功能；
   * 实现命令行调试模式，供高级用户修改原始命令；

7. **任务记录与转码历史管理**

   * 自动记录每次转码任务参数与输出路径；
   * 提供历史回放、导出日志等功能；



通过不断拓展，本项目有潜力演化为一款面向创作者、教育工作者、媒体处理人员的专业级工具，在简化复杂音视频处理流程的同时，提升效率与易用性。





