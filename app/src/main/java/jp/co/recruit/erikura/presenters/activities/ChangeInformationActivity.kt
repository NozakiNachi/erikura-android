package jp.co.recruit.erikura.presenters.activities

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.databinding.*


class ChangeInformationActivity : AppCompatActivity(), ChangeInformationEventHandlers {

    var user: User = User()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_information)

        val binding: ActivityChangeInformationBinding = DataBindingUtil.setContentView(this, R.layout.activity_change_information)
        binding.lifecycleOwner = this
        binding.handlers = this
    }
}

interface ChangeInformationEventHandlers {
}
class ChangeInformationViewModel: ViewModel() {
}
