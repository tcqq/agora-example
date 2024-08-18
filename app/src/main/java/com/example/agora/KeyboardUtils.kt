package com.example.agora

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.Rect
import android.view.View
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import timber.log.Timber
import kotlin.math.abs

/**
 * @author Perry Lance
 * @since 2018/06/02 Created
 */
object KeyboardUtils {

    fun showKeyboard(activity: Activity) {
        val imm = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        var view = activity.currentFocus
        if (view == null) {
            view = View(activity)
            view.isFocusable = true
            view.isFocusableInTouchMode = true
            view.requestFocus()
        }
        imm.showSoftInput(view, InputMethodManager.SHOW_FORCED)
    }

    fun showKeyboard(context: Context, view: View) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        view.isFocusable = true
        view.isFocusableInTouchMode = true
        view.requestFocus()
        imm.showSoftInput(view, InputMethodManager.SHOW_FORCED)
    }

    fun hideKeyboard(activity: Activity) {
        val imm = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        var view = activity.currentFocus
        if (view == null) view = View(activity)
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private var sDecorViewDelta = 0

    fun isSoftInputVisible(activity: Activity): Boolean {
        return getDecorViewInvisibleHeight(activity.window) > 0
    }

    private fun getDecorViewInvisibleHeight(window: Window): Int {
        val decorView = window.decorView
        val outRect = Rect()
        decorView.getWindowVisibleDisplayFrame(outRect)
        val delta = abs(decorView.bottom - outRect.bottom)
        if (delta <= getNavBarHeight()) {
            sDecorViewDelta = delta
            return 0
        }
        return delta - sDecorViewDelta
    }

    private fun getNavBarHeight(): Int {
        val res = Resources.getSystem()
        val resourceId = res.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId != 0) {
            res.getDimensionPixelSize(resourceId)
        } else {
            0
        }
    }

    /**
     * Fix the bug of 5497 in Android.
     *
     * Don't set adjustResize
     */
    fun fixAndroidBug5497(activity: Activity) {
        val window = activity.window
        val contentView = window.findViewById<FrameLayout>(android.R.id.content)
        val contentViewChild = contentView.getChildAt(0)
        val paddingBottom = contentViewChild.paddingBottom
        val contentViewInvisibleHeightPre5497 = intArrayOf(getContentViewInvisibleHeight(window))
        contentView.viewTreeObserver
            .addOnGlobalLayoutListener {
                val height = getContentViewInvisibleHeight(window)
                if (contentViewInvisibleHeightPre5497[0] != height) {
                    contentViewChild.setPadding(
                        contentViewChild.paddingLeft,
                        contentViewChild.paddingTop,
                        contentViewChild.paddingRight,
                        paddingBottom + getDecorViewInvisibleHeight(window)
                    )
                    contentViewInvisibleHeightPre5497[0] = height
                }
            }
    }

    private fun getContentViewInvisibleHeight(window: Window): Int {
        val contentView = window.findViewById<View>(android.R.id.content) ?: return 0
        val outRect = Rect()
        contentView.getWindowVisibleDisplayFrame(outRect)
        Timber.d("getContentViewInvisibleHeight: ${contentView.bottom - outRect.bottom}")
        val delta = abs(contentView.bottom - outRect.bottom)
        return if (delta <= getStatusBarHeight() + getNavBarHeight()) {
            0
        } else delta
    }

    private fun getStatusBarHeight(): Int {
        val resources = Resources.getSystem()
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return resources.getDimensionPixelSize(resourceId)
    }
}
