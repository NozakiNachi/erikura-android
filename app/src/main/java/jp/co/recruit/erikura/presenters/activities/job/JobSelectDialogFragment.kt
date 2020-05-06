package jp.co.recruit.erikura.presenters.activities.job

import JobUtil
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.gms.maps.model.LatLng
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.databinding.DialogJobSelectBinding
import jp.co.recruit.erikura.databinding.FragmentJobListItemBinding
import jp.co.recruit.erikura.presenters.util.setOnSafeClickListener
import jp.co.recruit.erikura.presenters.view_models.JobListItemViewModel
import java.io.File

class JobSelectDialogFragment(val jobs: List<Job>): DialogFragment(), JobSelectDialogHandler {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(activity!!)?.also { dialog ->

            dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
            dialog.window?.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)

            val binding = DataBindingUtil.inflate<DialogJobSelectBinding>(
                LayoutInflater.from(activity),
                R.layout.dialog_job_select,
                null,
                false
            )
            binding.lifecycleOwner = activity
            binding.handler = this

            dialog.setContentView(binding.root)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            val metrics = resources.displayMetrics

            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                metrics.heightPixels - (260 * metrics.density).toInt())

            dialog.setCanceledOnTouchOutside(true)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        dialog?.window?.let { window ->

            val lp = window.attributes
            lp.gravity = Gravity.BOTTOM
            window.attributes = lp

            val adapter = JobListAdapter(this@JobSelectDialogFragment.activity!!, jobs, null)
            adapter.onClickListner = object: JobListAdapter.OnClickListener {
                override fun onClick(job: Job) {
                    onClickCarouselItem(job)
                }
            }

            val recyclerView: RecyclerView = window.findViewById(R.id.job_select_dialog_recycler_view)
            recyclerView.setHasFixedSize(true)
            recyclerView.adapter = adapter
            recyclerView.addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
            recyclerView.addItemDecoration(JobListItemDecorator())
        }
    }

    // カルーセルクリック時の処理
    fun onClickCarouselItem(job: Job) {
        Log.v("ErikuraCarouselCel", "Click: ${job.toString()}")

        val intent= Intent(activity, JobDetailsActivity::class.java)
        intent.putExtra("job", job)
        startActivity(intent)
        dialog?.dismiss()
    }

    override fun onClickClose(view: View) {
        dialog?.dismiss()
    }
}

interface JobSelectDialogHandler {
    fun onClickClose(view: View)
}

class JobListAdapter(private val activity: FragmentActivity, var jobs: List<Job>, var currentPosition: LatLng? = null, val timeLabelType: JobUtil.TimeLabelType = JobUtil.TimeLabelType.SEARCH) : RecyclerView.Adapter<JobListHolder>() {
    var onClickListner: OnClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobListHolder {
        val binding = DataBindingUtil.inflate<FragmentJobListItemBinding>(
            LayoutInflater.from(parent.context),
            R.layout.fragment_job_list_item,
            parent,
            false
        )

        return JobListHolder(binding)
    }

    override fun getItemCount(): Int {
        return jobs.count()
    }

    override fun onBindViewHolder(holder: JobListHolder, position: Int) {
        holder.currentPosition = position
        holder.binding.viewModel = JobListItemViewModel(activity, jobs[position], currentPosition = currentPosition, timeLabelType = timeLabelType)
        holder.binding.lifecycleOwner = activity

        holder.binding.root.setOnSafeClickListener {
            onClickListner?.apply {
                onClick(jobs[position])
            }
        }

        val imageView: ImageView = holder.binding.root.findViewById(R.id.job_list_item_image)
        // ダウンロード
        val job = jobs[position]
        val thumbnailUrl = if (!job.thumbnailUrl.isNullOrBlank()) {job.thumbnailUrl}else {job.jobKind?.noImageIconUrl?.toString()}
        imageView.setImageDrawable(ErikuraApplication.instance.applicationContext.resources.getDrawable(R.drawable.ic_noimage, null))
        if (!thumbnailUrl.isNullOrBlank()) {
            val assetsManager = ErikuraApplication.assetsManager
            assetsManager.fetchAsset(activity, thumbnailUrl) { asset ->
                // 画像取得中に別のカルーセルに移動する可能性があるので、position が一致していることを確認する
                if (position == holder.currentPosition) {
                    Glide.with(activity).load(File(asset.path)).fitCenter().into(imageView)
                }
            }
        }
    }

    interface OnClickListener {
        fun onClick(job: Job)
    }
}

class JobListHolder(val binding: FragmentJobListItemBinding) : RecyclerView.ViewHolder(binding.root) {
    var currentPosition: Int = -1
}

class JobListItemDecorator: RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        outRect.top = view.resources.getDimensionPixelSize(R.dimen.job_list_item_margin)
        outRect.bottom = view.resources.getDimensionPixelSize(R.dimen.job_list_item_margin)
    }
}
