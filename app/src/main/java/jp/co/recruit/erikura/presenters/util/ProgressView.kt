package jp.co.recruit.erikura.presenters.util

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
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
            val a = context.obtainStyledAttributes(attrs, R.styleable.ProgressView)
            if (a.hasValue(R.styleable.ProgressView_progress)) {
                var progress = a.getInt(R.styleable.ProgressView_progress, 1)
            }

            // ボタンの切り替え
        }

    }
}