package jp.co.recruit.erikura.presenters.fragments

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomnavigation.LabelVisibilityMode
import jp.co.recruit.erikura.R

class ErikuraBottomNavigationView : BottomNavigationView {
    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, com.google.android.material.R.attr.bottomNavigationStyle)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        this.labelVisibilityMode = LabelVisibilityMode.LABEL_VISIBILITY_LABELED

        val displayMetrics = context.resources.displayMetrics
        this.itemIconSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20.0f, displayMetrics).toInt()
        this.itemTextAppearanceActive = R.style.bottomNavigationItemText
        this.itemTextAppearanceInactive = R.style.bottomNavigationItemText
    }
}