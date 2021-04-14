package jp.co.recruit.erikura.presenters.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.databinding.FragmentPreEntryFlowViewBinding
import java.text.SimpleDateFormat


class PreEntryFlowViewFragment() : BaseJobDetailFragment() {
    companion object {
        fun newInstance(user: User?): PreEntryFlowViewFragment {
            return PreEntryFlowViewFragment().also {
                it.arguments = Bundle().also { args ->
                    fillArguments(args, user)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val preEntryFlowView: View = inflater.inflate(R.layout.fragment_pre_entry_flow_view, container, false)
        job?.workingStartAt?.let { startAt ->
            // Inflate the layout for this fragment
            val caption1 = preEntryFlowView.findViewById<TextView>(R.id.preEntryFlowCaption1)
            caption1?.text = (ErikuraApplication.instance.getWorkingDay(startAt) + ErikuraApplication.instance.getString(R.string.jobDetails_preEntryFlowCaption1))
        }
        return preEntryFlowView
    }
}