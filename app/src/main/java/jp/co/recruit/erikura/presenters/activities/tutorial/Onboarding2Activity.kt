package jp.co.recruit.erikura.presenters.activities.tutorial

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import io.reactivex.android.schedulers.AndroidSchedulers
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.databinding.ActivityOnboarding2Binding
import java.util.*

class Onboarding2Activity : AppCompatActivity(), Onboarding2Handlers {
    val timer: Timer = Timer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityOnboarding2Binding =
            DataBindingUtil.setContentView(this, R.layout.activity_onboarding2)
        binding.lifecycleOwner = this
        binding.handlers = this
    }

    override fun onResume() {
        super.onResume()

//        timer.schedule(object: TimerTask() {
//            override fun run() {
//                AndroidSchedulers.mainThread().scheduleDirect {
//                    startNextActivity()
//                }
//            }
//        }, 5000)
    }

    override fun onPause() {
        super.onPause()
        timer.cancel()
    }

    override fun onClickNext(view: View) {
        startNextActivity()
    }

    fun startNextActivity() {
        timer.cancel()
        Intent(this, Onboarding3Activity::class.java).let { intent ->
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        }
    }
}

interface Onboarding2Handlers {
    fun onClickNext(view: View)
}