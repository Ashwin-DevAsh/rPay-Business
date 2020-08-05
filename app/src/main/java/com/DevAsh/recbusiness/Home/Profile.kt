package com.DevAsh.recbusiness.Home

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
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
import com.DevAsh.recbusiness.Helper.AlertHelper
import com.DevAsh.recbusiness.Home.Recovery.SelectLogo
import com.DevAsh.recbusiness.R
import com.DevAsh.recbusiness.Registration.Login
import com.DevAsh.recbusiness.Sync.SocketHelper
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import com.google.zxing.WriterException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_profile.*
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
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
            Handler().postDelayed({
                UiContext.removeFromCache(DetailsContext.id)
            },0)
            val permissions = arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if(packageManager.checkPermission(android.Manifest.permission.READ_CONTACTS,this.packageName)==PackageManager.PERMISSION_GRANTED ){
                startActivity(Intent(this,SelectLogo::class.java))
            }else{
                ActivityCompat.requestPermissions(this, permissions,1)
            }
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
                    println(response)
                    if(response?.getString("message")=="done"){
                        loadProfileNoCache()
                        UiContext.isProfilePictureChanged = true
                        UiContext.newProfile = newImage
                        SocketHelper.updateProfilePicture()
                    }else{
                        AlertHelper.showError("Error while uploading",this@Profile)
                    }
                }

                override fun onError(anError: ANError?) {
                    println(anError)
                }

            })
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
        }else if(requestCode==1){
            if(grantResults[0]==PackageManager.PERMISSION_GRANTED ){
                startActivity(Intent(this,SelectLogo::class.java))
            }
        }
    }


    private fun loadProfilePicture(){
        UiContext.loadProfileImage(
            this,
            DetailsContext.id,
            object : LoadProfileCallBack {
                override fun onSuccess() {
                    profilePicture.background=resources.getDrawable(R.drawable.image_avatar)
                    profilePicture.setPadding(35,35,35,35)
                }
                override fun onFailure() {

                }
            },
            profilePicture,
            R.drawable.profile
        )
    }

    private fun loadProfileNoCache(){
        UiContext.loadProfileNoCache(
            this,
            DetailsContext.id,
            object : LoadProfileCallBack {
                override fun onSuccess() {
                    profilePicture.background=resources.getDrawable(R.drawable.image_avatar)
                    profilePicture.setPadding(35,35,35,35)
                }
                override fun onFailure() {

                }
            },
            profilePicture,
            R.drawable.profile
        )
    }

    override fun onResume() {
        if(bitmapImage!=null){
            val logo = convertToFile(bitmapImage!!)
            if(logo!=null){
                uploadImage(logo, bitmapImage!!)
                bitmapImage=null
            }else{
                AlertHelper.showError("Error while uploading",this@Profile)
            }
        }
        super.onResume()
    }

    private fun convertToFile(bitmap: Bitmap):File?{
        var file: File? = null
        return try {
            file = File(Environment.getExternalStorageDirectory().toString() + File.separator + "logo.jpg")
            file.createNewFile()
            //Convert bitmap to byte array
            val bos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 0, bos)
            val bitmapdata = bos.toByteArray()
            //write the bytes in file
            val fos = FileOutputStream(file)
            fos.write(bitmapdata)
            fos.flush()
            fos.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            file
        }
    }


    companion object{
        var bitmapImage:Bitmap? = null
    }
}