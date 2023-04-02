package com.jasonhong.holocubic

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.bumptech.glide.Glide
import com.jasonhong.holocubic.R

class ImageSlideAdapter(private val context: Context, private val imageUrls: List<String>) :
    PagerAdapter() {

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view = LayoutInflater.from(context).inflate(R.layout.image_item, container, false)
        val imageView: VerticalFlipImageView = view.findViewById(R.id.imageView)

        Glide.with(context)
            .load(imageUrls[position])
            .override(288, 288)
            .centerInside()
            .into(imageView)

        container.addView(view)
        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    override fun getCount(): Int {
        return imageUrls.size
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }
}
