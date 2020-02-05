package jp.co.recruit.erikura.presenters.activities

import android.app.ActivityOptions
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.databinding.ActivityAppliedJobsBinding
import jp.co.recruit.erikura.presenters.activities.job.MapViewActivity

class AppliedJobsActivity : AppCompatActivity(), AppliedJobsHandlers {

    private val viewModel: AppliedJobsViewModel by lazy {
        ViewModelProvider(this).get(AppliedJobsViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityAppliedJobsBinding = DataBindingUtil.setContentView(this, R.layout.activity_applied_jobs)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this

        // FIXME: viewPager の設定
        // FIXME: tabItem のカスタマイズ
        // FIXME: fragment の実装
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        Log.v("MENU ITEM SELECTED: ", item.toString())
        when(item.itemId) {
            R.id.tab_menu_search_jobs -> {
                // 地図画面、またはリスト画面に遷移します
                Intent(this, MapViewActivity::class.java).let {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(it, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
                }
                finish()
            }
            R.id.tab_menu_applied_jobs -> {
                // 何も行いません
            }
            R.id.tab_menu_mypage -> {
                // FIXME: 画面遷移の実装
                Toast.makeText(this, "マイページ画面に遷移", Toast.LENGTH_LONG).show()
            }
        }
        return true
    }
}

class AppliedJobsViewModel: ViewModel() {}

interface AppliedJobsHandlers {
    fun onNavigationItemSelected(item: MenuItem): Boolean
}