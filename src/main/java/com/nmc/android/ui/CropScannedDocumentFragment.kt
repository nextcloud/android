package com.nmc.android.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PointF
import android.os.AsyncTask
import android.os.Bundle
import android.util.Pair
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import butterknife.Unbinder
import com.google.android.material.button.MaterialButton
import com.nmc.android.interfaces.OnDocScanListener
import com.nmc.android.interfaces.OnFragmentChangeListener
import com.nmc.android.utils.ScanBotSdkUtils
import com.owncloud.android.R
import io.scanbot.sdk.ScanbotSDK
import io.scanbot.sdk.core.contourdetector.DetectionResult
import io.scanbot.sdk.core.contourdetector.Line2D
import io.scanbot.sdk.process.CropOperation
import io.scanbot.sdk.ui.EditPolygonImageView
import io.scanbot.sdk.ui.MagnifierView
import java.util.concurrent.Executors

class CropScannedDocumentFragment : Fragment() {
    private lateinit var unbinder: Unbinder
    private lateinit var onFragmentChangeListener: OnFragmentChangeListener
    private lateinit var onDocScanListener: OnDocScanListener

    @BindView(R.id.crop_polygon_view)
    lateinit var editPolygonImageView: EditPolygonImageView

    @BindView(R.id.magnifier)
    lateinit var magnifierView: MagnifierView

    @BindView(R.id.crop_btn_reset_borders)
    lateinit var resetBordersButton: MaterialButton

    private var scannedDocIndex: Int = -1
    private lateinit var scanbotSDK: ScanbotSDK

    private lateinit var originalBitmap: Bitmap

