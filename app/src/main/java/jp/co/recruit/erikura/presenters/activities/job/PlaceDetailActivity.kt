package jp.co.recruit.erikura.presenters.activities.job

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import jp.co.recruit.erikura.R

class PlaceDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_place_detail)
    }
}
