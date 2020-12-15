package jp.co.recruit.erikura.presenters.activities.mypage

import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.ImageView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Place
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityFavoritePlacesBinding
import jp.co.recruit.erikura.databinding.FragmentFavoritePlaceItemBinding
import jp.co.recruit.erikura.presenters.activities.BaseTabbedActivity
import jp.co.recruit.erikura.presenters.activities.OwnJobsActivity
import jp.co.recruit.erikura.presenters.activities.TabEventHandlers
import jp.co.recruit.erikura.presenters.activities.job.MapViewActivity
import jp.co.recruit.erikura.presenters.activities.job.PlaceDetailActivity
import jp.co.recruit.erikura.presenters.util.setOnSafeClickListener

class FavoritePlacesActivity : BaseTabbedActivity(R.id.tab_menu_mypage), FavoritePlaceEventHandlers{
    private lateinit var favoritePlaceAdapter: FavoritePlaceAdapter
    private var placeList: List<Place> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)

        val binding: ActivityFavoritePlacesBinding = DataBindingUtil.setContentView(this, R.layout.activity_favorite_places)
        binding.lifecycleOwner = this
        binding.handlers = this

        favoritePlaceAdapter = FavoritePlaceAdapter(this, listOf()).also {
            it.onClickListener = object : FavoritePlaceAdapter.OnClickListener {
                override fun onClick(position: Int) {
                    onClickItem(position)
                }
            }
        }
        val favoritePlaceView: RecyclerView = findViewById(R.id.favorite_places_recycler_view)
        favoritePlaceView.setHasFixedSize(true)
        favoritePlaceView.adapter = favoritePlaceAdapter

    }

    override fun onStart() {
        super.onStart()
        Api(this).favoritePlaces {
            it.forEach { place ->
                Log.d("places", place.toString())
            }
            placeList = it
            favoritePlaceAdapter.places = it
            favoritePlaceAdapter.notifyDataSetChanged()
            //　表示がFDL形式の場合の処理
            ErikuraApplication.instance.removePushUriFromFDL(intent, "/app/link/jobs/favorite/")
        }
    }

    override fun onResume() {
        super.onResume()

        mypageCurrentActivity = this.javaClass
    }

    override fun onBackPressed() {
        super.onBackPressed()

        Intent(this, MypageActivity::class.java).let {
            it.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(it)
        }
    }

    private fun onClickItem(position: Int) {
        // 場所詳細画面へ遷移
        val intent= Intent(this, PlaceDetailActivity::class.java)
        intent.putExtra("workingPlace", placeList[position])
        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        startActivity(intent)
    }
}

// お気に入り場所一覧
class FavoritePlaceItemViewModel(activity: FragmentActivity, place: Place): ViewModel() {
    val image: MutableLiveData<Bitmap> = MutableLiveData()
    val workingPlace: MutableLiveData<String> = MutableLiveData()

    init {
        if ( (place.hasEntries) || (place.workingPlaceShort.isNullOrBlank()) ) {
            workingPlace.value = "${place.workingPlace?:""} ${place.workingBuilding}"
        }else {
            workingPlace.value = place.workingPlaceShort
        }
    }
}

class FavoritePlaceViewHolder(val binding: FragmentFavoritePlaceItemBinding): RecyclerView.ViewHolder(binding.root)

class FavoritePlaceAdapter(
    val activity: FragmentActivity,
    var places: List<Place>
    ):  RecyclerView.Adapter<FavoritePlaceViewHolder>() {
    var onClickListener: OnClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoritePlaceViewHolder {
        val binding = DataBindingUtil.inflate<FragmentFavoritePlaceItemBinding>(
            LayoutInflater.from(parent.context),
            R.layout.fragment_favorite_place_item,
            parent,
            false
        )

        return FavoritePlaceViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return places.count()
    }

    override fun onBindViewHolder(holder: FavoritePlaceViewHolder, position: Int) {
        holder.binding.lifecycleOwner = activity
        holder.binding.viewModel = FavoritePlaceItemViewModel(activity, places[position])
        holder.binding.root.setOnSafeClickListener {
            onClickListener?.apply {
                onClick(position)
            }
        }

        // ダウンロード
        val place = places[position]
        val imageView: ImageView = holder.binding.root.findViewById(R.id.favorite_place_item_image)
        val url = place.thumbnailUrl
        if (url.isNullOrBlank()) {
            imageView.setImageDrawable(ErikuraApplication.instance.applicationContext.resources.getDrawable(R.drawable.ic_noimage, null))
        }
        else {
            val assetsManager = ErikuraApplication.assetsManager
            assetsManager.fetchImage(activity, url!!, imageView)
        }
    }

    interface OnClickListener {
        fun onClick(position: Int)
    }
}

interface FavoritePlaceEventHandlers: TabEventHandlers {
}
