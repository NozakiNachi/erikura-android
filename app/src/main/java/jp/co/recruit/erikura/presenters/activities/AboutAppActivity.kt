package jp.co.recruit.erikura.presenters.activities

import android.app.ActivityOptions
import android.content.Intent
import android.app.AlertDialog
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.databinding.*
import jp.co.recruit.erikura.presenters.activities.registration.RegisterEmailActivity
import kotlinx.android.synthetic.main.activity_about_app.*



class AboutAppActivity : AppCompatActivity(), AboutAppEventHandlers{
    data class MenuItem(val id: Int, val label: String, val onSelect: () -> Unit)

    var user: User = User()

    private val viewModel: AboutAppViewModel by lazy {
        ViewModelProvider(this).get(AboutAppViewModel::class.java)
    }

    // FIXME: 正しいリンク先の作成
    var menuItems: ArrayList<MenuItem> = arrayListOf(
        MenuItem(0, "利用規約") {
            val intent = Intent(this, ChangeUserInformationActivity::class.java)
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        },
        MenuItem(1, "プライバシーポリシー") {
            val intent = Intent(this, AccountSettingActivity::class.java)
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        },
        MenuItem(2, "推奨環境") {
            val intent = Intent(this, NotificationSettingActivity::class.java)
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)


        val binding: ActivityAboutAppBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_about_app)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this


        val adapter = AboutAppAdapter(menuItems)
        adapter.setOnItemClickListener(object : AboutAppAdapter.OnItemClickListener {
            override fun onItemClickListener(item: MenuItem) {
                item.onSelect()
            }
        })

        about_app_recycler_view.adapter = adapter
        val itemDecoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        about_app_recycler_view.addItemDecoration(itemDecoration)
    }

    class AboutAppAdapter(private val menuItems: List<MenuItem>) : RecyclerView.Adapter<AboutAppAdapter.ViewHolder>()
    {
        class ViewHolder(val binding: FragmentAboutAppCellBinding) : RecyclerView.ViewHolder(binding.root)
        var listener: OnItemClickListener? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = DataBindingUtil.inflate<FragmentAboutAppCellBinding>(
                LayoutInflater.from(parent.context),
                R.layout.fragment_about_app_cell,
                parent,
                false
            )
            return ViewHolder(binding)
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val MenuListItem = menuItems.get(position)
            val viewModel = AboutAppMenuItemViewModel(MenuListItem)
            holder.binding.viewModel = viewModel

            holder.binding.root.setOnClickListener {
                listener?.onItemClickListener(menuItems[position])
            }
        }
        interface OnItemClickListener{
            fun onItemClickListener(item: MenuItem)
        }

        fun setOnItemClickListener(listener: OnItemClickListener){
            this.listener = listener
        }

        override fun getItemCount() = menuItems.size
    }
}


interface AboutAppEventHandlers {

}

class AboutAppViewModel: ViewModel() {
}

class AboutAppMenuItemViewModel(val item: AboutAppActivity.MenuItem) : ViewModel() {
}