package jp.co.recruit.erikura.presenters.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.business.models.Place
import jp.co.recruit.erikura.databinding.FragmentWorkingPlaceViewBinding

class WorkingPlaceViewFragment(private val place: Place) : Fragment() {
    private val viewModel: WorkingPlaceViewFragmentViewModel by lazy {
        ViewModelProvider(this).get(WorkingPlaceViewFragmentViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        container?.removeAllViews()
        val binding = FragmentWorkingPlaceViewBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = activity
        viewModel.setup(place)
        binding.viewModel = viewModel
        return binding.root
    }

}

class WorkingPlaceViewFragmentViewModel: ViewModel() {
    val titleText: MutableLiveData<String> = MutableLiveData()
    val captionText: MutableLiveData<String> = MutableLiveData()
    val captionVisibility: MutableLiveData<Int> = MutableLiveData()

    fun setup(place: Place) {
        if ( (place.hasEntries) || (place.workingPlaceShort.isNullOrBlank()) ) {
            captionText.value = place.workingPlace
            if(!(place?.workingBuilding.isNullOrBlank())) {
                titleText.value = place.workingBuilding
            }else {
                titleText.value = place.workingPlace
                captionVisibility.value = 8
            }
        }else {
            titleText.value = place.workingPlaceShort
            captionVisibility.value = 8
        }
    }
}
