package jp.co.recruit.erikura.presenters.util

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import jp.co.recruit.erikura.R


class ProgressView : FrameLayout {

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        View.inflate(context, R.layout.progress_view, this)

        if (attrs != null) {
            var progress = 0
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ProgressView)
            if (typedArray.hasValue(R.styleable.ProgressView_progress)) {
                progress = typedArray.getInt(R.styleable.ProgressView_progress, 1)
            }

            // ボタンの切り替え
            when(progress) {

            }

        }

    }
}