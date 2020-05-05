package jp.co.recruit.erikura.presenters.activities.mypage

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ToggleButton
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.OwnJobQuery
import jp.co.recruit.erikura.business.util.DateUtils
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityPaymentInformationBinding
import jp.co.recruit.erikura.databinding.FragmentPaymentInfoMonthlyCellBinding
import jp.co.recruit.erikura.databinding.FragmentPaymentInformationListCellBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity
import jp.co.recruit.erikura.presenters.activities.job.ErikuraCarouselAdapter
import jp.co.recruit.erikura.presenters.util.setOnSafeClickListener
import java.text.SimpleDateFormat
import java.util.*

class PaymentInformationActivity : BaseActivity(), PaymentInformationHandlers {
    private val viewModel: PaymentInformationViewModel by lazy {
        ViewModelProvider(this).get(PaymentInformationViewModel::class.java)
    }

    private lateinit var binding: ActivityPaymentInformationBinding
    private lateinit var monthlyPaymentAdapter: MonthlyPaymentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_payment_information)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this

        monthlyPaymentAdapter = MonthlyPaymentAdapter(this, listOf())
        binding.paymentInformationMonthlyList.adapter = monthlyPaymentAdapter
        binding.paymentInformationMonthlyList.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
    }

    override fun onStart() {
        super.onStart()
        fetchJobs()

        // ページ参照のトラッキングの送出
        Tracking.logEvent(event= "view_payment_history", params= bundleOf())
        Tracking.view(name= "/mypage/payment_history", title= "お支払い情報画面")
    }

    override fun onTargetYearSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        val year = (viewModel.targetYears.value ?: listOf())[position]
        viewModel.targetYear.value = year
        fetchJobs()
    }

    private fun fetchJobs() {
        val year = viewModel.targetYear.value ?: viewModel.currentYear
        val startAt = DateUtils.beginningOfMonth(DateUtils.at(year, 1, 1))
        val endAt = DateUtils.endOfMonth(DateUtils.at(year, 12, 31))
        Api(this).ownJob(OwnJobQuery(status = OwnJobQuery.Status.REPORTED, reportedFrom = startAt, reportedTo = endAt)) { jobs ->
            val months: MutableList<Date> = mutableListOf()
            val jobsMap: MutableMap<Date, MutableList<Job>> = mutableMapOf()

            // 月ごとに分割します
            jobs.forEach {
                val month = DateUtils.beginningOfMonth(it.report?.createdAt ?: Date())
                if (months.indexOf(month) < 0) {
                    months.add(month)
                    jobsMap[month] = mutableListOf()
                }
                jobsMap[month]?.add(it)
            }

            // Adapter にわたすための案件情報のリストを構築します
            val monthlyPayments: List<MonthlyPaymentInformation> = months.toList().sortedBy { it }.reversed().map { month ->
                MonthlyPaymentInformation(month, (jobsMap[month] ?: listOf<Job>()).sortedBy { it.report?.createdAt ?: Date() }.reversed())
            }
            monthlyPaymentAdapter.monthlyPayments = monthlyPayments
            monthlyPaymentAdapter.notifyDataSetChanged()

            if(jobs.size == 0){
                binding.payment.setVisibility(View.VISIBLE)
                binding.paymentLine.setVisibility(View.GONE)
//                binding.paymentLineTwo.setVisibility(View.GONE)
                binding.paymentExplain.setVisibility(View.GONE)
            }else{
                binding.payment.setVisibility(View.GONE)
                binding.paymentLine.setVisibility(View.VISIBLE)
//                binding.paymentLineTwo.setVisibility(View.VISIBLE)
                binding.paymentExplain.setVisibility(View.VISIBLE)
            }
        }

        // 口座情報が登録済みであれば、口座情報登録ボタンは表示しない。
        Api(this).payment() {
            if(it.bankName !== null) {
                binding.notPaymentDateButton.setVisibility(View.GONE)
                binding.notPaymentDateExplain.setVisibility(View.GONE)
            }
        }
    }

    // 未登録の場合、口座情報登録画面へ
    override fun onClickAccountSetting(view: View) {
        val intent = Intent(this, AccountSettingActivity::class.java)
        startActivity(intent)
    }
}

