package jp.co.recruit.erikura.presenters.activities.tutorial

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.databinding.ActivityOnboarding2Binding
import jp.co.recruit.erikura.presenters.activities.job.MapViewActivity

class Onboarding2Activity : BaseOnboardingActivity(), OnboardingHandlers {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityOnboarding2Binding =
            DataBindingUtil.setContentView(this, R.layout.activity_onboarding2)
        binding.lifecycleOwner = this
        binding.handlers = this
        binding.viewModel = viewModel
    }

    override fun onStart() {
        super.onStart()
        // ページ参照のトラッキングの送出
        Tracking.logEvent(event= "view_onboarding_2", params= bundleOf())
        Tracking.view(name= "/intro/description_2", title= "オンボーディング画面（ステップ2）")
    }

    override fun startNextActivity() {
        Intent(this, Onboarding3Activity::class.java).let { intent ->
            startActivity(intent)
        }
    }
}
