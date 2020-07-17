//package jp.co.recruit.erikura.presenters.fragments
//
//import android.content.Context
//import android.content.Intent
//import android.os.Bundle
//import android.text.SpannableStringBuilder
//import android.text.style.ForegroundColorSpan
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.BaseAdapter
//import android.widget.ImageView
//import android.widget.TextView
//import androidx.databinding.DataBindingUtil
//import androidx.fragment.app.FragmentActivity
//import androidx.lifecycle.MediatorLiveData
//import androidx.lifecycle.MutableLiveData
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.ViewModelProvider
//import androidx.recyclerview.widget.RecyclerView
//import jp.co.recruit.erikura.ErikuraApplication
//import jp.co.recruit.erikura.R
//import jp.co.recruit.erikura.business.models.Caution
//import jp.co.recruit.erikura.business.models.Job
//import jp.co.recruit.erikura.business.models.User
//import jp.co.recruit.erikura.data.network.Api
//import jp.co.recruit.erikura.databinding.FragmentPropertyNotesButtonBinding
//import jp.co.recruit.erikura.databinding.FragmentPropertyNotesItemBinding
//import jp.co.recruit.erikura.databinding.FragmentPropertyNotesItemFileBinding
//import jp.co.recruit.erikura.presenters.activities.job.PropertyNotesActivity
//
//class PropertyNotesItemFragment : BaseJobDetailFragment, PropertyNotesItemFragmentEventHandlers {
//    private val viewModel: PropertyNotesItemViewModel by lazy {
//        ViewModelProvider(this).get(PropertyNotesItemViewModel::class.java)
//    }
//    private var files: List<String> = listOf()
//
//    private lateinit var propertyNotesItemAdapter: PropertyNotesItemAdapter
//
//    constructor() : super()
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        // FIXME fileのurlを取得 PropertyNotesActivityのonBindViewHolderから遷移してviewを返す想定
////        files =
//        propertyNotesItemAdapter.files = files
//        val binding = FragmentPropertyNotesItemBinding.inflate(inflater, container, false)
//        binding.lifecycleOwner = activity
//        binding.viewModel = viewModel
//        binding.handlers = this
//        val propertyNotesItemFileView: RecyclerView = activity!!.findViewById(R.id.property_notes_file_list)
//        propertyNotesItemFileView.setHasFixedSize(true)
//        propertyNotesItemFileView.adapter = propertyNotesItemAdapter
//        return binding.root
//    }
//}
//
//class PropertyNotesItemViewModel: ViewModel() {
//    val isNotEmptyCaution = MediatorLiveData<Boolean>()
//    val isButtonEnabled = MediatorLiveData<Boolean>().also { result ->
//        result.value = isNotEmptyCaution.value
//    }
////    val propertyNotesButtonText: MutableLiveData<String> = MutableLiveData()
//}
//
//interface PropertyNotesItemFragmentEventHandlers {
//}
//
//class PropertyNotesItemViewHolder(val binding: FragmentPropertyNotesItemFileBinding): RecyclerView.ViewHolder(binding.root)
//
//class PropertyNotesItemAdapter(
//    val activity: FragmentActivity,
//    var inflater: LayoutInflater,
//    val files: List<String>) : BaseAdapter() {
//    internal data class ViewHolder(val image: ImageView)
//
//    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
//        var view = convertView
//        val viewHolder: ViewHolder
//        if (convertView == null) {
//            view = inflater.inflate(R.layout.fragment_property_notes_item_file, parent)
//            viewHolder = ViewHolder(view.findViewById(R.id.file))
//            view.tag = viewHolder
//        } else {
//            viewHolder = view!!.tag as ViewHolder
//        }
//        if (files[position].isNullOrBlank()) {
//            viewHolder.image.setImageDrawable(
//                ErikuraApplication.instance.applicationContext.resources.getDrawable(
//                    R.drawable.ic_noimage,
//                    null
//                )
//            )
//        } else {
//            val assetsManager = ErikuraApplication.assetsManager
//            assetsManager.fetchImage(activity, files[position], viewHolder.image)
//        }
//        return view!!
//    }
//
//    override fun getCount(): Int {
//        return files.count()
//    }
//
//    override fun getItem(position: Int): Int {
//        return position
//    }
//
//    override fun getItemId(position: Int): Long {
//        return position.toLong()
//    }
//}
////    val activity: FragmentActivity,
////    var files: List<String>
////): RecyclerView.Adapter<PropertyNotesItemViewHolder>() {
////    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PropertyNotesItemViewHolder {
////        // 表示するレイアウトを設定
////        val binding = DataBindingUtil.inflate<FragmentPropertyNotesItemFileBinding>(
////            LayoutInflater.from(parent.context),
////            R.layout.fragment_property_notes_item_file,
////            parent,
////            false
////        )
////        return PropertyNotesItemViewHolder(binding)
////
////    override fun onBindViewHolder(holder: PropertyNotesItemViewHolder, position: Int) {
////        val file = files[position]
////        val imageView: ImageView = holder.binding.root.findViewById(R.id.file)
////        if (file.isNullOrBlank()) {
////            imageView.setImageDrawable(ErikuraApplication.instance.applicationContext.resources.getDrawable(R.drawable.ic_noimage, null))
////        }
////        else {
////            val assetsManager = ErikuraApplication.assetsManager
////            assetsManager.fetchImage(activity, file!!, imageView)
////        }
////        holder.binding.lifecycleOwner = activity
////    }
//}