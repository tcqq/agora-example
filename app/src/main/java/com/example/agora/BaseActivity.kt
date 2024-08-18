package com.example.agora

import android.Manifest
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.MotionEvent
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.MenuRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import com.google.android.material.snackbar.Snackbar
import com.permissionx.guolindev.PermissionX
import timber.log.Timber
import kotlin.math.abs

/**
 * @author Perry Lance
 * @since 2016-03-12 Created
 */
abstract class BaseActivity : AppCompatActivity() {

    private val toolbarViewModel by viewModels<ToolbarViewModel>()

    private var actionDownX = 0f
    private var actionDownY = 0f


    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        super.onCreate(savedInstanceState)
        observeChange()
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Click on the blank space to close the soft keyboard and make the EditText lose focus.
     */
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (enableBlankTouch()) {
            val x = event.x
            val y = event.y
            if (event.action == MotionEvent.ACTION_DOWN) {
                actionDownX = x
                actionDownY = y
            } else if (event.action == MotionEvent.ACTION_UP && !(abs(x - actionDownX) > 50 || abs(y - actionDownY) > 50)) {
                val v = currentFocus
                if (v is EditText) {
                    val outRect = Rect()
                    v.getGlobalVisibleRect(outRect)
                    if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                        Timber.v("Hide keyboard")
                        KeyboardUtils.hideKeyboard(this)
                        v.isFocusable = false
                        v.isFocusableInTouchMode = true
                    }
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    fun nightMode(): Boolean {
        return resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

    fun setActionBar(toolbar: Toolbar) {
        setSupportActionBar(toolbar)
    }

    fun setActionBarTitle(@StringRes resId: Int) {
        supportActionBar?.setTitle(resId)
    }

    fun setActionBarTitle(title: CharSequence) {
        supportActionBar?.title = title
    }

    fun setActionBarSubtitle(@StringRes resId: Int) {
        supportActionBar?.setSubtitle(resId)
    }

    fun setActionBarSubtitle(subtitle: CharSequence) {
        supportActionBar?.subtitle = subtitle
    }

    fun setMenuFromResource(@MenuRes menuResId: Int) {
        toolbarViewModel.menuResId.value = menuResId
    }

    fun clearMenu() = setMenuFromResource(EMPTY_MENU_RES_ID)

    fun showToast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    fun showSnackBar(text: String, duration: Int = Snackbar.LENGTH_SHORT) {
        Snackbar.make(findViewById(android.R.id.content), text, duration).show()
    }

    fun backToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }

    open fun observeChange() {}


    open fun enableBlankTouch() = false

    // 使用 PermissionX 请求语音通话所需的权限
    fun requestAudioPermissions(onAllPermissionsGranted: () -> Unit) {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            permissions.add(Manifest.permission.FOREGROUND_SERVICE_MICROPHONE)
        }

        PermissionX.init(this)
            .permissions(permissions)
            .explainReasonBeforeRequest()
            .onExplainRequestReason { scope, deniedList ->
                scope.showRequestReasonDialog(
                    deniedList,
                    getString(R.string.permission_reason),
                    getString(R.string.confirm),
                    getString(R.string.cancel)
                )
            }
            .onForwardToSettings { scope, deniedList ->
                scope.showForwardToSettingsDialog(
                    deniedList,
                    getString(R.string.permission_settings),
                    getString(R.string.confirm),
                    getString(R.string.cancel)
                )
            }
            .request { allGranted, _, deniedList ->
                if (allGranted) {
                    onAllPermissionsGranted()
                    Timber.d("All permissions granted")
                } else {
                    Timber.d("Permissions denied: $deniedList")
                }
            }
    }

    // 请求视频通话所需的权限
    fun requestVideoPermissions(onAllPermissionsGranted: () -> Unit) {
        val videoPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }

        val notificationPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyArray()
        }

        val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            emptyArray()
        }

        // 合并所有需要的权限
        val allPermissions = videoPermissions + notificationPermissions + bluetoothPermissions

        PermissionX.init(this)
            .permissions(*allPermissions)
            .explainReasonBeforeRequest()
            .onExplainRequestReason { scope, deniedList ->
                scope.showRequestReasonDialog(
                    deniedList,
                    getString(R.string.permission_reason),
                    getString(R.string.confirm),
                    getString(R.string.cancel)
                )
            }
            .onForwardToSettings { scope, deniedList ->
                scope.showForwardToSettingsDialog(
                    deniedList,
                    getString(R.string.permission_settings),
                    getString(R.string.confirm),
                    getString(R.string.cancel)
                )
            }
            .request { allGranted, grantedList, deniedList ->
                if (allGranted) {
                    onAllPermissionsGranted()
                    Timber.d("所有权限已授予")
                } else {
                    Timber.d("以下权限被拒绝: $deniedList")
                }
            }
    }



    companion object {
        const val EMPTY_MENU_RES_ID = -1
    }
}