package jp.co.recruit.erikura.presenters.activities.tutorial

import android.content.Intent
import android.content.res.Resources
import android.util.DisplayMetrics
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.presenters.activities.job.MapViewActivity

abstract class BaseOnboardingActivity() : AppCompatActivity(), OnboardingHandlers {
    protected val viewModel: OnboardingViewModel by lazy {
        ViewModelProvider(this).get(OnboardingViewModel::class.java)
    }

    override fun onClickNext(view: View) {
        startNextActivity()
    }

    override fun onClickSkip(view: View) {
        ErikuraApplication.instance.setOnboardingDisplayed(true)
        Intent(this, MapViewActivity::class.java).let { intent ->
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
    }

    abstract fun startNextActivity()
}

open class OnboardingViewModel: ViewModel() {
    private val resources: Resources get() = ErikuraApplication.instance.resources
    private val displayMetrics: DisplayMetrics get() = resources.displayMetrics
    private val margingSettings: Map<String, Int> = decideMarginSettings()

    val skipTopMargin: Int get() = getMargin("skipTopMargin")
    val titleTopMargin: Int get() = getMargin("titleTopMargin")
    val textTopMargin: Int get() = getMargin("textTopMargin")
    val imageTopMargin: Int get() = getMargin("imageTopMargin")
    val stepTopMargin: Int get() = getMargin("stepTopMargin")
    val nextTopMargin: Int get() = getMargin("nextTopMargin")

    private fun getMargin(key: String): Int {
        return margingSettings[key] ?: 0
    }

    private fun decideMarginSettings(): Map<String, Int> {
        val height = displayMetrics.heightPixels / displayMetrics.density
        return when {
            height < 592 -> {
                mapOf(
                    "skipTopMargin"     to 5,
                    "titleTopMargin"    to -20,
                    "textTopMargin"     to 20,
                    "imageTopMargin"    to 10,
                    "stepTopMargin"     to 10,
                    "nextTopMargin"     to 20
                )
            }
            height < 700 -> {
                mapOf(
                    "skipTopMargin"     to 10,
                    "titleTopMargin"    to -10,
                    "textTopMargin"     to 20,
                    "imageTopMargin"    to 20,
                    "stepTopMargin"     to 20,
                    "nextTopMargin"     to 20
                )
            }
            else -> {
                mapOf(
                    "skipTopMargin"     to 20,
                    "titleTopMargin"    to 0,
                    "textTopMargin"     to 20,
                    "imageTopMargin"    to 20,
                    "stepTopMargin"     to 20,
                    "nextTopMargin"     to 40
                )
            }
        }
    }
}

interface OnboardingHandlers {
    fun onClickNext(view: View)
    fun onClickSkip(view: View)
}