package jp.co.recruit.erikura.presenters.fragments

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job

class NomalJobDetailsFragment(val job: Job?) : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_nomal_job_details, container, false)
    }

}
