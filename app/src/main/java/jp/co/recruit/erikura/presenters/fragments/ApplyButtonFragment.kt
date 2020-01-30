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
import jp.co.recruit.erikura.databinding.FragmentApplyButtonBinding

class ApplyButtonFragment(val job: Job?) : Fragment(), ApplyButtonFragmentEventHandlers {
    private val viewModel: ApplyButtonFragmentViewModel by lazy {
        ViewModelProvider(this).get(ApplyButtonFragmentViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentApplyButtonBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.handlers = this
        return binding.root
    }

    override fun onClickFavorite(view: View) {

    }

    override fun onClickApply(view: View) {

    }

}

class ApplyButtonFragmentViewModel: ViewModel() {
    val applyButtonVisibility: MutableLiveData<Int> = MutableLiveData(View.VISIBLE)
    val favrited: MutableLiveData<Boolean> = MutableLiveData(false)
}

interface ApplyButtonFragmentEventHandlers {
    fun onClickFavorite(view: View)
    fun onClickApply(view: View)
}
