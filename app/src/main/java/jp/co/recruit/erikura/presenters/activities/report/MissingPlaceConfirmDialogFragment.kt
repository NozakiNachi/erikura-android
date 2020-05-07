package jp.co.recruit.erikura.presenters.activities.report

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.databinding.DialogMissingPlaceConfirmBinding
import jp.co.recruit.erikura.presenters.util.setOnSafeClickListener


class MissingPlaceConfirmDialogFragment(private val missingPlaces: List<String>): DialogFragment()  {
    private val viewModel: MissingPlaceConfirmViewModel by lazy {
        ViewModelProvider(this).get(MissingPlaceConfirmViewModel::class.java)
    }
    var onClickListener: OnClickListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DataBindingUtil.inflate<DialogMissingPlaceConfirmBinding>(
            LayoutInflater.from(activity),
            R.layout.dialog_missing_place_confirm,
            null,
            false
        )
        binding.lifecycleOwner = activity
        binding.viewModel = viewModel

        viewModel.msg.value = createMsg()

        val builder = AlertDialog.Builder(activity)
        builder.setView(binding.root)

        binding.root.findViewById<Button>(R.id.missing_place_confirm_complete_button).setOnSafeClickListener {
            onClickListener?.apply {
                onClickComplete()
            }
        }

        return builder.create()
    }

    override fun onStart() {
        super.onStart()
        // ページ参照のトラッキングの送出
        Tracking.logEvent(event= "error_modal", params= bundleOf())
        Tracking.track(name= "error_modal")
    }

    private fun createMsg(): String {
        var msg = ErikuraApplication.instance.getString(R.string.report_confirm_missing_places)
        msg += "\n"
        missingPlaces.forEach {
            msg += it
        }
        msg += "\n"
        msg += ErikuraApplication.instance.getString(R.string.report_confirm_missing_places2)
        return msg
    }

    interface OnClickListener {
        fun onClickComplete()
    }
}

class MissingPlaceConfirmViewModel: ViewModel() {
    val msg: MutableLiveData<String> = MutableLiveData()
}
