package jp.co.recruit.erikura.presenters.activities.job

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.DialogFragment
import jp.co.recruit.erikura.R
import android.view.LayoutInflater
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.fitness.data.Value
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.DialogStartBinding
import jp.co.recruit.erikura.databinding.DialogStopBinding
import jp.co.recruit.erikura.presenters.util.GoogleFitApiManager
import jp.co.recruit.erikura.presenters.util.LocationManager
import java.util.*

class StopDialogFragment(private val job: Job?, private val steps: Int?) : DialogFragment(), StopDialogFragmentEventHandlers  {
    private val viewModel by lazy {
        ViewModelProvider(this).get(StopDialogFragmentViewModel::class.java)
    }

    private val fitApiManager: GoogleFitApiManager = ErikuraApplication.fitApiManager
    private val locationManager: LocationManager = ErikuraApplication.locationManager

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DataBindingUtil.inflate<DialogStopBinding>(
            LayoutInflater.from(activity),
            R.layout.dialog_stop,
            null,
            false
        )
        binding.lifecycleOwner = activity
        viewModel.setup(job)
        binding.viewModel = viewModel
        binding.handlers = this

        val builder = AlertDialog.Builder(activity)
        builder.setView(binding.root)
        return builder.create()
    }

    override fun onClikStop(view: View) {
        // stopJobの呼び出し
        job?.let {
            Api(activity!!).stopJob(job, locationManager.latLng ?: locationManager.latLngOrDefault, steps?: 0, 0.0) {
                val intent= Intent(activity, WorkingFinishedActivity::class.java)
                intent.putExtra("job", job)
                startActivity(intent)
            }
        }
//        if (fitApiManager.checkPermission()) {
//            job?.let {
//                var steps = 0
//                var distance = 0.0
//                var startTime = job.entry?.startedAt ?: job.entry?.createdAt ?: Date()
//                fitApiManager.readAggregateStepDelta(
//                    fitApiManager.setAccount(activity!!),
//                    startTime,
//                    Date(),
//                    activity!!
//                ) {
//                    Log.v("Step", "$it")
//                    steps = if(it == Value(0)) {0}else {it.asInt()}
//                    fitApiManager.readAggregateDistanceDelta(
//                        fitApiManager.setAccount(activity!!),
//                        startTime,
//                        Date(),
//                        activity!!
//                    ) {
//                        Log.v("Distance", "$it")
//                        distance = if(it == Value(0)) {0.0}else {it.asFloat().toDouble()}
//                        // stopJobの呼び出し
//                        Api(activity!!).stopJob(job, locationManager.latLng ?: locationManager.latLngOrDefault, steps, distance) {
//                            val intent= Intent(activity, WorkingFinishedActivity::class.java)
//                            intent.putExtra("job", job)
//                            startActivity(intent)
//                        }
//                    }
//                }
//            }
//
//        }else {
//            job?.let {
//                Api(activity!!).stopJob(it, locationManager.latLng ?: locationManager.latLngOrDefault, 0, 0.0) {
//                    val intent= Intent(activity, WorkingFinishedActivity::class.java)
//                    intent.putExtra("job", job)
//                    startActivity(intent)
//                }
//            }
//        }
    }
}

class StopDialogFragmentViewModel: ViewModel() {
    val caption: MutableLiveData<String> = MutableLiveData()
    val reportPlaces: MutableLiveData<String> = MutableLiveData()
    val reportPlacesVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)

    fun setup(job: Job?) {
        if (job != null) {
            if(!job.summaryTitles.isNullOrEmpty()) {
                caption.value = ErikuraApplication.instance.getString(R.string.applyDialog_caption2Pattern1)
                var summaryTitleStr = ""
                job.summaryTitles.forEachIndexed { index, s ->
                    summaryTitleStr += "(${index+1}) ${s}　"
                }
                reportPlaces.value = summaryTitleStr
                reportPlacesVisibility.value = View.VISIBLE
            }else {
                caption.value = ErikuraApplication.instance.getString(R.string.applyDialog_caption2Pattern2)
                reportPlacesVisibility.value = View.GONE
            }
        }
    }
}

interface StopDialogFragmentEventHandlers {
    fun onClikStop(view: View)
}