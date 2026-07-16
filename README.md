# GreenTea

GreenTea 是一个用于记录和管理血压数据的 Android 应用。

## 功能

- 保存血压记录，包括高压、低压、脉搏和时间
- 查看今日记录和历史记录
- 按日期筛选历史记录
- 删除单条记录
- 从 CSV 或 Excel 文件导入记录
- 导出为 `.xlsx` 文件
- 支持浅色 / 深色主题开屏
- 桌面图标与开屏资源分离

## 环境要求

- Android Studio
- JDK 11+
- Android SDK 36 或兼容的构建环境

## 构建与运行

### 调试构建

```bash
./gradlew assembleDebug
```

### 发布构建

```bash
./gradlew assembleRelease
```

## 开屏说明

- 开屏主题位于 `app/src/main/res/values/themes.xml`
- 深色模式开屏主题位于 `app/src/main/res/values-night/themes.xml`
- 开屏背景颜色由 `@color/splashscreen_background` 控制
- 开屏图片由 `@drawable/splashscreen_logo` 引用

## 图标说明

桌面图标与开屏已分离，桌面图标使用 `AndroidManifest.xml` 中声明的 `ic_launcher` 资源。

## 项目结构

- `app/src/main/java/com/cya544/greentea/MainActivity.kt`：主界面逻辑和 UI
- `app/src/main/res/values/themes.xml`：浅色开屏 / 应用主题
- `app/src/main/res/values-night/themes.xml`：深色开屏 / 应用主题
- `app/src/main/res/values/colors.xml`：浅色颜色配置
- `app/src/main/res/values-night/colors.xml`：深色颜色配置
- `app/src/main/res/mipmap-anydpi-v26/`：桌面图标入口

## 许可证

当前未提供许可证文件。
