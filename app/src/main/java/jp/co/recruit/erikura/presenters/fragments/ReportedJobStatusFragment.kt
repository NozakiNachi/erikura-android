package jp.co.recruit.erikura.presenters.fragments

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.business.models.Report
import jp.co.recruit.erikura.business.models.ReportStatus
import jp.co.recruit.erikura.databinding.FragmentReportedJobStatusBinding
import androidx.appcompat.app.AppCompatActivity


class ReportedJobStatusFragment (private val activity: AppCompatActivity, val report: Report?) : Fragment() {
    private val viewModel: ReportedJobStatusFragmentViewModel by lazy {
        ViewModelProvider(this).get(ReportedJobStatusFragmentViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        container?.removeAllViews()
        if (report != null) {
            viewModel.reportStatus = report.status
            if (report.rejectComment != null) {
                viewModel.reportRejectComment = report.rejectComment
            }
        }
        val binding = FragmentReportedJobStatusBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = activity
        binding.viewModel = viewModel
        return binding.root
    }
}

class ReportedJobStatusFragmentViewModel: ViewModel() {
    var reportRejectComment: String? = null
    val acceptedVisible: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val rejectedVisible: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val unconfirmedVisible: MutableLiveData<Int> = MutableLiveData(View.GONE)
    var reportStatus: ReportStatus = ReportStatus.Unconfirmed
        set(reportStatus) {
            field = reportStatus
            when (field){
                ReportStatus.Accepted -> {
                    acceptedVisible.value = View.VISIBLE
                    rejectedVisible.value = View.GONE
                    unconfirmedVisible.value = View.GONE
                }
                ReportStatus.Rejected -> {
                    acceptedVisible.value = View.GONE
                    rejectedVisible.value = View.VISIBLE
                    unconfirmedVisible.value = View.GONE
                }
                ReportStatus.Unconfirmed -> {
                    acceptedVisible.value = View.GONE
                    rejectedVisible.value = View.GONE
                    unconfirmedVisible.value = View.VISIBLE
                }
            }
        }
}