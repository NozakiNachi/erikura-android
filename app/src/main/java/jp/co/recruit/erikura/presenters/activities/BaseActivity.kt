package jp.co.recruit.erikura.presenters.activities

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import jp.co.recruit.erikura.presenters.activities.job.MapViewActivity

abstract class BaseActivity(val finishByBackButton: Boolean = false): AppCompatActivity() {
    companion object {
        var currentActivity: Activity? = null
    }

    override fun onResume() {
        super.onResume()
        currentActivity = this
    }

    // 戻るボタンの動きを再定義して、ルートで遷移している場合には、地図画面に戻るようにします
    override fun onBackPressed() {
        if (isTaskRoot && !finishByBackButton) {
            backToDefaultActivity()
        }
        else {
            // ルート画面出ない場合は、通常の遷移で戻ります
            super.onBackPressed()
        }
    }

    open fun backToDefaultActivity() {
        // ルートタスクの場合は、地図画面に遷移します
        Intent(this, MapViewActivity::class.java)?.let {
            it.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(it, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        }
    }
}
