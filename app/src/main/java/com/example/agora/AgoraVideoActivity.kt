package com.example.agora

import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.example.agora.databinding.ActivityAgoraVideo1Binding
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

    private lateinit var binding: ActivityAgoraVideo1Binding
    private var engine: RtcEngine? = null
    private var myUid = 0
    private var remoteUid = 0
    private var joined = false
    private var isMuted = false // 麦克风静音状态
    private var isSpeakerOn = false // 扬声器状态
    private val userIds = mutableListOf<Int>() // 存储当前频道的用户ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_agora_video1)
        setActionBar(binding.toolbar)
        setActionBarTitle("Agora Video")

        // 加入频道
        binding.joinButton.setOnClickListener {
            if (!joined) {
                val channelId = binding.channelName.text.toString()
                joinChannel(channelId)
            } else {
                leaveChannel()
            }
        }

        // 切换麦克风静音
        binding.muteButton.setOnClickListener {
            isMuted = !isMuted
            engine?.muteLocalAudioStream(isMuted)
            binding.muteButton.text = if (isMuted) getString(R.string.unmute) else getString(R.string.mute)
        }

        // 切换扬声器
        binding.speakerButton.setOnClickListener {
            isSpeakerOn = !isSpeakerOn
            engine?.setEnableSpeakerphone(isSpeakerOn)
            binding.speakerButton.text = if (isSpeakerOn) getString(R.string.earpiece) else getString(R.string.speaker)
        }

        // 切换摄像头
        binding.switchCameraButton.setOnClickListener {
            engine?.switchCamera()
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
            engine?.enableVideo()  // 启用视频功能
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
                binding.joinButton.text = getString(R.string.leave_channel)
                engine?.startPreview()  // 启动本地预览
            }
        }
    }

    private fun leaveChannel() {
        engine?.leaveChannel()
        engine?.stopPreview()
        userIds.clear() // 清除所有用户ID
        updateUserList() // 更新用户列表显示
        remoteUid = 0
        joined = false
        binding.joinButton.text = getString(R.string.join_channel)
    }

    private fun updateUserList() {
        val userListText = userIds.joinToString(separator = "\n") { "User ID: $it" }
        binding.userList.text = if (userListText.isEmpty()) "" else userListText
        binding.userList.visibility = if (userIds.isEmpty()) View.GONE else View.VISIBLE
    }

    private val iRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            myUid = uid
            Log.d("Agora", "Joined Channel $channel with UID $uid")
            runOnUiThread {
                userIds.add(uid)
                updateUserList()
                Toast.makeText(this@AgoraVideoActivity, getString(R.string.join_channel) + " $channel", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            remoteUid = uid
            Log.d("Agora", "Remote User Joined with UID $uid")

            runOnUiThread {
                userIds.add(uid)  // 添加新用户到列表
                updateUserList()

                val surfaceView = SurfaceView(this@AgoraVideoActivity)
                surfaceView.setZOrderMediaOverlay(true)
                binding.flRemote.removeAllViews()
                binding.flRemote.addView(surfaceView)

                // 设置远程视频画布
                engine?.setupRemoteVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid))
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            if (uid == remoteUid) {
                runOnUiThread {
                    binding.flRemote.removeAllViews()
                    userIds.remove(uid)  // 移除用户
                    updateUserList()

                    Toast.makeText(this@AgoraVideoActivity, getString(R.string.leave_channel), Toast.LENGTH_SHORT).show()
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