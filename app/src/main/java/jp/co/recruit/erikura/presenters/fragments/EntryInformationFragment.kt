package jp.co.recruit.erikura.presenters.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.databinding.FragmentEntryInformationBinding

class EntryInformationFragment : BaseJobDetailFragment {
    companion object {
        fun newInstance(user: User?): EntryInformationFragment {
            return EntryInformationFragment().also {
                it.arguments = Bundle().also { args ->
                    fillArguments(args, user)
                }
            }
        }
    }

    constructor(): super()

    private val viewModel: EntryInformationViewModel by lazy {
        ViewModelProvider(this).get(EntryInformationViewModel::class.java)
    }

    override fun refresh(job: Job?, user: User?) {
        super.refresh(job, user)
        viewModel.job.value = job
        viewModel.user.value = user
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentEntryInformationBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = activity
        binding.viewModel = viewModel
        viewModel.job.value = job
        viewModel.user.value = user

        return binding.root
    }

    class EntryInformationViewModel: ViewModel() {
        val job = MutableLiveData<Job>()
        val user = MutableLiveData<User>()

        val entryInformationVisible = MediatorLiveData<Int>().also { result ->
            result.addSource(job) {
                result.value = decideEntryInformationVisibility()
            }
            result.addSource(user) {
                result.value = decideEntryInformationVisibility()
            }
        }

        private fun decideEntryInformationVisibility(): Int {
            return job.value?.let { job ->
                if (job.isEntried && job.isOwner && !job.isReported && (job.entryInformation ?: "").isNotBlank()) {
                    return View.VISIBLE
                }
                else {
                    return View.GONE
                }
            } ?: View.GONE
        }
    }
}
