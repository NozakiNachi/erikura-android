package jp.co.recruit.erikura.presenters.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Place
import jp.co.recruit.erikura.databinding.FragmentJobsListBinding

class JobsListFragment(val place: Place) : Fragment() {
    private val viewModel: JobsListFragmentViewModel by lazy {
        ViewModelProvider(this).get(JobsListFragmentViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentJobsListBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        return inflater.inflate(R.layout.fragment_jobs_list, container, false)
    }

}

class JobsListFragmentViewModel: ViewModel() {
    val activeListVisible: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val futureListVisible: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val pastListVisible: MutableLiveData<Int> = MutableLiveData(View.GONE)
}
