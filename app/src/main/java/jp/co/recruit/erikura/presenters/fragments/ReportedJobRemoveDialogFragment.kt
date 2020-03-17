package jp.co.recruit.erikura.presenters.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import androidx.fragment.app.DialogFragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.databinding.DataBindingUtil
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.DialogReportedJobRemoveBinding
import jp.co.recruit.erikura.presenters.activities.job.JobDetailsActivity

class ReportedJobRemoveDialogFragment(private val job: Job?) : DialogFragment() , ReportedJobRemoveEventHandlers {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DataBindingUtil.inflate<DialogReportedJobRemoveBinding>(
            LayoutInflater.from(activity),
            R.layout.dialog_reported_job_remove,
            null,
            false
        )
        binding.lifecycleOwner = activity
        binding.handlers = this

        val builder = AlertDialog.Builder(activity)
        builder.setView(binding.root)
        return builder.create()
    }
    override fun onClickRemoveButton(view: View) {
        job?.report?.let {
            Api(activity!!).deleteReport(job.id) {
                val intent= Intent(activity, JobDetailsActivity::class.java)
                intent.putExtra("job", job)
                startActivity(intent)
                activity!!.finish()
            }
        }
    }
}

interface ReportedJobRemoveEventHandlers {
    fun onClickRemoveButton(view: View)
}