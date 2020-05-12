package jp.co.recruit.erikura.presenters.fragments

import androidx.fragment.app.Fragment
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.User

abstract class BaseJobDetailFragment(var job: Job?, var user: User?) : Fragment() {
    open fun refresh(job: Job?, user: User?) {
        this.job = job
        this.user = user
        // MEMO: viewModel の更新はサブクラスで実施します
    }
}