package com.example.agora

import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.example.agora.databinding.ActivityAgoraVideoBinding
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas
import kotlin.math.abs

/**
 * @author Perry Lance
 * @since 2024-08-19 Created
 */
class AgoraVideoActivity : BaseActivity() {

    private lateinit var binding: ActivityAgoraVideoBinding
    private var engine: RtcEngine? = null
    private var myUid = 0
    private var remoteUid = 0
    private var joined = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_agora_video)

        binding.btnJoin.setOnClickListener {
            if (!joined) {
                val channelId = binding.etChannel.text.toString()
                joinChannel(channelId)
            } else {
                leaveChannel()
            }
        }

        initializeAgoraEngine()
    }

    private fun initializeAgoraEngine() {
        try {
            val config = RtcEngineConfig().apply {
                mContext = applicationContext
                mAppId = getString(R.string.agora_app_id)
                mChannelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING
                mEventHandler = iRtcEngineEventHandler
            }
            engine = RtcEngine.create(config)
            // 启用视频功能
            engine?.enableVideo()
        } catch (e: Exception) {
            e.printStackTrace()
            finish()
        }
    }



    private fun joinChannel(channelId: String) {
        val surfaceView = SurfaceView(this).apply { setZOrderMediaOverlay(true) }
        binding.flLocal.removeAllViews()
        binding.flLocal.addView(surfaceView)

        engine?.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0))
        engine?.setClientRole(Constants.CLIENT_ROLE_BROADCASTER)

        TokenUtils.gen(this, channelId, 0) { token ->
            val options = ChannelMediaOptions().apply {
                autoSubscribeAudio = true
                autoSubscribeVideo = true
                publishCameraTrack = true
                publishMicrophoneTrack = true
            }
            val res = engine?.joinChannel(token, channelId, 0, options)
            if (res != 0) {
                showError(RtcEngine.getErrorDescription(abs(res!!)))
            } else {
                joined = true
                binding.btnJoin.text = "离开"
                engine?.startPreview()  // 启动本地预览
            }
        }
    }


    private fun leaveChannel() {
        engine?.leaveChannel()
        engine?.stopPreview()
        remoteUid = 0
        joined = false
        binding.btnJoin.text = "加入"
    }

    private val iRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            myUid = uid
            Log.d("Agora", "Joined Channel $channel with UID $uid")
            runOnUiThread {
                Toast.makeText(this@AgoraVideoActivity, "Joined Channel $channel with UID $uid", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            remoteUid = uid
            Log.d("Agora", "Remote User Joined with UID $uid")

            runOnUiThread {
                val surfaceView = SurfaceView(this@AgoraVideoActivity)
                surfaceView.setZOrderMediaOverlay(true)  // 确保在其他视图上层显示
                binding.flRemote.removeAllViews() // 清除之前的视图
                binding.flRemote.addView(surfaceView) // 添加远程视频的SurfaceView

                // 设置远程视频画布
                engine?.setupRemoteVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid))
            }
        }


        override fun onUserOffline(uid: Int, reason: Int) {
            if (uid == remoteUid) {
                runOnUiThread {
                    binding.flRemote.removeAllViews() // 移除远程用户的视图
                    Toast.makeText(this@AgoraVideoActivity, "Remote user offline", Toast.LENGTH_SHORT).show()
                }
                remoteUid = 0
                Log.d("Agora", "Remote User Offline with UID $uid")
            }
        }

        override fun onLeaveChannel(stats: RtcStats?) {
            myUid = 0
            Log.d("Agora", "Left Channel")
        }
    }


    private fun showError(message: String) {
        Log.e("Agora", "Error: $message")
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (joined) {
            engine?.leaveChannel()
            engine?.stopPreview()
        }
        RtcEngine.destroy()
        engine = null
    }
}