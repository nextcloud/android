package com.nextcloud.client.diagnostics

import android.os.Bundle
import android.widget.Button
import com.owncloud.android.R
import kotlinx.android.synthetic.main.acra_crash_report_dialog.*
import org.acra.dialog.BaseCrashReportDialog

class CrashAcraDialog : BaseCrashReportDialog() {

    private lateinit var ok: Button
    private lateinit var cancel: Button

    override fun init(savedInstanceState: Bundle?) {
        super.init(savedInstanceState)
        setContentView(R.layout.acra_crash_report_dialog)

        ok = acra_crash_report_dialog_ok
        cancel = acra_crash_report_dialog_cancel

        ok.setOnClickListener { sendCrash(null, null) }
        cancel.setOnClickListener { cancelReports() }
    }

}
