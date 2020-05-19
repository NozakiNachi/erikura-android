package jp.co.recruit.erikura.presenters.activities.job

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.model.LatLng
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.DialogStopBinding
import jp.co.recruit.erikura.presenters.util.LocationManager
import java.util.*

class StopDialogFragment(private val job: Job?) : DialogFragment(), StopDialogFragmentEventHandlers  {
    private val viewModel by lazy {
        ViewModelProvider(this).get(StopDialogFragmentViewModel::class.java)
    }

    private val locationManager: LocationManager = ErikuraApplication.locationManager

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DataBindingUtil.inflate<DialogStopBinding>(
            LayoutInflater.from(activity),
            R.layout.dialog_stop,
            null,
            false
        )
        binding.lifecycleOwner = activity
        viewModel.setup(job)
        binding.viewModel = viewModel
        binding.handlers = this

        val builder = AlertDialog.Builder(activity)
        builder.setView(binding.root)
        return builder.create()
    }

    override fun onClikStop(view: View) {
        // stopJobの呼び出し
        job?.let {
            val latLng: LatLng? = if (locationManager.checkPermission(this)) {
                locationManager.latLng
            } else {
                null
            }

            val now = Date()
            val limitAt = job.entry?.limitAt ?: now

            if (limitAt < now) {
                // 納期を過ぎてしまっている場合
                Api(activity!!).displayErrorAlert(listOf(getString(R.string.jobDetails_overLimit)))
                dismiss()
            }
            else {
                val steps = ErikuraApplication.pedometerManager.readStepCount()
                Api(activity!!).stopJob(job, latLng,
                    steps = steps,
                    distance = null, floorAsc = null, floorDesc = null, comment = null
                ) { entry_id, check_status, messages ->
                    checkStatus(job, steps, check_status, messages)
                }
            }
        }
    }

    override fun onClickConfirmation(view: View) {
        //入力された理由をリクエストをパラメーターに加え、再度実行する
        job?.let {
            val latLng: LatLng? = if (locationManager.checkPermission(this)) {
                locationManager.latLng
            } else {
                null
            }

            val now = Date()
            val limitAt = job.entry?.limitAt ?: now

            if (limitAt < now) {
                // 納期を過ぎてしまっている場合
                Api(activity!!).displayErrorAlert(listOf(getString(R.string.jobDetails_overLimit)))
                dismiss()
            }
            else {
                val steps = ErikuraApplication.pedometerManager.readStepCount()
                Api(activity!!).stopJob(job, latLng,
                    steps = steps,
                    distance = null, floorAsc = null, floorDesc = null, comment = viewModel.reason.value
                ) { entry_id, check_status, messages ->
                    checkStatus(job, steps, check_status, messages)
                }
            }
        }
    }

    private fun stopJobPassIntent(job: Job, steps: Int, message: String) {
        // 作業完了のトラッキングの送出
        Tracking.logEvent(event= "job_finished", params= bundleOf())
        Tracking.trackJobDetails(name= "job_finished", jobId= job?.id ?: 0, steps = steps)

        val intent= Intent(activity, WorkingFinishedActivity::class.java)
        intent.putExtra("job", job)
        intent.putExtra("message", message)
        startActivity(intent)
    }

    //API実行後のstatusによって処理を分岐させます。
    private fun checkStatus(job: Job, steps: Int, check_status: Int, messages: ArrayList<String>) {
        var message: String = messages?.joinToString("\n")
        viewModel.message.value = message
        when(check_status) {
            //判定順は終了不可、警告、終了可能
            ErikuraApplication.RESPONSE_NOT_ABLE_START_OR_END -> {
                //終了不可の場合はダイアログを表示
                val dialog = AlertDialog.Builder(activity)
                    .setView(R.layout.dialog_not_able_end)
                    .setPositiveButton("確認", null)
                    .create()
                dialog.show()
                val confirmation: Button = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                confirmation.setOnClickListener(View.OnClickListener {
                    fun onClick(view: View) {
                        dialog.dismiss()
                    }
                })
            }
            ErikuraApplication.RESPONSE_INPUT_REASON_ABLE_START_OR_END -> {
                //警告ダイアログ理由入力
                val dialog = AlertDialog.Builder(activity)
                    .setView(R.layout.dialog_input_reason_able_end)
                    .create()
                dialog.show()
            }
            ErikuraApplication.RESPONSE_ALERT_ABLE_START_OR_END -> {
                //警告ダイアログを表示
                val dialog = AlertDialog.Builder(activity)
                    .setView(R.layout.dialog_alert_able_end)
                    .setPositiveButton("確認", null)
                    .create()
                dialog.show()
                val confirmation: Button = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                confirmation.setOnClickListener(View.OnClickListener {
                    fun onClick(view: View) {
                        dialog.dismiss()
                        // 警告ダイアログの確認後、作業終了します
                        stopJobPassIntent(job, steps, message)
                    }
                })
            }
            ErikuraApplication.RESPONSE_ABLE_START_OR_END -> {
                //作業終了します
                stopJobPassIntent(job, steps, message)
            }
        }
    }
}

class StopDialogFragmentViewModel: ViewModel() {
    val caption: MutableLiveData<String> = MutableLiveData()
    val reportPlaces: MutableLiveData<String> = MutableLiveData()
    val reportPlacesVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val reason: MutableLiveData<String> = MutableLiveData()
    var message: MutableLiveData<String> = MutableLiveData()

    val isEnabledButton = MediatorLiveData<Boolean>().also { result ->
        result.addSource(reason) { result.value = isValid() }
    }

    fun setup(job: Job?) {
        if (job != null) {
            if(!job.summaryTitles.isNullOrEmpty()) {
                caption.value = ErikuraApplication.instance.getString(R.string.applyDialog_caption2Pattern1)
                var summaryTitleStr = ""
                job.summaryTitles.forEachIndexed { index, s ->
                    summaryTitleStr += "(${index+1}) ${s}　"
                }
                reportPlaces.value = summaryTitleStr
                reportPlacesVisibility.value = View.VISIBLE
            }else {
                caption.value = ErikuraApplication.instance.getString(R.string.applyDialog_caption2Pattern2)
                reportPlacesVisibility.value = View.GONE
            }
        }
    }

    private fun isValid(): Boolean {
        var valid = true
        // 必須チェック
        if (valid && reason.value.isNullOrBlank()) {
            valid = false
        }
        // 文字数チェック
        if (valid && (reason.value?.length ?: 0) > 50) {
            valid = false
        }
        return valid
    }
}

interface StopDialogFragmentEventHandlers {
    fun onClikStop(view: View)
    fun onClickConfirmation(view: View)
}