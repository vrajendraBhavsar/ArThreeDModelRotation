package com.example.arthreedmodelrotation

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class MainViewModel: ViewModel() {
    var changePositionX: MutableState<Float> = mutableStateOf(0f)
        private set

    var isLoadModelFromAssets: MutableState<Boolean> = mutableStateOf(false)
        private set
}