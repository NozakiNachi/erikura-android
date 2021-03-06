package jp.co.recruit.erikura.presenters.activities.tutorial

import android.content.Intent
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.view.View
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.databinding.ActivityOnboarding3Binding
import jp.co.recruit.erikura.presenters.activities.job.MapViewActivity

class Onboarding3Activity : BaseOnboardingActivity(), OnboardingHandlers {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityOnboarding3Binding =
            DataBindingUtil.setContentView(this, R.layout.activity_onboarding3)
        binding.lifecycleOwner = this
        binding.handlers = this
        binding.viewModel = viewModel
    }

    override fun onStart() {
        super.onStart()
        var explain = findViewById<TextView>(R.id.onboarding_3_explain)
        explain.setText(makeExplain())
        // ページ参照のトラッキングの送出
        Tracking.logEvent(event= "view_onboarding_3", params= bundleOf())
        Tracking.view(name= "/intro/description_3", title= "オンボーディング画面（ステップ3）")
    }

    override fun startNextActivity() {
        Intent(this, Onboarding4Activity::class.java).let { intent ->
            startActivity(intent)
        }
    }

    private fun makeExplain(): SpannableStringBuilder {
        var str = "お仕事の内容と、\nマニュアルを確認！\n"
        var str1 = SpannableStringBuilder(
            str + "\n必要な道具や注意点もチェック！")

        str1.setSpan(
            AbsoluteSizeSpan(14, true), str.length, str1.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return str1
    }
}
