package jp.co.recruit.erikura.presenters.fragments

import android.content.Intent
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.FragmentPropertyNotesButtonBinding
import jp.co.recruit.erikura.presenters.activities.job.PropertyNotesActivity

class PropertyNotesButtonButtonFragment : BaseJobDetailFragment, PropertyNotesButtonFragmentEventHandlers {
    companion object {
        fun newInstance(job: Job?, user: User?): PropertyNotesButtonButtonFragment {
            return PropertyNotesButtonButtonFragment().also {
                it.arguments = Bundle().also { args ->
                    fillArguments(args, job, user)
                }
            }
        }
    }

    private val viewModel: PropertyNotesButtonViewModel by lazy {
        ViewModelProvider(this).get(PropertyNotesButtonViewModel::class.java)
    }

    constructor(): super()

    override fun refresh(job: Job?, user: User?) {
        super.refresh(job, user)
        job?.let {
            Api(activity!!).reloadJob(it) { get_job ->
                createPropertyNotesButtonText(get_job.cautionsCount ?: 0)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        job?.let {
            Api(activity!!).reloadJob(it) { get_job ->
                createPropertyNotesButtonText(get_job.cautionsCount ?: 0)
            }
        }
        val binding = FragmentPropertyNotesButtonBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = activity
        binding.handler = this
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onClickPropertyNotes(view: View) {
        // ページ参照のトラッキングの送出
        job?.let { job ->
        Tracking.logEvent(event= "view_cautions", params= bundleOf())
        Tracking.viewCautions(name= "/places/cautions", title= "物件注意事項画面表示", jobId= job.id, placeId=job.placeId)

        val intent = Intent(activity, jp.co.recruit.erikura.presenters.activities.job.PropertyNotesActivity::class.java)
        intent.putExtra("place_id", job.placeId)
        startActivity(intent)
        }
    }

    private fun createPropertyNotesButtonText(cautionsCount: Int) {
        val button:  Button = activity!!.findViewById(R.id.property_notes_button_text)
        button.setAllCaps(false)
        var count = "（${cautionsCount}件）"
        var buttonText = ErikuraApplication.instance.getString(R.string.property_notes_title) + count
        if (cautionsCount > 0) {
        // FIXME 下記のテキストの色分けは一旦保留
        var spanColor: Int = ContextCompat.getColor(activity!!, R.color.coral)
            SpannableStringBuilder(buttonText).let {
               it.setSpan(ForegroundColorSpan(spanColor), buttonText.indexOf("（"), buttonText.indexOf("）"), 0)
//                viewModel.buttonText.value = it.subSequence(0, it.length).toString()
                button.text = it.subSequence(0, it.length).toString()
            }
        } else {
//            viewModel.buttonText.value = buttonText
            button.text = buttonText
        }
    }
}

class PropertyNotesButtonViewModel: ViewModel() {

//    var buttonText: MutableLiveData<String> = MutableLiveData()
//    val propertyNotesButtonText: MutableLiveData<String> = MutableLiveData()
}

interface PropertyNotesButtonFragmentEventHandlers {
    fun onClickPropertyNotes(view: View)
}