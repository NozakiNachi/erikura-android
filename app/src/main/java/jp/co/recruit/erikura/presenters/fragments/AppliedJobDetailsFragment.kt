package jp.co.recruit.erikura.presenters.fragments

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
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
import jp.co.recruit.erikura.presenters.activities.job.PreEntryFlowDialogFragment
import jp.co.recruit.erikura.presenters.util.LocationManager
import jp.co.recruit.erikura.presenters.view_models.BaseJobDetailViewModel
import java.util.*

class AppliedJobDetailsFragment : BaseJobDetailFragment, AppliedJobDetailsFragmentEventHandlers {
    companion object {
        fun newInstance(user: User?): AppliedJobDetailsFragment {
            val args = Bundle()
            fillArguments(args, user)

            return AppliedJobDetailsFragment().also {
                it.arguments = args
            }
        }
    }

    private val viewModel: AppliedJobDetailsFragmentViewModel by lazy {
        ViewModelProvider(this).get(AppliedJobDetailsFragmentViewModel::class.java)
    }

    private val locationManager: LocationManager = ErikuraApplication.locationManager
    private var allowLocationDialog: Dialog? = null
    private var allowPedometerDialog: Dialog? = null

    private var timer: Timer = Timer()
    private var timerHandler: Handler = Handler()

    private var inStartJob: Boolean = false

    private var jobInfoView: JobInfoViewFragment? = null
    private var manualImage: ManualImageFragment? = null
    private var entryInformationFragment: EntryInformationFragment? = null
    private var cancelButton: CancelButtonFragment? = null
    private var manualButton: ManualButtonFragment? = null
    private var thumbnailImage: ThumbnailImageFragment? = null
    private var jobDetailsView: JobDetailsViewFragment? = null
    private var mapView: MapViewFragment? = null
    private var propertyNotesButton: PropertyNotesButtonFragment? = null
    private var reportExamplesButton: ReportExamplesButtonFragment? = null
    private var isShowPreEntryFlowModal: Boolean = false

