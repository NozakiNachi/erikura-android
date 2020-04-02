package jp.co.recruit.erikura.presenters.activities.tutorial

import android.app.ActivityOptions
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import io.reactivex.android.schedulers.AndroidSchedulers
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.databinding.ActivityOnboarding0Binding
import jp.co.recruit.erikura.databinding.ActivityOnboarding1Binding
import java.util.*

class Onboarding1Activity : AppCompatActivity(), Onboarding1Handlers {
    val timer: Timer = Timer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityOnboarding1Binding =
            DataBindingUtil.setContentView(this, R.layout.activity_onboarding1)
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
        Intent(this, Onboarding2Activity::class.java).let { intent ->
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        }
    }
}

interface Onboarding1Handlers {
    fun onClickNext(view: View)
}