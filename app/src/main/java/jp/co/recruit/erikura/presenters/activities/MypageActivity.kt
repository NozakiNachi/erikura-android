package jp.co.recruit.erikura.presenters.activities

import android.app.ActivityOptions
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.BuildConfig
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityMypageBinding
import jp.co.recruit.erikura.presenters.activities.job.MapViewActivity

class MypageActivity : AppCompatActivity(), MypageEventHandlers {

    private val viewModel: MypageViewModel by lazy {
        ViewModelProvider(this).get(MypageViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)

        val binding: ActivityMypageBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_mypage)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this
    }

//    override fun onClickUnreachLink(view: View) {
//
//    }

    override fun onClickPaymentinformationLink(view: View) {
        // リンク先の作成
        val intent = Intent(this, RegisterEmailActivity::class.java)
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
    }

    override fun onClickJobEvaluation(view: View) {
        // リンク先の作成
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
    }

    override fun onClickConfiguration(view: View) {
        // リンク先の作成
        val intent = Intent(this, ConfigurationActivity::class.java)
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
    }

//    override fun onClickUnreachLink(view: View) {
//
//    }

}

class MypageViewModel: ViewModel() {}


interface MypageEventHandlers {
    // 今月の○○表示(非ログインチェック)
    //fun onClickUnreachLink(view: View)
    // お支払情報ページへのリンク
    fun onClickPaymentinformationLink(view: View)
    // 仕事へのコメント・いいねへのリンク
    fun onClickJobEvaluation(view: View)
    // 設定画面へのリンク
    fun onClickConfiguration(view: View)
    // お知らせ取得API
    //fun onClickUnreachLink(view: View)
}
