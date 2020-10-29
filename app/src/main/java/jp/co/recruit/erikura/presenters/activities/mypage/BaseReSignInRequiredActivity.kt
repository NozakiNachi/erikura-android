package jp.co.recruit.erikura.presenters.activities.mypage

import android.content.Intent
import android.os.Bundle
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.presenters.activities.BaseActivity
import java.util.*

abstract class BaseReSignInRequiredActivity(val fromActivity: Int, finishByBackButton: Boolean = false) : BaseActivity(finishByBackButton) {
    companion object {
        val REQUEST_RESIGN_IN = 0x1001
        val RESULT_RESIGN_IN_SUCCESS = 0x00
        val RESULT_RESIGN_IN_CANCELED = 0x01

        val ACTIVITY_CHANGE_USER_INFORMATION = 0x0001
        val ACTIVITY_ACCOUNT_SETTINGS = 0x0002
        val ACTIVITY_UPDATE_IDENTITY = 0x0003
    }

    private var savedInstanceState: Bundle? = null
    private var resignInChecked: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        this.savedInstanceState = savedInstanceState

        // 再認証が必要かどうか確認
        checkResignIn() { isResignIn ->
            if (isResignIn) {
                onCreateImpl(savedInstanceState)
            } else {
                startResignInActivity ()
            }
        }
    }

    open fun startResignInActivity () {
        finish()
        Intent(this, ResignInActivity::class.java).let { intent ->
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.putExtra("fromActivity", fromActivity)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_RESIGN_IN) {
            when (resultCode) {
                RESULT_RESIGN_IN_SUCCESS -> {
                    // 再認証に成功した場合
                }
            }
        }
    }

    /**
     * 再認証後に行う onCreate の処理を実装します
     */
    abstract fun onCreateImpl(savedInstanceState: Bundle?)

    // 再認証画面へ遷移
    protected fun checkResignIn(onComplete: (isResignIn: Boolean) -> Unit) {
        val nowTime = Date()
        val reSignTime = Api.userSession?.resignInExpiredAt

        if (Api.userSession?.resignInExpiredAt !== null) {
            // 過去の再認証から10分以上経っていたら再認証画面へ
            if (reSignTime!! < nowTime) {
                onComplete(false)
            } else {
                onComplete(true)
            }
        } else {
            // 一度も再認証していなければ、再認証画面へ
            onComplete(false)
        }
    }
}