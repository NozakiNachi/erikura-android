package jp.co.recruit.erikura.presenters.activities.errors

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.databinding.ActivityUpgradeRequiredBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity

class UpgradeRequiredActivity : BaseActivity(), UpgradeRequiredHandlers {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding: ActivityUpgradeRequiredBinding = DataBindingUtil.setContentView(this, R.layout.activity_upgrade_required)
        binding.lifecycleOwner = this
        binding.handlers = this
    }

    override fun onStart() {
        super.onStart()
        // ページ参照のトラッキングの送出
        Tracking.logEvent(event= "view_app_update", params= bundleOf())
        Tracking.view(name= "/common/update", title= "強制アップデート画面")
    }

    override fun onBackPressed() {
        // 戻るボタンを無効化します
        Log.v(ErikuraApplication.LOG_TAG, "Upgrade Required: BackButton")
    }

    override fun onClickUpate(view: View) {
        val playURL = "https://play.google.com/store/apps/details?id=${packageName}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(playURL))
        startActivity(intent)
    }
}

interface UpgradeRequiredHandlers {
    fun onClickUpate(view: View)
}