    constructor(): super()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isShowPreEntryFlowModal = (job?.entry?.fromPreEntry == true && job?.isPreEntryPeriod == true)
        if (isShowPreEntryFlowModal) {
            //?????????????????????????????????????????????????????????????????????????????????????????????????????????
            val dialog = PreEntryFlowDialogFragment.newInstance(job)
            dialog.show(childFragmentManager, "PreEntryFlow")

        }
    }

    override fun refresh(job: Job?, user: User?) {
        super.refresh(job, user)
        if (isAdded) {
            jobInfoView?.refresh(job, user)
            manualImage?.refresh(job, user)
            entryInformationFragment?.refresh(job, user)
            cancelButton?.refresh(job, user)
            manualButton?.refresh(job, user)
            thumbnailImage?.refresh(job, user)
            jobDetailsView?.refresh(job, user)
            mapView?.refresh(job, user)
            propertyNotesButton?.refresh(job, user)
            reportExamplesButton?.refresh(job, user)

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
        viewModel.setup(activity!!, job, user)
        updateTimeLimit()
        binding.viewModel = viewModel
        binding.handlers = this
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val transaction = childFragmentManager.beginTransaction()
        jobInfoView = JobInfoViewFragment.newInstance(user)
        manualImage = ManualImageFragment.newInstance(user)
        entryInformationFragment = EntryInformationFragment.newInstance(user)
        cancelButton = CancelButtonFragment.newInstance(user)
        manualButton = ManualButtonFragment.newInstance(user)
        thumbnailImage = ThumbnailImageFragment.newInstance(user)
        jobDetailsView = JobDetailsViewFragment.newInstance(user)
        mapView = MapViewFragment.newInstance(user)
        propertyNotesButton = PropertyNotesButtonFragment.newInstance(user)
        reportExamplesButton = ReportExamplesButtonFragment.newInstance(user)
        transaction.add(R.id.appliedJobDetails_jobInfoViewFragment, jobInfoView!!, "jobInfoView")
        transaction.add(R.id.appliedJobDetails_manualImageFragment, manualImage!!, "manualImage")
        transaction.add(R.id.appliedJobDetails_entryInformationFragment, entryInformationFragment!!, "entryInformation")
        transaction.add(R.id.appliedJobDetails_cancelButtonFragment, cancelButton!!, "cancelButton")
        transaction.add(R.id.appliedJobDetails_manualButtonFragment, manualButton!!, "manualButton")
        transaction.add(R.id.appliedJobDetails_thumbnailImageFragment, thumbnailImage!!,"thumbnailImage")
        transaction.add(R.id.appliedJobDetails_jobDetailsViewFragment, jobDetailsView!!,"jobDetailsView")
        transaction.add(R.id.appliedJobDetails_mapViewFragment, mapView!!, "mapView")
        transaction.add(R.id.jobDetails_propertyNotesButtonFragment, propertyNotesButton!!, "propertyNotesButton")
        transaction.add(R.id.jobDetails_reportExamplesButtonFragment, reportExamplesButton!!, "reportExamplesButton")
        transaction.commitAllowingStateLoss()
    }

    override fun onStart() {
        super.onStart()
        // ?????????????????????????????????????????????
        Tracking.logEvent(event= "view_job_entried", params= bundleOf())
        Tracking.viewJobDetails(name= "/entries/started/${job?.id ?: 0}", title= "?????????????????????", jobId= job?.id ?: 0)
    }

    override fun onResume() {
        super.onResume()
        ErikuraApplication.pedometerManager.start()

        if (allowLocationDialog != null) {
            if (ErikuraApplication.locationManager.checkPermission(this)) {
                allowLocationDialog?.dismiss()
                allowLocationDialog = null
            }
        }
        if (allowPedometerDialog != null) {
            if (ErikuraApplication.pedometerManager.checkPermission(this)) {
                allowPedometerDialog?.dismiss()
                allowPedometerDialog = null
            }
        }

        timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                timerHandler.post(Runnable {
                    updateTimeLimit()
                })
            }
        }, 1000, 1000) // ?????????????????????(?????????)
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
            onDisplayAlert = { dialog ->
                allowPedometerDialog = dialog
            },
            onPermissionNotGranted = {
                allowPedometerDialog = null
                updateTimeLimit()
                startJob()
            },
            onPermissionGranted = {
                allowPedometerDialog = null
                updateTimeLimit()
                startJob()
            })
        ErikuraApplication.locationManager.onRequestPermissionResult(this, requestCode, permissions, grantResults,
            onDisplayAlert = { dialog ->
                allowLocationDialog = dialog
            },
            onPermissionNotGranted = {
                allowLocationDialog = null
                updateTimeLimit()
                startJobWithPedometerPermissionCheck()
            },
            onPermissionGranted = {
                allowLocationDialog = null
                updateTimeLimit()
                startJobWithPedometerPermissionCheck()
            })
    }

    override fun onClickFavorite(view: View) {
        job?.place?.id?.let { placeId ->
            // ??????????????????????????????????????????
            val favorited = viewModel.favorited.value ?: false

            val favoriteButton: ToggleButton = this.view?.findViewById(R.id.favorite_button)!!
            // ?????????????????????????????????????????????????????????
            favoriteButton.isEnabled = false
            val api = Api(activity!!)
            val errorHandler: (List<String>?) -> Unit = { messages ->
                api.displayErrorAlert(messages)
                favoriteButton.isEnabled = true
            }
            if (favorited) {
                // ??????????????????????????????????????????????????????
                api.placeFavorite(placeId, onError = errorHandler) {
                    viewModel.favorited.value = true
                    favoriteButton.isEnabled = true
                }
            }
            else {
                // ???????????????????????????
                api.placeFavoriteDelete(placeId, onError = errorHandler) {
                    viewModel.favorited.value = false
                    favoriteButton.isEnabled = true
                }
            }
        }
    }

    override fun onClickStart(view: View) {
        if (job?.isPreEntriedWithinnPreEntryPeriod == true) {
            val dialog = AlertDialog.Builder(activity)
                .setView(R.layout.dialog_disabled_start_working_button)
                .setCancelable(true)
                .create()
            dialog.show()
        } else {
            //?????????????????????????????????????????????????????????????????????????????????
            if (!ErikuraApplication.instance.isAcceptedExplainGetPedometer()) {
                //???????????????????????????????????????????????????
                displayExplainGetPedometer()
            } else {
                //?????????????????????
                startJobWithPermissionCheck()
            }
        }
    }

    private fun onClickConfirmation(view: View) {
        //??????????????????????????????????????????????????????????????????????????????????????????
        job?.let { job ->
            val steps = ErikuraApplication.pedometerManager.readStepCount()
            val latLng: LatLng? = if (locationManager.checkPermission(this)) {
                locationManager.latLng
            } else {
                null
            }
            Log.v("DEBUG","??????????????????????????????????????? ${viewModel.reason.value}")
            Api(activity!!).startJob(
                job, latLng,
                steps = steps,
                distance = null, floorAsc = null, floorDesc = null, reason = viewModel.reason.value
            ) { entry_id, checkStatus, messages ->
                Log.v("DEBUG","????????????????????????checkStatus??? ${checkStatus}")
                checkStatuses(job, steps, checkStatus, messages)
            }
        }
    }

    private fun checkAcceptedExplainGetPedometer() {
        if (!ErikuraApplication.instance.isAcceptedExplainGetPedometer()) {
            //???????????????????????????????????????????????????
            BaseActivity.currentActivity?.let { activity ->
                val dialog = AlertDialog.Builder(activity)
                    .setView(R.layout.dialog_not_accepted_get_pedometer)
                    .create()
                dialog.show()
            }
        } else {
            //??????????????????????????????????????????
            startJobWithPermissionCheck()
        }
    }

    private fun startJobWithPermissionCheck() {
        startJobWithLocationPermissionCheck()
    }

    private fun startJobWithLocationPermissionCheck() {
        if (ErikuraApplication.locationManager.checkPermission(this)) {
            // ??????????????????????????????????????????????????????????????????
            startJobWithPedometerPermissionCheck()
        }
        else {
            // ?????????????????????????????????????????????????????????????????????
            ErikuraApplication.locationManager.requestPermission(this)
        }
    }

    private fun startJobWithPedometerPermissionCheck() {
        if (!ErikuraApplication.pedometerManager.checkPermission(activity!!)) {
            ErikuraApplication.pedometerManager.requestPermission(this)
        }
        else {
            startJob()
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

            Api(activity!!).startJob(
                job, latLng,
                steps = steps,
                distance = null, floorAsc = null, floorDesc = null, reason = null
            ) { entry_id, check_status, messages ->
                checkStatuses(job, steps, check_status, messages)
            }
        }
    }

    private fun startJobPassIntent(job: Job, steps: Int, message: String) {
        // ??????????????????????????????????????????
        Tracking.logEvent(event = "push_start_job", params = bundleOf())
        Tracking.trackJobDetails(name = "push_start_job", jobId = job.id, steps = steps)

        val intent = Intent(activity, JobDetailsActivity::class.java)
        intent.putExtra("job", job)
        intent.putExtra("onClickStart", true)
        intent.putExtra("message", message)
        startActivity(intent)
    }

    private fun updateTimeLimit() {
        val str = SpannableStringBuilder()
        val today = Date().time
        val limit = job?.entry?.limitAt?.time ?: 0
        val startAt = job?.workingStartAt?.time ?: 0
        val diff: Long = limit - today
        val startAtDiff = startAt -today

        if (diff >= 0) {
            if (job?.isPreEntriedWithinnPreEntryPeriod == true && startAtDiff >= 0) {
                //?????????????????????????????????????????????????????????
                str.append(
                    JobUtil.getFormattedDateWithoutYear(job?.workingStartAt?: Date())
                )
                str.append(
                    ("\n??? " +
                    JobUtil.getFormattedDateWithoutYear(
                        JobUtil.preEntryWorkingLimitAt(job?.workingStartAt?: Date())
                    ))
                )
                str.setSpan(
                    ForegroundColorSpan(Color.RED),
                    0,
                    str.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                str.append(ErikuraApplication.instance.getString(R.string.jobDetails_goWorkingByPreEntry))
                viewModel.timeLimitWarningPreEntryMessage.value = String.format(
                    ErikuraApplication.instance.getString(R.string.jobDetails_pressButtonByPreEntry),
                    JobUtil.getWorkingDay(job?.workingStartAt?: Date())
                    )
                viewModel.preEntryTimeLimit.value = str
                viewModel.msgVisibility.value = View.GONE
                viewModel.msgPreEntryVisibility.value = View.VISIBLE
            } else {
                val diffDates = diff / (1000 * 60 * 60 * 24)
                val diffHours = (diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60)
                val diffMinutes = (diff % (1000 * 60 * 60 * 24) % (1000 * 60 * 60)) / (1000 * 60)

                if (diffDates == 0L) {
                    if (diffHours == 0L) {
                        str.append("??????${diffMinutes}???")
                    } else if (diffMinutes == 0L) {
                        str.append("??????${diffHours}??????")
                    } else {
                        str.append("??????${diffHours}??????${diffMinutes}???")
                    }
                } else {
                    if (diffHours == 0L) {
                        str.append("??????${diffDates}???${diffMinutes}???")
                    } else if (diffMinutes == 0L) {
                        str.append("??????${diffDates}???${diffHours}??????")
                    } else {
                        str.append("??????${diffDates}???${diffHours}??????${diffMinutes}???")
                    }
                }

                str.append(ErikuraApplication.instance.getString(R.string.jobDetails_endOfWord_limit))
                str.setSpan(
                    ForegroundColorSpan(Color.RED),
                    0,
                    str.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                str.append(ErikuraApplication.instance.getString(R.string.jobDetails_goWorking))
                viewModel.timeLimitWarningMessage.value = ErikuraApplication.instance.getString(R.string.jobDetails_pressButton)
                viewModel.timeLimit.value = str
                viewModel.msgVisibility.value = View.VISIBLE
                viewModel.msgPreEntryVisibility.value = View.GONE
            }
            viewModel.startButtonVisibility.value = View.VISIBLE
        } else {
            str.append(ErikuraApplication.instance.getString(R.string.jobDetails_overLimit))
            str.setSpan(ForegroundColorSpan(Color.RED), 0, str.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            viewModel.timeLimit.value = str
            viewModel.timeLimitWarningMessage.value = ErikuraApplication.instance.getString(R.string.jobDetails_pressButton)
            viewModel.msgVisibility.value = View.VISIBLE
            viewModel.msgPreEntryVisibility.value = View.GONE
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
                //??????????????????
                dialog.dismiss()
                Api(activity).agree(){result ->
                    if (result){
                        ErikuraApplication.instance.setAcceptedExplainGetPedometer(true)
                        checkAcceptedExplainGetPedometer()
                    }
                }
            })
            val cancelButton: Button = dialog.findViewById(R.id.not_accepted_button)
            cancelButton.setOnClickListener(View.OnClickListener {
                //?????????????????????
                dialog.dismiss()
                ErikuraApplication.instance.setAcceptedExplainGetPedometer(false)
                checkAcceptedExplainGetPedometer()
            })
        }
    }

    //API????????????status??????????????????????????????????????????
    private fun checkStatuses(job: Job, steps: Int, checkStatus: Entry.CheckStatus, messages: ArrayList<String>) {
        var message: String? = ""
        if (!messages.isNullOrEmpty()) {
            messages.forEach {msg ->
                var appendText = "???"
                message += ((appendText.plus(msg)).plus("\n"))
            }
        }
        viewModel.message.value = message

        when(checkStatus) {
            //?????????????????????????????????????????????????????????????????????
            Entry.CheckStatus.ERROR -> {
                //????????????????????????????????????????????????
                val binding: DialogNotAbleStartBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(activity), R.layout.dialog_not_able_start,null, false)
                binding.lifecycleOwner = activity
                binding.viewModel = viewModel
                binding.handlers = this
                val dialog = AlertDialog.Builder(activity)
                    .setView(binding.root)
                    .setPositiveButton("??????", null)
                    .create()
                dialog.show()
                val confirmation: Button = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                confirmation.setOnClickListener(View.OnClickListener {
                        dialog.dismiss()
                })
            }
            Entry.CheckStatus.REASON_REQUIRED -> {
                //?????????????????????????????????
                viewModel.message.value = message.plus("\n????????????????????????????????????????????????????????????????????????\n" +
                        "?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????")

                val binding: DialogInputReasonAbleStartBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(activity), R.layout.dialog_input_reason_able_start, null, false)
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
                //??????????????????????????????????????????????????????????????????????????????
                startJobPassIntent(job, steps, message?: "")
            }
            Entry.CheckStatus.SUCCESS -> {
                //????????????
                startJobPassIntent(job, steps, message?: "")
            }
        }
    }
}

