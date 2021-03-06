package jp.co.recruit.erikura.presenters.activities

import android.app.ActivityOptions
import android.content.Intent
import android.content.res.Resources
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.LinearLayout
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.ErikuraConfig
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
    private val viewModel: StartViewModel by lazy {
        ViewModelProvider(this).get(StartViewModel::class.java)
    }

    var videoInitialized = false
    lateinit var mediaPlayer: MediaPlayer
    var pausedPosition: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)

        isSkipSmsVerification = true

        val intent = getIntent()
        if (intent != null) {
            intent.getStringExtra("extra")?.let { data ->
                NotificationData.fromJSON(data)?.openURI?.let { uri ->
                    ErikuraApplication.instance.pushUri = uri
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                    return
                }
            }
        }

        // ???????????????????????????????????????
        val errorMessages = intent.getStringArrayExtra("errorMessages")
        if(errorMessages != null){
            Api(this).displayErrorAlert(errorMessages.asList())
        }

        val binding: ActivityStartBinding = DataBindingUtil.setContentView(this, R.layout.activity_start)
        binding.lifecycleOwner = this
        binding.handlers = this
        binding.viewModel = viewModel

        if (Api.isLogin) {
            // ???????????????????????????SMS?????????????????????????????????????????????????????????????????????????????????????????????
            Intent(this, MapViewActivity::class.java).let { intent ->
                intent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                return
            }
        }

        isSkipSmsVerification = false

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

        mediaPlayer.reset()
        mediaPlayer.setSurface(surface)
        mediaPlayer.setDataSource(this, Uri.parse("android.resource://" + this.packageName + "/" + R.raw.movie), mapOf())
        mediaPlayer.prepareAsync()
        mediaPlayer.setOnPreparedListener { mp ->
            videoInitialized = true
            mp.seekTo(pausedPosition)
            mp.start()
        }
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
        // ?????????????????????????????????????????????
        Tracking.logEvent("view_top", bundleOf())
        Tracking.view(name = "/top", title = "???????????????")
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
            // ?????????????????????
            val intent = Intent(this, MapViewActivity::class.java)
            startActivity(intent)
        }
        else {
            // ??????????????????????????????????????????????????????????????????
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ErikuraApplication.REQUEST_LOGIN_CODE && resultCode == RESULT_OK) {
            if (ErikuraApplication.instance.isOnboardingDisplayed()) {
                Intent(this, MapViewActivity::class.java).let { intent ->
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            } else {
                // ??????????????????????????????????????????????????????????????????
                Intent(this, PermitLocationActivity::class.java).let { intent ->
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(
                        intent,
                        ActivityOptions.makeSceneTransitionAnimation(this).toBundle()
                    )
                    finish()
                }
            }
        }
    }

    private fun pauseVideo() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            pausedPosition = mediaPlayer.currentPosition
        }
    }

    private fun resumeVideo() {
        if (videoInitialized) {
            mediaPlayer.seekTo(pausedPosition)
            mediaPlayer.start()
        }
    }

    class StartViewModel: ViewModel() {
        private val resources: Resources get() = ErikuraApplication.instance.resources
        private val displayMetrics: DisplayMetrics get() = resources.displayMetrics
        private val margingSettings: Map<String, Int> = decideMarginSettings()

        val logoTopMargin: Int get() = getMargin("logoTopMargin")
        val logoBottomMargin: Int get() = getMargin("logoBottomMargin")

        private fun getMargin(key: String): Int {
            return margingSettings[key] ?: 0
        }

        private fun decideMarginSettings(): Map<String, Int> {
            val height = displayMetrics.heightPixels / displayMetrics.density
            return when {
                height < 592 -> {
                    mapOf(
                        "logoTopMargin"     to 30,
                        "logoBottomMargin"  to 30
                    )
                }
                height < 700 -> {
                    mapOf(
                        "logoTopMargin"     to 35,
                        "logoBottomMargin"  to 35
                    )
                }
                else -> {
                    mapOf(
                        "logoTopMargin"     to 40,
                        "logoBottomMargin"  to 40
                    )
                }
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