package com.DevAsh.recbusiness.Home.Transactions

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.view.View
import androidx.core.app.ActivityCompat
import com.DevAsh.recbusiness.Context.*
import com.DevAsh.recbusiness.R
import kotlinx.android.synthetic.main.activity_transaction_status.*
import kotlinx.android.synthetic.main.activity_transaction_status.badge

class TransactionDetails : AppCompatActivity() {

    lateinit var context: Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction_status)


        amount.text ="${TransactionContext.selectedTransaction?.amount}"
        selectedUserName.text = TransactionContext.selectedTransaction?.name
        badge.setBackgroundColor(Color.parseColor(TransactionContext.avatarColor))
        badge.text = TransactionContext.selectedTransaction?.name?.substring(0,1)
        badge.text = TransactionContext.selectedTransaction?.name.toString()[0].toString()


        transactionID.text = String.format("%020d", TransactionContext.selectedTransaction?.transactionId?.toInt())

        loadAvatar()


        if(TransactionContext.selectedTransaction?.isGenerated!!){
            logoContainer.visibility=View.VISIBLE
            subText.text = "Added  ${TransactionContext.selectedTransaction?.amount} ${TransactionContext.currency}"
            type.text = "Added by"
        }else{
            subText.text =
                "${if (TransactionContext.selectedTransaction?.type=="Send") "Paid" else TransactionContext.selectedTransaction?.type}  ${TransactionContext.selectedTransaction?.amount} ${TransactionContext.currency}"
            type.text = if (TransactionContext.selectedTransaction?.type=="Send") "Paid to" else "Received from"

        }

        if (TransactionContext.selectedTransaction?.type=="Send"){
            toDetails.text = "To: ${TransactionContext.selectedTransaction?.name}"
            toID.text = "${TransactionContext.selectedTransaction?.id}"

            fromDetails.text = "From: ${DetailsContext.name}"
            fromID.text = "${DetailsContext.id}"
        }else{
            toDetails.text = "To: ${DetailsContext.name}"
            toID.text = "${DetailsContext.id}"
            if(TransactionContext.selectedTransaction?.isGenerated!!){
                fromDetails.text = "${TransactionContext.selectedTransaction?.name} ID"
                fromID.text = "${TransactionContext.selectedTransaction?.id}"
            }else{
                fromDetails.text = "From: ${TransactionContext.selectedTransaction?.name}"
                fromID.text = "${TransactionContext.selectedTransaction?.id}".trim()
            }
        }

        share.setOnClickListener{
            val permissions = arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if(packageManager.checkPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE,packageName)== PackageManager.PERMISSION_GRANTED ){
                Handler().postDelayed({
                    share()
                },0)
            }else{
                ActivityCompat.requestPermissions(this, permissions,0)
            }
        }

        cancel.setOnClickListener{
            onBackPressed()
        }

        back.setOnClickListener{
            onBackPressed()
        }




        if (TransactionContext.selectedTransaction?.name.toString().startsWith("+")) {
           badge.text = TransactionContext.selectedTransaction?.name.toString().subSequence(1, 3)
           badge.textSize = 18F
        }

        context=this



    }

    private fun loadAvatar(){
        UiContext.loadProfileImage(this, TransactionContext.selectedTransaction?.id!!,object:LoadProfileCallBack{
            override fun onSuccess() {
                avatarContainer.visibility=View.GONE
                profile.visibility = View.VISIBLE
            }

            override fun onFailure() {
                avatarContainer.visibility= View.VISIBLE
                profile.visibility = View.GONE

            }

        },profile)
    }



    private fun share(){
        val view = findViewById<View>(R.id.mainContent)
        val bitmap2 = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap2)
        view.draw(canvas)
        val bitmapPath = MediaStore.Images.Media.insertImage(contentResolver, bitmap2,"title", null)
        val bitmapUri = Uri.parse(bitmapPath)
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_STREAM, bitmapUri )
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.type = "image/png"
        startActivity(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(requestCode==0){
            if(grantResults[0]== PackageManager.PERMISSION_GRANTED ){
                share()
            }
        }
    }

}