class AppliedJobDetailsFragmentViewModel : BaseJobDetailViewModel() {
    val bitmapDrawable: MutableLiveData<BitmapDrawable> = MutableLiveData()
    val timeLimit: MutableLiveData<SpannableStringBuilder> = MutableLiveData()
    val preEntryTimeLimit: MutableLiveData<SpannableStringBuilder> = MutableLiveData()
    var timeLimitWarningMessage: MutableLiveData<String> =  MutableLiveData()
    var timeLimitWarningPreEntryMessage: MutableLiveData<String> =  MutableLiveData()
    val msgVisibility = MediatorLiveData<Int>().also { result ->
        result.addSource(job) {
            result.value = if (job.value?.isPreEntriedWithinnPreEntryPeriod == true) {
                View.GONE
            }
            else {
                View.VISIBLE
            }
        }
    }
    val msgPreEntryVisibility = MediatorLiveData<Int>().also { result ->
        result.addSource(job) {
            result.value =
                if (job.value?.isPreEntriedWithinnPreEntryPeriod == true) {
                    View.VISIBLE
                } else {
                    View.INVISIBLE
                }
        }
    }
    val favorited: MutableLiveData<Boolean> = MutableLiveData(false)
    val startButtonVisibility: MutableLiveData<Int> = MutableLiveData(View.VISIBLE)
    val reason: MutableLiveData<String> = MutableLiveData()
    var message: MutableLiveData<String> = MutableLiveData()

