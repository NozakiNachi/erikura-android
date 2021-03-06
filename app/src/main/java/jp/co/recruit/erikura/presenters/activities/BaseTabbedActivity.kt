package jp.co.recruit.erikura.presenters.activities

import android.content.Intent
import android.util.Log
import android.view.MenuItem
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.material.bottomnavigation.BottomNavigationView
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.JobQuery
import jp.co.recruit.erikura.presenters.activities.job.MapViewActivity
import jp.co.recruit.erikura.presenters.activities.job.SearchJobActivity
import jp.co.recruit.erikura.presenters.activities.mypage.MypageActivity

open class BaseTabbedActivity(private val currentTabId: Int, finishByBackButton: Boolean = false): BaseActivity(finishByBackButton = finishByBackButton), TabEventHandlers {
    companion object {
        var searchJobCurrentActivity: Class<*> = MapViewActivity::class.java
        var searchJobQuery: JobQuery? = null
        var mypageCurrentActivity: Class<*> = MypageActivity::class.java
        var mapCameraPosition: CameraPosition? = null
    }

    protected var onTabButtonInitialization: Boolean = false

    override fun onStart() {
        super.onStart()

        // 下部のタブの選択肢を変更します
        selectTabItemId(currentTabId)
    }

    override fun onResume() {
        super.onResume()

        // 下部のタブの選択肢を変更します
        selectTabItemId(currentTabId)
    }

    protected fun selectTabItemId(menuId: Int) {
        // 下部のタブの選択肢を変更します
        try {
            onTabButtonInitialization = true
            val nav: BottomNavigationView = findViewById(R.id.tab_navigation)
            nav.selectedItemId = menuId
        }
        finally {
            onTabButtonInitialization = false
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // onStart で来た場合には、選択処理はスキップさせます
        if (onTabButtonInitialization) { return true }

        Log.v(ErikuraApplication.LOG_TAG, "Navigation Item Selected: ${item.toString()}")

        // 現在のタブの場合には何も行いません
        if (item.itemId == this.currentTabId)
            return true

        when(item.itemId) {
            // 応募した仕事タブ
            R.id.tab_menu_applied_jobs -> {
                Intent(this, OwnJobsActivity::class.java).let { intent ->
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                }
            }
            // 仕事を探すタブ
            R.id.tab_menu_search_jobs -> {
                Intent(this, searchJobCurrentActivity).let { intent ->
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    searchJobQuery?.let {
                        intent.putExtra(SearchJobActivity.EXTRA_SEARCH_CONDITIONS, it)
                    }
                    startActivity(intent)
                }
            }
            // マイページタブ
            R.id.tab_menu_mypage -> {
                Intent(this, mypageCurrentActivity).let { intent ->
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
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
