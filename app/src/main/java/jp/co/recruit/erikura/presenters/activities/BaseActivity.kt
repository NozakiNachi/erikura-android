package jp.co.recruit.erikura.presenters.activities

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity: AppCompatActivity() {
    companion object {
        var currentActivity: Activity? = null
    }

    override fun onResume() {
        super.onResume()
        currentActivity = this
    }
}
