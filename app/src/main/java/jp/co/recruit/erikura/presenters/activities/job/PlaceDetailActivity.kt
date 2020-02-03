package jp.co.recruit.erikura.presenters.activities.job

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
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
import jp.co.recruit.erikura.business.models.UserSession
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityPlaceDetailBinding
import jp.co.recruit.erikura.presenters.fragments.JobsListFragment
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

        val value = intent.getParcelableExtra<Place>("place")
        if (value != null) {
            place = value
        }else {
            handleIntent(intent)
        }
        Log.v("DEBUG", place.toString())

        val binding: ActivityPlaceDetailBinding = DataBindingUtil.setContentView(this, R.layout.activity_place_detail)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this
        viewModel.setupMapButton()
    }

    override fun onResume() {
        super.onResume()
        viewModel.thumbnailVisibility.value = 8
        fetchPlace()
    }

    override fun onClickOpenMap(view: View) {
        val uri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${place.latitude?:0},${place.longitude?:0}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)
    }

    override fun onClickFavorite(view: View) {
        if (viewModel.favorited.value?: false) {
            // お気に入り登録処理
            Api(this).placeFavorite(place.id) {
                viewModel.favorited.value = true
            }
        }else {
            // お気に入り削除処理
            Api(this).placeFavoriteDelete(place.id) {
                viewModel.favorited.value = false
            }
        }
    }

    private fun fetchPlace(){
        Api(this).place(place.id) { place ->
            viewModel.setupThumbnail(this, place)
            // お気に入りボタンの状態取得処理
            UserSession.retrieve()?.let {
                Api(this).placeFavoriteShow(place.id) {
                    viewModel.favorited.value = it
                }
            }
            val transaction = supportFragmentManager.beginTransaction()
            val workingPlaceView = WorkingPlaceViewFragment(place)
            val jobsList = JobsListFragment(place.jobs)
            transaction.replace(R.id.placeDetail_workingPlaceView, workingPlaceView)
            transaction.replace(R.id.placeDetail_jobsList, jobsList)
            transaction.commit()
        }
    }

    private fun handleIntent(intent: Intent) {
        val appLinkData: Uri? = intent.data
        val placeId = appLinkData!!.lastPathSegment!!.toInt()
        place.id = placeId
    }
}

class PlaceDetailViewModel: ViewModel() {
    val openMapButtonText: MutableLiveData<SpannableString> = MutableLiveData()
    val bitmap: MutableLiveData<Bitmap> = MutableLiveData()
    val thumbnailVisibility: MutableLiveData<Int> = MutableLiveData()
    val bitmapDrawable: MutableLiveData<BitmapDrawable> = MutableLiveData()
    val favorited: MutableLiveData<Boolean> = MutableLiveData(false)


    fun setupMapButton() {
        val str = SpannableString(ErikuraApplication.instance.getString(R.string.openMap))
        val drawable = ContextCompat.getDrawable(ErikuraApplication.instance.applicationContext, R.drawable.link)
        drawable!!.setBounds(0, 0, 40, 40)
        val span = ImageSpan(drawable, DynamicDrawableSpan.ALIGN_BASELINE)
        str.setSpan(span, 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        openMapButtonText.value = str
    }

    fun setupThumbnail(activity: Activity, place: Place) {
        // ダウンロード
        place.thumbnailUrl?.let { url ->
            val assetsManager = ErikuraApplication.assetsManager

            assetsManager.fetchImage(activity, url) { result ->
                activity.runOnUiThread {
                    bitmap.value = result
                    thumbnailVisibility.value = 0
                    val bitmapReduced = Bitmap.createScaledBitmap(result, 15, 15, true)
                    val bitmapDraw = BitmapDrawable(bitmapReduced)
                    bitmapDraw.alpha = 150
                    bitmapDrawable.value = bitmapDraw
                }
            }
        }
    }

}

interface PlaceDetailEventHandlers {
    fun onClickOpenMap(view: View)
    fun onClickFavorite(view: View)
}