package jp.co.recruit.erikura.presenters.fragments

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.*
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.*
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.*
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.JobAttachment
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.business.util.JobUtils
import jp.co.recruit.erikura.databinding.FragmentJobAttachmentListItemBinding
import jp.co.recruit.erikura.databinding.FragmentJobDetailsViewBinding
import jp.co.recruit.erikura.presenters.activities.job.PlaceDetailActivity
import jp.co.recruit.erikura.presenters.util.MessageUtils
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class JobDetailsViewFragment : BaseJobDetailFragment, JobDetailsViewFragmentEventHandlers {
    companion object {
        fun newInstance(user: User?): JobDetailsViewFragment {
            return JobDetailsViewFragment().also {
                it.arguments = Bundle().also { args ->
                    fillArguments(args, user)
                }
            }
        }
    }

    constructor(): super()

    private val viewModel: JobDetailsViewFragmentViewModel by lazy {
        ViewModelProvider(this).get(JobDetailsViewFragmentViewModel::class.java)
    }
    private val downloader: RxDownloader by lazy {
        RxDownloader(context ?: ErikuraApplication.instance)
    }
    private var writeStorageAlertDialog: AlertDialog? = null

    override fun refresh(job: Job?, user: User?) {
        super.refresh(job, user)
        viewModel.setup(job)
        setupPlaceLabel()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentJobDetailsViewBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = activity
        binding.viewModel = viewModel
        binding.handlers = this
        viewModel.setup(job)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupPlaceLabel()

        activity?.let { activity ->
            val listView = activity.findViewById(R.id.jobDetails_jobAttachements) as? RecyclerView
            val adapter = JobAttachmentAdapter(this, viewModel.jobAttachments.value ?: listOf())
            adapter.onClickHandler = object: JobAttachmentAdapter.OnClickHandler {
                override fun onClick(view: View, jobAttachment: JobAttachment) {
                    onClickAttachmentDownloadLink(jobAttachment)
                }
            }
            listView?.adapter = adapter
        }
    }

    override fun onStart() {
        super.onStart()

        if (writeStorageAlertDialog != null) {
            if (activity?.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                writeStorageAlertDialog?.dismiss()
                this.writeStorageAlertDialog = null
                startDownloader()
            }
        }
    }

    override fun onClickOpenMap(view: View) {
        val uri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${job?.latitude?:0},${job?.longitude?:0}")
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    override fun onClickCopyAddress(view: View) {
        (activity?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)?.let { clipboard ->
            if ((job?.place?.hasEntries ?: false) || (job?.place?.workingPlaceShort?.isNullOrBlank() ?: true)) {
                var address: String = job?.place?.workingPlace ?: ""
                if (!(job?.place?.workingBuilding ?: "").isNullOrBlank()) {
                    address += job?.place?.workingBuilding ?: ""
                }
                val clip = ClipData.newPlainText("住所", address)
                clipboard.setPrimaryClip(clip)
            }
            else {
                var address = job?.place?.workingPlaceShort ?: ""
                val clip = ClipData.newPlainText("住所", address)
                clipboard.setPrimaryClip(clip)
            }
        }

        Toast.makeText(activity, "住所をコピーしました", Toast.LENGTH_LONG).show()
    }

    private fun setupPlaceLabel() {
        var tv = activity!!.findViewById<TextView>(R.id.jobDetailsView_placeLink)

        var place = SpannableStringBuilder()
        job?.let { job ->
            if ( (job.place?.hasEntries?: false) || (job.place?.workingPlaceShort.isNullOrBlank()) ) {
                place.append(job.place?.workingPlace?:"")
                if(!(job.place?.workingBuilding.isNullOrBlank())) {
                    place.append("\n${job.place?.workingBuilding}")
                }
            }else {
                place.append(job.place?.workingPlaceShort)
            }
            place.append("　")
            var start = place.length
            place.bold { append(ErikuraApplication.instance.getString(R.string.jobDetails_workingPlaceLink)) }
            var end = place.length
            val linkTextAppearanceSpan = TextAppearanceSpan(ErikuraApplication.instance.applicationContext, R.style.linkText)
            place.setSpan(linkTextAppearanceSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            place.setSpan(object : ClickableSpan() {
                override fun onClick(view: View) {
                    // 場所詳細画面へ遷移
                    val intent= Intent(activity, PlaceDetailActivity::class.java)
                    intent.putExtra("workingPlace", job.place)
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent)
                }
            }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        tv.text = place
        tv.movementMethod = LinkMovementMethod.getInstance()
    }

    fun onClickAttachmentDownloadLink(jobAttachment: JobAttachment) {
        // ダウンロード用のリクエストを作成します
        val request = DownloadManager.Request(Uri.parse(jobAttachment.url))
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, jobAttachment.filename)
        request.setTitle(jobAttachment.label)
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE or DownloadManager.Request.NETWORK_WIFI)
        request.setMimeType(jobAttachment.mimeType)
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        downloader.enqueue(request)

        // 書き込み権限をチェックします
        if (activity?.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            startDownloader()
        }
        else {
            requestPermissions(arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                ErikuraApplication.REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_ID)
        }
    }

    val WriteStorageCheckedNotAskAgainKey = "WRITE_EXTERNAL_STORAGE_CHECKED_NOT_ASK_AGAIN"
    var checkedNotAskAgain: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(ErikuraApplication.instance).getBoolean(WriteStorageCheckedNotAskAgainKey, false)
        set(value) {
            PreferenceManager.getDefaultSharedPreferences(ErikuraApplication.instance)
                .edit()
                .putBoolean(WriteStorageCheckedNotAskAgainKey, value)
                .commit()
        }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == ErikuraApplication.REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_ID) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkedNotAskAgain = false
                startDownloader()
            }
            else {
                checkedNotAskAgain = shouldShowRequestPermissionRationale(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                activity?.let { activity ->
                    this.writeStorageAlertDialog = MessageUtils.displayWriteExternalStorageAlert(activity) {
                        this.writeStorageAlertDialog = null
                    }
                }
            }
        }
    }

    fun startDownloader() {
        Toast.makeText(context, "ダウンロードを開始します", Toast.LENGTH_LONG).show()
        downloader.execute()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { status ->
                    Toast.makeText(context, "ダウンロード完了: ${status.result.title}", Toast.LENGTH_LONG).show()
                    Log.d(ErikuraApplication.LOG_TAG, "DOWNLOAD: ${status}")
                },
                onError = { e ->
                    Toast.makeText(context, "ダウンロード失敗", Toast.LENGTH_LONG).show()
                    Log.e(ErikuraApplication.LOG_TAG, e.message, e)
                },
                onComplete = {
                    Log.d(ErikuraApplication.LOG_TAG, "Download Completed")

                }
            )
    }
}

