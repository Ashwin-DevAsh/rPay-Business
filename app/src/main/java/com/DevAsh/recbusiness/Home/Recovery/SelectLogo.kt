package com.DevAsh.recbusiness.Home.Recovery

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.DevAsh.recbusiness.Home.Profile
import com.DevAsh.recbusiness.R
import kotlinx.android.synthetic.main.activity_select_logo.*
import kotlinx.android.synthetic.main.widget_logo.view.*

class SelectLogo : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_logo)

        iconContainer.layoutManager = GridLayoutManager(this,3)
        iconContainer.adapter = IconsAdapter(arrayListOf(
            resources.getDrawable(R.drawable.hut_cafe),
            resources.getDrawable(R.drawable.tamil_cafe),
            resources.getDrawable(R.drawable.rec_mart),
            resources.getDrawable(R.drawable.rec_bill),
            resources.getDrawable(R.drawable.ug),
            resources.getDrawable(R.drawable.shop1),
            resources.getDrawable(R.drawable.shop2),
            resources.getDrawable(R.drawable.shop3),
            resources.getDrawable(R.drawable.shop4),
            resources.getDrawable(R.drawable.shop5),
            resources.getDrawable(R.drawable.org1),
            resources.getDrawable(R.drawable.org2),
            resources.getDrawable(R.drawable.org3),
            resources.getDrawable(R.drawable.org4),
            resources.getDrawable(R.drawable.org5),
            resources.getDrawable(R.drawable.org6),
            resources.getDrawable(R.drawable.org7)
            ),this)
    }
}


class IconsAdapter(private var items : ArrayList<Drawable>, val context: Activity) : RecyclerView.Adapter<IconHolder>() {

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconHolder {
        return IconHolder(LayoutInflater.from(context).inflate(R.layout.widget_logo, parent, false),context)
    }

    override fun onBindViewHolder(holder: IconHolder, position: Int) {
        holder.logo.setImageDrawable(items[position])
        val bitmap = items[position].toBitmap()
        holder.bitmap = bitmap
    }

}

class IconHolder (view: View, context: Activity) : RecyclerView.ViewHolder(view) {
    var bitmap:Bitmap?=null
    var logo = view.logo
    init {
        view.setOnClickListener{
            Profile.bitmapImage = bitmap
            context.finish()
        }
    }
}