    private var rotationDegrees = 0
    private var polygonPoints : List<PointF>? = null

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getInt(ARG_SCANNED_DOC_INDEX)?.let {
            scannedDocIndex = it
        }
        // Fragment locked in portrait screen orientation
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        run {
            try {
                onFragmentChangeListener = context as OnFragmentChangeListener
                onDocScanListener = context as OnDocScanListener
            } catch (ignored: Exception) {
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (requireActivity() is ScanActivity) {
            (requireActivity() as ScanActivity).showHideToolbar(true)
            (requireActivity() as ScanActivity).showHideDefaultToolbarDivider(true)
            (requireActivity() as ScanActivity).updateActionBarTitleAndHomeButtonByString(resources.getString(R.string.title_crop_scan))
        }
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_crop_scan, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        unbinder = ButterKnife.bind(this, view)
        scanbotSDK = (requireActivity() as ScanActivity).scanbotSDK
        detectDocument()
    }

    @OnClick(R.id.crop_btn_reset_borders)
    fun onClickListener(view: View) {
        when (view.id) {
            R.id.crop_btn_reset_borders -> {
                if (resetBordersButton.tag.equals(resources.getString(R.string.crop_btn_reset_crop_text))) {
                    updateButtonText(resources.getString(R.string.crop_btn_detect_doc_text))
                    resetCrop()
                } else if (resetBordersButton.tag.equals(resources.getString(R.string.crop_btn_detect_doc_text))) {
                    updateButtonText(resources.getString(R.string.crop_btn_reset_crop_text))
                    detectDocument()
                }
            }
        }
    }

    private fun updateButtonText(label: String) {
        resetBordersButton.tag = label
        resetBordersButton.text = label
    }

    private fun resetCrop() {
        polygonPoints = getResetPolygons()
        editPolygonImageView.polygon = getResetPolygons()
    }

    private fun getResetPolygons(): List<PointF> {
        val polygonList = mutableListOf<PointF>()
        val pointF = PointF(0.0f, 0.0f)
        val pointF1 = PointF(1.0f, 0.0f)
        val pointF2 = PointF(1.0f, 1.0f)
        val pointF3 = PointF(0.0f, 1.0f)
        polygonList.add(pointF)
        polygonList.add(pointF1)
        polygonList.add(pointF2)
        polygonList.add(pointF3)
        return  polygonList
    }

    private fun detectDocument() {
        InitImageViewTask().executeOnExecutor(Executors.newSingleThreadExecutor())
    }

    // We use AsyncTask only for simplicity here. Avoid using it in your production app due to memory leaks, etc!
    internal inner class InitImageViewTask : AsyncTask<Void?, Void?, InitImageResult>() {
        private var previewBitmap: Bitmap? = null

        override fun doInBackground(vararg params: Void?): InitImageResult {
            //originalBitmap = FileUtils.convertFileToBitmap(File(scannedDocPath))
            originalBitmap = onDocScanListener.scannedDocs[scannedDocIndex]
            previewBitmap = ScanBotSdkUtils.resizeForPreview(originalBitmap)

            val detector = scanbotSDK.contourDetector()
            val detectionResult = detector.detect(originalBitmap)
            val linesPair = Pair(detector.horizontalLines, detector.verticalLines)
            val polygon = detector.polygonF

            return when (detectionResult) {
                DetectionResult.OK,
                DetectionResult.OK_BUT_BAD_ANGLES,
                DetectionResult.OK_BUT_TOO_SMALL,
                DetectionResult.OK_BUT_BAD_ASPECT_RATIO -> {
                    InitImageResult(linesPair, polygon!!)
                }
                else -> InitImageResult(Pair(listOf(), listOf()), listOf())
            }
        }

        override fun onPostExecute(initImageResult: InitImageResult) {
            editPolygonImageView.setImageBitmap(previewBitmap)
            magnifierView.setupMagnifier(editPolygonImageView)

            // set detected polygon and lines into EditPolygonImageView
            polygonPoints = initImageResult.polygon
            editPolygonImageView.polygon = initImageResult.polygon
            editPolygonImageView.setLines(initImageResult.linesPair.first, initImageResult.linesPair.second)

            if (initImageResult.polygon.isNullOrEmpty()){
                resetCrop()
            }
        }
    }

    internal inner class InitImageResult(val linesPair: Pair<List<Line2D>, List<Line2D>>, val polygon: List<PointF>)

    private fun crop() {
        // crop & warp image by selected polygon (editPolygonView.getPolygon())
        val operations = listOf(CropOperation(editPolygonImageView.polygon))

        var documentImage = scanbotSDK.imageProcessor().processBitmap(originalBitmap, operations, false)
        documentImage?.let {
            if (rotationDegrees > 0) {
                // rotate the final cropped image result based on current rotation value:
                val matrix = Matrix()
                matrix.postRotate(rotationDegrees.toFloat())
                documentImage = Bitmap.createBitmap(it, 0, 0, it.width, it.height, matrix, true)
            }
            onDocScanListener.replaceScannedDoc(scannedDocIndex, documentImage)
            /* onDocScanListener.replaceScannedDoc(
                 scannedDocIndex, FileUtils.saveImage(
                     requireContext(),
                     documentImage, null
                 )
             )*/
            onFragmentChangeListener.onReplaceFragment(
                EditScannedDocumentFragment.newInstance(scannedDocIndex), ScanActivity
                    .FRAGMENT_EDIT_SCAN_TAG, false
            )
            // resultImageView.setImageBitmap(resizeForPreview(documentImage!!))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.edit_scan, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_save -> {
                crop()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unbinder.unbind()
    }

    fun getScannedDocIndex(): Int {
        return scannedDocIndex
    }

    companion object {
        private const val ARG_SCANNED_DOC_INDEX = "scanned_doc_index"

        @JvmStatic
        fun newInstance(index: Int): CropScannedDocumentFragment {
            val args = Bundle()
            args.putInt(ARG_SCANNED_DOC_INDEX, index)
            val fragment = CropScannedDocumentFragment()
            fragment.arguments = args
            return fragment
        }
    }
}