class JobDetailsViewFragmentViewModel: ViewModel() {
    val jobId: MutableLiveData<String> = MutableLiveData()
    val limit: MutableLiveData<String> = MutableLiveData()
    val msgVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val tool: MutableLiveData<String> = MutableLiveData()
    val summary: MutableLiveData<String> = MutableLiveData()
    val summaryTitles: MutableLiveData<String> = MutableLiveData()
    val summaryTitlesVisibility: MutableLiveData<Int> = MutableLiveData(View.VISIBLE)
    val openMapButtonText: MutableLiveData<SpannableString> = MutableLiveData()
    val workableTime =  MutableLiveData<String>()
    val workableTimeVisibility = MediatorLiveData<Int>().also { result ->
        result.addSource(workableTime) {
            result.value = if ((workableTime.value ?: "").isNotBlank()) { View.VISIBLE } else { View.GONE }
        }
    }
    val jobAttachments: MutableLiveData<List<JobAttachment>> = MutableLiveData()
    val jobAttachmentsVisibility = MediatorLiveData<Int>().also { result ->
        result.addSource(jobAttachments) { attachments: List<JobAttachment> ->
            result.value = if (attachments?.isNullOrEmpty()) { View.GONE } else { View.VISIBLE }
        }
    }

    fun setup(job: Job?){
        job?.let { job ->
            // お仕事ID
            jobId.value = job.id?.toString()
            // 納期
            setupLimit(job)
            // 持ち物
            setupTools(job)
            // 作業可能時間帯
            setupWorkableTime(job)
            // 仕事概要
            setupSummary(job)
            // 報告箇所
            setupSummaryTitles(job)
            // 添付ファイル
            setupJobAttachments(job)
        }

        val displayMetrics = ErikuraApplication.applicationContext.resources.displayMetrics
        val width = (16 * displayMetrics.density).toInt()
        val height = (16 * displayMetrics.density).toInt()
        val str = SpannableString(ErikuraApplication.instance.getString(R.string.openMap))
        val drawable = ContextCompat.getDrawable(ErikuraApplication.instance.applicationContext, R.drawable.link)
        drawable!!.setBounds(0, 0, width, height)
        val span = IconImageSpan(drawable, DynamicDrawableSpan.ALIGN_CENTER)
        str.setSpan(span, 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        openMapButtonText.value = str
    }

    /**
     * 納期表示を設定します
     */
    private fun setupLimit(job: Job) {
        if (job.entry != null && (job.entry?.owner ?: false)) {
            // 自身が応募したタスクの場合は、エントリ時刻と、24時間のリミット時刻を表示します
            var startAt = JobUtils.DateFormats.simple.format(job.entry?.createdAt ?: Date())
            var finishAt = JobUtils.DateFormats.simple.format(job.entry?.limitAt ?: Date())

            if (job.entry?.fromPreEntry == true) {
                // 先行応募で応募されていた場合
                // 先行応募の場合は納期の始まりを作業開始の時間、納期の締め切りを作業開始から24時間後の時刻とする
                startAt = JobUtils.DateFormats.simple.format( job.workingStartAt ?: Date())
                finishAt = JobUtils.DateFormats.simple.format( JobUtil.preEntryWorkingLimitAt(job.workingStartAt ?: Date()))
            }
            limit.value = "${startAt} 〜 ${finishAt}"
            job.entry?.limitAt?.also { limitAt ->
                if (!job.isReported && limitAt < Date()) {
                    msgVisibility.value = View.VISIBLE
                }
            }
        }
        else {
            // 自分が応募していないタスクについて募集期間を表示します
            if (job.isPreEntry) {
                // 先行応募中の場合 納期の締め切りを作業開始から24時間後の時刻とする
                val startAt = JobUtils.DateFormats.simple.format(job.workingStartAt ?: Date())
                val finishAt = JobUtils.DateFormats.simple.format( JobUtil.preEntryWorkingLimitAt(job.workingStartAt ?: Date()))
                limit.value = "${startAt} 〜 ${finishAt}"

            } else {
                val startAt = JobUtils.DateFormats.simple.format(job.workingStartAt ?: Date())
                val finishAt = JobUtils.DateFormats.simple.format(job.workingFinishAt ?: Date())
                limit.value = "${startAt} 〜 ${finishAt}"

            }
        }
    }

    /**
     * 持ち物を設定します
     */
    private fun setupTools(job: Job) {
        tool.value = job.tools
    }

    private fun setupWorkableTime(job: Job) {
        if ((job.workableStart ?: "").isNotBlank() && (job.workableFinish ?: "").isNotBlank()) {
            workableTime.value = "${job.workableStart}〜${job.workableFinish}の間に作業を実施してください(必須)"
        }
        else {
            workableTime.value = null
        }
    }
    /**
     * 仕事概要を設定します
     */
    private fun setupSummary(job: Job) {
        summary.value = job.summary
    }

    private fun setupSummaryTitles(job: Job) {
        // 報告箇所がからの場合は非表示とする
        if (job.summaryTitles.isEmpty()) {
            summaryTitlesVisibility.value = View.GONE
            summaryTitles.value = ""
        }
        else {
            summaryTitlesVisibility.value = View.VISIBLE
            summaryTitles.value = job.summaryTitles.joinToString("、")
        }
    }

    private fun setupJobAttachments(job: Job) {
        jobAttachments.value = job.jobAttachments
    }
}

interface JobDetailsViewFragmentEventHandlers {
    fun onClickOpenMap(view: View)
    fun onClickCopyAddress(view: View)
}

class IconImageSpan(drawable: Drawable, verticalAlignment: Int): ImageSpan(drawable, verticalAlignment) {
    private var drawableRef: WeakReference<Drawable>? = null

