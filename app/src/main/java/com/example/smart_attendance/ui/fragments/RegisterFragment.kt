package com.example.smart_attendance.ui.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.smart_attendance.R
import com.example.smart_attendance.data.Attendance
import com.example.smart_attendance.data.AttendanceRecordDisplay
import com.example.smart_attendance.data.Student
import com.example.smart_attendance.databinding.FragmentRegisterBinding
import com.example.smart_attendance.ui.viewmodels.AttendanceHistoryViewModel
import com.example.smart_attendance.ui.viewmodels.AttendanceViewModel
import com.example.smart_attendance.ui.viewmodels.StudentViewModel
import com.example.smart_attendance.utils.FaceRecognitionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.content.ContentValues
import android.os.Build

class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: StudentViewModel
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var detectedFaceBitmap: Bitmap? = null
    private lateinit var attendanceHistoryViewModel: AttendanceHistoryViewModel
    private lateinit var attendanceViewModel: AttendanceViewModel

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val bitmap = try {
                requireActivity().contentResolver.openInputStream(it)?.use {
                    BitmapFactory.decodeStream(it)
                }
            } catch (e: Exception) {
                Log.e("RegisterFragment", "Error loading image from gallery: ${e.message}")
                null
            }
            bitmap?.let { showImageAndDetectFace(it) } ?: run {
                Toast.makeText(context, "Failed to load image from gallery.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[StudentViewModel::class.java]
        attendanceHistoryViewModel = ViewModelProvider(requireActivity())[AttendanceHistoryViewModel::class.java]
        attendanceViewModel = ViewModelProvider(this)[AttendanceViewModel::class.java]
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(requireActivity(), REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        val subjectTypes = resources.getStringArray(R.array.subject_types)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, subjectTypes)
        binding.actvSubjectType.setAdapter(adapter)

        binding.btnCamera.setOnClickListener { takePhoto() }
        binding.btnGallery.setOnClickListener { galleryLauncher.launch("image/*") }
        binding.btnSave.setOnClickListener { saveStudent() }
        binding.btnExportClear.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                val attendanceList = attendanceHistoryViewModel.getAllAttendanceList()
                if (attendanceList.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "No attendance records to export.", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                val displayList = attendanceList.map { attendance ->
                    val student = attendanceHistoryViewModel.getStudentSummaryByEnrollmentNumber(attendance.studentEnrollmentNumber)
                    AttendanceRecordDisplay(
                        attendance.id,
                        student?.name ?: "Unknown Student",
                        student?.enrollmentNumber ?: "N/A",
                        attendance.timestamp,
                        student?.subjectName ?: "N/A",
                        student?.attendanceType ?: "N/A",
                        attendance.status
                    )
                }
                try {
                    val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    val fileName = "attendance_${sdf.format(Date())}.csv"

                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/")
                        }
                    }

                    val resolver = requireContext().contentResolver
                    val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    } else {
                        null
                    }

                    val dateFormat = SimpleDateFormat("MM-dd", Locale.getDefault())
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { outputStream ->
                            val writer = outputStream.bufferedWriter()
                            writer.append("Enrollment,Name,Date,Time,Subject Name,Subject Type,Status\n")
                            for (record in displayList) {
                                val formattedDate = dateFormat.format(Date(record.timestamp))
                                val formattedTime = timeFormat.format(Date(record.timestamp))
                                writer.append("${record.studentEnrollmentNumber},${record.studentName},${formattedDate},${formattedTime},${record.subjectName ?: "N/A"},${record.subjectType ?: "N/A"},${record.status}\n")
                            }
                            writer.flush()
                        }
                        attendanceHistoryViewModel.clearAllAttendance()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Attendance exported to Downloads/${fileName} and cleared!", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Failed to create file in Downloads. (API Level Issue?)", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        context?.let { Toast.makeText(it, "Failed to export: ${e.message}", Toast.LENGTH_LONG).show() }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isHidden && allPermissionsGranted()) {
            startCamera()
        }
    }

    override fun onPause() {
        super.onPause()
        stopCamera()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) {
            stopCamera()
        } else if (allPermissionsGranted()) {
            startCamera()
        }
    }

    private fun stopCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            cameraProviderFuture.get().unbindAll()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            if (!cameraProvider.hasCamera(cameraSelector)) {
                Log.e("RegisterFragment", "Selected camera (front camera) is not available.")
                context?.let { Toast.makeText(it, "Error: Front camera not found.", Toast.LENGTH_LONG).show() }
                return@addListener
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(viewLifecycleOwner, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e("RegisterFragment", "Camera initialization failed", exc)
                context?.let { Toast.makeText(it, "Camera initialization failed: ${exc.message}", Toast.LENGTH_LONG).show() }
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        binding.previewView.visibility = View.VISIBLE
        binding.ivFace.visibility = View.GONE
        val imageCapture = imageCapture ?: return
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: androidx.camera.core.ImageProxy) {
                    val bitmap = imageProxyToBitmap(imageProxy)
                    imageProxy.close()
                    showImageAndDetectFace(bitmap)
                }
                override fun onError(exception: ImageCaptureException) {
                    context?.let { Toast.makeText(it, "Photo capture failed: ${exception.message}", Toast.LENGTH_SHORT).show() }
                }
            }
        )
    }

    private fun imageProxyToBitmap(imageProxy: androidx.camera.core.ImageProxy): Bitmap {
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees.toFloat()
        val matrix = android.graphics.Matrix().apply { postRotate(rotationDegrees) }
        
        val buffer = imageProxy.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun showImageAndDetectFace(bitmap: Bitmap) {
        Log.d("RegisterFragment", "Attempting to detect face in bitmap: ${bitmap.width}x${bitmap.height}")
        binding.previewView.visibility = View.GONE
        binding.ivFace.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            val face = FaceRecognitionManager.detectFace(requireContext(), bitmap)
            withContext(Dispatchers.Main) {
                if (face != null) {
                    binding.ivFace.setImageBitmap(face)
                    detectedFaceBitmap?.recycle()
                    detectedFaceBitmap = face
                    Log.d("RegisterFragment", "Face detected! Setting detectedFaceBitmap and image view.")
                    context?.let { Toast.makeText(it, "Face detected!", Toast.LENGTH_SHORT).show() }
                } else {
                    binding.ivFace.setImageBitmap(bitmap)
                    detectedFaceBitmap?.recycle()
                    detectedFaceBitmap = null
                    Log.d("RegisterFragment", "No face detected. Setting full bitmap to image view.")
                    context?.let { Toast.makeText(it, "No face detected. Please try another image.", Toast.LENGTH_SHORT).show() }
                }
            }
        }
    }

    private fun saveBitmapToFile(bitmap: Bitmap): String? {
        val fileName = "face_${System.currentTimeMillis()}.jpeg"
        val file = File(requireContext().filesDir, fileName)
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            Log.d("RegisterFragment", "Image saved to: ${file.absolutePath}")
            return file.absolutePath
        } catch (e: IOException) {
            Log.e("RegisterFragment", "Error saving bitmap to file: ${e.message}")
            return null
        }
    }

    private fun saveStudent() {
        val name = binding.etName.text.toString().trim()
        val rollNo = binding.etRoll.text.toString().trim()
        val enrollmentNumber = binding.etEnrollmentNumber.text.toString().trim()
        val branch = binding.etBranch.text.toString().trim()
        val semester = binding.etSemester.text.toString().trim()
        val subjectName = binding.etSubjectName.text.toString().trim()
        val attendanceType = binding.actvSubjectType.text.toString().trim()

        if (detectedFaceBitmap == null) {
            context?.let { Toast.makeText(it, "Please capture or select a face image first.", Toast.LENGTH_SHORT).show() }
            return
        }

        if (name.isEmpty() || rollNo.isEmpty() || enrollmentNumber.isEmpty() || branch.isEmpty() || semester.isEmpty() || subjectName.isEmpty() || attendanceType.isEmpty()) {
            context?.let { Toast.makeText(it, "Please fill all fields.", Toast.LENGTH_SHORT).show() }
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val faceImagePath = saveBitmapToFile(detectedFaceBitmap!!) // Save detected face bitmap
            if (faceImagePath == null) {
                withContext(Dispatchers.Main) {
                    context?.let { Toast.makeText(it, "Failed to save face image.", Toast.LENGTH_SHORT).show() }
                }
                return@launch
            }

            val faceEmbedding = FaceRecognitionManager.getFaceEmbedding(requireContext(), detectedFaceBitmap!!) // Get face embedding

            if (faceEmbedding == null) {
                withContext(Dispatchers.Main) {
                    context?.let { Toast.makeText(it, "Failed to generate face embedding.", Toast.LENGTH_SHORT).show() }
                }
                return@launch
            }

            val student = Student(
                name = name, rollNo = rollNo, enrollmentNumber = enrollmentNumber, branch = branch, semester = semester, faceImagePath = faceImagePath, faceEmbedding = faceEmbedding, faceImageWidth = detectedFaceBitmap!!.width, faceImageHeight = detectedFaceBitmap!!.height, subjectName = subjectName, attendanceType = attendanceType
            )

            (viewModel as StudentViewModel).insert(student)

            val currentDayTimestamp = getStartOfDayTimestamp(System.currentTimeMillis())
            val initialAttendance = Attendance(
                studentEnrollmentNumber = enrollmentNumber,
                timestamp = currentDayTimestamp,
                status = "Not Present"
            )
            attendanceViewModel.markAttendance(initialAttendance)

            withContext(Dispatchers.Main) {
                context?.let { Toast.makeText(it, "Student and initial attendance registered successfully!", Toast.LENGTH_SHORT).show() }
                clearInputs()
            }
        }
    }

    private fun getStartOfDayTimestamp(timestamp: Long): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun clearInputs() {
        binding.etName.text?.clear()
        binding.etRoll.text?.clear()
        binding.etEnrollmentNumber.text?.clear()
        binding.etBranch.text?.clear()
        binding.etSemester.text?.clear()
        binding.etSubjectName.text?.clear()
        binding.actvSubjectType.text?.clear()
        binding.ivFace.setImageDrawable(null)
        binding.ivFace.visibility = View.GONE
        binding.previewView.visibility = View.VISIBLE
        detectedFaceBitmap?.recycle()
        detectedFaceBitmap = null
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
                if (!isHidden) {
                    startCamera()
                }
            } else {
                context?.let { Toast.makeText(it, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
