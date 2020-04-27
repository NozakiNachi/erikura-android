package jp.co.recruit.erikura.presenters.activities.tutorial

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import io.reactivex.android.schedulers.AndroidSchedulers
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.databinding.ActivityOnboarding2Binding
import jp.co.recruit.erikura.presenters.activities.job.MapViewActivity
import java.util.*

class Onboarding2Activity : AppCompatActivity(), Onboarding2Handlers {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityOnboarding2Binding =
            DataBindingUtil.setContentView(this, R.layout.activity_onboarding2)
        binding.lifecycleOwner = this
        binding.handlers = this
    }

    override fun onStart() {
        super.onStart()
        // ページ参照のトラッキングの送出
        Tracking.logEvent(event= "view_onboarding_2", params= bundleOf())
        Tracking.view(name= "/intro/description_2", title= "オンボーディング画面（ステップ2）")
    }

    override fun onClickNext(view: View) {
        startNextActivity()
    }

    override fun onClickSkip(view: View) {
        ErikuraApplication.instance.setOnboardingDisplayed(true)
        Intent(this, MapViewActivity::class.java).let { intent ->
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
    }

    private fun startNextActivity() {
        Intent(this, Onboarding3Activity::class.java).let { intent ->
            startActivity(intent)
        }
    }
}

interface Onboarding2Handlers {
    fun onClickNext(view: View)
    fun onClickSkip(view: View)
}