    override fun draw(canvas: Canvas,
                      text: CharSequence?, start: Int, end: Int,
                      x: Float, top: Int, y: Int, bottom: Int,
                      paint: Paint) {
        val drawable = getCachedDrawable()
        canvas.save()

        val transY: Float = (y - drawable.bounds.bottom + paint.fontMetricsInt.descent).toFloat()
        canvas.translate(x, transY)
        drawable.draw(canvas)

        canvas.restore()
    }

    private fun getCachedDrawable(): Drawable {
        return drawableRef?.let {
            it.get()
        } ?: run {
            val drawable = getDrawable()
            drawableRef = WeakReference(drawable)
            drawable
        }
    }
}

interface JobAttachmentItemHandler {
    fun onClick(view: View)
}

class JobAttachmentAdapter(val owner: LifecycleOwner, var jobAttachments: List<JobAttachment>) : RecyclerView.Adapter<JobAttachmentAdapter.JobAttachmentViewHolder>() {
    interface OnClickHandler {
        fun onClick(view: View, jobAttachment: JobAttachment)
    }
    class JobAttachmentItemViewModel(val jobAttachment: JobAttachment): ViewModel() {
        val labelWithAnchor: SpannableString get() {
            val spannableString = SpannableString(jobAttachment.label)
            spannableString.setSpan(UnderlineSpan(), 0, jobAttachment.label.length, 0)
            return spannableString
        }
    }
    class JobAttachmentViewHolder(val binding: FragmentJobAttachmentListItemBinding): RecyclerView.ViewHolder(binding.root)

