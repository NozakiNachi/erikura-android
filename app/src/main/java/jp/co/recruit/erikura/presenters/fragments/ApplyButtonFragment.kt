package jp.co.recruit.erikura.presenters.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ToggleButton
import androidx.core.os.bundleOf
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.FragmentApplyButtonBinding
import jp.co.recruit.erikura.presenters.activities.errors.LoginRequiredActivity
import jp.co.recruit.erikura.presenters.activities.job.ApplyDialogFragment
import jp.co.recruit.erikura.presenters.activities.mypage.UpdateIdentityActivity

class ApplyButtonFragment : BaseJobDetailFragment, ApplyButtonFragmentEventHandlers {
    companion object {
        fun newInstance(job: Job?, user: User?): ApplyButtonFragment {
            return ApplyButtonFragment().also {
                it.arguments = Bundle().also { args ->
                    fillArguments(args, job, user)
                }
            }
        }
    }

    constructor(): super()

    private val viewModel: ApplyButtonFragmentViewModel by lazy {
        ViewModelProvider(this).get(ApplyButtonFragmentViewModel::class.java)
    }

    override fun refresh(job: Job?, user: User?) {
        super.refresh(job, user)
        activity?.let { activity ->
            viewModel.setup(activity, job, user)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentApplyButtonBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = activity
        viewModel.setup(activity!!, job, user)
        binding.viewModel = viewModel
        binding.handlers = this
        return binding.root
    }

    override fun onClickFavorite(view: View) {
        job?.place?.id?.let { placeId ->
            // 現在のボタン状態を取得します
            val favorited = viewModel.favorited.value ?: false

            val favoriteButton: ToggleButton = this.view?.findViewById(R.id.favorite_button)!!
            // タップが聞かないのように無効化をします
            favoriteButton.isEnabled = false
            val api = Api(activity!!)
            val errorHandler: (List<String>?) -> Unit = { messages ->
                api.displayErrorAlert(messages)
                favoriteButton.isEnabled = true
            }
            if (favorited) {
                // ボタンがお気に入り状態なので登録処理
                api.placeFavorite(placeId, onError = errorHandler) {
                    viewModel.favorited.value = true
                    favoriteButton.isEnabled = true
                }
            }
            else {
                // お気に入り削除処理
                api.placeFavoriteDelete(placeId, onError = errorHandler) {
                    viewModel.favorited.value = false
                    favoriteButton.isEnabled = true
                }
            }
        }
    }

    override fun onClickApply(view: View) {
        if (Api.isLogin) {
            user?.id?.let { userId ->
                Api(activity!!).showIdVerifyStatus(userId, ErikuraApplication.NOT_GET_COMPARING_DATA) { status, _ ->
                    // 身分確認状況を取得
                    if (status == ErikuraApplication.ID_CONFIRMING_CODE || status == ErikuraApplication.ID_CONFIRMED_CODE || status == ErikuraApplication.FAILED_ONCE_APPROVED) {
                        // 身分証確認中、済の場合
                        val dialog = ApplyDialogFragment.newInstance(job)
                        dialog.show(childFragmentManager, "Apply")
                    } else {
                        // 身分証未確認の場合
                        // ページ参照のトラッキングの送出
                        Tracking.logEvent(event= "push_entry_through_identity_verification", params= bundleOf())
                        Tracking.pushEntryThroughIdentityVerification( "push_entry_through_identity_verification",  userId)
                        //　本人確認情報画面へ遷移
                        val intent = Intent(activity, UpdateIdentityActivity::class.java)
                        intent.putExtra(ErikuraApplication.FROM_WHERE, ErikuraApplication.FROM_ENTRY)
                        intent.putExtra("user", user)
                        intent.putExtra("job", job)
                        startActivity(intent)
                    }
                }
            }
        } else {
            val intent = Intent(activity, LoginRequiredActivity::class.java)
            startActivity(intent)
        }
    }

}

class ApplyButtonFragmentViewModel: ViewModel() {
    val job = MutableLiveData<Job>()
    val user = MutableLiveData<User>()

    val inAdvancePeriod: Boolean get() = job.value?.inAdvanceEntryPeriod ?: false
    val closeToHome: Boolean get() = job.value?.closeToHome ?: false

    val applyButtonVisibility: MutableLiveData<Int> = MutableLiveData(View.VISIBLE)
    val applyButtonText = MutableLiveData<String>(ErikuraApplication.instance.getString(R.string.entry))
    val favorited: MutableLiveData<Boolean> = MutableLiveData(false)

    val applyButtonEnabled = MediatorLiveData<Boolean>().also { result ->
        result.addSource(job) { updateApplyButtonStatus() }
    }

    fun setup(activity: Activity, job: Job?, user: User?){
        this.job.value = job
        this.user.value = user

        if (job != null) {
            // 案件受付期間外の判定
            if (Api.isLogin && user == null) {
                applyButtonVisibility.value = View.INVISIBLE
            }
            else {
                applyButtonVisibility.value = if (job.isApplicable(user)) {
                    View.VISIBLE
                }
                else {
                    View.INVISIBLE
                }
            }

            // お気に入り状態の取得
            if (Api.isLogin) {
                Api(activity).placeFavoriteShow(job.place?.id?: 0) {
                    favorited.value = it
                }
            }
        }
    }

    private fun updateApplyButtonStatus() {
        if (Api.isLogin && inAdvancePeriod && !closeToHome) {
            applyButtonEnabled.value = false
            applyButtonText.value = ErikuraApplication.instance.getString(R.string.advanceEntry)
        }
        else {
            applyButtonEnabled.value = true
            applyButtonText.value = ErikuraApplication.instance.getString(R.string.entry)
        }
    }
}

interface ApplyButtonFragmentEventHandlers {
    fun onClickFavorite(view: View)
    fun onClickApply(view: View)
}
