package jp.co.recruit.erikura.presenters.activities.tutorial

import android.app.ActivityOptions
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import io.reactivex.android.schedulers.AndroidSchedulers
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.databinding.ActivityOnboarding1Binding
import jp.co.recruit.erikura.databinding.ActivityOnboarding5Binding
import jp.co.recruit.erikura.presenters.activities.job.MapViewActivity
import java.util.*

class Onboarding5Activity : AppCompatActivity(), Onboarding5Handlers {
    val timer: Timer = Timer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityOnboarding5Binding =
            DataBindingUtil.setContentView(this, R.layout.activity_onboarding5)
        binding.lifecycleOwner = this
        binding.handlers = this
    }

    override fun onStart() {
        super.onStart()
        // ページ参照のトラッキングの送出
        Tracking.logEvent(event= "view_onboarding_5", params= bundleOf())
        Tracking.view(name= "/intro/description_5", title= "オンボーディング画面（ステップ5）")
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
        ErikuraApplication.instance.setOnboardingDisplayed(true)
        Intent(this, MapViewActivity::class.java).let { intent ->
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        }
    }
}

interface Onboarding5Handlers {
    fun onClickNext(view: View)
}