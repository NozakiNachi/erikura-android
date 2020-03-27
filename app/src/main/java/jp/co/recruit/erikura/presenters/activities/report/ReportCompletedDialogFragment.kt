package jp.co.recruit.erikura.presenters.activities.report

import android.app.ActivityOptions
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.DialogReportCompletedBinding
import jp.co.recruit.erikura.presenters.activities.mypage.AccountSettingActivity


class ReportCompletedDialogFragment: DialogFragment(), ReportCompletedEventHandlers {
    private val viewModel: ReportCompletedViewModel by lazy {
        ViewModelProvider(this).get(ReportCompletedViewModel::class.java)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DataBindingUtil.inflate<DialogReportCompletedBinding>(
            LayoutInflater.from(activity),
            R.layout.dialog_report_completed,
            null,
            false
        )
        binding.lifecycleOwner = activity
        binding.viewModel = viewModel
        binding.handlers = this

        val builder = AlertDialog.Builder(activity)
        builder.setView(binding.root)

        return builder.create()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        var msg = ErikuraApplication.instance.getString(R.string.report_completed_title)
        Api(activity!!).payment() {
            if (it.bankName == null) {
                msg = "${ErikuraApplication.instance.getString(R.string.report_completed_title)}\n${ErikuraApplication.instance.getString(R.string.report_completed_title2)}"
                viewModel.buttonVisibility.value = View.VISIBLE
                viewModel.msg.value = msg
            }
        }

        viewModel.msg.value = msg
    }

    override fun onClickBankRegistration(view: View) {
        val intent = Intent(activity, AccountSettingActivity::class.java)
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(activity).toBundle())
    }
}

class ReportCompletedViewModel: ViewModel() {
    val msg: MutableLiveData<String> = MutableLiveData()
    val buttonVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)
}

interface ReportCompletedEventHandlers {
    fun onClickBankRegistration(view: View)
}