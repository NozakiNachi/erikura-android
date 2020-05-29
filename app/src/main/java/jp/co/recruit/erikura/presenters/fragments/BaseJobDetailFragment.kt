package jp.co.recruit.erikura.presenters.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.User

abstract class BaseJobDetailFragment : Fragment {
    companion object {
        const val JOB_ARGUMENT = "job"
        const val USER_ARGUMENT = "user"

        fun fillArguments(bundle: Bundle, job: Job?, user: User?) {
            bundle.putParcelable(JOB_ARGUMENT, job)
            bundle.putParcelable(USER_ARGUMENT, user)
        }
    }

//    constructor(job: Job?, user: User?): super() {
//        arguments = Bundle().also { args ->
//            fillArguments(args, job, user)
//        }
//    }

    constructor() : super()

    protected var job: Job? = null
    protected var user: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let { args ->
            this.job = args.getParcelable(JOB_ARGUMENT)
            this.user = args.getParcelable(USER_ARGUMENT)
        }
    }

    open fun refresh(job: Job?, user: User?) {
        arguments?.let { args ->
            fillArguments(args, job, user)
        }
        this.job = job
        this.user = user
        // MEMO: viewModel の更新はサブクラスで実施します
    }
}