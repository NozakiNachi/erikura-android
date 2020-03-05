package jp.co.recruit.erikura.presenters.fragments

import android.app.ActivityOptions
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.databinding.FragmentReportedJobEditButtonBinding
import jp.co.recruit.erikura.presenters.activities.report.ReportConfirmActivity




class ReportedJobEditButtonFragment(val job: Job?) : Fragment(), ReportedJobEditButtonFragmentEventHandlers {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentReportedJobEditButtonBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = activity
        binding.handler = this
        return binding.root
    }

    override fun onClickReportedJobEditButton(view: View) {
        //FIXME: 納期前と後の分岐処理を追加する
        if(job?.isReportEditable?: false) {
            val intent = Intent(activity,ReportConfirmActivity::class.java)
            intent.putExtra("job",job)
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(activity).toBundle())
         }
    }
}

interface ReportedJobEditButtonFragmentEventHandlers {
    fun onClickReportedJobEditButton(view: View)
}
