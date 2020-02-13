package jp.co.recruit.erikura.presenters.fragments

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.OwnJobQuery
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.presenters.activities.job.JobListAdapter
import jp.co.recruit.erikura.presenters.activities.job.JobListItemDecorator

import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.databinding.FragmentFinishedJobsBinding
import jp.co.recruit.erikura.presenters.activities.job.JobDetailsActivity

//// TODO: Rename parameter arguments, choose names that match
//// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
//private const val ARG_PARAM1 = "param1"
//private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [FinishedJobsFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [FinishedJobsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class FinishedJobsFragment : Fragment(), FinishedJobsHandlers {
    private val viewModel: FinishedJobsViewModel by lazy {
        ViewModelProvider(this).get(FinishedJobsViewModel::class.java)
    }
    private lateinit var jobListView: RecyclerView
    private lateinit var jobListAdapter: JobListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val binding: FragmentFinishedJobsBinding = DataBindingUtil.inflate(
            inflater,R.layout.fragment_finished_jobs, container, false
        )
        binding.lifecycleOwner = activity
        binding.viewModel = viewModel
        binding.handlers = this

        jobListAdapter = JobListAdapter(activity!!, listOf(), null).also{
            it.onClickListner = object: JobListAdapter.OnClickListener {
                override fun onClick(job: Job) {
                    Intent(activity, JobDetailsActivity::class.java).let {
                        it.putExtra("job", job)
                        startActivity(it, ActivityOptions.makeSceneTransitionAnimation(activity!!).toBundle())
                    }
                }
            }
        }
        jobListView = binding.root.findViewById(R.id.finished_jobs_recycler_view)
        jobListView.setHasFixedSize(true)
        jobListView.adapter = jobListAdapter
        jobListView.addItemDecoration(DividerItemDecoration(activity!!, DividerItemDecoration.VERTICAL))
        jobListView.addItemDecoration(JobListItemDecorator())

        return binding.root
    }

    override fun onResume() {
        super.onResume()

        fetchFinishedJobs()
    }
//    // TODO: Rename method, update argument and hook method into UI event
//    fun onButtonPressed(uri: Uri) {
//        listener?.onFragmentInteraction(uri)
//    }
//
//    override fun onAttach(context: Context) {
//        super.onAttach(context)
//        if (context is OnFragmentInteractionListener) {
//            listener = context
//        } else {
//            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
//        }
//    }
//
//    override fun onDetach() {
//        super.onDetach()
//        listener = null
//    }
//
//    /**
//     * This interface must be implemented by activities that contain this
//     * fragment to allow an interaction in this fragment to be communicated
//     * to the activity and potentially other fragments contained in that
//     * activity.
//     *
//     *
//     * See the Android Training lesson [Communicating with Other Fragments]
//     * (http://developer.android.com/training/basics/fragments/communicating.html)
//     * for more information.
//     */
//    interface OnFragmentInteractionListener {
//        // TODO: Update argument type and name
//        fun onFragmentInteraction(uri: Uri)
//    }
//
//    companion object {
//        /**
//         * Use this factory method to create a new instance of
//         * this fragment using the provided parameters.
//         *
//         * @param param1 Parameter 1.
//         * @param param2 Parameter 2.
//         * @return A new instance of fragment FinishedJobsFragment.
//         */
//        // TODO: Rename and change types and number of parameters
//        @JvmStatic
//        fun newInstance(param1: String, param2: String) =
//            FinishedJobsFragment().apply {
//                arguments = Bundle().apply {
//                    putString(ARG_PARAM1, param1)
//                    putString(ARG_PARAM2, param2)
//                }
//            }
//    }
    private fun fetchFinishedJobs() {
        Api(context!!).ownJob(OwnJobQuery(status = OwnJobQuery.Status.FINISHED)) { jobs ->
            viewModel.finishedJobs.value = jobs
            jobListAdapter.jobs = viewModel.unreportedJobs
            jobListAdapter.notifyDataSetChanged()
        }
    }
}
class FinishedJobsViewModel: ViewModel(){
    val finishedJobs: MutableLiveData<List<Job>> = MutableLiveData(listOf())

    val unreportedJobs: List<Job> get() {
        val unreported = finishedJobs.value ?: listOf()
        return unreported
    }
}

interface FinishedJobsHandlers {

}