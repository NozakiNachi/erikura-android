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
import jp.co.recruit.erikura.R
import androidx.appcompat.app.AppCompatActivity


class ReportedJobStatusFragment (private val activity: AppCompatActivity, val report: Report,val reportStatus: ReportStatus) : Fragment() {
    private val viewModel: ReportedJobStatusFragmentViewModel by lazy {
        ViewModelProvider(this).get(ReportedJobStatusFragmentViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        container?.removeAllViews()
        val binding = FragmentReportedJobStatusBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = activity
        viewModel.setup(activity, report, reportStatus)
        binding.viewModel = viewModel
        return binding.root
    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri) {
        listener?.onFragmentInteraction(uri)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }
}

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ReportedJobStatusFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ReportedJobStatusFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}

class ReportedJobStatusFragmentViewModel: ViewModel() {

    var reportStatus: ReportStatus = Report.status
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

    val acceptedVisible: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val rejectedVisible: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val unconfirmedVisible: MutableLiveData<Int> = MutableLiveData(View.GONE)
}