    var onClickHandler: OnClickHandler? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobAttachmentViewHolder {
        val binding = DataBindingUtil.inflate<FragmentJobAttachmentListItemBinding>(
            LayoutInflater.from(parent.context),
            R.layout.fragment_job_attachment_list_item,
            parent,
            false
        )
        return JobAttachmentViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return jobAttachments.count()
    }

    override fun onBindViewHolder(holder: JobAttachmentViewHolder, position: Int) {
        val jobAttachment = jobAttachments[position]
        holder.binding.handler = object: JobAttachmentItemHandler {
            override fun onClick(view: View) {
                onClickHandler?.onClick(view, jobAttachment)
                Log.d(ErikuraApplication.LOG_TAG, "URL: ${jobAttachment.url}")
            }
        }
        holder.binding.viewModel = JobAttachmentItemViewModel(jobAttachment)
        holder.binding.lifecycleOwner = owner
    }
}

class RxDownloader(private val context: Context, private val requests: ArrayList<DownloadManager.Request> = ArrayList()) {
    private val downloadManager: DownloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val queuedRequests: HashMap<Long, DownloadManager.Request> = HashMap()
    private var receiver: BroadcastReceiver? = null

    fun enqueue(request: DownloadManager.Request): RxDownloader = apply {
        synchronized(this) {
            requests.add(request)
        }
    }

