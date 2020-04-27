package jp.co.recruit.erikura.presenters.activities

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import jp.co.recruit.erikura.presenters.activities.job.MapViewActivity

abstract class BaseActivity(val finishByBackButton: Boolean = false): AppCompatActivity() {
    companion object {
        var currentActivity: Activity? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.v("ERIKURA", "${this.javaClass.name}: onCreate")
    }

    override fun onStart() {
        super.onStart()
        Log.v("ERIKURA", "${this.javaClass.name}: onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.v("ERIKURA", "${this.javaClass.name}: onResume")
        currentActivity = this
    }

    override fun onPause() {
        super.onPause()
        Log.v("ERIKURA", "${this.javaClass.name}: onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.v("ERIKURA", "${this.javaClass.name}: onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.v("ERIKURA", "${this.javaClass.name}: onDestroy")
    }

    override fun onRestart() {
        super.onRestart()
        Log.v("ERIKURA", "${this.javaClass.name}: onRestart")
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        Log.v("ERIKURA", "${this.javaClass.name}: onSaveInstanceState")
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        Log.v("ERIKURA", "${this.javaClass.name}: onRestoreInstanceState")
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
            startActivity(it)
        }
    }
}
