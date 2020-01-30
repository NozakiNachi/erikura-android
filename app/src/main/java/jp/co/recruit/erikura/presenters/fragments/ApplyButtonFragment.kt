package jp.co.recruit.erikura.presenters.fragments

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.UserSession
import jp.co.recruit.erikura.data.network.Api
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
        binding.lifecycleOwner = activity
        viewModel.setup(activity!!, job)
        binding.viewModel = viewModel
        binding.handlers = this
        return binding.root
    }

    override fun onClickFavorite(view: View) {
        if (viewModel.favorited.value?: false) {
            // お気に入り登録処理
            Api(activity!!).placeFavorite(job?.place?.id?: 0) {
                viewModel.favorited.value = true
            }
        }else {
            // お気に入り削除処理
            Api(activity!!).placeFavoriteDelete(job?.place?.id?: 0) {
                viewModel.favorited.value = false
            }
        }
    }

    override fun onClickApply(view: View) {
        // FIXME: 応募確認ダイアログの表示
    }

}

class ApplyButtonFragmentViewModel: ViewModel() {
    val applyButtonVisibility: MutableLiveData<Int> = MutableLiveData(View.VISIBLE)
    val favorited: MutableLiveData<Boolean> = MutableLiveData(false)

    fun setup(activity: Activity, job: Job?){
        if (job != null) {
            // 案件受付期間外の判定
            if (job.isPastOrInactive || job.isFuture) {
                applyButtonVisibility.value = View.INVISIBLE
            }
            // お気に入り状態の取得
            UserSession.retrieve()?.let {
                Api(activity).placeFavoriteShow(job.place?.id?: 0) {
                    favorited.value = it
                }
            }
        }
    }
}

interface ApplyButtonFragmentEventHandlers {
    fun onClickFavorite(view: View)
    fun onClickApply(view: View)
}
