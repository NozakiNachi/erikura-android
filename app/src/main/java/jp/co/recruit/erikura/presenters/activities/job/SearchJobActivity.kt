package jp.co.recruit.erikura.presenters.activities.job

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.realm.Realm
import io.realm.Sort
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.data.storage.SearchHistory
import jp.co.recruit.erikura.databinding.ActivitySearchJobBinding
import java.util.*


class SearchJobActivity : AppCompatActivity(), SearchJobHandlers {
    private val realm: Realm get() = ErikuraApplication.realm
    private val viewModel: SearchJobViewModel by lazy {
        ViewModelProvider(this).get(SearchJobViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivitySearchJobBinding = DataBindingUtil.setContentView(this, R.layout.activity_search_job)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this

        val textField: AutoCompleteTextView = findViewById(R.id.search_job_text_field)
        val adapter = SearchHistoryAdapter(this, mutableListOf(SearchHistoryItem.CurrentLocation))
        textField.setAdapter(adapter)

        textField.setOnClickListener {
            // すでに表示されている場合のチェックは不要？
            if (!textField.isPopupShowing()) {
                textField.showDropDown()
            }
        }
        textField.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                textField.showDropDown()
            }
        }

        adapter.clear()
        adapter.add(SearchHistoryItem.CurrentLocation)
        adapter.addAll(getHistoryItems().map { SearchHistoryItem.Item(it.keyword) })
    }

    fun getHistoryItems(limit: Int = 3): List<SearchHistory> {
        return realm.where(SearchHistory::class.java).sort("lastUpdatedAt", Sort.DESCENDING).limit(limit.toLong()).findAll()
    }


    fun addHistoryItem(keyword: String) {
        // 検索履歴を保存します
        realm.executeTransaction { realm ->
            // キーワードに一致する履歴があれば時刻を更新、なければ新しく履歴を保存します
            var history = realm.where(SearchHistory::class.java).equalTo("keyword", keyword).findFirst() ?: run {
                realm.createObject(SearchHistory::class.java, keyword)
            }
            history.lastUpdatedAt = Date()
        }
    }

    override fun onClickDetailButton(view: View) {
        viewModel.detailButtonVisibility.value = View.GONE
        viewModel.detailConditionsVisibility.value = View.VISIBLE
    }

    override fun onClickSearchButton(view: View) {
        Log.v("KEYWORD: ", viewModel.keyword.value)

        val keyword = viewModel.keyword.value ?: ""
        if (keyword != "現在地周辺") {
            Api(this).geocode(keyword) { latLng ->
                addHistoryItem(keyword)
                // FIXME: 検索処理、地図・リスト画面への遷移
                Log.v("検索", latLng.toString())
            }
        }
        else {
            val locationManager = ErikuraApplication.locationManager
            locationManager.latLng?.let { latLng ->
                // FIXME: 検索処理、地図・リスト画面への遷移
                Log.v("検索", latLng.toString())
            }
        }
    }
}

class SearchJobViewModel: ViewModel() {
    val keyword: MutableLiveData<String> = MutableLiveData()

    val detailButtonVisibility: MutableLiveData<Int> = MutableLiveData(View.VISIBLE)
    val detailConditionsVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)

    val isSearchButtonEnabled = MediatorLiveData<Boolean>().also { result ->
        result.addSource(keyword) { result.value = isValid() }
    }

    fun isValid(): Boolean {
        var valid = true
        // キーワードの必須チェック
        if (keyword.value?.isBlank() ?: true) {
            valid = false
        }
        return valid
    }
}

interface SearchJobHandlers {
    fun onClickDetailButton(view: View)
    fun onClickSearchButton(view: View)
}

sealed class SearchHistoryItem {
    companion object {
        val iconCurrentLocation = R.drawable.history_location
        val iconHistory = R.drawable.history_item
    }

    val name: String
    val iconResourceId: Int

    constructor(name: String, icon: Int) {
        this.name = name
        this.iconResourceId = icon
    }

    override fun toString(): String {
        return name
    }

    object CurrentLocation: SearchHistoryItem("現在地周辺", iconCurrentLocation)
    class Item(name: String): SearchHistoryItem(name, iconHistory)
}

class SearchHistoryAdapter(context: Context, items: MutableList<SearchHistoryItem>): ArrayAdapter<SearchHistoryItem>(context, R.layout.fragment_history_downdown_item, R.id.history_dropdown_item_text, items) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val convertView = convertView ?: run {
            val inflater = LayoutInflater.from(context)
            inflater.inflate(R.layout.fragment_history_downdown_item, parent, false)
        }

        getItem(position)?.let {
            val textView: TextView = convertView.findViewById(R.id.history_dropdown_item_text)
            val imageView: ImageView = convertView.findViewById(R.id.history_dropdown_item_icon)

            textView.text = it.name
            imageView.setImageResource(it.iconResourceId)
        }
        return convertView
    }
}
