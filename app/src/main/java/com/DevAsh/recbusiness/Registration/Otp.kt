package com.DevAsh.recbusiness.Registration

import android.app.Activity
import android.content.*
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import com.DevAsh.recbusiness.Context.ApiContext
import com.DevAsh.recbusiness.Context.DetailsContext
import com.DevAsh.recbusiness.Context.RegistrationContext
import com.DevAsh.recbusiness.Context.StateContext
import com.DevAsh.recbusiness.Database.BankAccount
import com.DevAsh.recbusiness.Database.Credentials
import com.DevAsh.recbusiness.Helper.AlertHelper
import com.DevAsh.recbusiness.Helper.TransactionsHelper
import com.DevAsh.recbusiness.Home.HomePage
import com.DevAsh.recbusiness.Models.Merchant
import com.DevAsh.recbusiness.Models.Transaction
import com.DevAsh.recbusiness.R
import com.DevAsh.recbusiness.SplashScreen
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONArrayRequestListener
import com.androidnetworking.interfaces.JSONObjectRequestListener
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.jacksonandroidnetworking.JacksonParserFactory
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_otp.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.DecimalFormat


class Otp : AppCompatActivity() {

    lateinit var context: Context

    private val smsBroadcastReceiver by lazy { OtpReceiver() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp)

        AndroidNetworking.initialize(applicationContext)
        AndroidNetworking.setParserFactory(JacksonParserFactory())

        context = this

        topNumber.text = "Verify +${RegistrationContext.countryCode} ${RegistrationContext.phoneNumber}"
        info.text = "Waiting to automatically detect an SMS sent to +${RegistrationContext.countryCode} ${RegistrationContext.phoneNumber}"



        val client = SmsRetriever.getClient(this)
        val retriever = client.startSmsRetriever()
        retriever.addOnSuccessListener {
            val listener = object : OtpReceiver.Listener {
                override fun onSMSReceived(otpCode: String) {
                   println(otpCode)
                    try{
                        Integer.parseInt(otpCode)
                        otp.setText(otpCode)
                        Handler().postDelayed({
                            verify()
                        },200)
                    }catch (e:Throwable){
                        AlertHelper.showError("Unable to detect OTP",this@Otp)
                    }
                }
                override fun onTimeOut() {

                }
            }
            smsBroadcastReceiver.injectListener(listener)
            registerReceiver(smsBroadcastReceiver,IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION))
        }


        cancel.setOnClickListener{
            startActivity(Intent(context, Login::class.java))
            finish()
        }

        verify.setOnClickListener {
            verify()
        }
    }

    override fun onDestroy() {
        unregisterReceiver(smsBroadcastReceiver)
        super.onDestroy()
    }

    private fun verify(){
        val view = findViewById<View>(R.id.mainContent)
        StateContext.initRecentContact(arrayListOf())
        errorMessage.visibility= INVISIBLE
        if (otp.text.toString().length == 4) {
            hideKeyboardFrom(context,view)
            Handler().postDelayed({
                mainContent.visibility = INVISIBLE

            },300)
            AndroidNetworking.post(ApiContext.apiUrl + ApiContext.registrationPort + "/setOtpMerchant")
                .addBodyParameter("otpNumber", otp.text.toString())
                .addBodyParameter(
                    "number",
                    RegistrationContext.countryCode + RegistrationContext.phoneNumber
                )
                .setPriority(Priority.IMMEDIATE)
                .build()
                .getAsJSONArray(object : JSONArrayRequestListener {
                    override fun onResponse(response: JSONArray?) {
                        val otpObject = response?.getJSONObject(0)
                        if (otpObject != null && otpObject["message"] == "verified") {
                            try {


                                val user: JSONObject = otpObject["user"] as JSONObject
                                Realm.getDefaultInstance().executeTransaction { realm ->
                                    realm.delete(Credentials::class.java)
                                    val credentials = Credentials(
                                        user["name"].toString(),
                                        user["number"].toString(),
                                        user["email"].toString(),
                                        user["password"].toString(),
                                        otpObject["token"].toString(),
                                        true,
                                        user["status"].toString(),
                                        user["storename"].toString()
                                    )
                                    realm.insert(credentials)
                                    DetailsContext.setData(
                                        credentials!!.name,
                                        credentials.phoneNumber,
                                        credentials.email,
                                        credentials.password,
                                        credentials.token,
                                        credentials.status,
                                        credentials.storeName
                                    )

                                    Handler().postDelayed({
                                        try {
                                            addBankAccounts( user.getJSONArray("accountinfo"))
                                        }catch (e:Throwable){

                                        }
                                    },0)

                                    Handler().postDelayed({
                                        SplashScreen.getStatus()
                                    },0)
                                    Handler().postDelayed({
                                        AndroidNetworking.get(ApiContext.apiUrl + ApiContext.paymentPort + "/getMyState?id=${DetailsContext.id}")
                                            .addHeaders("jwtToken",DetailsContext.token)
                                            .setPriority(Priority.IMMEDIATE)
                                            .build()
                                            .getAsJSONObject(object: JSONObjectRequestListener {
                                                override fun onResponse(response: JSONObject?) {
                                                    val balance = response?.getInt("Balance")
                                                    StateContext.currentBalance = balance!!
                                                    val formatter = DecimalFormat("##,##,##,##,##,##,##,###")
                                                    StateContext.setBalanceToModel(formatter.format(balance))
                                                    val transactionObjectArray = response.getJSONArray("Transactions")
                                                    TransactionsHelper.addTransaction(transactionObjectArray)
                                                    startActivity(Intent(context, HomePage::class.java))
                                                    finish()
                                                }

                                                override fun onError(anError: ANError?) {
                                                    AlertHelper.showServerError(this@Otp)
                                                }

                                            })
                                    },0)
                                }

                            } catch (e: Exception) {
                                println(e)
                                startActivity(Intent(context, Register::class.java))
                                finish()
                            }

                        } else {
                            mainContent.visibility = VISIBLE
                            AlertHelper.showError("Invalid Otp",this@Otp)
                        }
                    }

                    override fun onError(anError: ANError?) {
                        AlertHelper.showServerError(this@Otp)
                        errorMessage.visibility=VISIBLE
                    }
                })
        } else {
            AlertHelper.showError("Invalid Otp",this@Otp)
            mainContent.visibility = VISIBLE
        }
    }

    fun addBankAccounts(bankAccounts:JSONArray){
        val bankAccountsDatabase = ArrayList<BankAccount>()
        for(i in 0 until bankAccounts.length()){
            val account = bankAccounts.getJSONObject(i)
            val bankAccount =  com.DevAsh.recbusiness.Models.BankAccount(
                holderName = account.getString("holderName"),
                bankName = account.getString("bankName"),
                accountNumber = account.getString("accountNumber"),
                IFSC = account.getString("ifsc")
            )
            val bankAccountDatabase =  BankAccount(
                account.getString("holderName"),
                account.getString("bankName"),
                account.getString("ifsc"),
                account.getString("accountNumber")
            )
            bankAccountsDatabase.add(bankAccountDatabase)
            StateContext.addBankAccounts(
                bankAccount
            )
        }
        Realm.getDefaultInstance().executeTransactionAsync{
            it.insert(bankAccountsDatabase)
        }
    }


    override fun onBackPressed() {
        val startMain = Intent(Intent.ACTION_MAIN)
        startMain.addCategory(Intent.CATEGORY_HOME)
        startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(startMain)
    }

    private fun hideKeyboardFrom(context: Context, view: View) {
        val imm: InputMethodManager =
            context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions!!, grantResults!!)
    }

}
