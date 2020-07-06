package jp.co.recruit.erikura.presenters.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.databinding.FragmentPropertyNotesButtonBinding
import jp.co.recruit.erikura.presenters.activities.job.PropertyNotesActivity

class PropertyNotesButtonButtonFragment : BaseJobDetailFragment, PropertyNotesButtonFragmentEventHandlers {
    companion object {
        fun newInstance(job: Job?, user: User?, cautionsCount: Int?): PropertyNotesButtonButtonFragment {
            return PropertyNotesButtonButtonFragment().also {
                it.arguments = Bundle().also { args ->
                    fillArguments(args, job, user, cautionsCount)
                }
            }
        }
    }

    private val viewModel: PropertyNotesButtonViewModel by lazy {
        ViewModelProvider(this).get(PropertyNotesButtonViewModel::class.java)
    }

    constructor(): super()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        createPropertyNotesButtonText(job?.cautionsCount?: 0)
        val binding = FragmentPropertyNotesButtonBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = activity
        binding.handler = this
        return binding.root
    }

    override fun onClickPropertyNotes(view: View) {
        val intent = Intent(activity, PropertyNotesActivity::class.java)
        intent.putExtra("place_id", job?.placeId)
        startActivity(intent)
    }

    fun createPropertyNotesButtonText(cautionsCount: Int) {
        val textView: TextView? = activity!!.findViewById(R.id.property_notes_button_text)
        var count = "（0件）"
        var buttonText = ErikuraApplication.instance.getString(R.string.property_notes_title) + count
        viewModel.isNotEmptyCaution.value = false
        if (cautionsCount > 0) {
            viewModel.isNotEmptyCaution.value = true
            count = "（${cautionsCount}件）"

        // FIXME 下記のテキストの色分けは一旦保留
        var spanColor: Int = ContextCompat.getColor(activity!!, R.color.coral)
            buttonText = ErikuraApplication.instance.getString(R.string.property_notes_title) + (count to spanColor)
//            SpannableStringBuilder(button_text).let {
//                it.setSpan(ForegroundColorSpan(spanColor), button_text.indexOf("（"), button_text.indexOf("）"), 0)
//                textView.text = it.subSequence(0, it.length)
//            }
            textView?.text = buttonText
        }
    }
}

class PropertyNotesButtonViewModel: ViewModel() {
    val isNotEmptyCaution = MediatorLiveData<Boolean>()
    val isButtonEnabled = MediatorLiveData<Boolean>().also { result ->
        result.value = isNotEmptyCaution.value
    }
//    val propertyNotesButtonText: MutableLiveData<String> = MutableLiveData()
}

interface PropertyNotesButtonFragmentEventHandlers {
    fun onClickPropertyNotes(view: View)
}