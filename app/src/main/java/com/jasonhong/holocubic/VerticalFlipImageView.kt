package com.jasonhong.holocubic

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

class VerticalFlipImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    override fun onDraw(canvas: Canvas) {
        canvas.save()
        canvas.scale(1f, -1f, width / 2f, height / 2f)
        super.onDraw(canvas)
        canvas.restore()
    }
}
