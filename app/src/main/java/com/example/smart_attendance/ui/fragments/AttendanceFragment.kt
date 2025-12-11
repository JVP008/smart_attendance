package com.example.smart_attendance.ui.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.ImageCaptureException
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.smart_attendance.data.Student
import com.example.smart_attendance.databinding.FragmentAttendanceBinding
import com.example.smart_attendance.ui.viewmodels.AttendanceViewModel
import com.example.smart_attendance.ui.viewmodels.StudentViewModel
import com.example.smart_attendance.utils.FaceRecognitionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class AttendanceFragment : Fragment() {
    private var _binding: FragmentAttendanceBinding? = null
    private val binding get() = _binding!!
    private lateinit var studentViewModel: StudentViewModel
    private lateinit var attendanceViewModel: AttendanceViewModel
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null

    private var capturedFaceBitmap: Bitmap? = null
    private var currentTargetStudent: Student? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAttendanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        studentViewModel = ViewModelProvider(this)[StudentViewModel::class.java]
        attendanceViewModel = ViewModelProvider(this)[AttendanceViewModel::class.java]
        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.ivCapturedFace.visibility = View.GONE
        binding.btnRecognizeFace.visibility = View.GONE

        if (allPermissionsGranted()) {
            startCameraPreview()
        } else {
            ActivityCompat.requestPermissions(requireActivity(), REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        binding.btnCaptureAttendance.setOnClickListener {
            handleCaptureAttendance()
        }

        binding.btnRecognizeFace.setOnClickListener {
            handleRecognizeFace()
        }
    }

    private fun handleCaptureAttendance() {
        binding.ivCapturedFace.visibility = View.GONE
        binding.btnRecognizeFace.visibility = View.GONE
        capturedFaceBitmap?.recycle()
        capturedFaceBitmap = null
        currentTargetStudent = null

        val enrollmentNumber = binding.etEnrollmentForAttendance.text.toString().trim()
        if (enrollmentNumber.isNotEmpty()) {
            lifecycleScope.launch(Dispatchers.IO) {
                val student = studentViewModel.getStudentByEnrollment(enrollmentNumber)
                withContext(Dispatchers.Main) {
                    if (student != null) {
                        Toast.makeText(context, "Student found. Capturing face...", Toast.LENGTH_SHORT).show()
                        currentTargetStudent = student
                        captureFaceForDisplayAndComparison()
                    } else {
                        Toast.makeText(context, "Student with Enrollment $enrollmentNumber not found", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            Toast.makeText(context, "Please enter an Enrollment Number", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleRecognizeFace() {
        val student = currentTargetStudent
        val faceBitmap = capturedFaceBitmap

        if (student != null && faceBitmap != null) {
            performFaceRecognition(student, faceBitmap)
        } else {
            context?.let {
                Toast.makeText(it, "No face captured or student selected.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startCameraPreview() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(viewLifecycleOwner, cameraSelector, preview, imageCapture!!)
            } catch (exc: Exception) {
                Log.e("AttendanceFragment", "Camera initialization failed: ${exc.message}", exc)
                context?.let {
                    Toast.makeText(it, "Camera initialization failed: ${exc.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun captureFaceForDisplayAndComparison() {
        val currentImageCapture = imageCapture ?: return

        currentImageCapture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    val fullBitmap = imageProxyToBitmap(imageProxy)
                    imageProxy.close()

                    lifecycleScope.launch(Dispatchers.IO) {
                        val detectedFace = FaceRecognitionManager.detectFace(requireContext(), fullBitmap)
                        fullBitmap.recycle()

                        withContext(Dispatchers.Main) {
                            if (detectedFace != null) {
                                capturedFaceBitmap?.recycle()
                                capturedFaceBitmap = detectedFace
                                binding.ivCapturedFace.setImageBitmap(capturedFaceBitmap)
                                binding.ivCapturedFace.visibility = View.VISIBLE
                                binding.btnRecognizeFace.visibility = View.VISIBLE
                                Toast.makeText(context, "Face captured. Click Recognize Face.", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "No face detected. Try again.", Toast.LENGTH_LONG).show()
                                binding.ivCapturedFace.visibility = View.GONE
                                binding.btnRecognizeFace.visibility = View.GONE
                            }
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("AttendanceFragment", "Image capture failed: ${exception.message}", exception)
                    requireActivity().runOnUiThread {
                        Toast.makeText(context, "Image capture failed: ${exception.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    private fun performFaceRecognition(student: Student, faceBitmap: Bitmap) {
        lifecycleScope.launch(Dispatchers.IO) {
            val isMatch = FaceRecognitionManager.recognize(
                requireContext(),
                faceBitmap,
                student.faceEmbedding,
                student.faceImageWidth,
                student.faceImageHeight
            )

            withContext(Dispatchers.Main) {
                if (isMatch) {
                    markAttendance(student)
                } else {
                    Toast.makeText(context, "Face verification failed. Please try again.", Toast.LENGTH_LONG).show()
                }
                // Reset UI
                binding.ivCapturedFace.visibility = View.GONE
                binding.btnRecognizeFace.visibility = View.GONE
                capturedFaceBitmap?.recycle()
                capturedFaceBitmap = null
                currentTargetStudent = null
            }
        }
    }

    private fun markAttendance(student: Student) {
        lifecycleScope.launch(Dispatchers.IO) {
            val today = System.currentTimeMillis()
            val existingAttendance = attendanceViewModel.getAttendanceForStudentAndDate(student.enrollmentNumber, today)

            if (existingAttendance != null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Attendance already marked for ${student.name} today", Toast.LENGTH_LONG).show()
                }
            } else {
                attendanceViewModel.markAttendanceAsPresent(
                    student.enrollmentNumber,
                    today,
                    student.name,
                    student.faceImageWidth,
                    student.faceImageHeight
                )
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Attendance marked for ${student.name}", Toast.LENGTH_LONG).show()
                    binding.etEnrollmentForAttendance.text?.clear()
                }
            }
        }
    }

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees.toFloat()
        val matrix = android.graphics.Matrix().apply { postRotate(rotationDegrees) }

        return when (imageProxy.format) {
            android.graphics.ImageFormat.JPEG -> {
                val buffer = imageProxy.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            }
            android.graphics.ImageFormat.YUV_420_888 -> {
                val yBuffer = imageProxy.planes[0].buffer
                val uBuffer = imageProxy.planes[1].buffer
                val vBuffer = imageProxy.planes[2].buffer

                val ySize = yBuffer.remaining()
                val uSize = uBuffer.remaining()
                val vSize = vBuffer.remaining()

                val nv21 = ByteArray(ySize + uSize + vSize)
                yBuffer.get(nv21, 0, ySize)
                vBuffer.get(nv21, ySize, vSize)
                uBuffer.get(nv21, ySize + vSize, uSize)

                val yuvImage = android.graphics.YuvImage(nv21, android.graphics.ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
                val out = java.io.ByteArrayOutputStream()
                yuvImage.compressToJpeg(android.graphics.Rect(0, 0, yuvImage.width, yuvImage.height), 75, out)
                val imageBytes = out.toByteArray()
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            }
            else -> throw IllegalArgumentException("Unsupported image format: ${imageProxy.format}")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        cameraExecutor.shutdown()
        ProcessCameraProvider.getInstance(requireContext()).get().unbindAll()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCameraPreview()
            } else {
                Toast.makeText(context, "Permissions not granted.", Toast.LENGTH_SHORT).show()
                requireActivity().finish()
            }
        }
    }

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val REQUEST_CODE_PERMISSIONS = 10
    }
}
