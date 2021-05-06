package com.nmc.android.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.nmc.android.OnDocScanListener
import com.nmc.android.OnFragmentChangeListener
import com.owncloud.android.R
import io.scanbot.sdk.ScanbotSDK
import io.scanbot.sdk.SdkLicenseError
import io.scanbot.sdk.camera.CameraOpenCallback
import io.scanbot.sdk.camera.FrameHandlerResult
import io.scanbot.sdk.camera.PictureCallback
import io.scanbot.sdk.camera.ScanbotCameraView
import io.scanbot.sdk.contourdetector.ContourDetectorFrameHandler
import io.scanbot.sdk.contourdetector.DocumentAutoSnappingController
import io.scanbot.sdk.core.contourdetector.DetectionResult
import io.scanbot.sdk.docprocessing.PageProcessor
import io.scanbot.sdk.entity.Language
import io.scanbot.sdk.ocr.OpticalCharacterRecognizer
import io.scanbot.sdk.ocr.process.OcrResult
import io.scanbot.sdk.persistence.Page
import io.scanbot.sdk.persistence.PageFileStorage
import io.scanbot.sdk.process.CropOperation
import io.scanbot.sdk.process.ImageFilterType
import io.scanbot.sdk.process.Operation
import io.scanbot.sdk.process.PDFPageSize
import io.scanbot.sdk.ui.PolygonView
import io.scanbot.sdk.ui.camera.ShutterButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.ArrayList

class ScanDocumentFragment : Fragment(), ContourDetectorFrameHandler.ResultHandler {

    private lateinit var cameraView: ScanbotCameraView
    private lateinit var polygonView: PolygonView
    private lateinit var userGuidanceHint: AppCompatTextView
    private lateinit var autoSnappingToggleButton: MaterialButton
    private lateinit var shutterButton: ShutterButton
    private lateinit var progressBar: ProgressBar

    private lateinit var contourDetectorFrameHandler: ContourDetectorFrameHandler
    private lateinit var autoSnappingController: DocumentAutoSnappingController

    private lateinit var scanbotSDK: ScanbotSDK

    private var lastUserGuidanceHintTs = 0L
    private var flashEnabled = false
    private var autoSnappingEnabled = true
    private val ignoreBadAspectRatio = true

    //OCR
    private lateinit var opticalCharacterRecognizer: OpticalCharacterRecognizer
    private lateinit var pageFileStorage: PageFileStorage
    private lateinit var pageProcessor: PageProcessor

    private val uiScope = CoroutineScope(Dispatchers.Main)

    private lateinit var onDocScanListener: OnDocScanListener
    private lateinit var onFragmentChangeListener: OnFragmentChangeListener

