package com.DevAsh.recbusiness.Registration

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import com.DevAsh.recbusiness.Context.ApiContext
import com.DevAsh.recbusiness.Context.RegistrationContext
import com.DevAsh.recbusiness.Helper.AlertHelper
import com.DevAsh.recbusiness.R
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONArrayRequestListener
import com.google.android.gms.auth.api.credentials.Credential
import com.google.android.gms.auth.api.credentials.Credentials
import com.google.android.gms.auth.api.credentials.HintRequest
import com.jacksonandroidnetworking.JacksonParserFactory
import kotlinx.android.synthetic.main.activity_login.*
import org.json.JSONArray

class Login : AppCompatActivity() {
    var isMoved = false
    lateinit var context:Context
    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        AndroidNetworking.initialize(applicationContext)
        AndroidNetworking.setParserFactory(JacksonParserFactory())

        context = this

        Handler().postDelayed({
            requestHint()
        },500)

        cancel.setOnClickListener{
            onBackPressed()
        }

        sendOtp.setOnClickListener{
              next()
        }

    }

    fun next(){
        if(phoneNumber.text.toString().length==10){
            val view = findViewById<View>(R.id.mainContent)
            hideKeyboardFrom(context,view)
            Handler().postDelayed({
                mainContent.visibility= View.GONE
            },500)
            RegistrationContext.phoneNumber = phoneNumber.text.toString()
            AndroidNetworking.get(ApiContext.apiUrl+ApiContext.registrationPort+"/getOtpMerchant?number=${RegistrationContext.countryCode+RegistrationContext.phoneNumber}")
                .setPriority(Priority.IMMEDIATE)
                .build()
                .getAsJSONArray(object : JSONArrayRequestListener {
                    override fun onResponse(response: JSONArray?) {
                        if(!isMoved){
                            isMoved=true
                            startActivity(Intent(context,Otp::class.java))
                            finish()
                        }
                    }

                    override fun onError(anError: ANError?) {
                        AlertHelper.showServerError(this@Login)

                    }
                })

        }else{

        }
    }

    override fun onBackPressed() {
        val startMain = Intent(Intent.ACTION_MAIN)
        startMain.addCategory(Intent.CATEGORY_HOME)
        startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(startMain)
        finishAndRemoveTask()
    }

    private fun hideKeyboardFrom(context: Context, view: View) {
        val imm: InputMethodManager =
            context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun requestHint() {
        val hintRequest = HintRequest.Builder()
            .setPhoneNumberIdentifierSupported(true)
            .build()
        val credentialsClient = Credentials.getClient(this)
        val intent = credentialsClient.getHintPickerIntent(hintRequest)
        startIntentSenderForResult(
            intent.intentSender,
           1,
            null, 0, 0, 0
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                val credential: Credential = data?.getParcelableExtra(Credential.EXTRA_KEY)!!
                try {
                    val number = credential.id
                    val numberWithoutCountryCode = number.substring(number.length-10,number.length)
                    phoneNumber.setText(numberWithoutCountryCode)
                    Handler().postDelayed({
                        next()

                    },200)
                }catch (e:Throwable){
                    AlertHelper.showError("Error while parse number !",this)
                }


            }
        }
    }
}

