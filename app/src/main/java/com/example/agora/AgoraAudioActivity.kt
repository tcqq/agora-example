package com.example.agora

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.core.view.isGone
import androidx.databinding.DataBindingUtil
import com.example.agora.databinding.ActivityAgoraAudioBinding
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.Constants.USER_OFFLINE_DROPPED
import io.agora.rtc2.Constants.USER_OFFLINE_QUIT
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import timber.log.Timber

/**
 * @author Perry Lance
 * @since 2024-08-18 Created
 */
class AgoraAudioActivity : BaseActivity() {

    private lateinit var binding: ActivityAgoraAudioBinding
    private lateinit var rtcEngine: RtcEngine
    private var isJoined = false
    private var isMuted = false // 麦克风静音状态
    private var isSpeakerOn = false // 扬声器状态
    private val userIds = mutableListOf<Int>() // 存储当前频道的用户ID

    // Agora Event Handler
    private val eventHandler = object : IRtcEngineEventHandler() {
        // 检测本地用户加入频道
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            runOnUiThread {
                isJoined = true
                addUser(uid) // 本地用户加入频道
                showToast("本地用户加入频道成功: $channel, UID: $uid")
                Timber.d("Joined channel: $channel with UID: $uid")
            }
        }

        // 检测远程用户加入频道
        override fun onUserJoined(uid: Int, elapsed: Int) {
            runOnUiThread {
                addUser(uid) // 远程用户加入频道
                showToast("远程用户加入: UID $uid")
                Timber.d("User $uid joined the channel")
            }
        }

        // 检测远程用户离开频道
        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread {
                removeUser(uid) // 远程用户离开频道
                val reasonStr = when (reason) {
                    USER_OFFLINE_QUIT -> "正常退出"
                    USER_OFFLINE_DROPPED -> "连接丢失"
                    else -> "未知原因"
                }
                showToast("远程用户离开: UID $uid, 原因: $reasonStr")
                Timber.d("User $uid left the channel, reason: $reasonStr")
            }
        }

        // 检测本地用户离开频道
        override fun onLeaveChannel(stats: RtcStats?) {
            runOnUiThread {
                isJoined = false
                userIds.clear() // 清空所有用户ID
                updateUserList() // 更新用户列表
                showToast("本地用户已离开频道")
                Timber.d("Left the channel")
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_agora_audio)
        setActionBar(binding.toolbar)
        setActionBarTitle("Agora Audio")

        // 请求权限并初始化
        requestAudioPermissions {
            initAgoraEngine()
        }

        // 点击加入频道按钮时执行
        binding.joinButton.setOnClickListener {
            val channelName = binding.channelName.text.toString()
            if (channelName.isNotEmpty()) {
                showToast(getString(R.string.join_channel) + ": $channelName")
                joinChannel(channelName)
            } else {
                showToast(getString(R.string.channel_name_hint))
            }
        }

        // 点击离开频道按钮时执行
        binding.leaveButton.setOnClickListener {
            showToast(getString(R.string.leave_channel))
            leaveChannel()
        }

        // 点击麦克风静音/取消静音按钮时执行
        binding.muteButton.setOnClickListener {
            isMuted = !isMuted
            rtcEngine.muteLocalAudioStream(isMuted)
            binding.muteButton.text = if (isMuted) getString(R.string.unmute) else getString(R.string.mute)
        }

        // 点击扬声器/听筒切换按钮时执行
        binding.speakerButton.setOnClickListener {
            isSpeakerOn = !isSpeakerOn
            rtcEngine.setEnableSpeakerphone(isSpeakerOn)
            binding.speakerButton.text = if (isSpeakerOn) getString(R.string.earpiece) else getString(R.string.speaker)
        }
    }

    private fun initAgoraEngine() {
        val config = RtcEngineConfig().apply {
            mContext = applicationContext
            mAppId = getString(R.string.agora_app_id);
            mEventHandler = eventHandler
        }

        try {
            rtcEngine = RtcEngine.create(config)
            Timber.d("Agora engine initialized")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun joinChannel(channelName: String) {
        val options = ChannelMediaOptions().apply {
            autoSubscribeAudio = true
        }
        // 生成Token并加入频道
        TokenUtils.gen(this, channelName, 0) { token ->
            if (token != null) {
                rtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
                rtcEngine.joinChannel(token, channelName, 0, options)
                startRecordingService()  // 加入频道后启动录音服务
                Timber.d("Joining channel with token")
            } else {
                Timber.e("Token generation failed")
            }
        }
    }

    private fun leaveChannel() {
        if (isJoined) {
            rtcEngine.leaveChannel()
            isJoined = false
            stopRecordingService()  // 离开频道后停止录音服务
            Timber.d("Left the channel")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        RtcEngine.destroy()
        stopRecordingService()
    }

    private fun startRecordingService() {
        val intent = Intent(this, LocalRecordingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopRecordingService() {
        val intent = Intent(this, LocalRecordingService::class.java)
        stopService(intent)
    }

    // 添加用户并更新显示
    private fun addUser(uid: Int) {
        if (!userIds.contains(uid)) {
            userIds.add(uid)
            updateUserList()
        }
    }

    // 移除用户并更新显示
    private fun removeUser(uid: Int) {
        if (userIds.contains(uid)) {
            userIds.remove(uid)
            updateUserList()
        }
    }

    // 更新用户列表显示
    private fun updateUserList() {
        val userListText = userIds.joinToString(separator = "\n") { "User ID: $it" }
        binding.userList.text = userListText
        binding.userList.isGone = userIds.isEmpty()
    }
}