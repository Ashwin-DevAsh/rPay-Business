package com.DevAsh.recbusiness.Home.Recovery

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.DevAsh.recbusiness.Context.ApiContext
import com.DevAsh.recbusiness.Context.DetailsContext
import com.DevAsh.recbusiness.Database.Credentials
import com.DevAsh.recbusiness.Database.ExtraValues
import com.DevAsh.recbusiness.Database.RealmHelper
import com.DevAsh.recbusiness.Helper.AlertHelper
import com.DevAsh.recbusiness.Helper.PasswordHashing
import com.DevAsh.recbusiness.R
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_change_password.*
import org.json.JSONObject

class NewPassword : AppCompatActivity() {
    private var newPasswordText = ""
    var newHashedPassword=""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RealmHelper.init(this)
        setContentView(R.layout.activity_new_password)

        done.setOnClickListener{v ->

            newPasswordText = password.text.toString()
           if(password.text.length<8 || confirmPassword.text.length<8){
                AlertHelper.showError("Password must contain at least 8 characters", this@NewPassword)
            }else if(password.text.toString() != confirmPassword.text.toString()){
                AlertHelper.showError("password not matching", this@NewPassword)
            }else{
                changePassword(v)
            }
        }
    }

    private fun changePassword(view: View){
        hideKeyboardFrom(context = this,view = view)
        Handler().postDelayed({
            mainContent.visibility= View.INVISIBLE
        },500)
        newHashedPassword = PasswordHashing.encryptMsg(newPasswordText)
        AndroidNetworking.post(ApiContext.apiUrl+ ApiContext.registrationPort+"/newPasswordMerchant")
            .addHeaders("token",DetailsContext.token)
            .addBodyParameter(object{
                val id = DetailsContext.id
                val emailID =  DetailsContext.email
                val newPassword = newHashedPassword
            })
            .setPriority(Priority.IMMEDIATE)
            .build()
            .getAsJSONObject(object: JSONObjectRequestListener {
                override fun onResponse(response: JSONObject?) {
                    println(response)
                    mainContent.visibility= View.VISIBLE
                    if(response?.getString("message")=="done"){
                        updateDatabase(newHashedPassword)

                    }else{
                        AlertHelper.showAlertDialog(
                            this@NewPassword,
                            "Failed !", "There is some issue with our server",
                            object : AlertHelper.AlertDialogCallback {
                                override fun onDismiss() {
                                    mainContent.visibility = View.VISIBLE
                                }

                                override fun onDone() {
                                    mainContent.visibility = View.VISIBLE
                                }

                            }
                        )
                    }
                }

                override fun onError(anError: ANError?) {
                    mainContent.visibility= View.VISIBLE
                    AlertHelper.showAlertDialog(
                       this@NewPassword,
                        "Failed !",
                        "There is some issue with our server",
                        object : AlertHelper.AlertDialogCallback {
                            override fun onDismiss() {
                                mainContent.visibility = View.VISIBLE
                            }

                            override fun onDone() {
                                mainContent.visibility = View.VISIBLE
                            }
                        }
                    )
                }
            })
    }

    fun updateDatabase(newPassword:String){
        Realm.getDefaultInstance().executeTransaction{ r->
            val data = r.where(Credentials::class.java).findFirst()
            data?.setPassword(newPassword)
            DetailsContext.password = newPassword
            val extraValues=  r.where(ExtraValues::class.java).findFirst()
            extraValues?.isEnteredPasswordOnce=false
            AlertHelper.showAlertDialog(
                this@NewPassword,
                "Successful !",
                "Your password has been changed successfully",
                object : AlertHelper.AlertDialogCallback {
                    override fun onDismiss() {
                        mainContent.visibility = View.VISIBLE
                        onBackPressed()
                    }

                    override fun onDone() {
                        mainContent.visibility = View.VISIBLE
                        onBackPressed()
                    }
                }
            )
        }
    }


    private fun hideKeyboardFrom(context: Context, view: View) {
        val imm: InputMethodManager =
            context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}