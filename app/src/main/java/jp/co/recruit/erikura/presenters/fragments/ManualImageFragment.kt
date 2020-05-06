package jp.co.recruit.erikura.presenters.fragments

import JobUtil
import android.app.Activity
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.databinding.FragmentManualImageBinding

class ManualImageFragment(job: Job?, user: User?) : BaseJobDetailFragment(job, user), ManualImageFragmentEventHandlers {
    private val viewModel by lazy {
        ViewModelProvider(this).get(ManualImageFragmentViewModel::class.java)
    }

    override fun refresh(job: Job?, user: User?) {
        super.refresh(job, user)
        activity?.let {
            viewModel.setup(it, view!!)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentManualImageBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = activity
        binding.viewModel = viewModel
        binding.handler = this
        viewModel.setup(activity!!, binding.root)
        return binding.root
    }

    override fun onClickManualImage(view: View) {
        if(job?.manualUrl != null){
            activity?.let { activity ->
                JobUtil.openManual(activity, job!!)
            }
        }
    }

}

class ManualImageFragmentViewModel: ViewModel() {
    val bitmap: MutableLiveData<Bitmap> = MutableLiveData()

    fun setup(activity: Activity, root: View) {
        val imageView: ImageView = root.findViewById(R.id.manual_image)
        val assetsManager = ErikuraApplication.assetsManager
        val url = ErikuraApplication.instance.getString(R.string.jobDetails_manualImageURL)

        assetsManager.fetchImage(activity, url, imageView)
    }
}

interface ManualImageFragmentEventHandlers {
    fun onClickManualImage(view: View)
}