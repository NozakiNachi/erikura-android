package jp.co.recruit.erikura.presenters.activities.job

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.CancelReason
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.DialogCancelBinding
import jp.co.recruit.erikura.presenters.activities.OwnJobsActivity

class CancelDialogFragment: DialogFragment(), CancelDialogFragmentEventHandlers {
    companion object {
        const val JOB_ARGUMENT = "job"
        fun newInstance(job: Job?): CancelDialogFragment {
            return CancelDialogFragment().also {
                it.arguments = Bundle().also { args ->
                    args.putParcelable(JOB_ARGUMENT, job)
                }
            }
        }
    }

    private var job: Job? = null
    private val viewModel: CancelDialogFragmentViewModel by lazy {
        ViewModelProvider(this).get(CancelDialogFragmentViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { args ->
            job = args.getParcelable(JOB_ARGUMENT)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DataBindingUtil.inflate<DialogCancelBinding>(
            LayoutInflater.from(activity),
            R.layout.dialog_cancel,
            null,
            false
        )
        binding.lifecycleOwner = activity
        viewModel.setup(activity!!)
        binding.viewModel = viewModel
        binding.handlers = this

        binding.root.setOnTouchListener { view, event ->
            if (view != null) {
                val imm: InputMethodManager = activity!!.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
            }
            return@setOnTouchListener false
        }

        val builder = AlertDialog.Builder(activity)
        builder.setView(binding.root)
        return builder.create()
    }

    override fun onReasonSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        viewModel.reasonSelected = position

        viewModel.reasonsItems.value?.let { items ->
            val item = items[position]
            if (item is CancelReasonItem.Other) {
                viewModel.reasonVisibility.value = View.VISIBLE
            }
            else {
                viewModel.reasonVisibility.value = View.GONE
            }
        }
    }

    override fun onClickCancel(view: View) {
        viewModel.selectedItem?.let { item ->
            val reasonCode: Int = item.value?.id ?: 0
            val comment: String? = if (item is CancelReasonItem.Other) {
                viewModel.reasonText.value
            }
            else {
                null
            }
            job?.let { job ->
                if (isCancellable()) {
                    Api(activity!!).cancel(job, reasonCode, comment) {
                        // ページ参照のトラッキングの送出
                        Tracking.logEvent(event= "view_job_cacel_finish", params= bundleOf())
                        Tracking.viewJobDetails(name= "/entries/cancelled/${job?.id ?:0}", title= "キャンセル完了画面", jobId= job?.id ?: 0)

                        // 応募キャンセルに合わせて作業報告も削除されるので、削除済みのフラグを立てます
                        job?.report?.deleted = true

                        // キャンセル後は応募した仕事一覧に戻る
                        Intent(activity, OwnJobsActivity::class.java).let { intent ->
                            intent.putExtra(OwnJobsActivity.EXTRA_FROM_CANCEL_JOB, true)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        }
                    }
                }else {
                    val errorMessages = mutableListOf(ErikuraApplication.instance.getString(R.string.jobDetails_overLimit))
                    Api(activity!!).displayErrorAlert(errorMessages)
                    dismiss()
                }
            }
        }
    }

    /**
     * キャンセル処理を行えるか
     */
    private fun isCancellable(): Boolean {
        val expired = job?.entry?.isExpired() ?: false
        val rejected = job?.report?.isRejected ?: false

        // 作業期間内であればキャンセル可能
        if (!expired) { return true }
        // 作業期間を過ぎていても、作業報告がリジェクト状態であればキャンセル可能
        if (rejected) { return true }
        // 作業期間を過ぎていて、リジェクトされていない場合はキャンセル不可
        //   => 作業報告済みの場合はクライアントで確認されるのを待っているか、承認済みのため
        //   => 未報告の場合はバッチ等で削除される?
        return false
    }
}

class CancelDialogFragmentViewModel: ViewModel() {
    val reasonText: MutableLiveData<String> = MutableLiveData()
    val reasonVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val reasons: MutableLiveData<List<CancelReason>> = MutableLiveData()
    var reasonSelected: Int = 0

    val reasonsItems = MediatorLiveData<List<CancelReasonItem>>().also { result ->
        result.addSource(reasons) {
            val items = mutableListOf<CancelReasonItem>()
            // その他がデフォルト値で選択されているようにする
            items.add(0, CancelReasonItem.Other)
            it.forEach { reason -> items.add(CancelReasonItem.Item(reason)) }
            result.value = items
        }
    }

    val selectedItem: CancelReasonItem? get() {
        return reasonsItems.value?.let { items ->
            return items[reasonSelected]
        }
    }

    val isEnabledCancelButton = MediatorLiveData<Boolean>().also { result ->
        result.addSource(reasonVisibility) { result.value = isValid() }
        result.addSource(reasonText) { result.value = isValid() }
    }

    fun setup(activity: Activity) {
        Api(activity).cancelReasons {
            reasons.value = it
        }
    }

    private fun isValid(): Boolean {
        var valid = true

        if (reasonVisibility.value == View.VISIBLE) {
            // その他理由の入力項目が表示されているのでバリデーションを行います

            // 必須チェック
            if (valid && reasonText.value.isNullOrBlank()) {
                valid = false
            }
            // 文字数チェック
            if (valid && (reasonText.value?.length ?: 0) > 50) {
                valid = false
            }
        }
        return valid
    }
}

interface CancelDialogFragmentEventHandlers {
    fun onReasonSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
    fun onClickCancel(view: View)
}

sealed class CancelReasonItem(val label: String, val value: CancelReason?) {
    object Other: CancelReasonItem("その他", null)
    class Item(cancelReason: CancelReason) : CancelReasonItem(cancelReason.content, cancelReason)

    override fun toString(): String {
        return label
    }
}