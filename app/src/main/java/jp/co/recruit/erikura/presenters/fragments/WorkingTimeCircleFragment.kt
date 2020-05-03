package jp.co.recruit.erikura.presenters.fragments

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.databinding.FragmentWorkingTimeCircleBinding
import jp.co.recruit.erikura.presenters.activities.job.JobDetailsActivity
import java.util.*

class WorkingTimeCircleFragment(private val job: Job?) : Fragment(), WorkingTimeCircleFragmentEventHandlers {
    private val viewModel: WorkingTimeCircleFragmentViewModel by lazy {
        ViewModelProvider(this).get(WorkingTimeCircleFragmentViewModel::class.java)
    }
    private var timer: Timer = Timer()
    private var timerHandler: Handler = Handler()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        container?.removeAllViews()
        val binding = FragmentWorkingTimeCircleBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handler = this
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        updateTimer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                timerHandler.post(Runnable {
                    updateTimer()
                })
            }
        }, 1000, 1000) // 実行したい間隔(ミリ秒)
    }

    override fun onClickCircle(view: View) {
        timer.cancel()
        val intent= Intent(activity, JobDetailsActivity::class.java)
        intent.putExtra("job", job)
        startActivity(intent)
    }

    // 1秒ごとに呼び出される処理
    private fun updateTimer(){
        job?.let {
            var now = Date()
            var startTime = job.entry?.startedAt?: job.entry?.createdAt?: now
            var diff = now.time - startTime.time
            // タイマーの表示形式を設定
            var total_in_seconds = diff / 1000
            var second = (total_in_seconds % 60).toInt()
            var minute = (total_in_seconds / 60).toInt()
            if(minute == 0){
                viewModel.workingTime.value = String.format("%d秒", second)
            } else {
                viewModel.workingTime.value = String.format("%d分%d秒", minute, second)
            }
        }
    }
}

class WorkingTimeCircleFragmentViewModel: ViewModel() {
    val workingTime: MutableLiveData<String> = MutableLiveData("0分0秒")
}

interface WorkingTimeCircleFragmentEventHandlers {
    fun onClickCircle(view: View)
}
