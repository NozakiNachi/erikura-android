package jp.co.recruit.erikura.presenters.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.User

abstract class BaseJobDetailFragment : Fragment {
    companion object {
        const val JOB_ARGUMENT = "job"
        const val USER_ARGUMENT = "user"

        fun fillArguments(bundle: Bundle, job: Job?, user: User?) {
            ErikuraApplication.instance.currentJob = job
            bundle.putParcelable(USER_ARGUMENT, user)
        }
    }

    constructor() : super()

    protected var job: Job? = null
    protected var user: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let { args ->
            this.job = ErikuraApplication.instance.currentJob
            this.user = args.getParcelable(USER_ARGUMENT)
        }
    }

    open fun refresh(job: Job?, user: User?) {
        arguments?.let { args ->
            fillArguments(args, job, user)
        }
        this.job = job
        this.user = user
        ErikuraApplication.instance.currentJob = job
        // MEMO: viewModel の更新はサブクラスで実施します
    }
}