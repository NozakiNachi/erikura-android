package jp.co.recruit.erikura.presenters.fragments

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.style.*
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.model.LatLng
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.Entry
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.DialogInputReasonAbleStartBinding
import jp.co.recruit.erikura.databinding.DialogNotAbleStartBinding
import jp.co.recruit.erikura.databinding.FragmentAppliedJobDetailsBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity
import jp.co.recruit.erikura.presenters.activities.job.JobDetailsActivity
import jp.co.recruit.erikura.presenters.util.LocationManager
import jp.co.recruit.erikura.presenters.util.setOnSafeClickListener
import java.lang.StringBuilder
import java.util.*


class AppliedJobDetailsFragment(
    private val activity: AppCompatActivity,
    job: Job?,
    user: User?
) : BaseJobDetailFragment(job, user), AppliedJobDetailsFragmentEventHandlers {
    private val viewModel: AppliedJobDetailsFragmentViewModel by lazy {
        ViewModelProvider(this).get(AppliedJobDetailsFragmentViewModel::class.java)
    }

    private val locationManager: LocationManager = ErikuraApplication.locationManager
    private var allowPedometerDialog: Dialog? = null

    private var timer: Timer = Timer()
    private var timerHandler: Handler = Handler()

    private var inStartJob: Boolean = false

    private var jobInfoView: JobInfoViewFragment? = null
    private var manualImage: ManualImageFragment? = null
    private var cancelButton: CancelButtonFragment? = null
    private var manualButton: ManualButtonFragment? = null
    private var thumbnailImage: ThumbnailImageFragment? = null
    private var jobDetailsView: JobDetailsViewFragment? = null
    private var mapView: MapViewFragment? = null

    override fun refresh(job: Job?, user: User?) {
        super.refresh(job, user)
        if (isAdded) {
            jobInfoView?.refresh(job, user)
            manualImage?.refresh(job, user)
            cancelButton?.refresh(job, user)
            manualButton?.refresh(job, user)
            thumbnailImage?.refresh(job, user)
            jobDetailsView?.refresh(job, user)
            mapView?.refresh(job, user)

            activity?.let {
                viewModel.setup(it, job, user)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        container?.removeAllViews()
        val binding = FragmentAppliedJobDetailsBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = activity
        viewModel.setup(activity, job, user)
        updateTimeLimit()
        binding.viewModel = viewModel
        binding.handlers = this
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val transaction = childFragmentManager.beginTransaction()
        jobInfoView = JobInfoViewFragment(job, user)
        manualImage = ManualImageFragment(job, user)
        cancelButton = CancelButtonFragment(job, user)
        manualButton = ManualButtonFragment(job, user)
        thumbnailImage = ThumbnailImageFragment(job, user)
        jobDetailsView = JobDetailsViewFragment(job, user)
        mapView = MapViewFragment(activity, job, user)
        transaction.add(R.id.appliedJobDetails_jobInfoViewFragment, jobInfoView!!, "jobInfoView")
        transaction.add(R.id.appliedJobDetails_manualImageFragment, manualImage!!, "manualImage")
        transaction.add(R.id.appliedJobDetails_cancelButtonFragment, cancelButton!!, "cancelButton")
        transaction.add(R.id.appliedJobDetails_manualButtonFragment, manualButton!!, "manualButton")
        transaction.add(R.id.appliedJobDetails_thumbnailImageFragment, thumbnailImage!!,"thumbnailImage")
        transaction.add(R.id.appliedJobDetails_jobDetailsViewFragment, jobDetailsView!!,"jobDetailsView")
        transaction.add(R.id.appliedJobDetails_mapViewFragment, mapView!!, "mapView")
        transaction.commitAllowingStateLoss()
    }

    override fun onStart() {
        super.onStart()
        // ページ参照のトラッキングの送出
        Tracking.logEvent(event= "view_job_entried", params= bundleOf())
        Tracking.viewJobDetails(name= "/entries/started/${job?.id ?: 0}", title= "作業開始前画面", jobId= job?.id ?: 0)
    }

    override fun onResume() {
        super.onResume()
        ErikuraApplication.pedometerManager.start()
        allowPedometerDialog?.dismiss()
        allowPedometerDialog = null

        timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                timerHandler.post(Runnable {
                    updateTimeLimit()
                })
            }
        }, 1000, 1000) // 実行したい間隔(ミリ秒)
    }

    override fun onPause() {
        super.onPause()
        ErikuraApplication.pedometerManager.stop()
        timer.cancel()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        ErikuraApplication.pedometerManager.onRequestPermissionResult(this, requestCode, permissions, grantResults,
            onPermissionNotGranted = {
                updateTimeLimit()
                if (inStartJob) {
                    startJob()
                }
                inStartJob = false
            },
            onPermissionGranted = {
                updateTimeLimit()
                if (inStartJob) {
                    startJob()
                }
                inStartJob = false
            })
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

    override fun onClickStart(view: View) {
        //明確に歩数取得することを説明するダイアログに許可したか
        if (!ErikuraApplication.instance.isAcceptedExplainGetPedometer()) {
            //許可していない場合ダイアログを表示
            displayExplainGetPedometer()
        } else {
            //許可してた場合
            checkPermissionPedometer()
        }
    }

    private fun onClickConfirmation(view: View) {
        //入力された理由をリクエストをパラメーターに加え、再度実行する
        job?.let { job ->
            val steps = ErikuraApplication.pedometerManager.readStepCount()
            val latLng: LatLng? = if (locationManager.checkPermission(this)) {
                locationManager.latLng
            } else {
                null
            }
            Log.v("DEBUG","クリック押下後　理由取得＝ ${viewModel.reason.value}")
            Api(activity).startJob(
                job, latLng,
                steps = steps,
                distance = null, floorAsc = null, floorDesc = null, reason = viewModel.reason.value
            ) { entry_id, checkStatus, messages ->
                Log.v("DEBUG","クリック押下後　checkStatus＝ ${checkStatus}")
                checkStatuses(job, steps, checkStatus, messages)
            }
        }
    }

    private fun checkAcceptedExplainGetPedometer() {
        if (!ErikuraApplication.instance.isAcceptedExplainGetPedometer()) {
            //ダイアログ表示後許可していない場合
            BaseActivity.currentActivity?.let { activity ->
                val dialog = AlertDialog.Builder(activity)
                    .setView(R.layout.dialog_not_accepted_get_pedometer)
                    .create()
                dialog.show()
            }
        } else {
            //ダイアログ表示後許可した場合
            Api(activity).agree(){
                checkPermissionPedometer()
            }
        }
    }

    private fun checkPermissionPedometer() {
        if (!ErikuraApplication.pedometerManager.checkPermission(activity)) {
            inStartJob = true
            checkNotAskAgainPedometer()
        }
        else {
            startJob()
        }
    }

    private fun checkNotAskAgainPedometer() {
        if (ErikuraApplication.pedometerManager.checkedNotAskAgain) {
            var openSettings = false
            BaseActivity.currentActivity?.let { activity ->
                val dialog = AlertDialog.Builder(activity)
                    .setView(R.layout.dialog_allow_activity_recognition)
                    .create()
                dialog.show()

                val label1: TextView = dialog.findViewById(R.id.allow_activity_label1)
                val sb = SpannableStringBuilder("・設定画面から「権限」＞「身体活動」をタップし「許可」を選択してください。")
                val becomeBold: (String) -> Unit = { keyword ->
                    val start = sb.toString().indexOf(keyword)
                    sb.setSpan(StyleSpan(R.style.label_w6), start, start + keyword.length, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
                }
                becomeBold("「権限」")
                becomeBold("「身体活動」")
                becomeBold("「許可」")

                label1.text = sb

                val button: Button = dialog.findViewById(R.id.update_button)
                button.setOnSafeClickListener {
                    openSettings = true
                    val uriString = "package:" + ErikuraApplication.instance.packageName
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse(uriString))
                    startActivity(intent)
                }

                allowPedometerDialog = dialog
                // ダイアログが消えた場合の対応
                dialog.setOnDismissListener {
                    allowPedometerDialog = null
                    if (!openSettings) {
                        startJob()
                    }
                }
            }
        }
        else {
            ErikuraApplication.pedometerManager.requestPermission(this)
        }
    }

    private fun startJob() {
        job?.let { job ->
            val steps = ErikuraApplication.pedometerManager.readStepCount()
            val latLng: LatLng? = if (locationManager.checkPermission(this)) {
                locationManager.latLng
            } else {
                null
            }

            Api(activity).startJob(
                job, latLng,
                steps = steps,
                distance = null, floorAsc = null, floorDesc = null, reason = null
            ) { entry_id, check_status, messages ->
                checkStatuses(job, steps, check_status, messages)
            }
        }
    }

    private fun startJobPassIntent(job: Job, steps: Int, sb: String) {
        // 作業開始のトラッキングの送出
        Tracking.logEvent(event = "push_start_job", params = bundleOf())
        Tracking.trackJobDetails(name = "push_start_job", jobId = job.id, steps = steps)

        val intent = Intent(activity, JobDetailsActivity::class.java)
        intent.putExtra("job", job)
        intent.putExtra("onClickStart", true)
        intent.putExtra("message", sb)
        startActivity(intent)
    }

    private fun updateTimeLimit() {
        val str = SpannableStringBuilder()
        val today = Date().time
        val limit = job?.entry?.limitAt?.time ?: 0
        val diff: Long = limit - today

        if (diff >= 0) {
            val diffHours = diff / (1000 * 60 * 60)
            val diffMinutes = (diff % (1000 * 60 * 60)) / (1000 * 60)

            if (diffHours == 0L) {
                str.append("あと${diffMinutes}分以内\n")
            } else if (diffMinutes == 0L) {
                str.append("あと${diffHours}時間以内\n")
            } else {
                str.append("あと${diffHours}時間${diffMinutes}分以内\n")
            }
            str.setSpan(
                ForegroundColorSpan(Color.RED),
                0,
                str.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            str.append(ErikuraApplication.instance.getString(R.string.jobDetails_goWorking))
            viewModel.timeLimit.value = str
            viewModel.msgVisibility.value = View.VISIBLE
            viewModel.startButtonVisibility.value = View.VISIBLE
        } else {
            str.append(ErikuraApplication.instance.getString(R.string.jobDetails_overLimit))
            str.setSpan(ForegroundColorSpan(Color.RED), 0, str.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            viewModel.timeLimit.value = str
            viewModel.msgVisibility.value = View.GONE
            viewModel.startButtonVisibility.value = View.INVISIBLE
        }
    }

    fun displayExplainGetPedometer() {
        BaseActivity.currentActivity?.let { activity ->
            val dialog = AlertDialog.Builder(activity)
                .setView(R.layout.dialog_explain_get_pedometer)
                .setCancelable(false)
                .create()
            dialog.show()
            val button: Button = dialog.findViewById(R.id.accepted_button)
            button.setOnClickListener(View.OnClickListener{
                //同意した場合
                dialog.dismiss()
                ErikuraApplication.instance.setAcceptedExplainGetPedometer(true)
                checkAcceptedExplainGetPedometer()
            })
            val cancelButton: Button = dialog.findViewById(R.id.not_accepted_button)
            cancelButton.setOnClickListener(View.OnClickListener {
                //同意しない場合
                dialog.dismiss()
                ErikuraApplication.instance.setAcceptedExplainGetPedometer(false)
                checkAcceptedExplainGetPedometer()
            })
        }
    }

    //API実行後のstatusによって処理を分岐させます。
    private fun checkStatuses(job: Job, steps: Int, checkStatus: Entry.CheckStatus, messages: ArrayList<String>) {

        val sb = SpannableStringBuilder()
        messages.forEach { msg ->
            val start = sb.length
//            sb.append("・")
            sb.append(msg)
            val end = sb.length
            val lb = LeadingMarginSpan.Standard(0,100)
//            val le = ListElementSpan(20)
//            sb.setSpan(lb, start , end, 0)
            val bl = BulletSpan(100).getLeadingMargin(false)
            sb.setSpan( bl, start , end, 0)
//            sb.setSpan(le, start , end, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
            sb.append("\n")
        }

        when(checkStatus) {
            //判定順は開始不可、警告理由入力、警告、開始可能
//            Entry.CheckStatus.ERROR -> {
//                //開始不可の場合はダイアログを表示
//                val binding: DialogNotAbleStartBinding = DataBindingUtil.inflate(
//                    LayoutInflater.from(activity), R.layout.dialog_not_able_start,null, false)
//                binding.lifecycleOwner = activity
//                binding.viewModel = viewModel
//                binding.handlers = this
//                val dialog = AlertDialog.Builder(activity)
//                    .setView(binding.root)
//                    .setPositiveButton("確認", null)
//                    .create()
//                dialog.show()
//                val confirmation: Button = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
//                confirmation.setOnClickListener(View.OnClickListener {
//                        dialog.dismiss()
//                })
//            }
            Entry.CheckStatus.REASON_REQUIRED -> {
                //警告ダイアログ理由入力
                sb.append("\nこのまま作業を開始する場合は理由を記入ください。\n" +
                        "（理由によっては、作業報告が差し戻しとなる場合があります）")
                val binding: DialogInputReasonAbleStartBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(activity), R.layout.dialog_input_reason_able_start, null, false)
                binding.alertMessage.setText(sb)
                binding.lifecycleOwner = activity
                binding.viewModel = viewModel
                binding.handlers = this
                binding.root.setOnTouchListener { view, event ->
                    if (view != null) {
                        val imm: InputMethodManager = activity!!.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(view.windowToken, 0)
                    }
                    return@setOnTouchListener false
                }
                val dialog = AlertDialog.Builder(activity)
                    .setView(binding.root)
                    .setCancelable(false)
                    .create()
                dialog.show()
                val button: Button = dialog.findViewById(R.id.confirmation_button)
                button.setOnClickListener(View.OnClickListener{
                        dialog.dismiss()
                        onClickConfirmation(it)
                })
                val cancelButton: Button = dialog.findViewById(R.id.cancel_button)
                cancelButton.setOnClickListener(View.OnClickListener {
                    dialog.dismiss()
                })
            }
            Entry.CheckStatus.SUCCESS_WITH_WARNING -> {
                //警告ダイアログは開始ログに注入して表示する、作業開始
                var message = messages.joinToString("\n")
                startJobPassIntent(job, steps, message?: "")
            }
            Entry.CheckStatus.SUCCESS -> {
                //作業開始
                var message = messages.joinToString("\n")
                startJobPassIntent(job, steps, message?: "")
            }
        }
    }
}

class AppliedJobDetailsFragmentViewModel : ViewModel() {
    val bitmapDrawable: MutableLiveData<BitmapDrawable> = MutableLiveData()
    val timeLimit: MutableLiveData<SpannableStringBuilder> = MutableLiveData()
    val msgVisibility: MutableLiveData<Int> = MutableLiveData(View.VISIBLE)
    val favorited: MutableLiveData<Boolean> = MutableLiveData(false)
    val startButtonVisibility: MutableLiveData<Int> = MutableLiveData(View.VISIBLE)
    val reason: MutableLiveData<String> = MutableLiveData()
//    var message: MutableLiveData<String> = MutableLiveData()

    val isEnabledButton = MediatorLiveData<Boolean>().also { result ->
        result.addSource(reason) { result.value = isValid() }
    }

    fun setup(activity: Activity, job: Job?, user: User?) {
        if (job != null) {
            // ダウンロード
            val thumbnailUrl = if (!job.thumbnailUrl.isNullOrBlank()) {job.thumbnailUrl}else {job.jobKind?.noImageIconUrl?.toString()}
            if (thumbnailUrl.isNullOrBlank()) {
                val drawable = ErikuraApplication.instance.applicationContext.resources.getDrawable(R.drawable.ic_noimage, null)
                val bitmapReduced = Bitmap.createScaledBitmap( drawable.toBitmap(), 15, 15, true)
                val bitmapDraw = BitmapDrawable(bitmapReduced)
                bitmapDraw.alpha = 150
                bitmapDrawable.value = bitmapDraw
            }else {
                val assetsManager = ErikuraApplication.assetsManager
                assetsManager.fetchImage(activity, thumbnailUrl) { result ->
                    activity.runOnUiThread {
                        val bitmapReduced = Bitmap.createScaledBitmap(result, 15, 15, true)
                        val bitmapDraw = BitmapDrawable(bitmapReduced)
                        bitmapDraw.alpha = 150
                        bitmapDrawable.value = bitmapDraw
                    }
                }
            }

            // お気に入り状態の取得
            if (Api.isLogin) {
                Api(activity).placeFavoriteShow(job.place?.id ?: 0) {
                    favorited.value = it
                }
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

interface AppliedJobDetailsFragmentEventHandlers {
    fun onClickFavorite(view: View)
    fun onClickStart(view: View)
}

public class ListElementSpan : MetricAffectingSpan {
    var mHeight: Int? = null

    constructor(height: Int){
        mHeight = height
    }
    override fun updateMeasureState(textPaint: TextPaint) {
    }

    override fun updateDrawState(tp: TextPaint?) {
        mHeight?.let{mH->
            tp?.let {
                tp.baselineShift += mH
            }
        }
    }
}