class PaymentInformationViewModel: ViewModel() {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val targetYear: MutableLiveData<Int> = MutableLiveData()
    val targetYears: MutableLiveData<List<Int>> = MutableLiveData()
    val targetYearItems = MediatorLiveData<List<String>>().also { result ->
        result.addSource(targetYears) { years ->
            result.value = years.map { String.format("%d年", it) }
        }
    }

    init {
        val calendar = Calendar.getInstance()
        val year = currentYear
        // 初期の対象年は、今年を設定
        targetYear.value = year
        // 対象年は３年前まで指定可能
        targetYears.value = listOf(year, year - 1, year - 2, year - 3)
    }
}

interface PaymentInformationHandlers {
    fun onTargetYearSelected(parent: AdapterView<*>, view: View, position: Int, id: Long)
    fun onClickAccountSetting(view: View)
}

data class MonthlyPaymentInformation(val month: Date, val jobs: List<Job>)

class MonthlyPaymentViewModel(val monthlyPayment: MonthlyPaymentInformation): ViewModel() {
    val monthString: String get() = SimpleDateFormat("yyyy年MM月分").format(monthlyPayment.month)
    val rewardString: String get() {
        var reward = 0
        monthlyPayment.jobs.forEach { reward += it.fee }
        return String.format("%,d円", reward)
    }
    val countString: String get() = String.format("(%,d件)", monthlyPayment.jobs.count())

    val opened: MutableLiveData<Boolean> = MutableLiveData(false)
    val listVisibility = MediatorLiveData<Int>().also { result ->
        result.addSource(opened) { result.value = if (it) { View.VISIBLE } else { View.GONE } }
    }
}

class MonthlyPaymentViewHolder(val binding: FragmentPaymentInfoMonthlyCellBinding) : RecyclerView.ViewHolder(binding.root)

class MonthlyPaymentAdapter(val activity: FragmentActivity, var monthlyPayments: List<MonthlyPaymentInformation>): RecyclerView.Adapter<MonthlyPaymentViewHolder>() {
    var onClickListener: ErikuraCarouselAdapter.OnClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonthlyPaymentViewHolder {
        val binding: FragmentPaymentInfoMonthlyCellBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.fragment_payment_info_monthly_cell,
            parent, false
        )
        return MonthlyPaymentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MonthlyPaymentViewHolder, position: Int) {
        val binding = holder.binding
        val monthlyPayment = monthlyPayments[position]

        binding.lifecycleOwner = activity
        binding.viewModel = MonthlyPaymentViewModel(monthlyPayment)

        val adapter = PaymentListAdapater(activity, monthlyPayment.jobs)
        binding.paymentInfoMonthlyCellList.adapter = adapter

        binding.root.setOnSafeClickListener {
            val toggle: ToggleButton = binding.root.findViewById(R.id.payment_info_monthly_cell_toggle)
            toggle.isChecked = !toggle.isChecked
        }
    }

    override fun getItemCount(): Int {
        return monthlyPayments.count()
    }

    interface OnClickListener {
        fun onClick(job: Job)
    }
}

class PaymentListViewModel(val job: Job): ViewModel() {
    val dateString: String get() = SimpleDateFormat("MM/dd").format(job.report?.createdAt ?: Date())
    val rewardString: String get() = String.format("%,d円", job.fee)
    val titleString: String get() = job.title ?: job.jobKind?.name ?: ""
}

class PaymentListViewHolder(val binding: FragmentPaymentInformationListCellBinding): RecyclerView.ViewHolder(binding.root)

class PaymentListAdapater(val activity: FragmentActivity, var jobs: List<Job>): RecyclerView.Adapter<PaymentListViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentListViewHolder {
        val binding: FragmentPaymentInformationListCellBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.fragment_payment_information_list_cell,
            parent, false
        )
        return PaymentListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PaymentListViewHolder, position: Int) {
        val binding = holder.binding
        val job = jobs[position]

        binding.lifecycleOwner = activity
        binding.viewModel = PaymentListViewModel(job)
    }

    override fun getItemCount(): Int {
        return jobs.count()
    }
}