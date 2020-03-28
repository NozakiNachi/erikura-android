package jp.co.recruit.erikura.presenters.activities.mypage

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.databinding.ActivityFavoritePlacesBinding

class FavoritePlacesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)

        val binding: ActivityFavoritePlacesBinding = DataBindingUtil.setContentView(this, R.layout.activity_favorite_places)

    }
}

// お気に入り場所一覧

