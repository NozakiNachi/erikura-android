package jp.co.recruit.erikura.presenters.fragments

import android.content.Intent
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Caution
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.FragmentPropertyNotesButtonBinding
import jp.co.recruit.erikura.presenters.activities.job.PropertyNotesActivity

class PropertyNotesFragment : BaseJobDetailFragment, PropertyNotesFragmentEventHandlers {
    companion object {
        fun newInstance(job: Job?, user: User?, cautionsCount: Int?): PropertyNotesFragment {
            return PropertyNotesFragment().also {
                it.arguments = Bundle().also { args ->
                    fillArguments(args, job, user, cautionsCount)
                }
            }
        }
    }

    private val viewModel: PropertyNotesViewModel by lazy {
        ViewModelProvider(this).get(PropertyNotesViewModel::class.java)
    }

    constructor(): super()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        createPropertyNotesButtonText(cautionsCount?: 0)
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
        val textView: TextView = activity!!.findViewById(R.id.property_notes_button_text)
        var count = "（0件）"
        if (cautionsCount > 0) {
            viewModel.isNotEmptyCaution.value = true
            count = "（${cautionsCount}件）"
        }
        // FIXME 下記のテキストの色分けは一旦保留
//        var spanColor: Int = FF4242
//        if (cautions.isNotEmpty()) {
//            viewModel.isNotEmptyCaution.value = true
//            var num = cautions.count()
//            count = "（${num}件）"
//            var button_text = ErikuraApplication.instance.getString(R.string.property_notes_title) + count
//            SpannableStringBuilder(button_text).let {
//                it.setSpan(ForegroundColorSpan(spanColor), button_text.indexOf("（"), button_text.indexOf("）"), 0)
//                textView.text = it.subSequence(0, it.length)
//            }
//        }
//        else {
            viewModel.isNotEmptyCaution.value = false
            textView.text = ErikuraApplication.instance.getString(R.string.property_notes_title) + count
//        }
    }
}

class PropertyNotesViewModel: ViewModel() {
    val isNotEmptyCaution = MediatorLiveData<Boolean>()
    val isButtonEnabled = MediatorLiveData<Boolean>().also { result ->
        result.value = isNotEmptyCaution.value
    }
//    val propertyNotesButtonText: MutableLiveData<String> = MutableLiveData()
}

interface PropertyNotesFragmentEventHandlers {
    fun onClickPropertyNotes(view: View)
}