package jp.co.recruit.erikura.presenters.activities.job

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.presenters.activities.BaseActivity

class PropertyNotesActivity : BaseActivity(), PropertyNotesEventHandlers {
    private val viewModel: PropertyNotesViewModel by lazy {
        ViewModelProvider(this).get(PropertyNotesViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //placeIDを取得しAPI実行
        //実行結果をitemFragmentに埋め込みviewを作成
        //viewをセットする
    }


    override fun onClickShowOtherFAQ(view: View) {
        //トップのFAQへ飛ぶ
    }

}

class PropertyNotesViewModel: ViewModel() {
    //recyclerviewをviewをidで渡す

}

interface PropertyNotesEventHandlers {
    fun onClickShowOtherFAQ(view: View)
}