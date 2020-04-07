package jp.co.recruit.erikura.presenters.activities

import android.app.ActivityOptions
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.VideoView
import androidx.databinding.DataBindingUtil
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityStartBinding
import jp.co.recruit.erikura.presenters.activities.job.MapViewActivity
import jp.co.recruit.erikura.presenters.activities.registration.RegisterEmailActivity
import jp.co.recruit.erikura.presenters.activities.tutorial.PermitLocationActivity
import jp.co.recruit.erikura.services.NotificationData

class StartActivity : BaseActivity(), StartEventHandlers {
    lateinit var video: VideoView

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)

        val intent = getIntent()
        if (intent != null) {
            intent.getStringExtra("extra")?.let { data ->
                NotificationData.fromJSON(data)?.openURI?.let { uri ->
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                    return
                }
            }
        }

        // エラーメッセージを受け取る
        val errorMessages = intent.getStringArrayExtra("errorMessages")
        if(errorMessages != null){
            Api(this).displayErrorAlert(errorMessages.asList())
        }

        val binding: ActivityStartBinding = DataBindingUtil.setContentView(this, R.layout.activity_start)
        binding.lifecycleOwner = this
        binding.handlers = this

        video = findViewById(R.id.v)

        // ビデオの設定
        video.setVideoURI(Uri.parse("android.resource://" + this.packageName + "/" + R.raw.movie))
        video.start()
        // ループ再生処理
        video.setOnCompletionListener {
            video.seekTo(0)
            video.start()
        }

        if (!ErikuraApplication.pedometerManager.checkPermission(this)) {
            ErikuraApplication.pedometerManager.requestPermission(this)
        }

        if (Api.isLogin) {
            // すでにログイン済の場合には以降の処理はスキップして、地図画面に遷移します
            Intent(this, MapViewActivity::class.java).let { intent ->
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            finish()
            return
        }
    }

    override fun onResume() {
        super.onResume()
        video.resume()
        video.start()
//
//        if (ErikuraApplication.pedometerManager.checkPermission(this)) {
//            ErikuraApplication.pedometerManager.start()
//        }
    }

    override fun onPause() {
        super.onPause()
        video.stopPlayback()
//        ErikuraApplication.pedometerManager.stop()
    }

    override fun onClickRegisterButton(view: View) {
        val intent = Intent(this, RegisterEmailActivity::class.java)
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
    }

    override fun onClickLoginButton(view: View) {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
    }

    override fun onClickStartWithoutLogin(view: View) {
        if (ErikuraApplication.instance.isOnboardingDisplayed()) {
            // 地図画面へ遷移
            val intent = Intent(this, MapViewActivity::class.java)
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        }
        else {
            // 位置情報の許諾、オンボーディングを表示します
            Intent(this, PermitLocationActivity::class.java).let { intent ->
                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when(requestCode) {
            ErikuraApplication.REQUEST_ACTIVITY_RECOGNITION_PERMISSION_ID -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    ErikuraApplication.pedometerManager.start()
                }
            }
        }
    }
}

interface StartEventHandlers {
    fun onClickRegisterButton(view: View)
    fun onClickLoginButton(view: View)
    fun onClickStartWithoutLogin(view: View)
}