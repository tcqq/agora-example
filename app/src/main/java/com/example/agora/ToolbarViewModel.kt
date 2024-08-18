package com.example.agora

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * @author Perry Lance
 * @since 2021-06-04 Created
 */
class ToolbarViewModel : ViewModel() {
    val menuResId = MutableLiveData<Int>()
}
