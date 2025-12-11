package com.example.smart_attendance.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.example.smart_attendance.R
import java.io.ByteArrayOutputStream
import org.opencv.core.Mat
import org.opencv.core.CvType
import org.opencv.android.Utils
import org.opencv.imgproc.Imgproc
import org.opencv.core.MatOfRect
import org.opencv.core.Size
import org.opencv.objdetect.CascadeClassifier
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object FaceRecognitionManager {
    private const val STANDARD_FACE_DIMENSION = 96 // Fixed size for face embeddings
    private var cascade: CascadeClassifier? = null

    private fun initializeCascade(context: Context) {
        if (cascade != null) return

        val cascadeFile = File(context.filesDir, "haarcascade_frontalface_default.xml")
        if (!cascadeFile.exists()) {
            try {
                context.assets.open("haarcascade_frontalface_default.xml").use { input: InputStream ->
                    FileOutputStream(cascadeFile).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                Log.e("FaceRecognitionManager", "Error copying cascade file from assets: ${e.message}")
                return
            }
        }

        cascade = CascadeClassifier(cascadeFile.absolutePath)
        if (cascade!!.empty()) {
            Log.e("FaceRecognitionManager", "Failed to load cascade classifier at ${cascadeFile.absolutePath}")
            cascade = null
        }
    }

    fun detectFace(context: Context, bitmap: Bitmap): Bitmap? {
        initializeCascade(context)
        if (cascade == null) {
            Log.e("FaceRecognitionManager", "Cascade classifier is not initialized.")
            return null
        }

        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2GRAY)
        Imgproc.equalizeHist(mat, mat)

        val faces = MatOfRect()
        val minFaceSize = Size(150.0, 150.0)
        val maxFaceSize = Size()
        cascade!!.detectMultiScale(mat, faces, 1.1, 3, 0, minFaceSize, maxFaceSize)

        val faceArray = faces.toArray()
        if (faceArray.isNotEmpty()) {
            val r = faceArray[0]
            val x = r.x.coerceAtLeast(0)
            val y = r.y.coerceAtLeast(0)
            val w = r.width.coerceAtMost(bitmap.width - x)
            val h = r.height.coerceAtMost(bitmap.height - y)

            return Bitmap.createBitmap(bitmap, x, y, w, h)
        }
        return null
    }

    fun getFaceEmbedding(context: Context, faceBitmap: Bitmap): ByteArray? {
        if (faceBitmap.isRecycled) {
            Log.e("FaceRecognitionManager", "Input bitmap is recycled.")
            return null
        }
        val resizedFaceBitmap = Bitmap.createScaledBitmap(faceBitmap, STANDARD_FACE_DIMENSION, STANDARD_FACE_DIMENSION, true)
        val mat = Mat()
        val grayMat = Mat()
        return try {
            Utils.bitmapToMat(resizedFaceBitmap, mat)
            Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_RGBA2GRAY)

            val byteBuffer = ByteArray((grayMat.total() * grayMat.elemSize()).toInt())
            grayMat.get(0, 0, byteBuffer)
            byteBuffer
        } catch (e: Exception) {
            Log.e("FaceRecognitionManager", "Error generating face embedding: ${e.message}")
            null
        } finally {
            mat.release()
            grayMat.release()
            if (resizedFaceBitmap != faceBitmap) resizedFaceBitmap.recycle()
        }
    }

    fun recognize(context: Context, inputBitmap: Bitmap, targetFaceEmbedding: ByteArray, targetWidth: Int, targetHeight: Int): Boolean {
        val inputFace = detectFace(context, inputBitmap)
        if (inputFace == null) {
            Log.e("FaceRecognitionManager", "No face detected in input bitmap during recognition attempt.")
            return false
        }

        val resizedInputFace = Bitmap.createScaledBitmap(inputFace, STANDARD_FACE_DIMENSION, STANDARD_FACE_DIMENSION, true)
        inputFace.recycle()

        val inputMat = Mat()
        val inputGray = Mat()
        val targetGray = Mat(STANDARD_FACE_DIMENSION, STANDARD_FACE_DIMENSION, CvType.CV_8UC1)

        return try {
            Utils.bitmapToMat(resizedInputFace, inputMat)
            Imgproc.cvtColor(inputMat, inputGray, Imgproc.COLOR_RGBA2GRAY)

            if (targetFaceEmbedding.size != STANDARD_FACE_DIMENSION * STANDARD_FACE_DIMENSION) {
                Log.e("FaceRecognitionManager", "Target embedding has incorrect size: ${targetFaceEmbedding.size}, expected: ${STANDARD_FACE_DIMENSION * STANDARD_FACE_DIMENSION}")
                return false
            }
            targetGray.put(0, 0, targetFaceEmbedding)

            val distance = org.opencv.core.Core.norm(inputGray, targetGray, org.opencv.core.Core.NORM_L2)
            val threshold = 60000.0
            val confidence = (1.0 - (distance / threshold)) * 100.0
            Log.d("FaceRecognitionManager", "Face match confidence: ${String.format("%.2f", confidence)}%, Distance: $distance, Threshold: $threshold")

            distance < threshold
        } catch (e: Exception) {
            Log.e("FaceRecognitionManager", "Error during face recognition: ${e.message}", e)
            false
        } finally {
            inputMat.release()
            inputGray.release()
            targetGray.release()
            resizedInputFace.recycle()
        }
    }
}
