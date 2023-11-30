package com.example.basma2

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.media.Image
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.example.basma2.databinding.FragmentSecondBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfDMatch
import org.opencv.core.MatOfFloat
import org.opencv.core.MatOfKeyPoint
import org.opencv.core.Point
import org.opencv.features2d.BFMatcher
import org.opencv.features2d.ORB
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File
import java.lang.Math.abs
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt


/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SecondFragment : Fragment() {

    private var distance: Float= 0.0F
    private var stopDetectorRunning: Boolean=false
    private var savedUri: Uri?=null
    private var faceData1: MutableList<PointF>?= ArrayList<PointF>()
    private lateinit var savedFaceBounds: android.graphics.Rect
    private var savedFaceDetected: Boolean=false
    private var counter: CountDownTimer?=null
    private var contourPoints1: MutableList<PointF>?= ArrayList<PointF>()
    private var contourPoints2: MutableList<PointF>?= ArrayList<PointF>()
    var face1: MutableList<Face>?= ArrayList<Face>()
    private var savedFaceBoundsList : MutableList<android.graphics.Rect>?= ArrayList<android.graphics.Rect>()
    var savedFaceDataList =  mutableListOf<MutableList<Float>>()
    var newFrontFace=0.0
    var newRightFace=0.0
    var newLeftFace=0.0
    private var first: Boolean=true
    private var _binding: FragmentSecondBinding? = null
    private var camera: Camera? = null
    private lateinit var imageAnalysis: ImageAnalysis

    private val faceDetector by lazy {
        val highAccuracyOpts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
//            .enableTracking()
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()

// Real-time contour detection
        val realTimeOpts = FaceDetectorOptions.Builder()
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .build()
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
        FaceDetection.getClient(options)
    }
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonSecond.setOnClickListener {
//            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
            val imageCapture = ImageCapture.Builder()
                .setTargetRotation(Surface.ROTATION_0)
                .build()


            val cameraProvider = ProcessCameraProvider.getInstance(requireContext()).get()

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            cameraProvider.unbindAll()

            cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture)
            val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

            val outputDirectory = getOutputDirectory()

            val photoFile = File(
                outputDirectory,
                SimpleDateFormat(FILENAME_FORMAT, Locale.US
                ).format(System.currentTimeMillis()) + ".jpg")

            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            imageCapture.takePicture(
                outputOptions, ContextCompat.getMainExecutor(requireContext()), object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exc: ImageCaptureException) {
                        Log.e(SecondFragment.TAG, "Photo capture failed: ${exc.message}", exc)
                    }

                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        savedUri = Uri.fromFile(photoFile)
                        val msg = "Photo capture succeeded: $savedUri"
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                        Log.d(SecondFragment.TAG, msg)
//                        val imageProxy = imageCapture.takePicture(executor, imageCaptureCallback)
//                        val image = imageProxy.image
//                        val buffer = image.planes[0].buffer
//                        val bytes = ByteArray(buffer.capacity())
//                        buffer.get(bytes)
//                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }
                })

        }
        binding.buttonSecond2.setOnClickListener {
            if(stopDetectorRunning)
                stopDetectorRunning=false
            val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }


                imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(ContextCompat.getMainExecutor(requireContext()), { imageProxy ->
                            detectFaces(imageProxy)
                        })
                    }

                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                try {
                    cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
//                    camera = cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis)
                } catch (exc: Exception) {
                    Log.e(TAG, "Error occurred: ${exc.message}", exc)
                }
            }, ContextCompat.getMainExecutor(requireContext()))
        }

    }

    private fun getOutputDirectory(): String {
        val m: PackageManager = requireActivity().getPackageManager()
        var directionPath: String = requireActivity().getPackageName()
        try {
            val p = m.getPackageInfo(directionPath!!, 0)
            directionPath = p.applicationInfo.dataDir
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w("yourtag", "Error Package name not found ", e)
        }

        return directionPath
    }

    fun calculateDistance(point1: PointF, point2: PointF): Float {
        // حساب المسافة بين نقطتين
        return sqrt((point2.x - point1.x).pow(2) + (point2.y - point1.y).pow(2))
    }

    fun calculateSimilarity(value1: Float, value2: Float): Float {
        // قم بحساب التشابه أو الدرجة بين value1 و value2
        // يمكن استخدام مجموعة متنوعة من الطرق، مثل حساب الاختلاف المطلق، أو نسبة الاختلاف، إلخ.
        return (1 / (1 + abs(value1 - value2))) // مثال لدالة تقييم بسيطة
    }
    fun calculateMatchingScoreForLeftFace(face1: Face, face2: Face): Float {
        val leftEye1 = face1.getLandmark(FaceLandmark.LEFT_EYE)
        val rightEye1 = face1.getLandmark(FaceLandmark.RIGHT_EYE)
        val noseBase1 = face1.getLandmark(FaceLandmark.NOSE_BASE)
        val mouseLeft1 = face1.getLandmark(FaceLandmark.MOUTH_LEFT)
        val mouseRight1 = face1.getLandmark(FaceLandmark.MOUTH_RIGHT)
        val earLeft1 = face1.getLandmark(FaceLandmark.LEFT_EAR)
        val earRight1 = face1.getLandmark(FaceLandmark.RIGHT_EAR)
        val mouseBottom1 = face1.getLandmark(FaceLandmark.MOUTH_BOTTOM)

        val leftEye2 = face2.getLandmark(FaceLandmark.LEFT_EYE)
        val rightEye2 = face2.getLandmark(FaceLandmark.RIGHT_EYE)
        val noseBase2 = face2.getLandmark(FaceLandmark.NOSE_BASE)
        val mouseLeft2 = face2.getLandmark(FaceLandmark.MOUTH_LEFT)
        val mouseRight2 = face2.getLandmark(FaceLandmark.MOUTH_RIGHT)
        val earLeft2 = face2.getLandmark(FaceLandmark.LEFT_EAR)
        val earRight2 = face2.getLandmark(FaceLandmark.RIGHT_EAR)
        val mouseBottom2 = face2.getLandmark(FaceLandmark.MOUTH_BOTTOM)

        val distanceRighToLeftEyes1 = calculateDistance(leftEye1!!.position, rightEye1!!.position)
        val distanceRighToLeftEyes2 = calculateDistance(leftEye2!!.position, rightEye2!!.position)

        val distanceNoseToLeftEye1 = calculateDistance(leftEye1.position, noseBase1!!.position)
        val distanceNoseToLeftEye2 = calculateDistance(leftEye2.position, noseBase2!!.position)

        val distanceNoseToLeftMouse1 = calculateDistance(mouseLeft1!!.position, noseBase1!!.position)
        val distanceNoseToLeftMouse2 = calculateDistance(mouseLeft2!!.position, noseBase2!!.position)

        val distanceNoseToBottomMouse1 = calculateDistance(noseBase1!!.position, mouseBottom1!!.position)
        val distanceNoseToBottomMouse2 = calculateDistance(noseBase2!!.position, mouseBottom2!!.position)

        val distanceNoseToLeftEar1 = calculateDistance(earLeft1!!.position, noseBase1!!.position)
        val distanceNoseToLeftEar2 = calculateDistance(earLeft2!!.position, noseBase2!!.position)

        val distanceMouseLeft1ToLeftEar1 = calculateDistance(earLeft1!!.position, mouseLeft1!!.position)
        val distanceMouseLeft2ToLeftEar2 = calculateDistance(earLeft2!!.position, mouseLeft2!!.position)

        val distanceLeftEye1ToLeftEar1 = calculateDistance(earLeft1!!.position, leftEye1!!.position)
        val distanceLeftEye2ToLeftEar2 = calculateDistance(earLeft2!!.position, leftEye2!!.position)


        val matchingScore =(calculateSimilarity(distanceRighToLeftEyes1, distanceRighToLeftEyes2) + calculateSimilarity(distanceNoseToLeftEye1, distanceNoseToLeftEye2)+
                calculateSimilarity(distanceNoseToLeftMouse1, distanceNoseToLeftMouse2) + calculateSimilarity(distanceNoseToBottomMouse1, distanceNoseToBottomMouse2)+
                calculateSimilarity(distanceNoseToLeftEar1, distanceNoseToLeftEar2) + calculateSimilarity(distanceMouseLeft1ToLeftEar1, distanceMouseLeft2ToLeftEar2)
                +calculateSimilarity(distanceLeftEye1ToLeftEar1, distanceLeftEye2ToLeftEar2))/7

        // قد تحتاج إلى توحيد النتائج وتقييمها بشكل أفضل وذلك يعتمد على تحليل البيانات
        return matchingScore
    }

    fun calculateMatchingScoreForRightFace(face1: Face, face2: Face): Float {
        val leftEye1 = face1.getLandmark(FaceLandmark.LEFT_EYE)
        val rightEye1 = face1.getLandmark(FaceLandmark.RIGHT_EYE)
        val noseBase1 = face1.getLandmark(FaceLandmark.NOSE_BASE)
        val mouseLeft1 = face1.getLandmark(FaceLandmark.MOUTH_LEFT)
        val mouseRight1 = face1.getLandmark(FaceLandmark.MOUTH_RIGHT)
        val earLeft1 = face1.getLandmark(FaceLandmark.LEFT_EAR)
        val earRight1 = face1.getLandmark(FaceLandmark.RIGHT_EAR)
        val mouseBottom1 = face1.getLandmark(FaceLandmark.MOUTH_BOTTOM)

        val leftEye2 = face2.getLandmark(FaceLandmark.LEFT_EYE)
        val rightEye2 = face2.getLandmark(FaceLandmark.RIGHT_EYE)
        val noseBase2 = face2.getLandmark(FaceLandmark.NOSE_BASE)
        val mouseLeft2 = face2.getLandmark(FaceLandmark.MOUTH_LEFT)
        val mouseRight2 = face2.getLandmark(FaceLandmark.MOUTH_RIGHT)
        val earLeft2 = face2.getLandmark(FaceLandmark.LEFT_EAR)
        val earRight2 = face2.getLandmark(FaceLandmark.RIGHT_EAR)
        val mouseBottom2 = face2.getLandmark(FaceLandmark.MOUTH_BOTTOM)

        val distanceRighToLeftEyes1 = calculateDistance(leftEye1!!.position, rightEye1!!.position)
        val distanceRighToLeftEyes2 = calculateDistance(leftEye2!!.position, rightEye2!!.position)

        val distanceNoseToRightEye1 = calculateDistance(noseBase1!!.position, rightEye1!!.position)
        val distanceNoseToRightEye2 = calculateDistance(noseBase2!!.position, rightEye2!!.position)

        val distanceNoseToRightMouse1 = calculateDistance(noseBase1!!.position, mouseRight1!!.position)
        val distanceNoseToRightMouse2 = calculateDistance(noseBase2!!.position, mouseRight2!!.position)

        val distanceNoseToBottomMouse1 = calculateDistance(noseBase1!!.position, mouseBottom1!!.position)
        val distanceNoseToBottomMouse2 = calculateDistance(noseBase2!!.position, mouseBottom2!!.position)

        val distanceNoseToRightEar1 = calculateDistance(noseBase1!!.position, earRight1!!.position)
        val distanceNoseToRightEar2 = calculateDistance(noseBase2!!.position, earRight2!!.position)

        val distanceMouseRight1ToRightEar1 = calculateDistance(earRight1!!.position, mouseRight1!!.position)
        val distancemouseRight2ToRightEar2 = calculateDistance(earRight2!!.position, mouseRight2!!.position)

        val distanceEyeRight1ToRightEar1 = calculateDistance(earRight1!!.position, rightEye1!!.position)
        val distanceEyeRight2ToRightEar2 = calculateDistance(earRight2!!.position, rightEye2!!.position)

        val matchingScore =(calculateSimilarity(distanceRighToLeftEyes1, distanceRighToLeftEyes2) + calculateSimilarity(distanceNoseToRightEye1, distanceNoseToRightEye2)+
                calculateSimilarity(distanceNoseToRightMouse1, distanceNoseToRightMouse2) + calculateSimilarity(distanceNoseToBottomMouse1, distanceNoseToBottomMouse2)+
                calculateSimilarity(distanceNoseToRightEar1, distanceNoseToRightEar2) + calculateSimilarity(distanceMouseRight1ToRightEar1, distancemouseRight2ToRightEar2)
                +calculateSimilarity(distanceEyeRight1ToRightEar1, distanceEyeRight2ToRightEar2))/7

        // قد تحتاج إلى توحيد النتائج وتقييمها بشكل أفضل وذلك يعتمد على تحليل البيانات
        return matchingScore
    }


    fun calculateMatchingScoreForFaceFront(face1: Face, face2: Face): Float {
        val leftEye1 = face1.getLandmark(FaceLandmark.LEFT_EYE)
        val rightEye1 = face1.getLandmark(FaceLandmark.RIGHT_EYE)
        val noseBase1 = face1.getLandmark(FaceLandmark.NOSE_BASE)
        val mouseLeft1 = face1.getLandmark(FaceLandmark.MOUTH_LEFT)
        val mouseRight1 = face1.getLandmark(FaceLandmark.MOUTH_RIGHT)
        val earLeft1 = face1.getLandmark(FaceLandmark.LEFT_EAR)
        val earRight1 = face1.getLandmark(FaceLandmark.RIGHT_EAR)
        val mouseBottom1 = face1.getLandmark(FaceLandmark.MOUTH_BOTTOM)

        val leftEye2 = face2.getLandmark(FaceLandmark.LEFT_EYE)
        val rightEye2 = face2.getLandmark(FaceLandmark.RIGHT_EYE)
        val noseBase2 = face2.getLandmark(FaceLandmark.NOSE_BASE)
        val mouseLeft2 = face2.getLandmark(FaceLandmark.MOUTH_LEFT)
        val mouseRight2 = face2.getLandmark(FaceLandmark.MOUTH_RIGHT)
        val earLeft2 = face2.getLandmark(FaceLandmark.LEFT_EAR)
        val earRight2 = face2.getLandmark(FaceLandmark.RIGHT_EAR)
        val mouseBottom2 = face2.getLandmark(FaceLandmark.MOUTH_BOTTOM)

        val distanceRighToLeftEyes1 = calculateDistance(leftEye1!!.position, rightEye1!!.position)
        val distanceRighToLeftEyes2 = calculateDistance(leftEye2!!.position, rightEye2!!.position)

        val distanceNoseToLeftEye1 = calculateDistance(leftEye1.position, noseBase1!!.position)
        val distanceNoseToLeftEye2 = calculateDistance(leftEye2.position, noseBase2!!.position)

        val distanceNoseToRightEye1 = calculateDistance(noseBase1!!.position, rightEye1!!.position)
        val distanceNoseToRightEye2 = calculateDistance(noseBase2!!.position, rightEye2!!.position)

        val distanceNoseToLeftMouse1 = calculateDistance(mouseLeft1!!.position, noseBase1!!.position)
        val distanceNoseToLeftMouse2 = calculateDistance(mouseLeft2!!.position, noseBase2!!.position)

        val distanceNoseToRightMouse1 = calculateDistance(noseBase1!!.position, mouseRight1!!.position)
        val distanceNoseToRightMouse2 = calculateDistance(noseBase2!!.position, mouseRight2!!.position)

        val distanceNoseToBottomMouse1 = calculateDistance(noseBase1!!.position, mouseBottom1!!.position)
        val distanceNoseToBottomMouse2 = calculateDistance(noseBase2!!.position, mouseBottom2!!.position)

//        val distanceNoseToLeftEar1 = calculateDistance(earLeft1!!.position, noseBase1!!.position)
//        val distanceNoseToLeftEar2 = calculateDistance(earLeft2!!.position, noseBase2!!.position)
//
//        val distanceNoseToRightEar1 = calculateDistance(noseBase1!!.position, earRight1!!.position)
//        val distanceNoseToRightEar2 = calculateDistance(noseBase2!!.position, earRight2!!.position)
//
//        val distanceMouseLeft1ToLeftEar1 = calculateDistance(earLeft1!!.position, mouseLeft1!!.position)
//        val distanceMouseLeft2ToLeftEar2 = calculateDistance(earLeft2!!.position, mouseLeft2!!.position)
//
//        val distanceMouseRight1ToRightEar1 = calculateDistance(earRight1!!.position, mouseRight1!!.position)
//        val distancemouseRight2ToRightEar2 = calculateDistance(earRight2!!.position, mouseRight2!!.position)
//
//        val distanceLeftEye1ToLeftEar1 = calculateDistance(earLeft1!!.position, leftEye1!!.position)
//        val distanceLeftEye2ToLeftEar2 = calculateDistance(earLeft2!!.position, leftEye2!!.position)
//
//        val distanceEyeRight1ToRightEar1 = calculateDistance(earRight1!!.position, rightEye1!!.position)
//        val distanceEyeRight2ToRightEar2 = calculateDistance(earRight2!!.position, rightEye2!!.position)

        val matchingScore =(calculateSimilarity(distanceRighToLeftEyes1, distanceRighToLeftEyes2) + calculateSimilarity(distanceNoseToLeftEye1, distanceNoseToLeftEye2)+
                calculateSimilarity(distanceNoseToRightEye1, distanceNoseToRightEye2) + calculateSimilarity(distanceNoseToLeftMouse1, distanceNoseToLeftMouse2)+
                calculateSimilarity(distanceNoseToRightMouse1, distanceNoseToRightMouse2) + calculateSimilarity(distanceNoseToBottomMouse1, distanceNoseToBottomMouse2)
//                +calculateSimilarity(distanceNoseToLeftEar1, distanceNoseToLeftEar2) + calculateSimilarity(distanceNoseToRightEar1, distanceNoseToRightEar2)+
//                calculateSimilarity(distanceMouseLeft1ToLeftEar1, distanceMouseLeft2ToLeftEar2) + calculateSimilarity(distanceMouseRight1ToRightEar1, distancemouseRight2ToRightEar2)+
//                calculateSimilarity(distanceLeftEye1ToLeftEar1, distanceLeftEye2ToLeftEar2) + calculateSimilarity(distanceEyeRight1ToRightEar1, distanceEyeRight2ToRightEar2)
                )/6

        // قد تحتاج إلى توحيد النتائج وتقييمها بشكل أفضل وذلك يعتمد على تحليل البيانات
        return matchingScore
    }


    fun calculateMatchingScoreForEyeWithNoise(face1: Face, face2: Face): Float {
        val leftEye1 = face1.getLandmark(FaceLandmark.LEFT_EYE)
        val rightEye1 = face1.getLandmark(FaceLandmark.RIGHT_EYE)
        val noseBase1 = face1.getLandmark(FaceLandmark.NOSE_BASE)

        val leftEye2 = face2.getLandmark(FaceLandmark.LEFT_EYE)
        val rightEye2 = face2.getLandmark(FaceLandmark.RIGHT_EYE)
        val noseBase2 = face2.getLandmark(FaceLandmark.NOSE_BASE)

        val distanceEyes1 = calculateDistance(leftEye1!!.position, rightEye1!!.position)
        val distanceEyes2 = calculateDistance(leftEye2!!.position, rightEye2!!.position)

        val distanceNose1 = calculateDistance(leftEye1.position, noseBase1!!.position)
        val distanceNose2 = calculateDistance(leftEye2.position, noseBase2!!.position)


        val matchingScore = calculateSimilarity(distanceEyes1, distanceEyes2) + calculateSimilarity(distanceNose1, distanceNose2)

        // قد تحتاج إلى توحيد النتائج وتقييمها بشكل أفضل وذلك يعتمد على تحليل البيانات
        return matchingScore
    }

    fun calculateMatchingScoreForMouseWithNoise(face1: Face, face2: Face): Float {

        val noseBase1 = face1.getLandmark(FaceLandmark.NOSE_BASE)
        val mouseLeft1 = face1.getLandmark(FaceLandmark.MOUTH_LEFT)
        val mouseRight1 = face1.getLandmark(FaceLandmark.MOUTH_RIGHT)

        val noseBase2 = face2.getLandmark(FaceLandmark.NOSE_BASE)
        val mouseLeft2 = face2.getLandmark(FaceLandmark.MOUTH_LEFT)
        val mouseRight2 = face2.getLandmark(FaceLandmark.MOUTH_RIGHT)


        val distanceMouseLeft1ToNoice1 = calculateDistance(mouseLeft1!!.position, noseBase1!!.position)
        val distanceMouseLeft2ToNoice2 = calculateDistance(mouseLeft2!!.position, noseBase2!!.position)

        val distanceMouseRight1ToNoice1 = calculateDistance(mouseRight1!!.position, noseBase1!!.position)
        val distanceMouseRight2ToNoice2 = calculateDistance(mouseRight2!!.position, noseBase2!!.position)

        val matchingScore = calculateSimilarity(distanceMouseLeft1ToNoice1, distanceMouseLeft2ToNoice2) + calculateSimilarity(distanceMouseRight1ToNoice1, distanceMouseRight2ToNoice2)

        // قد تحتاج إلى توحيد النتائج وتقييمها بشكل أفضل وذلك يعتمد على تحليل البيانات
        return matchingScore
    }

    fun calculateMatchingScoreEarWithNoise(face1: Face, face2: Face): Float {

        val noseBase1 = face1.getLandmark(FaceLandmark.NOSE_BASE)
        val earLeft1 = face1.getLandmark(FaceLandmark.LEFT_EAR)
        val earRight1 = face1.getLandmark(FaceLandmark.RIGHT_EAR)

        val noseBase2 = face2.getLandmark(FaceLandmark.NOSE_BASE)
        val earLeft2 = face2.getLandmark(FaceLandmark.LEFT_EAR)
        val earRight2 = face2.getLandmark(FaceLandmark.RIGHT_EAR)


        val distanceEarLeft1ToNoice1 = calculateDistance(earLeft1!!.position, noseBase1!!.position)
        val distanceEarLeft2ToNoice2 = calculateDistance(earLeft2!!.position, noseBase2!!.position)

        val distanceEarRight1ToNoice1 = calculateDistance(earRight1!!.position, noseBase1!!.position)
        val distanceEarRight2ToNoice2 = calculateDistance(earRight2!!.position, noseBase2!!.position)

        val matchingScore = calculateSimilarity(distanceEarLeft1ToNoice1, distanceEarLeft2ToNoice2) + calculateSimilarity(distanceEarRight1ToNoice1, distanceEarRight2ToNoice2)

        // قد تحتاج إلى توحيد النتائج وتقييمها بشكل أفضل وذلك يعتمد على تحليل البيانات
        return matchingScore
    }

    fun compareFaces( savedFaceData: MutableList<PointF>?,
                      newFaceData: MutableList<PointF>?): Float {
        // يجب تحويل savedFaceData و newFaceData لنوع البيانات المناسب للمقارنة
        // قم بحساب المسافة بين البيانات المحفوظة والبيانات الجديدة
        val distance = calculateDistance(savedFaceData, newFaceData)
        // إرجاع القيمة كمقياس للتشابه بين الوجهين
        return distance
    }

    fun calculateDistance( point1: MutableList<PointF>?,
                           point2: MutableList<PointF>?): Float {
        // التحقق من أن النقطتين لديهما نفس الأبعاد
        require(point1!!.size == point2!!.size)

        var sum = 0.0
        for (i in point1.indices) {
            val diff = (point1[i].x - point2[i].x)
            sum += diff * diff
        }
        return Math.sqrt(sum).toFloat()
    }
    @OptIn(ExperimentalGetImage::class) private fun detectFaces(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        val bitmap = toBitmap(mediaImage)

        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            faceDetector.process(image)
                .addOnSuccessListener { faces ->
                    GlobalScope.launch(Dispatchers.Default) {
                        launch {
                            try {
//                    val trackingId = face.trackingId
//                    val faceEmbedding = face.getFaceEmbedding()
//                    val distance = faceEmbedding.distance(storedFaceEmbedding)
//                    if (distance < threshold) {
//                        // Face is recognized
//                    }
//                    checkImagesBitMap(bitmap)
                                // قم بتحميل مكتبة OpenCV
//                    OpenCVLoader.initDebug()
//                    val result: Double = compareImages(image1, image2)
//
//
//// ضبط الـ threshold
                                val threshold = 0.70 // اختيار عتبة (threshold) مناسب يعتمد على نتائج الاختبارات
                                val thresholdFront = 0.646
                                val thresholdLeft = 0.559
                                val thresholdRight = 0.719
//
//// قرار استخدام threshold لتحديد ما إذا كانت الوجوه متطابقة أم لا
//                    if (result < threshold) {
//                        // الوجه المحدد هو نفسه الوجه السابق
//                    } else {
//                        // الوجه المحدد ليس نفس الوجه السابق
//                    }
                                if (faces.isNotEmpty()) {

                                    if (face1!!.size > 8 && faces.size > 0&&!stopDetectorRunning) {
//                                        calculateByContours(faces)
                                        val faceData2 =
                                            faces[0].getContour(FaceContour.FACE)?.points
                                        var distance = compareFaces(faceData1, faceData2)
                                        val currentFaceBounds = faces[0].boundingBox
                                        val face2 = faces[0]
                                        var i = 0
                                        for (face in face1!!) {
                                            GlobalScope.launch(Dispatchers.Default) {
                                                launch {
                                                    try {

                                                        val matchingScoreForEyeWithNoise = calculateMatchingScoreForEyeWithNoise(face, face2)
                                                        val matchingScore2ForMouseWithNoise = calculateMatchingScoreForMouseWithNoise(face, face2)
                                                        val matchingScore3ForEarWithNoise = calculateMatchingScoreEarWithNoise(face, face2)
                                                        val matchingScoreForFaceFront = calculateMatchingScoreForFaceFront(face, face2)
                                                        val matchingScoreForRightFace = calculateMatchingScoreForRightFace(face, face2)
                                                        val matchingScoreForLeftFace = calculateMatchingScoreForLeftFace(face, face2)

//                                                        Log.v("MANUAL_TESTING_LOG", "matchingScoreForFaceFront" + matchingScoreForFaceFront + "  matchingScore: " + matchingScoreForRightFace+ "  matchingScore3ForEarWithNoise: " + matchingScoreForLeftFace)
                                                        if(newFrontFace<matchingScoreForFaceFront){
                                                            newFrontFace= matchingScoreForFaceFront.toDouble()
                                                        }
                                                        if(newRightFace<matchingScoreForRightFace){
                                                            newRightFace= matchingScoreForRightFace.toDouble()
                                                        }
                                                        if(newLeftFace<matchingScoreForLeftFace){
                                                            newLeftFace= matchingScoreForLeftFace.toDouble()
                                                        }

                                                        if(newFrontFace<matchingScore3ForEarWithNoise){
                                                            newFrontFace= matchingScore3ForEarWithNoise.toDouble()
                                                        }
                                                        if(newRightFace<matchingScoreForEyeWithNoise){
                                                            newRightFace= matchingScoreForEyeWithNoise.toDouble()
                                                        }
                                                        if(newLeftFace<matchingScore2ForMouseWithNoise){
                                                            newLeftFace= matchingScore2ForMouseWithNoise.toDouble()
                                                        }
                                                        Log.v("MANUAL_TESTING_LOG", "newFrontFace" + newFrontFace + "  newRightFace: " + newRightFace+ "  newLeftFace: " + newLeftFace)

                                                        Log.v("MANUAL_TESTING_LOG", "distance" + matchingScore2ForMouseWithNoise + "  matchingScore: " + matchingScoreForEyeWithNoise+ "  matchingScore3ForEarWithNoise: " + matchingScore3ForEarWithNoise)
//                                                        if ((matchingScoreForFaceFront > thresholdFront || matchingScoreForRightFace > thresholdRight
//                                                            || matchingScoreForLeftFace > thresholdLeft)) {
                                                      if ((matchingScore3ForEarWithNoise > 1.55 || matchingScoreForEyeWithNoise > 1.77
                                                          || matchingScore2ForMouseWithNoise > 1.65)) {
//                                                        if (matchingScore3ForEarWithNoise > threshold && matchingScoreForEyeWithNoise > threshold && matchingScore2ForMouseWithNoise > threshold) {
                                                            activity?.runOnUiThread {
                                                                try {
                                                                    Log.v("MANUAL_TESTING_LOG", "newFrontFace" + newFrontFace + "  newRightFace: " + newRightFace+ "  newLeftFace: " + newLeftFace)

                                                                    Toast.makeText(
                                                                        requireContext(),
                                                                        "face detection done 2",
                                                                        Toast.LENGTH_SHORT
                                                                    ).show()
                                                                    stopDetectorRunning=true
                                                                    // Code to run on the main thread
                                                                    // For example, UI updates
                                                                    Navigation.findNavController(
                                                                        binding.root
                                                                    )
                                                                        .navigate(R.id.action_SecondFragment_to_FirstFragment)
                                                                } catch (e: Exception) {
                                                                }
                                                            }
                                                            // الوجه المحدد هو نفسه الوجه السابق
                                                        } else {
                                                            // الوجه المحدد ليس نفس الوجه السابق
                                                        }
                                                        if (i < face1!!.size) {
                                                            i++
                                                        }
                                                    } catch (e: Exception) {
                                                    }
                                                }
                                            }
                                        }


                                        // يمكنك استخدام matchingScore لتحديد تطابق الوجه
                                    } else {
                                        faceData1 = faces[0].getContour(FaceContour.FACE)?.points
                                        face1!!.add(faces[0])
                                        savedFaceBoundsList!!.add(faces[0].boundingBox)

//                                        val contours = faces[0].allContours
//                                        // Initialize an array to store the points
//                                        val points = mutableListOf<Float>()
//                                        // Loop through the contours and add the points to the array
//                                        for (contour in contours) {
//                                            for (point in contour.points) {
//                                                points.add(point.x)
//                                                points.add(point.y)
//                                            }
//                                        }
//                                        savedFaceDataList.add(points)

                                        if (face1!!.size == 8) {
                                            activity?.runOnUiThread {
                                                stopDetectorRunning=true
                                                Toast.makeText(
                                                    requireContext(),
                                                    "face detection done 1",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }


                                        }
                                    }

                                    if (savedFaceDetected) {
                                        // يمكنك استخدام خوارزمية التعرف على الوجه
                                        // للتحقق من وجود الوجه المطابق مسبقًا
                                        // على سبيل المثال، يمكنك تعيين savedFaceBounds مسبقاً

                                        val currentFaceBounds = faces[0].boundingBox
                                        Log.v(
                                            "MANUAL_TESTING_LOG",
                                            "currentFaceBounds: " + currentFaceBounds
                                        )

                                        if (areBoundsEqual(savedFaceBounds, currentFaceBounds)) {

//                                activity?.runOnUiThread {
//                                    try {
//                                        // Code to run on the main thread
//                                        // For example, UI updates
//                                        Navigation.findNavController(binding.root)
//                                            .navigate(R.id.action_SecondFragment_to_FirstFragment)
//                                    }catch (e:Exception){}
//                                }

//                                requireActivity().onBackPressed()
                                            // وجد الوجه المطابق
                                        } else {
                                            // الوجه ليس المطابق
                                        }

                                    } else {
                                        savedFaceBounds = faces[0].boundingBox
                                        Log.v("MANUAL_TESTING_LOG", "noseBase: " + savedFaceBounds)

                                        savedFaceDetected = true
                                        // لم يتم حفظ الوجه مسبقاً
                                    }
                                } else {
                                    // لم يتم الكشف عن وجوه
                                }
                            } catch (e: Exception) {
                            }
                        }
                    }

//                    facesCheckes(faces,imageProxy)
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Face detection failed: ${exception.message}", exception)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }

    private fun calculateByContours(faces: MutableList<Face>) {
            GlobalScope.launch(Dispatchers.Default) {
                launch {
                    try {
                        val contours = faces[0].allContours
                        // Initialize an array to store the points
                        val points = mutableListOf<Float>()
                        // Loop through the contours and add the points to the array
                        for (contour in contours) {
                            for (point in contour.points) {
                                points.add(point.x)
                                points.add(point.y)
                            }
                        }
                        for (savedFaceData in savedFaceDataList) {
                            distance = euclideanDistance(points,savedFaceData)
                            Log.v("MANUAL_TESTING_LOG", "distance" + distance)

                        }
                        val threshold = 0.5f
                        // Compare the distance with the threshold
                        if (distance <= threshold) {
                            activity?.runOnUiThread {
                                try {

                                    // Code to run on the main thread
                                    // For example, UI updates
                                    Navigation.findNavController(
                                        binding.root
                                    )
                                        .navigate(R.id.action_SecondFragment_to_FirstFragment)
                                } catch (e: Exception) {
                                }
                            }
                            // The distance is less than or equal to the threshold, the faces are matching
                        } else {
                            // The distance is greater than the threshold, the faces are not matching
                        }
                        // Return the distance as a measure of similarity

                    } catch (e: Exception) {
                    }
                }
        }
    }

    // Define a function to calculate the Euclidean distance between two arrays of points
    fun euclideanDistance(array1: MutableList<Float>, array2: MutableList<Float>): Float {
        // Check if the arrays have the same length
        if (array1.size != array2.size) {
            throw IllegalArgumentException("The arrays must have the same length")
        }
        // Initialize the sum of squared differences
        var sum = 0f
        // Loop through the arrays and calculate the squared difference for each element
        for (i in array1.indices) {
            sum += (array1[i] - array2[i]).pow(2)
        }
        // Return the square root of the sum
        return sqrt(sum)
    }

    private fun checkImagesBitMap(bitmap: Bitmap?) {
        // تحميل الصورتين من ملفات على الجهاز
        val face1 = Imgcodecs.imread("face1.jpg")
        val face2 = Imgcodecs.imread("face2.jpg")

// إنشاء كائن ORB لاستخراج النقاط المميزة
        val orb = ORB.create()

// إنشاء مصفوفتين لتخزين النقاط المميزة والمصفوفات المماثلة
        val keypoints1 = MatOfKeyPoint()
        val descriptors1 = Mat()
        val keypoints2 = MatOfKeyPoint()
        val descriptors2 = Mat()

// استخراج النقاط المميزة والمصفوفات المماثلة من كل صورة
        orb.detectAndCompute(face1, Mat(), keypoints1, descriptors1)
        orb.detectAndCompute(face2, Mat(), keypoints2, descriptors2)

// إنشاء كائن BFMatcher لمطابقة النقاط المميزة
        val matcher = BFMatcher.create(BFMatcher.BRUTEFORCE_HAMMING, true)

// إنشاء مصفوفة لتخزين نتائج المطابقة
        val matches = MatOfDMatch()

// مطابقة النقاط المميزة بين الصورتين
        matcher.match(descriptors1, descriptors2, matches)

// حساب معدل المطابقة بين النقاط المميزة
        val matchRate = matches.toList().sumByDouble { it.distance.toDouble() } / matches.rows()

// طباعة معدل المطابقة
        println("Match rate: $matchRate")

// إذا كان معدل المطابقة أقل من حد معين (مثلا 50)، فإن الوجهين متشابهين
        if (matchRate < 50) {
            println("The faces are similar")
        } else {
            println("The faces are not similar")
        }

    }


    // فحص حركة الوجه
    fun isFaceMoving(previousBitmap: Bitmap, currentBitmap: Bitmap, faces: List<Face>): Boolean {
        if (faces.isNotEmpty()) {
            val face = faces[0]

            // قم بتحويل الصور إلى Mat (مصفوفة الصورة)
            val previousMat = Mat()
            val currentMat = Mat()
            Utils.bitmapToMat(previousBitmap, previousMat)
            Utils.bitmapToMat(currentBitmap, currentMat)

            // قم بتحويل الصور إلى درجات رمادية لسهولة التحقق
            Imgproc.cvtColor(previousMat, previousMat, Imgproc.COLOR_BGR2GRAY)
            Imgproc.cvtColor(currentMat, currentMat, Imgproc.COLOR_BGR2GRAY)

            // قم بحساب تدفق الحركة البصري بين الصورتين
            val flow = MatOfFloat()
//            Imgproc.calcOpticalFlowFarneback(
//                previousMat, currentMat, flow,
//                0.5, 3, 15, 3, 5, 1.2, 0
//            )

            // قم بحساب متوسط تدفق الحركة للوجه
            val faceCenter = Point(
                (face.boundingBox.left + face.boundingBox.right) / 2.0,
                (face.boundingBox.top + face.boundingBox.bottom) / 2.0
            )
            val flowAtFace = flow[faceCenter.y.toInt(), faceCenter.x.toInt()]
            val averageFlow = flowAtFace?.let { Math.sqrt(it[0].toDouble().pow(2) + it[1].toDouble().pow(2)) }

            // اعتبر الوجه متحركًا إذا كان التدفق أعلى من قيمة محددة (يمكن ضبطها حسب الحاجة)
            val threshold = 0.5
            return averageFlow ?: 0.0 > threshold
        }

        return false
    }

    private fun toBitmap(mediaImage: Image?): Bitmap? {
        val planes = mediaImage?.planes
        val buffer = planes?.get(0)!!.buffer
        val bytes = ByteArray(buffer.capacity())
        buffer.get(bytes)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        return bitmap
    }

    // دالة للتحقق من تطابق الـ bounding boxes
    private fun areBoundsEqual(bounds1: android.graphics.Rect, bounds2: android.graphics.Rect): Boolean {
        val threshold = 30 // عتبة مقبولة للتطابق

        val leftDiff = abs(bounds1.left - bounds2.left)
        val topDiff = abs(bounds1.top - bounds2.top)
        val rightDiff = abs(bounds1.right - bounds2.right)
        val bottomDiff = abs(bounds1.bottom - bounds2.bottom)

        return leftDiff < threshold && topDiff < threshold && rightDiff < threshold && bottomDiff < threshold
    }

    private fun facesCheckes(faces: List<Face>, imageProxy: ImageProxy) {
        for (face in faces) {
            logExtrasForTesting(face)
            val gson = Gson()
            val json = gson.toJson(faces)
            val savedFaceDetections = json.toString()

//                        val jsonString = "{\"id\":1,\"name\":\"John Doe\",\"email\":\"john@example.com\"}"
//                        val user = gson.fromJson(savedFaceDetections, Face::class.java)
            val listType = object : TypeToken<MutableList<Face>>() {}.type
            val faces: MutableList<Face> = gson.fromJson(savedFaceDetections, listType)
            val leftEye1 = face.getLandmark(FaceLandmark.LEFT_EYE)
            val rightEye1 = face.getLandmark(FaceLandmark.RIGHT_EYE)
            // استخراج الميزات الوجهية
            val leftEye: PointF? = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
            val rightEye: PointF? = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position
            val noseBase: PointF? = face.getLandmark(FaceLandmark.NOSE_BASE)?.position
            val mouthLeft: PointF? = face.getLandmark(FaceLandmark.MOUTH_LEFT)?.position
            val mouthRight: PointF? = face.getLandmark(FaceLandmark.MOUTH_RIGHT)?.position
            Log.v("MANUAL_TESTING_LOG", "leftEye: " + leftEye1)
            Log.v("MANUAL_TESTING_LOG", "leftEye: " + rightEye1)
            Log.v("MANUAL_TESTING_LOG", "leftEye: " + leftEye+" "+faces.size)
            Log.v("MANUAL_TESTING_LOG", "rightEye: " + rightEye)
            Log.v("MANUAL_TESTING_LOG", "noseBase: " + noseBase)
            Log.v("MANUAL_TESTING_LOG", "mouthLeft: " + mouthLeft)
            Log.v("MANUAL_TESTING_LOG", "mouthRight: " + gson.toJson(face.getContour(FaceContour.FACE)?.points).toString())
            Log.v("MANUAL_TESTING_LOG", "mouthRight: " + savedFaceDetections)

//                        if (faces1.size == 1 && faces2.size == 1) {
//                            val face1 = faces1[0]
//                            val face2 = faces2[0]
//                            // احصل على قائمة من نقاط الملامح لكل وجه
            if (first) {
                contourPoints1 = face.getContour(FaceContour.FACE)?.points
                first = false
//                faceDetector.close()
            } else {
                contourPoints2 = face.getContour(FaceContour.FACE)?.points
                if (compareFaceFeatures(contourPoints1!!, contourPoints2!!)) {
                    Toast.makeText(
                        requireContext(),
                        "face detection done",
                        Toast.LENGTH_SHORT
                    ).show()
//                    imageProxy.close()
                    requireActivity().onBackPressed()
                }
            }

//
//                            // حساب مؤشر Face Contour Similarity بين القائمتين باستخدام خوارزمية ما
//                            val similarityIndex = calculateFaceContourSimilarity(contourPoints1, contourPoints2)
//
//                            // طباعة النتيجة
//                            println("Face Contour Similarity = $similarityIndex")
//                        } else {
//                            // التعامل مع حالات الفشل
//                            handleFailure()
//                        }
            if (faces.size == 1) {
                val face = faces[0]
                // احصل على بيانات الوجه مثل موقع العينين والأنف والفم
                val leftEye = face.leftEyeOpenProbability
                val rightEye = face.rightEyeOpenProbability
//                            val nose = face.allLandmarks.get(0).landmarkType
                val nose = face.getLandmark(FaceLandmark.NOSE_BASE)?.position
                val mouthLeft = face.getLandmark(FaceLandmark.MOUTH_LEFT)?.position
                val mouthRight = face.getLandmark(FaceLandmark.MOUTH_RIGHT)?.position

                // احسب مؤشر تشابه الوجه بين الصورتين باستخدام خوارزمية ما
//                            val similarityIndex = calculateSimilarityIndex(faceData, storedFaceData)

                // إذا كان المؤشر أعلى من حد معين، فاعتبر أن الوجه ينتمي لصاحب الهاتف
//                            if (similarityIndex > threshold) {
//                                // فتح الهاتف أو تنفيذ أي إجراء آخر
//                                unlockPhone()
//                            } else {
//                                // إظهار رسالة خطأ أو طلب كلمة مرور أو تنفيذ أي إجراء آخر
////                                showError()
//                            }
            } else {
                // إظهار رسالة خطأ أو طلب كلمة مرور أو تنفيذ أي إجراء آخر
//                            showError()
            }
            // قم بتنفيذ الإجراءات الضرورية باستخدام الميزات الوجهية


            // يمكنك تنفيذ الإجراءات المناسبة لكشف الوجه هنا
        }
//        counter = object : CountDownTimer(2015, 50) {
//            override fun onTick(millisUntilFinished: Long) {
//
//            }
//            override fun onFinish() {
//
//
//            }
//        }.start()
    }


    // دالة لمقارنة الصور
    fun compareImages(image1: Mat, image2: Mat): Double {
        val image1Gray = Mat()
        val image2Gray = Mat()

        // قم بتحويل الصور إلى صور رمادية
        Imgproc.cvtColor(image1, image1Gray, Imgproc.COLOR_BGR2GRAY)
        Imgproc.cvtColor(image2, image2Gray, Imgproc.COLOR_BGR2GRAY)

        // قم بحساب الاختلاف بين الصور باستخدام MSE
        val diff = Mat()
        Core.absdiff(image1Gray, image2Gray, diff)
        Core.multiply(diff, diff, diff)

        val mse = Core.mean(diff)

        return mse.`val`[0] + mse.`val`[1] + mse.`val`[2]
    }
    private fun doBack() {
//        counter!!.cancel()
        requireActivity().onBackPressed()
    }

    private fun logExtrasForTesting(face: Face?) {
        if (face != null) {
            Log.v("MANUAL_TESTING_LOG", "face bounding box: " + face.boundingBox.flattenToString())
            Log.v("MANUAL_TESTING_LOG", "face Euler Angle X: " + face.headEulerAngleX)
            Log.v("MANUAL_TESTING_LOG", "face Euler Angle Y: " + face.headEulerAngleY)
            Log.v("MANUAL_TESTING_LOG", "face Euler Angle Z: " + face.headEulerAngleZ)

            // All landmarks
            val landMarkTypes = intArrayOf(
                FaceLandmark.MOUTH_BOTTOM,
                FaceLandmark.MOUTH_RIGHT,
                FaceLandmark.MOUTH_LEFT,
                FaceLandmark.RIGHT_EYE,
                FaceLandmark.LEFT_EYE,
                FaceLandmark.RIGHT_EAR,
                FaceLandmark.LEFT_EAR,
                FaceLandmark.RIGHT_CHEEK,
                FaceLandmark.LEFT_CHEEK,
                FaceLandmark.NOSE_BASE
            )
            val landMarkTypesStrings = arrayOf(
                "MOUTH_BOTTOM",
                "MOUTH_RIGHT",
                "MOUTH_LEFT",
                "RIGHT_EYE",
                "LEFT_EYE",
                "RIGHT_EAR",
                "LEFT_EAR",
                "RIGHT_CHEEK",
                "LEFT_CHEEK",
                "NOSE_BASE"
            )
            for (i in landMarkTypes.indices) {
                val landmark = face.getLandmark(landMarkTypes[i])
                if (landmark == null) {
                    Log.v(
                        "MANUAL_TESTING_LOG",
                        "No landmark of type: " + landMarkTypesStrings[i] + " has been detected"
                    )
                } else {
                    val landmarkPosition = landmark.position
                    val landmarkPositionStr = String.format(
                        Locale.US,
                        "x: %f , y: %f",
                        landmarkPosition.x,
                        landmarkPosition.y
                    )

                    Log.v(
                        "MANUAL_TESTING_LOG",
                        "Position for face landmark: "
                                + landMarkTypesStrings[i]
                                + " is :"
                                + landmarkPositionStr
                    )
//                    doBack()
                }
            }
            Log.v(
                "MANUAL_TESTING_LOG",
                "face left eye open probability: " + face.leftEyeOpenProbability
            )
            Log.v(
                "MANUAL_TESTING_LOG",
                "face right eye open probability: " + face.rightEyeOpenProbability
            )
            Log.v("MANUAL_TESTING_LOG", "face smiling probability: " + face.smilingProbability)
            Log.v("MANUAL_TESTING_LOG", "face tracking id: " + face.trackingId)
        }
    }
    private fun compareFaceFeatures(detectedFeatures: List<PointF>, storedFeatures: List<PointF>): Boolean {
        // التأكد من أن عدد النقاط المميزة متساوي بين الميزات المكتشفة والمخزنة
        if (detectedFeatures.size != storedFeatures.size) {
            return false
        }

        val threshold = 0.1 // عتبة لتحديد تطابق الميزات

        var distance = 0.0
        for (i in detectedFeatures.indices) {
            val detectedPoint = detectedFeatures[i]
            val storedPoint = storedFeatures[i]

            // حساب المسافة بين النقطتين باستخدام Euclidean Distance
            val dx = detectedPoint.x - storedPoint.x
            val dy = detectedPoint.y - storedPoint.y
            val pointDistance = Math.sqrt((dx * dx + dy * dy).toDouble())

            // إضافة المسافة إلى المجموع الإجمالي
            distance += pointDistance
        }

        // حساب المتوسط ​​للمسافة
        val averageDistance = distance / detectedFeatures.size

        // مقارنة المتوسط ​​مع العتبة
        return averageDistance <= threshold
    }

    companion object {
        const val TAG = "MainActivity"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}