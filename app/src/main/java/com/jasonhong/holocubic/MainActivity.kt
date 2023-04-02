package com.jasonhong.holocubic

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import android.widget.ImageView
import com.bumptech.glide.Glide
import android.graphics.Matrix
import android.os.Handler
import android.os.Looper
import androidx.viewpager.widget.ViewPager
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import java.security.MessageDigest


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val viewPager = findViewById<ViewPager>(R.id.viewPager)

        val imageUrls = listOf(
            "https://cdn.shopify.com/s/files/1/0059/8835/2052/products/Okame_Cherry_FGT_1024x1024.jpg?v=1567030775",
            "https://img.thrfun.com/img/229/574/oriole_tx2.jpg",
            "https://2.bp.blogspot.com/-gj7W66B3aHE/WRg1CdbcNSI/AAAAAAAADAg/fCYhtnehyM4i6Crr1_SKYbVMBWgWzcCKgCEw/s1600/03262017_de+Onrust_01713924.jpg",
            "https://wallpapercave.com/wp/hogH5l6.jpg",
            "https://wallpapercave.com/wp/MVvcYVb.jpg",
            "https://wallpapercave.com/wp/VxY1aG0.jpg"

            // Add more image URLs here
        )

        val handler = Handler(Looper.getMainLooper())
        val update = object : Runnable {
            override fun run() {
                val nextItem = (viewPager.currentItem + 1) % imageUrls.size
                viewPager.setCurrentItem(nextItem, true)
                handler.postDelayed(this, 3000) // Change images every 3 seconds
            }
        }

        handler.postDelayed(update, 3000) // Start the slideshow with a 3-second delay

        val adapter = ImageSlideAdapter(this, imageUrls)
        viewPager.adapter = adapter

    }
}

