package jp.co.recruit.erikura.presenters.activities.job

import android.app.ActivityOptions
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.model.LatLng
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.databinding.DialogJobSelectBinding
import jp.co.recruit.erikura.databinding.FragmentJobListItemBinding
import jp.co.recruit.erikura.presenters.view_models.JobListItemViewModel

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
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(activity).toBundle())
        dialog?.dismiss()
    }

    override fun onClickClose(view: View) {
        dialog?.dismiss()
    }
}

interface JobSelectDialogHandler {
    fun onClickClose(view: View)
}

class JobListAdapter(private val activity: FragmentActivity, var jobs: List<Job>, var currentPosition: LatLng?) : RecyclerView.Adapter<JobListHolder>() {
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
        holder.binding.viewModel = JobListItemViewModel(activity, jobs[position], currentPosition)
        holder.binding.lifecycleOwner = activity

        holder.binding.root.setOnClickListener {
            onClickListner?.apply {
                onClick(jobs[position])
            }
        }
    }

    interface OnClickListener {
        fun onClick(job: Job)
    }
}

class JobListHolder(val binding: FragmentJobListItemBinding) : RecyclerView.ViewHolder(binding.root)

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
