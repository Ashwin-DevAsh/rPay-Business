package com.DevAsh.recbusiness.Home

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.view.View
import androidmads.library.qrgenearator.QRGContents
import androidmads.library.qrgenearator.QRGEncoder
import androidx.core.app.ActivityCompat
import com.DevAsh.recbusiness.Context.ApiContext
import com.DevAsh.recbusiness.Context.DetailsContext
import com.DevAsh.recbusiness.R
import com.google.zxing.WriterException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import kotlinx.android.synthetic.main.activity_display_qrcode.*
import kotlinx.android.synthetic.main.activity_profile.*
import kotlinx.android.synthetic.main.activity_profile.qr
import kotlinx.android.synthetic.main.activity_profile.qrName
import kotlinx.android.synthetic.main.activity_profile.share

class DisplayQrcode : AppCompatActivity() {

    private lateinit var bitmap: Bitmap
    private lateinit var bitmapUri: Uri
    private lateinit var bitmapPath:String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display_qrcode)


        qrName.text = DetailsContext.name
        storeName.text = DetailsContext.storeName


        val jwt = Jwts.builder().claim("name", DetailsContext.storeName)
            .claim("number", DetailsContext.phoneNumber)
            .claim("id", DetailsContext.id)
            .signWith(SignatureAlgorithm.HS256, ApiContext.qrKey)
            .compact()

        val qrgEncoder =
            QRGEncoder(jwt.toString(), null, QRGContents.Type.TEXT, 1000)
        qrgEncoder.colorWhite = getColor(R.color.colorPrimary)
        try {
            bitmap = qrgEncoder.bitmap
            qr.setImageBitmap(bitmap)
        } catch (e: WriterException) {

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
    }

    private fun share(){
        val view = findViewById<View>(R.id.qrContent)
        val bitmap2 = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap2)
        view.draw(canvas)
        bitmapPath = MediaStore.Images.Media.insertImage(contentResolver, bitmap2,"title", null)
        bitmapUri = Uri.parse(bitmapPath)
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_STREAM, bitmapUri )
        intent.putExtra(Intent.EXTRA_TEXT,"Hey! this is R-Pay QrCode")
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
            if(grantResults[0]==PackageManager.PERMISSION_GRANTED ){
                share()
            }
        }
    }
}