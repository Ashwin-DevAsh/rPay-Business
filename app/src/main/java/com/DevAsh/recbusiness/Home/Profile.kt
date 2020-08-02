package com.DevAsh.recbusiness.Home

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.view.View
import android.widget.TextView
import androidmads.library.qrgenearator.QRGContents
import androidmads.library.qrgenearator.QRGEncoder
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.DevAsh.recbusiness.Context.ApiContext
import com.DevAsh.recbusiness.Context.DetailsContext
import com.DevAsh.recbusiness.Context.LoadProfileCallBack
import com.DevAsh.recbusiness.Context.UiContext
import com.DevAsh.recbusiness.R
import com.DevAsh.recbusiness.Registration.Login
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import com.google.zxing.WriterException
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_profile.*
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream


class Profile : AppCompatActivity() {
    private lateinit var bitmap: Bitmap
    private lateinit var bitmapUri:Uri
    private lateinit var bitmapPath:String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Realm.init(this)
        setContentView(R.layout.activity_profile)

        qrName.text = DetailsContext.name
        name.text =DetailsContext.name
        email.text =DetailsContext.email
        phone.text =  "+"+DetailsContext.phoneNumber


        val jwt = Jwts.builder().claim("name", DetailsContext.storeName)
            .claim("number", DetailsContext.phoneNumber)
            .claim("id",DetailsContext.id)
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

        loadProfilePicture()

        changePassword.setOnClickListener{
            startActivity(Intent(this,ChangePassword::class.java))
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

        back.setOnClickListener{
            onBackPressed()
        }

        transactions.setOnClickListener{
            startActivity(Intent(this,AllTransactions::class.java))
        }

        profilePicture.setOnClickListener{
//            Handler().postDelayed({
//                UiContext.removeFromCache(DetailsContext.id)
//            },0)
//            CropImage.activity()
//                .setGuidelines(CropImageView.Guidelines.ON)
//                .start(this)
        }

        logout.setOnClickListener{
            val mBottomSheetDialog = AlertDialog.Builder(this)
            val sheetView: View = layoutInflater.inflate(R.layout.confirm_sheet, null)
            val done = sheetView.findViewById<TextView>(R.id.done)
            val cancel = sheetView.findViewById<TextView>(R.id.cancel)
            mBottomSheetDialog.setView(sheetView)
            val dialog = mBottomSheetDialog.show()
            cancel.setOnClickListener{
                dialog.dismiss()
            }
            done.setOnClickListener{
                dialog.dismiss()
                logOut()
            }

        }

    }

    private fun logOut(){
        Realm.getDefaultInstance().executeTransaction{ realm ->
            realm.deleteAll()
            val intent = Intent(applicationContext, Login::class.java)
            finishAffinity()
            startActivity(intent)
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

    private fun uploadImage(file:File,newImage:Bitmap){
        AndroidNetworking.upload(ApiContext.apiUrl+ApiContext.registrationPort+"/addProfilePicture/"+DetailsContext.id)
            .addHeaders("token", DetailsContext.token)
            .addMultipartFile("profilePicture",file)
            .setPriority(Priority.HIGH)
            .build()
            .setUploadProgressListener {
                    bytesUploaded, totalBytes -> println("$bytesUploaded $totalBytes")
            }.getAsJSONObject(object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject?) {
                    profilePicture.setImageBitmap(newImage)
                    println(response)
                    UiContext.isProfilePictureChanged = true
                    UiContext.newProfile = newImage
                }

                override fun onError(anError: ANError?) {
                    println(anError)
                }

            })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(data)
            if (resultCode == Activity.RESULT_OK) {
                val resultUri = result.uri
                val bitmap = Bitmap.createScaledBitmap(  MediaStore.Images.Media.getBitmap(this.contentResolver, resultUri), 500, 500, true);
                Handler().postDelayed({
                    UiContext.removeFromCache(DetailsContext.id)
                },0)
                uploadImage(saveBitmapToFile(File(resultUri.path!!)),bitmap)

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                println(result.error)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)

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

    private fun loadProfilePicture(){
        UiContext.loadProfileImage(
            this,
            DetailsContext.id,
            object : LoadProfileCallBack {
                override fun onSuccess() {

                }

                override fun onFailure() {

                }
            },
            profilePicture,
            R.drawable.profile
        )
    }


    private fun saveBitmapToFile(file: File): File {
        val realFile = file
        return try {

            // BitmapFactory options to downsize the image
            val o: BitmapFactory.Options = BitmapFactory.Options()
            o.inJustDecodeBounds = true
            o.inSampleSize = 6
            // factor of downsizing the image
            var inputStream = FileInputStream(file)
            //Bitmap selectedBitmap = null;
            BitmapFactory.decodeStream(inputStream, null, o)
            inputStream.close()

            // The new size we want to scale to
            val REQUIRED_SIZE = 75

            // Find the correct scale value. It should be the power of 2.
            var scale = 1
            while (o.outWidth / scale / 2 >= REQUIRED_SIZE &&
                o.outHeight / scale / 2 >= REQUIRED_SIZE
            ) {
                scale *= 2
            }
            val o2: BitmapFactory.Options = BitmapFactory.Options()
            o2.inSampleSize = scale
            inputStream = FileInputStream(file)
            val selectedBitmap: Bitmap? = BitmapFactory.decodeStream(inputStream, null, o2)
            inputStream.close()
            file.createNewFile()
            val outputStream = FileOutputStream(file)
            selectedBitmap?.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            file
        } catch (e: Exception) {
            println(e)
            realFile
        }
    }
}