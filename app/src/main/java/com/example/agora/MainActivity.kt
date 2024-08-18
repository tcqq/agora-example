package com.example.agora

import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.example.agora.databinding.ActivityMainBinding

/**
 * @author Perry Lance
 * @since 2024-08-18 Created
 */
class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        setActionBar(binding.toolbar)

        binding.agoraAudio.setOnClickListener {
            showToast("agoraAudio")
            startActivity(Intent(this, AgoraAudioActivity::class.java))
        }

        binding.agoraVideo.setOnClickListener {
            showToast("agoraVideo")
        }
    }
}