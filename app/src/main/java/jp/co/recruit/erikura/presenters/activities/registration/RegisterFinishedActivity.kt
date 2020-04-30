package jp.co.recruit.erikura.presenters.activities.registration

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.databinding.ActivityRegisterFinishedBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity
import jp.co.recruit.erikura.presenters.activities.job.MapViewActivity

class RegisterFinishedActivity : BaseActivity(),
    RegisterFinishedEventHandlers {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_finished)

        val binding: ActivityRegisterFinishedBinding = DataBindingUtil.setContentView(this, R.layout.activity_register_finished)
        binding.lifecycleOwner = this
        binding.handlers = this
    }

    override fun onStart() {
        super.onStart()
        // ページ参照のトラッキングの送出
        Tracking.logEvent(event= "view_register_finish", params= bundleOf())
        Tracking.view(name= "/user/register/completed", title= "本登録画面（完了）")
    }

    override fun onClickGo(view: View) {
        // 地図画面へ遷移します
        // FIXME: チュートリアルの表示
        val intent = Intent(this, MapViewActivity::class.java)
        startActivity(intent)
    }
}

interface RegisterFinishedEventHandlers {
    fun onClickGo(view: View)
}
