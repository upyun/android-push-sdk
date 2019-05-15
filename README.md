# android-push-sdk

## SDK 概述

`android-push-sdk` 是一个适用于 Android 平台 RTMP 推流的 SDK，可高度定制化和二次开发，为 Android 开发者提供简单，快捷的接口。新版 SDK 已经去除ffmpeg 不再依赖 so 库，方便开发者调试和开发，并提供带滤镜版本和不带滤镜版本。

## 推流器功能特性

* 音频编码：`AAC` 

* 视频编码：`H.264`

* 支持音频，视频硬件编码

* 支持视频软编码

* 推流协议：`RTMP`

* 支持音视频目标码率设置

* 支持弱网情况下自动丢帧

* 美颜和其他常用滤镜

## SDK 使用

* 普通版：

   1.导入 Android lib Module [uppush](https://github.com/upyun/android-push-sdk/tree/master/uppush)

   2.SDK 已经上传 Jcenter，Android Studio 的用户可以直接在 gradle 中添加一条 dependencies:

   ```
   compile 'com.upyun:uppush:0.9.1'
   ```
* 滤镜版本：导入 java lib Module [magicfilter](https://github.com/upyun/android-push-sdk/tree/master/magicfilter)

## SDK 使用DEMO
* 普通版：运行 Module [APP](https://github.com/upyun/android-push-sdk/tree/master/app)。

* 滤镜版：运行 Modeule [magicfilterdemo](https://github.com/upyun/android-push-sdk/tree/master/magicfilterdemo)。

* 录屏版：可实现录屏直播，需 Android 5.0及以上系统支持，录屏 SDK [文档](https://github.com/upyun/android-push-sdk/tree/master/README_RecordScr.md)及 DEMO [下载链接](http://formtest.b0.upaiyun.com/assert/luping.zip)。

* 连麦版：可实现连麦直播，[文档](https://github.com/upyun/android-push-sdk/tree/master/README_RTC.md)和 DEMO [下载地址](http://formtest.b0.upaiyun.com/assert/lianmai.zip)。
## SDK 使用示例

### 添加 SDK 所需权限
```
	<uses-permission android:name="android.permission.CAMERA"/>
    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.RECORD_AUDIO"/>
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
```


### 初始化 PushClient 传入 SurfaceView 和 Config 参数：

```java
	 public PushClient(SurfaceView surface, Config config)

```

**参数说明：**

* `SurfaceView `  Android 原生 view，初始化后传入

*  `config ` 配置参数，按需要设置，Config 包含以下设置项

```java
	//分辨率 HIGH(1280*720) NORMAL(640*480) LOW(320*240)
    public Resolution resolution;
    //推流地址
    public String url;
    //比特率
    public int bitRate;
    //每秒帧数
    public int fps;
    //摄像头类型 前置，后置
    public int cameraType;
    //显示方向 水平，竖直
    public Orientation orientation;
    //使用软编码
    public boolean useSofeEncode;
    
```

### 开始和关闭推流：

```java
	public void startPush()
	public void stopPush()
``` 

### 修改设置配置：

```java
	 public void setConfig(Config config)
```

## SDK 最低要求

Android 4.1(API 16) 以上

## UPYUN 直播平台自主配置流程

**1.注册新建又拍云账号**  

[注册地址](https://console.upyun.com/#/register/)  

**2.进行账户认证**  

[账户认证](https://console.upyun.com/#/account/profile/)  

**3.创建服务**  

[创建服务](https://console.upyun.com/#/services/)  

填写服务名称，简称 `bucket`  

资源获取方式选择自主源站  

加速域名和回源地址如有则填写，如无可以任意编辑结构合法内容，如域名为 `a.com`，回源 `IP` 为 `1.1.1.1`  

创建好服务后如需进行直播功能测试和加速，会有对应的服务人员与您联系，您将需求按如下格式整理好发送给服务人员，我们会尽快为您提供测试服务

格式如下  

账户名：`xxxxx`  

服务名称：`bucket` 名

`app` 名：如 `show/*`  `show` 代表应用名称，`*` 代表目录后可以为 `stream id`

拉流需要支持的格式：`rtmp` 或 `http-flv` 或 `hls` (三个至少选其中一个)  

对外服务的推流域名：`xxx.com` （如无可不填写）  

对外服务的拉流域名：`xxx.com` （如无可不填写）  

## 版本历史

0.1.0 基本的直播推流器

* 推流器支持rtmp推流

0.2.0 完善推流功能

* 修复音频录制bug

* 支持自定义推流参数

* 支持后台推流

0.3.0 修改传输层

* 更改数据传输层，去除ffmpeg的依赖

* 推流支持自由剪裁像素尺寸

* 增加异常处理，避免crash

0.4.0 滤镜功能

* 增加滤镜版本推流器和相关demo

* 增加对焦和闪光灯

* 增加单音频推流

* 修复其他bug

0.9.1 预览界面比例适应（9.18）

* 预览界面比例自适应，修复全屏时图像拉伸问题

1.0.0 动态码率&自动重连（1.6）

* 支持码率调整

* 增加自动重连

1.0.1 添加软编码&性能优化

* 添加软编码

* 图像处理性能优化 

## 反馈与建议

 邮箱：<livesdk@upai.com>
 
 QQ: `3392887145`
