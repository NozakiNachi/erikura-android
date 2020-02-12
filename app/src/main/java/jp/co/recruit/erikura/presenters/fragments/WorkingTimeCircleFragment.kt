package jp.co.recruit.erikura.presenters.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.databinding.FragmentWorkingTimeCircleBinding

class WorkingTimeCircleFragment(private val job: Job?) : Fragment(), WorkingTimeCircleFragmentEventHandlers {
    private val viewModel: WorkingTimeCircleFragmentViewModel by lazy {
        ViewModelProvider(this).get(WorkingTimeCircleFragmentViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        container?.removeAllViews()
        val binding = FragmentWorkingTimeCircleBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handler = this
        return binding.root
    }

    override fun onClickCircle(view: View) {
        // FIXME: 該当の案件詳細画面へ遷移
    }
}

class WorkingTimeCircleFragmentViewModel: ViewModel() {
    val workingTime: MutableLiveData<String> = MutableLiveData("0分0秒")
}

interface WorkingTimeCircleFragmentEventHandlers {
    fun onClickCircle(view: View)
}
