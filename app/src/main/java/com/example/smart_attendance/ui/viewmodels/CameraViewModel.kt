package com.example.smart_attendance.ui.viewmodels

import android.content.Context
import android.util.Log
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner

class CameraViewModel : ViewModel() {

    private val _cameraInitialized = MutableLiveData<Boolean>()
    val cameraInitialized: LiveData<Boolean> get() = _cameraInitialized

    fun initializeCamera(context: Context, previewView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build()

            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(context as LifecycleOwner, cameraSelector, preview)
                _cameraInitialized.postValue(true)
            } catch (e: Exception) {
                Log.e("CameraViewModel", "Use case binding failed", e)
                _cameraInitialized.postValue(false)
            }
        }, ContextCompat.getMainExecutor(context))
    }
}
