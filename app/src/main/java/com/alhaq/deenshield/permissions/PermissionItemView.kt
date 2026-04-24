package com.alhaq.deenshield.permissions

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.alhaq.deenshield.R
import com.alhaq.deenshield.databinding.PermissionItemViewBinding

class PermissionItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding: PermissionItemViewBinding

    var isGranted: Boolean = false
        set(value) {
            field = value
            updateStatus()
        }

    init {
        val inflater = LayoutInflater.from(context)
        binding = PermissionItemViewBinding.inflate(inflater, this)

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.PermissionItemView)
        val title = typedArray.getString(R.styleable.PermissionItemView_permissionTitle)
        binding.permissionTitle.text = title
        typedArray.recycle()

        updateStatus()
    }

    private fun updateStatus() {
        if (isGranted) {
            binding.permissionStatus.text = context.getString(R.string.granted)
            binding.permissionStatus.setTextColor(ContextCompat.getColor(context, R.color.md_theme_primary))
        } else {
            binding.permissionStatus.text = context.getString(R.string.denied)
            binding.permissionStatus.setTextColor(ContextCompat.getColor(context, R.color.md_theme_error))
        }
    }
}