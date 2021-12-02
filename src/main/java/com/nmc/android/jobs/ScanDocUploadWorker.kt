package com.nmc.android.jobs

import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.text.TextUtils
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.nextcloud.client.account.UserAccountManager
import com.nmc.android.ui.SaveScannedDocumentFragment
import com.nmc.android.ui.ScanActivity
import com.nmc.android.utils.FileUtils
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.files.services.FileUploader
import com.owncloud.android.files.services.NameCollisionPolicy
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.ui.notifications.NotificationUtils
import com.owncloud.android.utils.StringUtils
import com.owncloud.android.utils.theme.ThemeColorUtils
import io.scanbot.sdk.ScanbotSDK
import io.scanbot.sdk.core.contourdetector.DetectionResult
import io.scanbot.sdk.entity.Language
import io.scanbot.sdk.ocr.OpticalCharacterRecognizer
import io.scanbot.sdk.ocr.process.OcrResult
import io.scanbot.sdk.persistence.Page
import io.scanbot.sdk.persistence.PageFileStorage
import io.scanbot.sdk.process.PDFPageSize
import io.scanbot.sdk.process.PDFRenderer
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.encryption.AccessPermission
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.security.SecureRandom

class ScanDocUploadWorker constructor(
    private val context: Context,
    params: WorkerParameters,
    private val notificationManager: NotificationManager,
    private val accountManager: UserAccountManager
) : Worker(context, params) {

    private lateinit var scanbotSDK: ScanbotSDK
    private lateinit var pdfRenderer: PDFRenderer
    private lateinit var pageFileStorage: PageFileStorage
    private lateinit var opticalCharacterRecognizer: OpticalCharacterRecognizer
    private val savedFiles = mutableListOf<String>()

    companion object {
        const val TAG = "ScanDocUploadWorkerJob"
        const val DATA_REMOTE_PATH = "data_remote_path"
        const val DATA_SCAN_FILE_TYPES = "data_scan_file_types"
        const val DATA_SCAN_PDF_PWD = "data_scan_pdf_pwd"
        const val DATA_DOC_FILE_NAME = "data_doc_file_name"
        const val IMAGE_COMPRESSION_PERCENTAGE = 85
    }

    override fun doWork(): Result {
        initScanBotSDK()
        val remoteFolderPath = inputData.getString(DATA_REMOTE_PATH)
        val scanDocFileTypes = inputData.getString(DATA_SCAN_FILE_TYPES)
        val scanDocPdfPwd = inputData.getString(DATA_SCAN_PDF_PWD)
        val docFileName = inputData.getString(DATA_DOC_FILE_NAME)

        val fileTypes = StringUtils.convertStringToList(scanDocFileTypes)
        val bitmapList: List<Bitmap> = ScanActivity.filteredImages

        val randomId = SecureRandom()
        val pushNotificationId = randomId.nextInt()
        showNotification(pushNotificationId)

        for (type in fileTypes) {
            when (type) {
                SaveScannedDocumentFragment.SAVE_TYPE_JPG -> {
                    saveJPGImageFiles(docFileName, bitmapList)
                }
                SaveScannedDocumentFragment.SAVE_TYPE_PNG -> {
                    savePNGImageFiles(docFileName, bitmapList)
                }
                SaveScannedDocumentFragment.SAVE_TYPE_PDF -> {
                    saveNonOCRPDFFile(docFileName, bitmapList, scanDocPdfPwd)
                }
                SaveScannedDocumentFragment.SAVE_TYPE_PDF_OCR -> {
                    savePDFWithOCR(docFileName, bitmapList, scanDocPdfPwd)
                }
                SaveScannedDocumentFragment.SAVE_TYPE_TXT -> {
                    saveTextFile(docFileName, bitmapList)
                }
            }
        }
        notificationManager.cancel(pushNotificationId)

        uploadScannedDocs(remoteFolderPath)

        return Result.success()
    }

    private fun showNotification(pushNotificationId: Int) {
        val notificationBuilder =
            NotificationCompat.Builder(context, NotificationUtils.NOTIFICATION_CHANNEL_SCAN_DOC_SAVE)
                .setSmallIcon(R.drawable.notification_icon)
                .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.notification_icon))
                .setColor(ThemeColorUtils.primaryColor(context, true))
                .setContentTitle(context.resources.getString(R.string.app_name))
                .setContentText(context.resources.getString(R.string.foreground_service_save))
                .setAutoCancel(false)

        notificationManager.notify(pushNotificationId, notificationBuilder.build())
    }

    private fun initScanBotSDK() {
        scanbotSDK = ScanbotSDK(context)
        pdfRenderer = scanbotSDK.pdfRenderer()
        pageFileStorage = scanbotSDK.pageFileStorage()
        opticalCharacterRecognizer = scanbotSDK.ocrRecognizer()
    }

    private fun saveJPGImageFiles(fileName: String?, bitmapList: List<Bitmap>) {
        for (i in bitmapList.indices) {
            var newFileName = fileName
            val bitmap = bitmapList[i]
            if (i > 0) {
                newFileName += "($i)"
            }

            val jpgFile = FileUtils.saveJpgImage(context, bitmap, newFileName, IMAGE_COMPRESSION_PERCENTAGE)
            savedFiles.add(jpgFile.path)
        }
    }

    private fun savePNGImageFiles(fileName: String?, bitmapList: List<Bitmap>) {
        for (i in bitmapList.indices) {
            var newFileName = fileName
            val bitmap = bitmapList[i]
            if (i > 0) {
                newFileName += "($i)"
            }

            val pngFile = FileUtils.savePngImage(context, bitmap, newFileName, IMAGE_COMPRESSION_PERCENTAGE)
            savedFiles.add(pngFile.path)
        }
    }

    private fun saveNonOCRPDFFile(fileName: String?, bitmapList: List<Bitmap>, pdfPassword: String?) {

        val pageList = getScannedPages(bitmapList)
        val pdfFile: File? = pdfRenderer.renderDocumentFromPages(pageList, PDFPageSize.A4)
        if (pdfFile != null) {
            val renamedFile = File(pdfFile.parent + OCFile.PATH_SEPARATOR + fileName + ".pdf")
            if (pdfFile.renameTo(renamedFile)) {
                Log_OC.d(TAG, "File successfully renamed")
            }
            savePdfFile(pdfPassword, renamedFile)
        }
    }

    /**
     * save pdf file if pdf password is set else add it to list
     */
    private fun savePdfFile(pdfPassword: String?, renamedFile: File) {
        if (!TextUtils.isEmpty(pdfPassword)) {
            pdfWithPassword(renamedFile, pdfPassword)
        } else {
            savedFiles.add(renamedFile.path)
        }
    }

    private fun pdfWithPassword(pdfFile: File, pdfPassword: String?) {
        try {
            val document: PDDocument = PDDocument.load(pdfFile)
            //Creating access permission object
            val ap = AccessPermission()
            //Creating StandardProtectionPolicy object
            val spp = StandardProtectionPolicy(pdfPassword, pdfPassword, ap)
            //Setting the length of the encryption key
            spp.encryptionKeyLength = 128
            //Setting the access permissions
            spp.permissions = ap
            //Protecting the document
            document.protect(spp)

            //save the encrypted pdf file
            val os = FileOutputStream(pdfFile)
            document.save(os)

            //close the document
            document.close()

            //add the file to list
            savedFiles.add(pdfFile.path)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
    }

    private fun getScannedPages(bitmapList: List<Bitmap>): List<Page> {
        val pageList: MutableList<Page> = ArrayList()
        for (bitmap in bitmapList) {
            val page = Page(pageFileStorage.add(bitmap), emptyList(), DetectionResult.OK)
            pageList.add(page)
        }
        return pageList
    }

    private fun savePDFWithOCR(fileName: String?, bitmapList: List<Bitmap>, pdfPassword: String?) {
        val languages = setOf(Language.ENG)
        val ocrResult: OcrResult =
            opticalCharacterRecognizer.recognizeTextWithPdfFromPages(
                getScannedPages(bitmapList),
                PDFPageSize.A4,
                languages
            )
        val ocrPageList: List<OcrResult.OCRPage> = ocrResult.ocrPages
        if (ocrPageList.isNotEmpty()) {
            val ocrText = ocrResult.recognizedText
        }
        val ocrPDFFile = ocrResult.sandwichedPdfDocumentFile
        if (ocrPDFFile != null) {
            val renamedFile = File(ocrPDFFile.parent + OCFile.PATH_SEPARATOR + fileName + "_OCR.pdf")
            if (ocrPDFFile.renameTo(renamedFile)) {
                Log_OC.d(TAG, "OCR File successfully renamed")
            }
            savePdfFile(pdfPassword, renamedFile)
        }
    }

    private fun saveTextFile(fileName: String?, bitmapList: List<Bitmap>) {
        val languages = setOf(Language.ENG)
        for (i in bitmapList.indices) {
            var newFileName = fileName
            val bitmap = bitmapList[i]
            if (i > 0) {
                newFileName += "($i)"
            }
            val page = Page(pageFileStorage.add(bitmap), emptyList(), DetectionResult.OK)
            val pageList: MutableList<Page> = ArrayList()
            pageList.add(page)
            val ocrResult: OcrResult = opticalCharacterRecognizer.recognizeTextFromPages(pageList, languages)
            val ocrPageList: List<OcrResult.OCRPage> = ocrResult.ocrPages
            if (ocrPageList.isNotEmpty()) {
                val ocrText = ocrResult.recognizedText
                val txtFile = FileUtils.writeTextToFile(context, ocrText, newFileName)
                savedFiles.add(txtFile.path)
            }
        }
    }

    private fun uploadScannedDocs(remotePathBase: String?) {
        val remotePaths = arrayOfNulls<String>(savedFiles.size)
        for (j in remotePaths.indices) {
            remotePaths[j] = remotePathBase + File(savedFiles[j]).name
        }

        FileUploader.uploadNewFile(
            context,
            accountManager.currentAccount,
            savedFiles.toTypedArray(),
            remotePaths,
            null,  // MIME type will be detected from file name
            FileUploader.LOCAL_BEHAVIOUR_DELETE,
            false,  // do not create parent folder if not existent
            UploadFileOperation.CREATED_BY_USER,
            false,
            false,
            NameCollisionPolicy.RENAME
        )
    }
}
