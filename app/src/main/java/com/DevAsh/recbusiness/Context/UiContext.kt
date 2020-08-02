package com.DevAsh.recbusiness.Context


import android.content.Context
import android.graphics.Bitmap
import android.widget.ImageView
import com.DevAsh.recbusiness.R
import com.squareup.picasso.Callback
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso
import java.lang.Exception

object UiContext {

    var newProfile:Bitmap?=null

    var colors = arrayListOf(
        "#E6f9a825","#E600b0ff","#E6ff1744","#E6512da8","#E600acc1","#E61a237e","#E65d4037","#E6880e4f","#E68bc34a"
    )



    var isProfilePictureChanged=false

    fun loadProfileImage(context: Context,
                         id:String,
                         loadProfileCallBack: LoadProfileCallBack,
                         imageView:ImageView,
                         errorPlaceHolder:Int= R.drawable.place_holder
    ){
        Picasso.get()
            .load(ApiContext.imageURL+id+".jpg")
            .placeholder(errorPlaceHolder)
            .error(errorPlaceHolder)
            .into(imageView, object : Callback {
                override fun onSuccess() {
                    loadProfileCallBack.onSuccess()
                }
                override fun onError(e: Exception?) {
                    loadProfileCallBack.onFailure()
                }

            })

    }

    fun removeFromCache(id:String){
        Picasso.get().invalidate(ApiContext.imageURL+id+".jpg")
    }

    fun UpdateImage(imageView:ImageView
    ){
        imageView.setImageBitmap(newProfile)

    }
}

interface LoadProfileCallBack{
    fun onSuccess()
    fun onFailure()
}