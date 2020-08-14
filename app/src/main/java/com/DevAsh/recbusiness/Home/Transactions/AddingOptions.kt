package com.DevAsh.recbusiness.Home.Transactions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import com.DevAsh.recbusiness.Context.ApiContext
import com.DevAsh.recbusiness.Context.DetailsContext
import com.DevAsh.recbusiness.Context.StateContext
import com.DevAsh.recbusiness.Context.HelperVariables
import com.DevAsh.recbusiness.Helper.AlertHelper
import com.DevAsh.recbusiness.Helper.TransactionsHelper
import com.DevAsh.recbusiness.R
import com.DevAsh.recbusiness.Sync.SocketHelper
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import kotlinx.android.synthetic.main.activity_adding_options.*
import org.json.JSONObject
import java.text.DecimalFormat
import java.util.*
import kotlin.collections.ArrayList

class AddingOptions : AppCompatActivity(), PaymentResultListener {
    lateinit var amount:String
    var context = this
    var addingOption:String? = "Upi transaction"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_adding_options)

        amount = intent.getStringExtra("amount")!!

        upi.setOnClickListener{
            addingOption = "Upi transaction"
            loadingScreen.visibility = View.VISIBLE
            payUsingUpi(amount,"9840176511@ybl","Barath","R-pay")
        }

        razorpay.setOnClickListener{v->
            addingOption = "Gateway transaction"
            loadingScreen.visibility = View.VISIBLE
            Handler().postDelayed({
                startPayment()
            },1000)
        }
    }

    override fun onPaymentError(p0: Int, p1: String?) {
        loadingScreen.visibility = View.GONE
    }

    override fun onPaymentSuccess(p0: String?) {
        val amount = this.amount
        Handler().postDelayed({
            loadingScreen.visibility = View.VISIBLE
        },500)
        AndroidNetworking.post(ApiContext.apiUrl + ApiContext.paymentPort + "/addMoney")
            .setContentType("application/json; charset=utf-8")
            .addHeaders("jwtToken", DetailsContext.token)
            .addApplicationJsonBody(object{
                var amount = amount
                var to = object {
                    var id = DetailsContext.id
                    var name = DetailsContext.name
                    var number = DetailsContext.phoneNumber
                    var email = DetailsContext.email
                }
                var from = object {
                    var id = p0
                    var name = addingOption
                    var number = "None"
                    var email = "None"
                }
            })
            .setPriority(Priority.IMMEDIATE)
            .build()
            .getAsJSONObject(object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject?) {
                    loadingScreen.visibility = View.VISIBLE
                    if(response?.get("message")=="done"){
                        AndroidNetworking.get(ApiContext.apiUrl + ApiContext.paymentPort + "/getMyState?id=${DetailsContext.id}")
                            .addHeaders("jwtToken", DetailsContext.token)
                            .setPriority(Priority.IMMEDIATE)
                            .build()
                            .getAsJSONObject(object: JSONObjectRequestListener {
                                override fun onResponse(response: JSONObject?) {
                                    val jsonData = JSONObject()
                                    jsonData.put("to",
                                        HelperVariables.selectedUser?.number.toString().replace("+",""))
                                    SocketHelper.socket?.emit("notifyPayment",jsonData)
                                    val balance = response?.getInt("Balance")
                                    val formatter = DecimalFormat("##,##,##,##,##,##,##,###")
                                    StateContext.setBalanceToModel(formatter.format(balance))
                                    StateContext.currentBalance= balance!!
                                    val transactionObjectArray = response.getJSONArray("Transactions")
                                    TransactionsHelper.addTransaction(transactionObjectArray)
                                    val intent = Intent(context,Successful::class.java)
                                    intent.putExtra("type","addMoney")
                                    intent.putExtra("amount",amount)
                                    startActivity(intent)
                                    context.finish()
                                }
                                override fun onError(anError: ANError?) {
                                    AlertHelper.showAlertDialog(this@AddingOptions,
                                        "Failed !",
                                        "your transaction of $amount ${HelperVariables.currency} is failed. if any amount debited it will refund soon"
                                    )
                                }
                            })
                    }else{
                        AlertHelper.showAlertDialog(this@AddingOptions,
                            "Failed !",
                            "your transaction of ${HelperVariables.amount} ${HelperVariables.currency} is failed. if any amount debited it will refund soon"
                        )
                    }
                }
                override fun onError(anError: ANError?) {

                }
            })
    }


    override fun onResume() {
        loadingScreen.visibility = View.GONE
        super.onResume()
    }

    private fun payUsingUpi(
        amount: String?,
        upiId: String?,
        name: String?,
        note: String?
    ) {
        val uri: Uri = Uri.parse("upi://pay").buildUpon()
            .appendQueryParameter("pa", upiId)
            .appendQueryParameter("pn", name)
            .appendQueryParameter("tn", note)
            .appendQueryParameter("am", amount)
            .appendQueryParameter("cu", "INR")
            .build()
        val upiPayIntent = Intent(Intent.ACTION_VIEW)
        upiPayIntent.data = uri
        val chooser = Intent.createChooser(upiPayIntent, "Pay with")
        if (null != chooser.resolveActivity(packageManager)) {
            startActivityForResult(chooser, 1)
        } else {
          AlertHelper.showError("No UPI app found, please install one to continue",this@AddingOptions)
        }
    }

    private fun startPayment() {
        val activity: Activity = this
        val co = Checkout()
        co.setKeyID("rzp_test_txenFuJupWfNO6")
        try {
            val options = JSONObject()
            options.put("name","Adding Money")
            options.put("description","This process require a 2% commission")
            options.put("currency","INR")
            options.put("amount",((Integer.parseInt(amount)*100)+((Integer.parseInt(amount)*100)*0.02
                    )).toString())
            val prefill = JSONObject()
            prefill.put("email",DetailsContext.email)
            prefill.put("contact",DetailsContext.phoneNumber)
            options.put("prefill",prefill)
            co.open(activity,options)
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            1 -> if (Activity.RESULT_OK == resultCode || resultCode == 11) {
                if (data != null) {
                    val trxt = data.getStringExtra("response")
                    Log.d("UPI", "onActivityResult: $trxt")
                    val dataList: ArrayList<String> = ArrayList()
                    dataList.add(trxt)
                    upiPaymentDataOperation(dataList)
                } else {
                    Log.d("UPI", "onActivityResult: " + "Return data is null")
                    val dataList: ArrayList<String> = ArrayList()
                    dataList.add("nothing")
                    upiPaymentDataOperation(dataList)
                }
            }
        }
    }

    private fun upiPaymentDataOperation(data: ArrayList<String>) {
        if (isConnectionAvailable(this)) {
            var str = data[0]
            var paymentCancel = ""
            if (str == null) str = "discard"
            var status = ""
            var approvalRefNo = ""
            val response = str.split("&".toRegex()).toTypedArray()
            for (i in response.indices) {
                val equalStr =
                    response[i].split("=".toRegex()).toTypedArray()
                if (equalStr.size >= 2) {
                    if (equalStr[0].toLowerCase(Locale.ROOT) == "Status".toLowerCase(Locale.ROOT)) {
                        status = equalStr[1].toLowerCase(Locale.ROOT)
                    } else if (equalStr[0]
                            .toLowerCase(Locale.ROOT) == "ApprovalRefNo".toLowerCase(Locale.ROOT) || equalStr[0]
                            .toLowerCase(Locale.ROOT) == "txnRef".toLowerCase(Locale.ROOT)
                    ) {
                        approvalRefNo = equalStr[1]
                    }
                } else {
                    paymentCancel = "Payment cancelled by user."
                }
            }
            when {
                status == "success" -> {
                    AlertHelper.showError( "Transaction successful.  $approvalRefNo",this)
                    onPaymentSuccess(approvalRefNo)
                }
                "Payment cancelled by user." == paymentCancel -> {
                    loadingScreen.visibility = View.GONE
                    AlertHelper.showError( "Payment cancelled by user.", this)
                }
                else -> {
                    loadingScreen.visibility = View.GONE
                    AlertHelper.showError(
                        "Transaction failed.Please try again",
                        this
                        )
                }
            }
        } else {
            AlertHelper.showError(
                "Internet connection is not available. Please check and try again",
                this
            )
        }
    }

    private fun isConnectionAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (connectivityManager != null) {
            val netInfo = connectivityManager.activeNetworkInfo
            if (netInfo != null && netInfo.isConnected
                && netInfo.isConnectedOrConnecting
                && netInfo.isAvailable
            ) {
                return true
            }
        }
        return false
    }
}