package com.DevAsh.recbusiness.Home

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
import com.DevAsh.recbusiness.Helper.PasswordHashing
import com.DevAsh.recbusiness.Helper.AlertHelper
import com.DevAsh.recbusiness.Helper.AlertHelper.showAlertDialog
import com.DevAsh.recbusiness.R
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_change_password.*
import org.json.JSONObject


class ChangePassword : AppCompatActivity() {
    lateinit var oldPasswordText:String
    lateinit var newPasswordText:String
    var newHashedPassword:String? = null
    var context=this
    lateinit var parentView:View
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RealmHelper.init(context = this)
        setContentView(R.layout.activity_change_password)
        parentView = findViewById(R.id.mainContent)
        done.setOnClickListener{v ->
            oldPasswordText = oldPassword.text.toString()
            newPasswordText = password.text.toString()
            if(oldPassword.text.toString() != PasswordHashing.decryptMsg(DetailsContext.password!!)){
                AlertHelper.showError("Invalid old password", this@ChangePassword)
            }else if(oldPassword.text.toString() == password.text.toString()){
                AlertHelper.showError("old password must not be new password", this@ChangePassword)
            }else if(oldPassword.text.length<8 || password.text.length<8 || confirmPassword.text.length<8){
                AlertHelper.showError("Password must contain at least 8 characters", this@ChangePassword)
            }else if(password.text.toString() != confirmPassword.text.toString()){
                AlertHelper.showError("password not matching", this@ChangePassword)
            }else{
               changePassword(v)
            }
        }

        cancel.setOnClickListener{
            onBackPressed()
        }
    }


    private fun changePassword(view: View){
        hideKeyboardFrom(context = this,view = view)
        Handler().postDelayed({
            mainContent.visibility=View.INVISIBLE
        },500)
        newHashedPassword = PasswordHashing.encryptMsg(newPasswordText)
        AndroidNetworking.post(ApiContext.apiUrl+ ApiContext.profilePort+"/changeMerchantPassword")
            .addHeaders("token",DetailsContext.token)
            .addBodyParameter(object{
                val id = DetailsContext.id
                val oldPassword = PasswordHashing.encryptMsg(oldPasswordText)
                val newPassword = newHashedPassword
            })
            .setPriority(Priority.IMMEDIATE)
            .build()
            .getAsJSONObject(object:JSONObjectRequestListener{
                override fun onResponse(response: JSONObject?) {
                    mainContent.visibility=View.VISIBLE
                    if(response?.getString("message")=="done"){
                          updateDatabase(newHashedPassword!!)
                    }else{
                        showAlertDialog(
                            this@ChangePassword,
                            "Failed !","There is some issue with our server",
                            object: AlertHelper.AlertDialogCallback {
                                override fun onDismiss() {
                                    mainContent.visibility=View.VISIBLE
                                }

                                override fun onDone() {
                                    mainContent.visibility=View.VISIBLE
                                }

                            }
                        )
                    }
                }

                override fun onError(anError: ANError?) {
                    println(anError?.errorCode)
                    mainContent.visibility=View.VISIBLE

                    showAlertDialog(
                        this@ChangePassword,
                        "Failed !",
                        "There is some issue with our server",
                        object: AlertHelper.AlertDialogCallback {
                            override fun onDismiss() {
                                mainContent.visibility=View.VISIBLE
                            }

                            override fun onDone() {
                                mainContent.visibility=View.VISIBLE
                            }
                        }
                    )
                }
            })
    }

    fun updateDatabase(newPassword:String){
        Realm.getDefaultInstance().executeTransaction{r->
            val data = r.where(Credentials::class.java).findFirst()
            data?.setPassword(newPassword)
            DetailsContext.password = newPassword
            val extraValues=  r.where(ExtraValues::class.java).findFirst()
            extraValues?.isEnteredPasswordOnce=false
            showAlertDialog(
                this@ChangePassword,
                "Successful !",
                "Your password has been changed successfully",
                object: AlertHelper.AlertDialogCallback {
                    override fun onDismiss() {
                        mainContent.visibility=View.VISIBLE
                        onBackPressed()
                    }

                    override fun onDone() {
                        mainContent.visibility=View.VISIBLE
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