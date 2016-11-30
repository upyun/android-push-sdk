##RTMP推流-录屏直播(Android)

功能概述：
手机录屏直播，即可以直接把主播的手机画面作为直播源，同时可以叠加摄像头预览，应用于游戏直播、移动端APP演示等需要手机屏幕画面的场景。

**1. SDK使用须知**

本SDK需运行于Android 5.0以上系统，主播只需要在直播前安装并启动直播App，然后按Home将App切到后台，之后主播屏幕上的内容都可以作为直播内容。
其内部原理是使用Android系统提供的录屏API进行画面采集，并由RTMP SDK底层模块负责编码和RTMP推流。


**2. SDK对接方法**

**step 1. 创建CaptureEngine对象**

CaptureConfig mConfig = new CaptureConfig(context, mRtmpUrl, mBitrate, mPushMode);
CaptureEngine mEngine = new CaptureEngine(mConfig);


创建一个CaptureEngine对象，该对象主要用来完成屏幕截取和编码推流。

不过在创建 CaptureEngine 对象时，需要您指定一个CaptureConfig对象，该对象用来是决定推流时各个环节的配置参数，比如推流地址、码率、模式（横屏或竖屏）等，
手机录屏直播提供了三个级别的分辨率可供选择：360*640，540*960，720*1280，且推流过程中，不支持动态切换分辨率和推流模式。必须先停止推流，等配置好之后，再
重新进行推流。

相比于摄像头直播，录屏直播的不确定性会大很多，其中一个最大的不确定性因素就是录屏的场景。

（1） 一种极端就是手机屏幕停在一个界面保持不动，比如桌面，这个时候编码器可以用很小的码率输出就能完成任务。

（2）另一种极端情况就是手机屏幕每时每刻都在发生剧烈的变化，比如主播在玩《神庙逃跑》，这个时候即使 540 * 960 的普通分辨率也至少需要 2Mbps 的码率才能保证没有马赛克。


**step 2. 开启和关闭推流**

// 开启推流

MediaProjection mMediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data);
mEngine.start(mMediaProjection);

// 关闭推流

mEngine.stop();


**step 3. 开启和关闭摄像头预览**

摄像头预览窗口是以悬浮窗的方式实现的，开启预览时，需要添加如下权限：
```
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.CAMERA" />
```

注意：如果摄像头预览无法开启，查看手机权限管理，该录屏直播App的悬浮窗权限是否开启。

// 开启摄像头预览

mEngine.setCameraEnable(true);

// 关闭摄像头预览

mEngine.setCameraEnable(false);


**3. SDK需要的所有权限**

```
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.RECORD_AUDIO"/>
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.CAMERA" />
```