    private lateinit var calledFrom: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getString(ARG_CALLED_FROM)?.let {
            calledFrom = it
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            onDocScanListener = context as OnDocScanListener
            onFragmentChangeListener = context as OnFragmentChangeListener
        } catch (ignored: Exception) {

        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        //supportRequestWindowFeature(WindowCompat.FEATURE_ACTION_BAR_OVERLAY)
        if (requireActivity() is ScanActivity) {
            (requireActivity() as ScanActivity).showHideToolbar(false)
        }
        return inflater.inflate(R.layout.fragment_scan_document, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        askPermission()
        scanbotSDK = (requireActivity() as ScanActivity).scanbotSDK
        initOCR()
        cameraView = view.findViewById<View>(R.id.camera) as ScanbotCameraView

        // In this example we demonstrate how to lock the orientation of the UI (Activity)
        // as well as the orientation of the taken picture to portrait.
        cameraView.lockToPortrait(true)

        // See https://github.com/doo/scanbot-sdk-example-android/wiki/Using-ScanbotCameraView#preview-mode
        //cameraView.setPreviewMode(io.scanbot.sdk.camera.CameraPreviewMode.FIT_IN);
        cameraView.setCameraOpenCallback(object : CameraOpenCallback {
            override fun onCameraOpened() {
                cameraView.postDelayed({
                    cameraView.setAutoFocusSound(false)

                    // Shutter sound is ON by default. You can disable it:
                    // cameraView.setShutterSound(false);

                    cameraView.continuousFocus()
                    cameraView.useFlash(flashEnabled)
                }, 700)
            }
        })
        progressBar = view.findViewById(R.id.scan_doc_progress_bar)
        polygonView = view.findViewById<View>(R.id.polygonView) as PolygonView
        // polygonView.setFillColor(POLYGON_FILL_COLOR)
        //polygonView.setFillColorOK(POLYGON_FILL_COLOR_OK)

        contourDetectorFrameHandler = ContourDetectorFrameHandler.attach(cameraView, scanbotSDK.contourDetector())

        // Please note: https://github.com/doo/Scanbot-SDK-Examples/wiki/Detecting-and-drawing-contours#contour-detection-parameters
        contourDetectorFrameHandler.setAcceptedAngleScore(60.0)
        contourDetectorFrameHandler.setAcceptedSizeScore(75.0)
        contourDetectorFrameHandler.addResultHandler(polygonView.contourDetectorResultHandler)
        contourDetectorFrameHandler.addResultHandler(this)

        autoSnappingController = DocumentAutoSnappingController.attach(cameraView, contourDetectorFrameHandler)
        autoSnappingController.setIgnoreBadAspectRatio(ignoreBadAspectRatio)

        // Please note: https://github.com/doo/Scanbot-SDK-Examples/wiki/Autosnapping#sensitivity
        autoSnappingController.setSensitivity(0.85f)
        cameraView.addPictureCallback(object : PictureCallback() {
            override fun onPictureTaken(image: ByteArray, imageOrientation: Int) {
                processPictureTaken(image, imageOrientation)
            }
        })
        userGuidanceHint = view.findViewById(R.id.userGuidanceHint)

        shutterButton = view.findViewById(R.id.shutterButton)
        shutterButton.setOnClickListener { cameraView.takePicture(false) }
        shutterButton.visibility = View.VISIBLE

        view.findViewById<View>(R.id.scan_doc_btn_flash).setOnClickListener {
            flashEnabled = !flashEnabled
            cameraView.useFlash(flashEnabled)
        }
        view.findViewById<View>(R.id.scan_doc_btn_cancel).setOnClickListener {
            //if fragment opened from Edit Scan Fragment then on cancel click it should go to that fragment
            if (calledFrom == EditScannedDocumentFragment.TAG) {
                openEditScanFragment()
            } else {
                //else default behaviour
                (requireActivity() as ScanActivity).onBackPressed()
            }
        }

        autoSnappingToggleButton = view.findViewById(R.id.scan_doc_btn_automatic)
        autoSnappingToggleButton.setOnClickListener {
            autoSnappingEnabled = !autoSnappingEnabled
            setAutoSnapEnabled(autoSnappingEnabled)
        }
        autoSnappingToggleButton.post { setAutoSnapEnabled(autoSnappingEnabled) }
    }

    private fun askPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager
                .PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(), arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun initOCR() {
        opticalCharacterRecognizer = scanbotSDK.ocrRecognizer()
        pageFileStorage = scanbotSDK.pageFileStorage()
        pageProcessor = scanbotSDK.pageProcessor()
    }

    override fun onResume() {
        super.onResume()
        cameraView.onResume()
    }

    override fun onPause() {
        super.onPause()
        cameraView.onPause()
    }

    override fun handle(result: FrameHandlerResult<ContourDetectorFrameHandler.DetectedFrame, SdkLicenseError>): Boolean {
        // Here you are continuously notified about contour detection results.
        // For example, you can show a user guidance text depending on the current detection status.
        //don't update the text if fragment is removing
        if (!isRemoving) {
            userGuidanceHint.post {
                if (result is FrameHandlerResult.Success<*>) {
                    showUserGuidance((result as FrameHandlerResult.Success<ContourDetectorFrameHandler.DetectedFrame>).value.detectionResult)
                }
            }
        }
        return false // typically you need to return false
    }

    private fun showUserGuidance(result: DetectionResult) {
        if (!autoSnappingEnabled) {
            return
        }
        if (System.currentTimeMillis() - lastUserGuidanceHintTs < 400) {
            return
        }

        // Make sure to reset the default polygon fill color (see the ignoreBadAspectRatio case).
        //polygonView.setFillColor(POLYGON_FILL_COLOR)
        when (result) {
            DetectionResult.OK -> {
                userGuidanceHint.text = resources.getString(R.string.result_scan_doc_dont_move)
                userGuidanceHint.visibility = View.VISIBLE
            }
            DetectionResult.OK_BUT_TOO_SMALL -> {
                userGuidanceHint.text = resources.getString(R.string.result_scan_doc_move_closer)
                userGuidanceHint.visibility = View.VISIBLE
            }
            DetectionResult.OK_BUT_BAD_ANGLES -> {
                userGuidanceHint.text = resources.getString(R.string.result_scan_doc_perspective)
                userGuidanceHint.visibility = View.VISIBLE
            }
            DetectionResult.ERROR_NOTHING_DETECTED -> {
                userGuidanceHint.text = resources.getString(R.string.result_scan_doc_no_doc)
                userGuidanceHint.visibility = View.VISIBLE
            }
            DetectionResult.ERROR_TOO_NOISY -> {
                userGuidanceHint.text = resources.getString(R.string.result_scan_doc_bg_noisy)
                userGuidanceHint.visibility = View.VISIBLE
            }
            DetectionResult.OK_BUT_BAD_ASPECT_RATIO -> {
                if (ignoreBadAspectRatio) {
                    userGuidanceHint.text = resources.getString(R.string.result_scan_doc_dont_move)
                    // change polygon color to "OK"
                    // polygonView.setFillColor(POLYGON_FILL_COLOR_OK)
                } else {
                    userGuidanceHint.text = resources.getString(R.string.result_scan_doc_aspect_ratio)
                }
                userGuidanceHint.visibility = View.VISIBLE
            }
            DetectionResult.ERROR_TOO_DARK -> {
                userGuidanceHint.text = resources.getString(R.string.result_scan_doc_poor_light)
                userGuidanceHint.visibility = View.VISIBLE
            }
            else -> userGuidanceHint.visibility = View.GONE
        }
        lastUserGuidanceHintTs = System.currentTimeMillis()
    }

    private fun processPictureTaken(image: ByteArray, imageOrientation: Int) {
        requireActivity().runOnUiThread {
            cameraView.onPause()
            progressBar.visibility = View.VISIBLE
            //cameraView.visibility = View.GONE
        }
        // Here we get the full image from the camera.
        // Please see https://github.com/doo/Scanbot-SDK-Examples/wiki/Handling-camera-picture
        // This is just a demo showing the detected document image as a downscaled(!) preview image.

        // Decode Bitmap from bytes of original image:
        val options = BitmapFactory.Options()
        // Please note: In this simple demo we downscale the original image to 1/8 for the preview!
        //options.inSampleSize = 8
        // Typically you will need the full resolution of the original image! So please change the "inSampleSize" value to 1!
        options.inSampleSize = 1
        var originalBitmap = BitmapFactory.decodeByteArray(image, 0, image.size, options)

        // Rotate the original image based on the imageOrientation value.
        // Required for some Android devices like Samsung!
        if (imageOrientation > 0) {
            val matrix = Matrix()
            matrix.setRotate(imageOrientation.toFloat(), originalBitmap.width / 2f, originalBitmap.height / 2f)
            originalBitmap = Bitmap.createBitmap(
                originalBitmap,
                0,
                0,
                originalBitmap.width,
                originalBitmap.height,
                matrix,
                false
            )
        }
        val detector = scanbotSDK.contourDetector()
        // Run document detection on original image:
        detector.detect(originalBitmap)
        val operations: MutableList<Operation> = ArrayList()
        operations.add(CropOperation(detector.polygonF!!))
        val documentImage = scanbotSDK.imageProcessor().processBitmap(originalBitmap, operations, false)

        //  val file = saveImage(documentImage)
        // Log.d("SCANNING","File : $file")
        if (documentImage != null) {
            onDocScanListener.addScannedDoc(documentImage)
           // onDocScanListener.addScannedDoc(FileUtils.saveImage(requireContext(), documentImage, null))
            openEditScanFragment()

            /*  uiScope.launch {
                  recognizeTextWithoutPDFTask(documentImage)
              }*/
        }
        // RecognizeTextWithoutPDFTask(documentImage).execute()

        //resultView.post { resultView.setImageBitmap(documentImage) }

        // continue scanning
/*        cameraView.postDelayed({
            cameraView.continuousFocus()
            cameraView.startPreview()
        }, 1000)*/
    }

    private fun openEditScanFragment() {
        onFragmentChangeListener.onReplaceFragment(
            EditScannedDocumentFragment.newInstance(onDocScanListener.scannedDocs.size-1), ScanActivity
                .FRAGMENT_EDIT_SCAN_TAG, false
        )
    }

    private fun setAutoSnapEnabled(enabled: Boolean) {
        autoSnappingController.isEnabled = enabled
        contourDetectorFrameHandler.isEnabled = enabled
        polygonView.visibility = if (enabled) View.VISIBLE else View.GONE
        autoSnappingToggleButton.text = resources.getString(R.string.automatic) + " ${
            if (enabled) "ON" else
                "OFF"
        }"
        if (enabled) {
            shutterButton.showAutoButton()
        } else {
            shutterButton.showManualButton()
            userGuidanceHint.visibility = View.GONE
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //permission is granted
                //Nothing to be done
            } else {
                //permission not granted
                for (permission in permissions) {
                    val showRationale = shouldShowRequestPermissionRationale(permission)
                    if (!showRationale) {
                        // user also CHECKED "never ask again"
                        // you can either enable some fall back,
                        // disable features of your app
                        // or open another dialog explaining
                        // again the permission and directing to
                        // the app setting
                        Toast.makeText(
                            requireContext(), "Please navigate to App info in settings and give permission " +
                                "manually.", Toast.LENGTH_LONG
                        ).show()
                    } else if (Manifest.permission.CAMERA == permission || Manifest.permission.READ_EXTERNAL_STORAGE == permission
                        || Manifest.permission.WRITE_EXTERNAL_STORAGE == permission
                    ) {
                        // user did NOT check "never ask again"
                        // this is a good place to explain the user
                        // why you need the permission and ask if he wants
                        // to accept it (the rationale)
                        Toast.makeText(
                            requireContext(),
                            "You cannot scan document without camera permission.",
                            Toast.LENGTH_SHORT
                        ).show()
                        requireActivity().finish()
                        // askPermission()
                    }
                    // else if ( /* possibly check more permissions...*/) {
                    // }
                }
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private suspend fun recognizeTextWithoutPDFTask(bitmap: Bitmap) {
        withContext(Dispatchers.Default) {
            val ocrResult: OcrResult?
            try {
                //val bitmap = loadImage()
                val newPageId = pageFileStorage.add(bitmap)
                val page = Page(newPageId, emptyList(), DetectionResult.OK, ImageFilterType.BINARIZED)

                val processedPage = pageProcessor.detectDocument(page)

                val pages = listOf(processedPage)
                val languages = setOf(Language.ENG)

                // opticalCharacterRecognizer.recognizeTextFromPages(pages, languages)

                ocrResult = opticalCharacterRecognizer.recognizeTextWithPdfFromPages(pages, PDFPageSize.A4, languages)
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE

                ocrResult.let {
                    Log.d("SCANNING", "Scanning done")
                    if (it.ocrPages.isNotEmpty()) {
                        Log.d("Main Activity", it.recognizedText)

                        // Toast.makeText(this@MainActivity,
                        //     """
                        //         Recognized page content:
                        //         ${it.recognizedText}
                        //         """.trimIndent(),
                        //     Toast.LENGTH_LONG).show()

                        // bounding boxes and text results of recognized paragraphs, lines and words:
                        /* for(ocr in it.ocrPages){
                             Log.d("Main Activity","Lines: ${ocr.lines}")
                             Log.d("Main Activity","Para: ${ocr.paragraphs}")
                             Log.d("Main Activity","Words: ${ocr.words}")
                         }*/
                    }

                }

                ocrResult.sandwichedPdfDocumentFile?.let { file ->

                    Log.d("Main Activity", file.path)

                    /*val intent = Intent()
                    intent.putExtra(EXTRA_SCAN_DOCUMENT_PATH, file.path)
                    requireActivity().setResult(Activity.RESULT_OK, intent)
                    requireActivity().finish()*/

                }
            }
        }
    }

    companion object {
        private val POLYGON_FILL_COLOR = Color.parseColor("#55ff0000")
        private val POLYGON_FILL_COLOR_OK = Color.parseColor("#4400ff00")
        private const val CAMERA_PERMISSION_REQUEST_CODE: Int = 811

        @JvmStatic
        val EXTRA_SCAN_DOCUMENT_PATH = ScanDocumentFragment::class.java.canonicalName +
            ".EXTRA_SCAN_DOCUMENT_PATH"

        @JvmStatic
        val ARG_CALLED_FROM = "arg called_From"

        /* @JvmStatic
         fun startScanDocumentActivityForResult(activity: Activity, requestCode: Int) {
             val intent = Intent(activity, ScanDocumentFragment::class.java)
             activity.startActivityForResult(intent, requestCode)
         }*/
        @JvmStatic
        fun newInstance(calledFrom: String): ScanDocumentFragment {
            val args = Bundle()
            args.putString(ARG_CALLED_FROM, calledFrom)
            val fragment = ScanDocumentFragment()
            fragment.arguments = args
            return fragment
        }
    }
}