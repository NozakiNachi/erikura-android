package jp.co.recruit.erikura.presenters.activities.job

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Place
import jp.co.recruit.erikura.presenters.fragments.WorkingPlaceViewFragment

class PlaceDetailActivity : AppCompatActivity() {

    var place: Place = Place()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_place_detail)

        place = intent.getParcelableExtra("place")
        Log.v("DEBUG", place.toString())
    }

    override fun onResume() {
        super.onResume()

        val transaction = supportFragmentManager.beginTransaction()
        val workingPlaceView = WorkingPlaceViewFragment(place)
        transaction.replace(R.id.placeDetail_workingPlaceView, workingPlaceView)
        transaction.commit()
    }
}
