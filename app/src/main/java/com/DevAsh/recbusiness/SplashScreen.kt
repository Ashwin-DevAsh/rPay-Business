package com.DevAsh.recbusiness

import android.bluetooth.BluetoothClass
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.telephony.TelephonyManager
import android.view.View
import android.view.animation.TranslateAnimation
import androidx.appcompat.app.AppCompatActivity
import com.DevAsh.recbusiness.Context.ApiContext
import com.DevAsh.recbusiness.Context.DetailsContext
import com.DevAsh.recbusiness.Context.StateContext
import com.DevAsh.recbusiness.Context.TransactionContext
import com.DevAsh.recbusiness.Database.Credentials
import com.DevAsh.recbusiness.Database.RealmHelper
import com.DevAsh.recbusiness.Helper.AlertHelper
import com.DevAsh.recbusiness.Helper.TransactionsHelper
import com.DevAsh.recbusiness.Home.HomePage
import com.DevAsh.recbusiness.Home.Profile
import com.DevAsh.recbusiness.Registration.Login
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import com.jacksonandroidnetworking.JacksonParserFactory
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_profile.*
import org.json.JSONObject
import java.text.DecimalFormat
import java.text.ParseException
import java.text.SimpleDateFormat


class SplashScreen : AppCompatActivity() {

    lateinit var context: Context
    lateinit var parentLayout: View

    override fun onNewIntent(intent: Intent) {
        val extras = intent.extras
        if (extras != null) {
            if (extras.containsKey("openTransactionPage")) {
                TransactionContext.openTransactionPage=true
            }
        }
        super.onNewIntent(intent)

    }

    override fun onCreate(savedInstanceState: Bundle?) {

        AndroidNetworking.initialize(applicationContext)
        AndroidNetworking.setParserFactory(JacksonParserFactory())
        super.onCreate(savedInstanceState)

        RealmHelper.init(this)

//        val appSignatureHelper = GetHash(this)
//        println(appSignatureHelper.appSignatures[0]+"App Sign")



        setContentView(R.layout.activity_splash_screen)
        context = this
        parentLayout = findViewById(android.R.id.content)


        val credentials:Credentials? =  Realm.getDefaultInstance().where(Credentials::class.java).findFirst()


        if(credentials!=null && credentials.isLogin==true){
            StateContext.initRecentContact(arrayListOf())
            Handler().postDelayed({
                  try {
                      DetailsContext.setData(
                          credentials.name,
                          credentials.phoneNumber,
                          credentials.email,
                          credentials.password,
                          credentials.token,
                          credentials.status,
                          credentials.storeName
                      )
                  }catch (e:Throwable){
                      Handler().postDelayed({
                          startActivity(Intent(context,Login::class.java))
                          finish()
                      },2000)
                      return@postDelayed
                  }

                Handler().postDelayed({
                    getStatus()
                },0)
                AndroidNetworking.get(ApiContext.apiUrl + ApiContext.paymentPort + "/getMyState?id=${DetailsContext.id}")
                        .addHeaders("jwtToken",DetailsContext.token)
                        .setPriority(Priority.IMMEDIATE)
                        .build()
                        .getAsJSONObject(object: JSONObjectRequestListener {
                            override fun onResponse(response: JSONObject?) {
                                println(DetailsContext.token)
                                val balance = response?.getInt("Balance")
                                StateContext.currentBalance = balance!!
                                val formatter = DecimalFormat("##,##,##,##,##,##,##,###")
                                StateContext.setBalanceToModel(formatter.format(balance))
                                startActivity(Intent(context, HomePage::class.java))
                                Handler().postDelayed({
                                    val transactionObjectArray = response.getJSONArray("Transactions")
                                    TransactionsHelper.addTransaction(transactionObjectArray)
                                },0)
                                finish()
                            }
                            override fun onError(anError: ANError?) {
                                  AlertHelper.showServerError(this@SplashScreen)
                            }
                        })
            },0)
        }else{
            Handler().postDelayed({
                startActivity(Intent(context,Login::class.java))
                finish()
            },2000)
        }


    }

    companion object{
        fun dateToString(timeStamp:String):String{
            val time = timeStamp.subSequence(0,timeStamp.length-3).split(" ")[1]
            try {
                val format = SimpleDateFormat("MM-dd-yyyy")
                val df2 = SimpleDateFormat("MMM dd")
                return df2.format(format.parse(timeStamp))+" "+time
            } catch (e: ParseException) {
                e.printStackTrace()
            }

            return timeStamp
        }
    }

    fun getStatus(){
        AndroidNetworking.get(ApiContext.apiUrl + ApiContext.registrationPort + "/getMerchant?id=${DetailsContext.id}")
            .addHeaders("jwtToken",DetailsContext.token)
            .setPriority(Priority.IMMEDIATE)
            .build()
            .getAsJSONObject(object: JSONObjectRequestListener {
                override fun onResponse(response: JSONObject?) {
                    print(response)
                    try {

                        DetailsContext.isVerified = response?.getString("status")=="active"
                    }catch (e:Throwable){

                    }
                }
                override fun onError(anError: ANError?) {
                    println("err")
                    println(anError?.localizedMessage)
                }
            })
    }

}
