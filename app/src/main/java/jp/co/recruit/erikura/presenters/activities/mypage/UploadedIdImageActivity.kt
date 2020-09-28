package jp.co.recruit.erikura.presenters.activities.mypage

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.databinding.ActivityUploadedIdImageBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity
import jp.co.recruit.erikura.presenters.activities.job.JobDetailsActivity
import jp.co.recruit.erikura.presenters.activities.job.MapViewActivity
import jp.co.recruit.erikura.presenters.activities.tutorial.PermitLocationActivity

class UploadedIdImageActivity : BaseActivity(), UploadedIdImageEventHandlers {
    var fromWhere: Int? = null
    var job = Job()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityUploadedIdImageBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_uploaded_id_image)
        binding.lifecycleOwner = this
        binding.handlers = this

        fromWhere =
            intent.getIntExtra(ErikuraApplication.FROM_WHERE, ErikuraApplication.FROM_NOT_FOUND)
        if (fromWhere == ErikuraApplication.FROM_ENTRY) {
            job = intent.getParcelableExtra("job")
        }
    }

    override fun onStart() {
        super.onStart()
        // ページ参照のトラッキングの送出
        Tracking.logEvent(event = "view_user_verifications_completed", params = bundleOf())
        Tracking.view("/user/verifications/completed", "身分証確認完了画面")

    }

    override fun onClickNext(view: View) {
        // 身分証確認の遷移元、本登録、応募の２パターンへ遷移します。
        when (fromWhere) {
            ErikuraApplication.FROM_REGISTER -> {
                // 地図画面へ
                if (ErikuraApplication.instance.isOnboardingDisplayed()) {
                    // 地図画面へ遷移
                    val intent = Intent(this, MapViewActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                } else {
                    // 位置情報の許諾、オンボーディングを表示します
                    Intent(this, PermitLocationActivity::class.java).let { intent ->
                        intent.flags =
                            Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                        finish()
                    }
                }
            }
            ErikuraApplication.FROM_ENTRY -> {
                // 仕事詳細へ遷移し応募確認ダイアログへ
                val intent = Intent()
                intent.putExtra("displayApplyDialog", true)
                setResult(RESULT_OK, intent)
                finish()
            }
        }
    }

    // 戻るボタンの制御
    override fun onBackPressed() {
        when (fromWhere) {
            //　応募経由の場合のみonActivityResultで画面を遷移
            ErikuraApplication.FROM_ENTRY -> {
                val intent = Intent()
                setResult(RESULT_OK, intent)
                finish()
            }
            else -> {
                super.onBackPressed()
            }
        }
    }
}

interface UploadedIdImageEventHandlers {
    fun onClickNext(view: View)
}