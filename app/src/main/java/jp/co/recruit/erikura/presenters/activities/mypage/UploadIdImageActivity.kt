package jp.co.recruit.erikura.presenters.activities.mypage

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.business.models.ComparingData
import jp.co.recruit.erikura.presenters.activities.BaseActivity

class UploadIdImageActivity : BaseActivity(), UploadIdImageEventHandlers{
    var comparingData = ComparingData()
    var userId: Int? = null
    var fromWhere: Int? = null
    private val viewModel: UploadIdImageViewModel by lazy {
        ViewModelProvider(this).get(UploadIdImageViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        comparingData = intent.getParcelableExtra("comparingData")
        userId = intent.getIntExtra("userId", 0)
        fromWhere = intent.getIntExtra(ErikuraApplication.FROM_WHERE, ErikuraApplication.FROM_NOT_FOUND)
    }
}

class UploadIdImageViewModel : ViewModel() {

}

interface UploadIdImageEventHandlers{

}