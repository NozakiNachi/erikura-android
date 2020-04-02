package jp.co.recruit.erikura.presenters.activities.tutorial

import android.app.ActivityOptions
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import io.reactivex.android.schedulers.AndroidSchedulers
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.databinding.ActivityOnboarding0Binding
import java.util.*

class Onboarding0Activity : AppCompatActivity(), Onboarding0Handlers {
    val timer: Timer = Timer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityOnboarding0Binding =
            DataBindingUtil.setContentView(this, R.layout.activity_onboarding0)
        binding.lifecycleOwner = this
        binding.handlers = this
    }

    override fun onResume() {
        super.onResume()

        timer.schedule(object: TimerTask() {
            override fun run() {
                AndroidSchedulers.mainThread().scheduleDirect {
                    startNextActivity()
                }
            }
        }, 2000)
    }

    override fun onPause() {
        super.onPause()
        timer.cancel()
    }

    fun startNextActivity() {
        timer.cancel()
        Intent(this, Onboarding1Activity::class.java).let { intent ->
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())

        }
    }
}

interface Onboarding0Handlers {

}