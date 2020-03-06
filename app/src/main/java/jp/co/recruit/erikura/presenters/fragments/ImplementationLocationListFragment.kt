package jp.co.recruit.erikura.presenters.fragments

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.model.LatLng
import jp.co.recruit.erikura.ErikuraApplication

import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.OutputSummary
import jp.co.recruit.erikura.databinding.FragmentImplementationLocationListBinding

//FIXME:削除予定クラス

class ImplementationLocationListFragment : AppCompatActivity() {
    private val viewModel by lazy {
        ViewModelProvider(this).get(ImplementationLocationListViewModel::class.java)
    }

    var job = Job()
    var fromConfirm = false
    var pictureIndex = 0
    var outputSummaryList: MutableList<OutputSummary> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_implementation_location_list)

        val binding: FragmentImplementationLocationListBinding = DataBindingUtil.setContentView(this,R.layout.fragment_implementation_location_list)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        val reportSummaryView: RecyclerView = findViewById(R.id.report_confirm_report_summaries)
        reportSummaryView.setHasFixedSize(true)
    }

    override fun onStart() {
        super.onStart()
        job = intent.getParcelableExtra<Job>("job")
        pictureIndex = intent.getIntExtra("pictureIndex", 0)
        fromConfirm = intent.getBooleanExtra("fromConfirm", false)
        outputSummaryList = job.report?.outputSummaries?.toMutableList()?: mutableListOf()

        setup()
    }

    private fun setup() {
        val max = (job.report?.outputSummaries?.lastIndex?: 0) + 1
        viewModel.summaryTitle.value = ErikuraApplication.instance.getString(R.string.report_form_caption, pictureIndex+1, max)
        createImage()
    }
    private fun createImage() {
        val imageView: ImageView = findViewById(R.id.report_form_image)
        job.report?.let {
            val item = it.outputSummaries[pictureIndex].photoAsset
            item?.let {
                item.loadImage(this, imageView)
            }
        }
    }
}

class ImplementationLocationListViewModel( activity: Activity, view: View, summary: OutputSummary, summariesCount: Int, position: Int,val job: Job, val timeLabelType: JobUtil.TimeLabelType): ViewModel() {
    private val imageView: ImageView = view.findViewById(R.id.report_summary_item_image)

    val goodCount: Int get() = job?.report?.operatorLikeCount ?: 0
    val commentCount: Int get() = job?.report?.operatorCommentsCount ?: 0
    val goodText: String get() = String.format("%,d件", goodCount)
    val commentText: String get() = String.format("%,d件", commentCount)
    val hasGood: Boolean get() = goodCount > 0
    val hasComment: Boolean get() = commentCount > 0

    val summaryTitle: MutableLiveData<String> = MutableLiveData()
    val summaryName: MutableLiveData<String> = MutableLiveData()
    val summaryStatus: MutableLiveData<String> = MutableLiveData()
    val summaryComment: MutableLiveData<String> = MutableLiveData()

    val goodVisible: Int get() = if (timeLabelType == JobUtil.TimeLabelType.OWNED && hasGood) { View.VISIBLE } else { View.GONE }
    val commentVisible: Int get() = if (timeLabelType == JobUtil.TimeLabelType.OWNED && hasComment) { View.VISIBLE } else { View.GONE }
    init {
        summary.photoAsset?.let {
            it.loadImage(activity, imageView)
        }

        summaryTitle.value = ErikuraApplication.instance.getString(R.string.report_form_caption, position+1, summariesCount)
        summaryName.value = summary.place
        summaryStatus.value = summary.evaluation
        summaryComment.value = summary.comment
    }
}