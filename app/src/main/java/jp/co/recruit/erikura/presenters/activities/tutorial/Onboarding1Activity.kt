package jp.co.recruit.erikura.presenters.activities.tutorial

import android.app.ActivityOptions
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.reactivex.android.schedulers.AndroidSchedulers
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.databinding.ActivityOnboarding0Binding
import jp.co.recruit.erikura.databinding.ActivityOnboarding1Binding
import jp.co.recruit.erikura.presenters.activities.job.MapViewActivity
import java.util.*

class Onboarding1Activity : AppCompatActivity(), Onboarding1Handlers {
    private val viewModel: Onboarding1ViewModel by lazy {
        ViewModelProvider(this).get(Onboarding1ViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityOnboarding1Binding =
            DataBindingUtil.setContentView(this, R.layout.activity_onboarding1)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this
        viewModel.setup(desideMargin())
    }

    override fun onStart() {
        super.onStart()
        // ページ参照のトラッキングの送出
        Tracking.logEvent(event= "view_onboarding_1", params= bundleOf())
        Tracking.view(name= "/intro/description_1", title= "オンボーディング画面（ステップ1）")
    }

    override fun onClickNext(view: View) {
        startNextActivity()
    }

    override fun onClickSkip(view: View) {
        ErikuraApplication.instance.setOnboardingDisplayed(true)
        Intent(this, MapViewActivity::class.java).let { intent ->
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        }
    }

    private fun startNextActivity() {
        Intent(this, Onboarding2Activity::class.java).let { intent ->
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        }
    }

    //スクリーンの縦幅を元にマージンを決めます。
    fun desideMargin(): Map<String, Int> {
        val dm = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(dm)

        val height = dm.heightPixels
        Log.v("DEBUG","画面縦幅=" + height)
        if (height <= 1500) {
            //mapでimageの場合とtextの場合をセットする
            return mapOf(
                "skipTopMargin" to 5,
                "stepTopMargin" to 1,
                "explainTopMargin" to 10,
                "imageTopMargin" to 10,
                "nextTopMargin" to 10
            )
        } else if (height <= 2100) {
            return mapOf(
                "skipTopMargin" to 20,
                "stepTopMargin" to 5,
                "explainTopMargin" to 20,
                "imageTopMargin" to 20,
                "nextTopMargin" to 40
            )
        } else {
            return mapOf(
                "skipTopMargin" to 40,
                "stepTopMargin" to 30,
                "explainTopMargin" to 20,
                "imageTopMargin" to 20,
                "nextTopMargin" to 40
            )
        }
    }
}

class Onboarding1ViewModel: ViewModel() {
    val skipTopMargin: MutableLiveData<Int> = MutableLiveData()
    val stepTopMargin: MutableLiveData<Int> = MutableLiveData()
    val explainTopMargin: MutableLiveData<Int> = MutableLiveData()
    val imageTopMargin: MutableLiveData<Int> = MutableLiveData()
    val nextTopMargin: MutableLiveData<Int> = MutableLiveData()

    fun setup(margin: Map<String,Int>) {
        Log.v("DEBUG", "skipTopMargin=" + margin.getValue("skipTopMargin"))
        skipTopMargin.value = margin.getValue("skipTopMargin")
        stepTopMargin.value = margin.getValue("stepTopMargin")
        explainTopMargin.value = margin.getValue("explainTopMargin")
        imageTopMargin.value = margin.getValue("imageTopMargin")
        nextTopMargin.value = margin.getValue("nextTopMargin")

    }
}

interface Onboarding1Handlers {
    fun onClickNext(view: View)
    fun onClickSkip(view: View)
}