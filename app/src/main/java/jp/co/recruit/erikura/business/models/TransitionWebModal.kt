package jp.co.recruit.erikura.business.models

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.Button
import androidx.core.os.bundleOf
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.data.network.Api

class TransitionWebModal {
    companion object {
        fun transitionWebModal(view: View, context: Context, job: Job?, user: User?) {
            val dialog = AlertDialog.Builder(context)
                .setView(R.layout.dialog_confirm_transition_web)
                .setCancelable(false)
                .create()
            dialog.show()
            val button: Button = dialog.findViewById(R.id.open_button)
            button.setOnClickListener(View.OnClickListener {
                //開く場合
                dialog.dismiss()
                Api(context).createToken() { token ->
                    //カスタマWebの作業報告編集画面を開く
                    val jobReportURLString =
                        ErikuraConfig.jobReportURLString(job?.id, token)
                    Uri.parse(jobReportURLString)?.let { uri ->
                        try {
                            Tracking.logEvent(event = "push_web_report", params = bundleOf())
                            Tracking.trackWebReport(
                                name = "push_web_report",
                                job_kind_id = job?.jobKind?.id ?: 0,
                                user_id = user?.id ?: 0
                            )
                            Intent(Intent.ACTION_VIEW, uri).let { intent ->
                                intent.setPackage("com.android.chrome")
                                context.startActivity(intent)
                            }
                        } catch (e: ActivityNotFoundException) {
                            Intent(Intent.ACTION_VIEW, uri).let { intent ->
                                context.startActivity(intent)
                            }
                        }
                    }
                }
            })
            val cancelButton: Button = dialog.findViewById(R.id.cancel_button)
            cancelButton.setOnClickListener(View.OnClickListener {
                //キャンセル場合
                dialog.dismiss()
            })
        }
    }
}
