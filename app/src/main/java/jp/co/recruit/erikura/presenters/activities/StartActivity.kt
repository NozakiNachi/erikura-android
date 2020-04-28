package jp.co.recruit.erikura.presenters.activities

import android.content.Intent
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.VideoView
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityStartBinding
import jp.co.recruit.erikura.presenters.activities.job.MapViewActivity
import jp.co.recruit.erikura.presenters.activities.registration.RegisterEmailActivity
import jp.co.recruit.erikura.presenters.activities.tutorial.PermitLocationActivity
import jp.co.recruit.erikura.services.NotificationData
import kotlinx.android.synthetic.main.activity_start.*

/*
TextureView.SurfaceTextureListener, MediaPlayer.OnVideoSizeChangedListene
 */
class StartActivity : BaseActivity(finishByBackButton = true), StartEventHandlers, TextureView.SurfaceTextureListener, MediaPlayer.OnVideoSizeChangedListener {
    var videoInitialized = false
    lateinit var mediaPlayer: MediaPlayer
    var pausedPosition: Int = 0

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

        if (Api.isLogin) {
            // すでにログイン済の場合には以降の処理はスキップして、地図画面に遷移します
            Intent(this, MapViewActivity::class.java).let { intent ->
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            finish()
            return
        }

        val displayMetrics = resources.displayMetrics

        val displayHeightInDp = displayMetrics.heightPixels / displayMetrics.density
        val size = ((displayHeightInDp - 380) * displayMetrics.density).toInt()

        val width = 412.0
        val height = 412.0

        val layoutWidth = Math.min(displayMetrics.widthPixels, size)
        val layoutHeight = Math.min((height * (displayMetrics.widthPixels / width)).toInt(), size)
        start_texture.layoutParams = LinearLayout.LayoutParams(layoutWidth, layoutHeight)

        binding.root.forceLayout()

        start_texture.surfaceTextureListener = this
        mediaPlayer = MediaPlayer()
        mediaPlayer.setOnCompletionListener {
            mediaPlayer.seekTo(0)
            mediaPlayer.start()
        }
    }

    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture?, width: Int, height: Int) {
        val surface: Surface = Surface(surfaceTexture)

        mediaPlayer.setSurface(surface)
        mediaPlayer.setDataSource(this, Uri.parse("android.resource://" + this.packageName + "/" + R.raw.movie), mapOf())
        videoInitialized = true
        resumeVideo()
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
        return false
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
    }

    override fun onVideoSizeChanged(mp: MediaPlayer?, width: Int, height: Int) {
    }

    override fun onStart() {
        super.onStart()
        // ページ参照のトラッキングの送出
        Tracking.logEvent("view_top", bundleOf())
        Tracking.view(name = "/top", title = "トップ画面")
    }

    override fun onResume() {
        super.onResume()
        resumeVideo()
    }

    override fun onPause() {
        super.onPause()
        pauseVideo()
    }

    override fun onClickRegisterButton(view: View) {
        val intent = Intent(this, RegisterEmailActivity::class.java)
        startActivity(intent)
    }

    override fun onClickLoginButton(view: View) {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }

    override fun onClickStartWithoutLogin(view: View) {
        if (ErikuraApplication.instance.isOnboardingDisplayed()) {
            // 地図画面へ遷移
            val intent = Intent(this, MapViewActivity::class.java)
            startActivity(intent)
        }
        else {
            // 位置情報の許諾、オンボーディングを表示します
            Intent(this, PermitLocationActivity::class.java).let { intent ->
                startActivity(intent)
            }
        }
    }

    override fun onClickVideo(view: View) {
        if (mediaPlayer.isPlaying) {
            pauseVideo()
        }
        else {
            resumeVideo()
        }
    }

    private fun pauseVideo() {
        if (mediaPlayer.isPlaying) {
            pausedPosition = mediaPlayer.currentPosition
            mediaPlayer.stop()
        }
    }

    private fun resumeVideo() {
        if (videoInitialized) {
            mediaPlayer.prepareAsync()
            mediaPlayer.setOnPreparedListener { mp ->
                mp.seekTo(pausedPosition)
                mp.start()
            }
        }
    }
}

interface StartEventHandlers {
    fun onClickRegisterButton(view: View)
    fun onClickLoginButton(view: View)
    fun onClickStartWithoutLogin(view: View)
    fun onClickVideo(view: View)
}