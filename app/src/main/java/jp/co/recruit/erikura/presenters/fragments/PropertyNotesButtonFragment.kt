package jp.co.recruit.erikura.presenters.fragments

import android.content.Intent
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.FragmentPropertyNotesButtonBinding

class PropertyNotesButtonFragment : BaseJobDetailFragment, PropertyNotesButtonFragmentEventHandlers {
    companion object {
        fun newInstance(job: Job?, user: User?): PropertyNotesButtonFragment {
            return PropertyNotesButtonFragment().also {
                it.arguments = Bundle().also { args ->
                    fillArguments(args, job, user)
                }
            }
        }
    }

    constructor(): super()

    override fun refresh(job: Job?, user: User?) {
        super.refresh(job, user)
        createPropertyNotesButtonText(job?.cautionsCount ?: 0)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentPropertyNotesButtonBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = activity
        binding.handler = this
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        createPropertyNotesButtonText(job?.cautionsCount ?: 0)
    }

    override fun onClickPropertyNotes(view: View) {
        // ページ参照のトラッキングの送出
        job?.let { job ->
            Tracking.logEvent(event= "view_cautions", params= bundleOf())
            Tracking.viewCautions(name= "/places/cautions", title= "物件注意事項画面表示", jobId= job.id, placeId=job.placeId)

            val intent = Intent(activity, jp.co.recruit.erikura.presenters.activities.job.PropertyNotesActivity::class.java)
            intent.putExtra("place_id", job.placeId)
            intent.putExtra("job_kind_id", job.jobKind?.id)
            startActivity(intent)
        }
    }

    private fun createPropertyNotesButtonText(cautionsCount: Int) {
        val button:  Button = activity!!.findViewById(R.id.property_notes_button_text)
        button.setAllCaps(false)
        var count = "（${cautionsCount}件）"
        var buttonText = ErikuraApplication.instance.getString(R.string.property_notes_title) + count
        if (cautionsCount > 0) {
        var spanColor: Int = ContextCompat.getColor(activity!!, R.color.coral)
            SpannableStringBuilder(buttonText).let {
               it.setSpan(ForegroundColorSpan(spanColor), buttonText.indexOf("（"), buttonText.length, 0)
                button.text = it.subSequence(0, it.length)
            }
        } else {
            button.text = buttonText
        }
    }
}

interface PropertyNotesButtonFragmentEventHandlers {
    fun onClickPropertyNotes(view: View)
}