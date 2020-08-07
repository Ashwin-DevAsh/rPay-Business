package com.DevAsh.recbusiness.Home.Recovery

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.DevAsh.recbusiness.Context.ApiContext
import com.DevAsh.recbusiness.Context.DetailsContext
import com.DevAsh.recbusiness.Helper.AlertHelper
import com.DevAsh.recbusiness.R
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONArrayRequestListener
import kotlinx.android.synthetic.main.activity_recovery_options.*
import org.json.JSONArray

class RecoveryOptions : AppCompatActivity() {
    var context = this
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recovery_options)

        mail.setOnClickListener{
            mainContent.visibility= View.INVISIBLE
            AndroidNetworking.get(ApiContext.apiUrl+ ApiContext.registrationPort+"/getRecoveryOtpMerchant/?emailID=${DetailsContext.email}")
                .addHeaders("token",DetailsContext.token)
                .setPriority(Priority.IMMEDIATE)
                .build()
                .getAsJSONArray(object : JSONArrayRequestListener {
                    override fun onResponse(response: JSONArray?) {
                        println(response)
                        if(response!!.getJSONObject(0)["message"]=="done"){
                            startActivity(Intent(context, RecoveryOtp::class.java))
                            finish()
                        }else{
                            mainContent.visibility= View.VISIBLE
                            AlertHelper.showServerError(this@RecoveryOptions)
                        }

                    }
                    override fun onError(anError: ANError?) {
                        mainContent.visibility= View.VISIBLE
                        println(anError)
                        AlertHelper.showServerError(this@RecoveryOptions)

                    }
                })
        }
    }
}