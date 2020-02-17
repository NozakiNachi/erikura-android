package jp.co.recruit.erikura.presenters.util

import android.database.Cursor
import android.provider.MediaStore
import androidx.core.database.getLongOrNull
import androidx.recyclerview.widget.RecyclerView

abstract class RecyclerViewCursorAdapter<VH : RecyclerView.ViewHolder>(cursor: Cursor?): RecyclerView.Adapter<VH>() {
    private var rowIdColumn: Int = -1

    var cursor: Cursor? = null
        set(value) {
            if (value != field) {
                val oldCursor = field
                if (value != null) {
                    field = value
                    rowIdColumn = field?.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID) ?: -1
                    notifyDataSetChanged()
                }
                else {
                    notifyItemRangeRemoved(0 , itemCount)
                    field = value
                    rowIdColumn = -1
                }
            }
        }

    init {
        setHasStableIds(true)
        this.cursor = cursor
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        if (!isDataValid()) {
            throw IllegalStateException("cursor is not valid.")
        }
        // isDataValidを満たす場合には cursor != null になる
        val cursor = this.cursor!!
        if (!cursor.moveToPosition(position)) {
            throw IllegalStateException("Could not move cursor to position[${position}]")
        }

        onBindViewHolder(holder, position, cursor)
    }

    abstract fun onBindViewHolder(viewHolder: VH, position: Int, cursor: Cursor)

    override fun getItemViewType(position: Int): Int {
        if (!isDataValid()) {
            throw IllegalStateException("cursor is not valid.")
        }
        // isDataValidを満たす場合には cursor != null になる
        val cursor = this.cursor!!
        if (!cursor.moveToPosition(position)) {
            throw IllegalStateException("Could not move cursor to position[${position}]")
        }

        return getItemViewType(position, cursor)
    }

    open fun getItemViewType(position: Int, cursor: Cursor): Int {
        return 0
    }

    override fun getItemCount(): Int {
        return if (isDataValid()) {
            cursor?.count ?: 0
        }
        else {
            0
        }
    }

    override fun getItemId(position: Int): Long {
        if (!isDataValid()) {
            throw IllegalStateException("cursor is not valid.")
        }
        // isDataValidを満たす場合には cursor != null になる
        val cursor = this.cursor!!
        if (!cursor.moveToPosition(position)) {
            throw IllegalStateException("Could not move cursor to position[${position}]")
        }
        return cursor.getLongOrNull(rowIdColumn) ?: 0
    }

    private fun isDataValid(): Boolean {
        return !isClosed()
    }

    private fun isClosed(): Boolean {
        return cursor?.isClosed ?: true
    }
}
