# 连麦使用说明

##连麦说明
连麦模块提供两台设备之间进行连麦操作,且两台设备都可以作为主播进行 rtmp 推流直播。

### DEMO 使用它说明

	
	第一行输入推流地址，第二行输入房间ID。
	JOIN CHANNEL：只加入连麦房间，不进行推流
	JOIN CHANNEL,PUSH AUDIO ONLY：加入连麦房间,只推流音频
	JOIN CHANNEL，PUSH VIDEO AND AUDIO: 加入连麦房间，并推流音视频


### 

###初始化连麦引擎

```
	RtcEngine create(Context context, String appId, IRtcEngineEventHandler handler)

```

 **参数**
 
 * appId  使用连麦功能所需验证ID 

 * IRtcEngineEventHandler 连麦状态回调

 
 ```
  		//用户进入
   		@Override
   		public void onUserJoined(int uid, int elapsed) {
        	for (MediaUiHandler uiHandler : uiHandlers) {
            	uiHandler.onMediaEvent(MediaUiHandler.USER_JOINED, uid);
        	}
    	}

    	@Override
    	public void onUserOffline(int uid, int reason) {
        	for (MediaUiHandler uiHandler : uiHandlers) {
            	uiHandler.onMediaEvent(MediaUiHandler.USER_OFFLINE, uid);
        	}
    	}
 ```
 
 其他用户连接如房间和离开房间回调

 
 ```
        videoSource = new AgoraVideoSource(); // define main class for customize video source
        videoSource.Attach();
        rtcEngine.enableVideo();


        yuvEnhancer = new AgoraYuvEnhancer(context);
        yuvEnhancer.StartPreProcess();
 ```
 
 开启视频数据自采集。
 
 
###加入和离开房间

```
	rtcEngine.setVideoProfile(VideoProfile.VIDEO_PROFILE_480P, true);
```


设置连麦分辨率为640*480


```
	rtcEngine.joinChannel(null, channelId, null, 0);

```

加入房间

 **参数**
 
 * channelId： 连麦房间ID

 
 ```
 	rtcEngine.leaveChannel();
 ```
 
 
 离开房间
 
 
### 摄像头数据采集
 
 
  摄像头数据采集部分在 Camera2BasicFragment 和 CameraBasicFragment 类中，其中 Camera2BasicFragment 类采用 Camera2 API 仅 API lev21 以上支持。CameraBasicFragment 采用 Camera API 可以支持低版本 Android 系统。
 
 
### 合图推流

```
 	private SrsFlvMuxer mSrsFlvMuxer = new SrsFlvMuxer(new 	RtmpPublisher.EventHandler() {
        @Override
        public void onRtmpConnecting(String msg) {
            Log.e(LOG_TAG, msg);
        }

        @Override
        public void onRtmpConnected(String msg) {
            Log.e(LOG_TAG, msg);
        }

        @Override
        public void onRtmpVideoStreaming(String msg) {
        }

        @Override
        public void onRtmpAudioStreaming(String msg) {
        }

        @Override
        public void onRtmpStopped(String msg) {
            Log.e(LOG_TAG, msg);
        }

        @Override
        public void onRtmpDisconnected(String msg) {
            Log.e(LOG_TAG, msg);
        }

        @Override
        public void onRtmpOutputFps(final double fps) {
            Log.i(LOG_TAG, String.format("Output Fps: %f", fps));
        }
    });
```

初始化推流器

```
	mSrsFlvMuxer.start(pushUrl);
```
开始推流

**参数**

* pushUrl：	推流地址

```
	videoPreProcessing.getYuvData(localVideoBuffer, localWidth, localHeight, localRotation, peerUid == -1 ? true : false);
	videoPreProcessing.getPcmData();
```
获取合图后 YUV 数据和 PCM 数据