    fun execute(): Observable<DownloadStatus> {
        if (requests.isEmpty()) {
            return Observable.empty()
        }
        return Observable.create { emitter ->
            receiver = object: BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent == null) return

                    if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == intent.action) {
                        val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                        if (!queuedRequests.contains(id)) {
                            return
                        }

                        resolveDownloadStatus(id, emitter)
                        queuedRequests.remove(id)
                        if (queuedRequests.isEmpty()) {
                            emitter.onComplete()
                        }
                    }
                }
            }
            context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

            synchronized(this) {
                requests.forEach {
                    val downloadId = downloadManager.enqueue(it)
                    queuedRequests.put(downloadId, it)
                    Log.d(ErikuraApplication.LOG_TAG, "Start Download: ID=${downloadId}")
                }
                // 全てキューに入っているので、requests はクリアします
                requests.clear()
            }

            emitter.setCancellable {
                queuedRequests.forEach {
                    downloadManager.remove(it.key)
                }
                receiver?.let {
                    context.unregisterReceiver(it)
                }
            }
        }
    }

    private fun resolveDownloadStatus(id: Long, emitter: ObservableEmitter<DownloadStatus>) {
        val query = DownloadManager.Query().apply {
            setFilterById(id)
        }

        val cursor = downloadManager.query(query)
        if (cursor.moveToFirst()) {
            val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
            val reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON))
            val requestResult: RequestResult = createRequestResult(id, cursor)
            Log.d(ErikuraApplication.LOG_TAG, "Download Finished: RESULT=${requestResult}, status=${status}, reason=${reason}")

            when(status) {
                DownloadManager.STATUS_FAILED -> {
                    val failedReason = "REASON=${reason}"
                    Log.d(ErikuraApplication.LOG_TAG, "Download: ID=${id}, FAILED, reason=${reason}")
                    emitter.onNext(DownloadStatus.Failed(requestResult, failedReason))
                    emitter.onError(DownloadFailedException(failedReason, queuedRequests[id]))
                }
                DownloadManager.STATUS_PAUSED -> {
                    val pausedReason = "REASON=${reason}"
                    Log.d(ErikuraApplication.LOG_TAG, "Download: ID=${id}, PAUSED, reason=${reason}")
                    emitter.onNext(DownloadStatus.Paused(requestResult, pausedReason))
                }
                DownloadManager.STATUS_PENDING -> {
                    Log.d(ErikuraApplication.LOG_TAG, "Download: ID=${id}, PENDING")
                    emitter.onNext(DownloadStatus.Pending(requestResult))
                }
                DownloadManager.STATUS_RUNNING -> {
                    Log.d(ErikuraApplication.LOG_TAG, "Download: ID=${id}, RUNNING")
                    emitter.onNext(DownloadStatus.Running(requestResult))
                }
                DownloadManager.STATUS_SUCCESSFUL -> {
                    Log.d(ErikuraApplication.LOG_TAG, "Download: ID=${id}, SUCCESSFUL")
                    emitter.onNext(DownloadStatus.Successful(requestResult))
                }
            }
        }
        cursor.close()
    }

    private fun createRequestResult(id: Long, cursor: Cursor): RequestResult =
        RequestResult(
            id = id,
            remoteUri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_URI)),
            localUri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)),
            mediaType = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE)),
            totalSize = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)),
            title = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_TITLE)),
            description = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_DESCRIPTION))
        )

    data class RequestResult(
        val id: Long,
        val remoteUri: String,
        val localUri: String,
        val mediaType: String,
        val totalSize: Int,
        val title: String?,
        val description: String?
    )

    sealed class DownloadStatus(val result: RequestResult) {
        class Successful(result: RequestResult) : DownloadStatus(result)
        class Running(result: RequestResult) : DownloadStatus(result)
        class Pending(result: RequestResult) : DownloadStatus(result)
        class Paused(result: RequestResult, val reason: String) : DownloadStatus(result)
        class Failed(result: RequestResult, val reason: String) : DownloadStatus(result)
    }

    // 再リクエストできるようにRequestを持たせるようにする
    class DownloadFailedException(message: String, val request: DownloadManager.Request?) : Throwable(message)
}
