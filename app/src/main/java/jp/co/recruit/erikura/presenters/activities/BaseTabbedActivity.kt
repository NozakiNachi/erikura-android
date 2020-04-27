package jp.co.recruit.erikura.presenters.activities

import android.app.ActivityOptions
import android.content.Intent
import android.util.Log
import android.view.MenuItem
import com.google.android.material.bottomnavigation.BottomNavigationView
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.presenters.activities.job.MapViewActivity
import jp.co.recruit.erikura.presenters.activities.mypage.MypageActivity

open class BaseTabbedActivity(private val currentTabId: Int, finishByBackButton: Boolean = false): BaseActivity(finishByBackButton = finishByBackButton), TabEventHandlers {
    companion object {
        var searchJobCurrentActivity: Class<*> = MapViewActivity::class.java
        var mypageCurrentActivity: Class<*> = MypageActivity::class.java
    }

    override fun onStart() {
        super.onStart()

        // 下部のタブの選択肢を変更します
        val nav: BottomNavigationView = findViewById(R.id.tab_navigation)
        nav.selectedItemId = currentTabId
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        Log.v(ErikuraApplication.LOG_TAG, "Navigation Item Selected: ${item.toString()}")

        // 現在のタブの場合には何も行いません
        if (item.itemId == this.currentTabId)
            return true

        when(item.itemId) {
            // 応募した仕事タブ
            R.id.tab_menu_applied_jobs -> {
                Intent(this, OwnJobsActivity::class.java).let { intent ->
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    //startActivity(intent)
                    startActivity(intent)
                }
            }
            // 仕事を探すタブ
            R.id.tab_menu_search_jobs -> {
                Intent(this, searchJobCurrentActivity).let { intent ->
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    //startActivity(intent)
                    startActivity(intent)
                }
            }
            // マイページタブ
            R.id.tab_menu_mypage -> {
                Intent(this, mypageCurrentActivity).let { intent ->
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    //startActivity(intent)
                    startActivity(intent)
                }
            }
        }
        return true
    }
}

interface TabEventHandlers {
    fun onNavigationItemSelected(item: MenuItem): Boolean
}
