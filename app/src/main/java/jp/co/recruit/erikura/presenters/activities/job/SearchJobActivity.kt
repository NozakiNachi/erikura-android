package jp.co.recruit.erikura.presenters.activities.job

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import jp.co.recruit.erikura.R


class SearchJobActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_job)

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

        // FIXME: 履歴の保存処理と、履歴の取得処理を実装すること => Realm 使う？
        // FIXME: items の更新はどうやってやればいいの？
        adapter.clear()
        adapter.addAll(listOf(
            SearchHistoryItem.CurrentLocation,
            SearchHistoryItem.Item("東京駅"),
            SearchHistoryItem.Item("新宿駅"),
            SearchHistoryItem.Item("渋谷")
        ))
    }
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
