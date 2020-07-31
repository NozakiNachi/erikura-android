package jp.co.recruit.erikura.presenters.fragments

import android.content.Intent
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.FragmentPropertyNotesButtonBinding
import jp.co.recruit.erikura.databinding.FragmentReportExamplesButtonBinding

class ReportExamplesButtonFragment : BaseJobDetailFragment, ReportExamplesButtonFragmentEventHandlers {
    companion object {
        fun newInstance(job: Job?, user: User?): ReportExamplesButtonFragment {
            return ReportExamplesButtonFragment().also {
                it.arguments = Bundle().also { args ->
                    fillArguments(args, job, user)
                }
            }
        }
    }

    constructor(): super()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentReportExamplesButtonBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = activity
        binding.handler = this
        return binding.root
    }

    override fun onClickReportExamples(view: View) {
        job?.let { job ->
            val intent = Intent(activity, jp.co.recruit.erikura.presenters.activities.report.ReportExamplesActivity::class.java)
            intent.putExtra("job", job)
            startActivity(intent)
        }
    }
}

interface ReportExamplesButtonFragmentEventHandlers {
    fun onClickReportExamples(view: View)
}