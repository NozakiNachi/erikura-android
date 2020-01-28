package jp.co.recruit.erikura.presenters.activities.job

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Place
import jp.co.recruit.erikura.databinding.ActivityPlaceDetailBinding
import jp.co.recruit.erikura.presenters.fragments.WorkingPlaceViewFragment

class PlaceDetailActivity : AppCompatActivity(), PlaceDetailEventHandlers {
    private val viewModel: PlaceDetailViewModel by lazy {
        ViewModelProvider(this).get(PlaceDetailViewModel::class.java)
    }

    var place: Place = Place()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_place_detail)

        place = intent.getParcelableExtra("place")
        Log.v("DEBUG", place.toString())

        val binding: ActivityPlaceDetailBinding = DataBindingUtil.setContentView(this, R.layout.activity_place_detail)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this
        viewModel.setupMapButton()
    }

    override fun onResume() {
        super.onResume()

        val transaction = supportFragmentManager.beginTransaction()
        val workingPlaceView = WorkingPlaceViewFragment(place)
        transaction.replace(R.id.placeDetail_workingPlaceView, workingPlaceView)
        transaction.commit()
    }

    override fun onClickOpenMap(view: View) {
        val uri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${place.latitude?:0},${place.longitude?:0}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)
    }
}

class PlaceDetailViewModel: ViewModel() {
    val openMapButtonText: MutableLiveData<SpannableString> = MutableLiveData()

    fun setupMapButton() {
        val str = SpannableString(ErikuraApplication.instance.getString(R.string.openMap))
        val drawable = ContextCompat.getDrawable(ErikuraApplication.instance.applicationContext, R.drawable.link)
        drawable!!.setBounds(0, 0, 40, 40)
        val span = ImageSpan(drawable, DynamicDrawableSpan.ALIGN_BASELINE)
        str.setSpan(span, 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        openMapButtonText.value = str
    }

}

interface PlaceDetailEventHandlers {
    fun onClickOpenMap(view: View)
}