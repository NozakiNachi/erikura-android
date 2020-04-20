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
import jp.co.recruit.erikura.databinding.ActivityOnboarding0Binding
import jp.co.recruit.erikura.databinding.ActivityOnboarding1Binding
import jp.co.recruit.erikura.presenters.activities.job.MapViewActivity
import java.util.*

class Onboarding1Activity : AppCompatActivity(), Onboarding1Handlers {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityOnboarding1Binding =
            DataBindingUtil.setContentView(this, R.layout.activity_onboarding1)
        binding.lifecycleOwner = this
        binding.handlers = this
    }

    override fun onStart() {
        super.onStart()
        // ページ参照のトラッキングの送出
        Tracking.logEvent(event= "view_onboarding_1", params= bundleOf())
        Tracking.view(name= "/intro/description_1", title= "オンボーディング画面（ステップ1）")
    }

    override fun onClickNext(view: View) {
        startNextActivity()
    }

    override fun onClickSkip(view: View) {
        ErikuraApplication.instance.setOnboardingDisplayed(true)
        Intent(this, MapViewActivity::class.java).let { intent ->
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        }
    }

    private fun startNextActivity() {
        Intent(this, Onboarding2Activity::class.java).let { intent ->
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        }
    }
}

interface Onboarding1Handlers {
    fun onClickNext(view: View)
    fun onClickSkip(view: View)
}