    var buttonStyle = MediatorLiveData<Drawable>().also { result ->
        result.addSource(job) {
            if (job.value?.isPreEntriedWithinnPreEntryPeriod == true) {
                val drawable: Drawable = ErikuraApplication.instance.applicationContext.resources.getDrawable(R.color.silver)
                result.value = drawable
            } else {
                val drawable: Drawable = ErikuraApplication.instance.applicationContext.resources.getDrawable(R.color.pumpkinOrange)
                result.value = drawable
            }
        }
    }

    val isEnabledButton = MediatorLiveData<Boolean>().also { result ->
        result.addSource(reason) { result.value = isValid() }
    }
    val reportExamplesButtonVisibility: MutableLiveData<Int> = MutableLiveData(View.VISIBLE)

    fun setup(activity: Activity, job: Job?, user: User?) {
        this.job.value = job
        this.user.value = user

        if (job != null) {
            // ??????????????????
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

            // ??????????????????????????????
            if (Api.isLogin) {
                Api(activity).placeFavoriteShow(job.place?.id ?: 0) {
                    favorited.value = it
                }
            }
            //????????????????????????0?????????????????????
            job.goodExamplesCount?.let { reportExampleCount ->
                if (reportExampleCount == 0) {
                    reportExamplesButtonVisibility.value = View.GONE
                }
            }
            // ?????????????????????????????????
            if (job.isPreEntriedWithinnPreEntryPeriod) {
                buttonStyle.value = ErikuraApplication.instance.applicationContext.resources.getDrawable(R.color.silver)
            } else {
                buttonStyle.value = ErikuraApplication.instance.applicationContext.resources.getDrawable(R.color.pumpkinOrange)
            }

        }
    }

    private fun isValid(): Boolean {
        var valid = true
        // ??????????????????
        if (valid && reason.value.isNullOrBlank()) {
            valid = false
        }
        // ?????????????????????
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