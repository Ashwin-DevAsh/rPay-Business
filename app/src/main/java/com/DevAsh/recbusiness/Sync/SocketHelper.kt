package com.DevAsh.recbusiness.Sync

import com.DevAsh.recbusiness.Context.ApiContext
import com.DevAsh.recbusiness.Context.DetailsContext
import com.DevAsh.recbusiness.Context.StateContext
import com.DevAsh.recbusiness.Models.Transaction
import com.DevAsh.recbusiness.SplashScreen
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import com.github.nkzawa.socketio.client.IO
import com.github.nkzawa.socketio.client.Socket
import org.json.JSONObject
import java.text.DecimalFormat

object SocketHelper {

    private val url = ApiContext.apiUrl+ ApiContext.syncPort
    var socket:Socket? = null
    var newUser:Boolean = false

    fun connect(){
        socket = IO.socket(url)
        socket?.connect()

        socket?.on("connect") {
            println("connecting ....")
            val data = JSONObject(
               mapOf(
                   "id"  to DetailsContext.id,
                   "fcmToken" to DetailsContext.fcmToken
               )
            )
            socket?.emit("getInformation",data)

        }

        socket?.on("doUpdate"){

        }


        socket?.on("disconnect"){
            println("disconnecting...")
        }

        socket?.on("receivedPayment"){
            getMyState()
        }

    }

   fun getMyState(){
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
                    StateContext.currentBalance= balance!!
                    val transactionObjectArray = response?.getJSONArray("Transactions")
                    val transactions = ArrayList<Transaction>()
                    for (i in 0 until transactionObjectArray!!.length()) {
                        transactions.add(
                            0, Transaction(
                                name = if (transactionObjectArray.getJSONObject(i)["From"] == DetailsContext.id)
                                    transactionObjectArray.getJSONObject(i)["ToName"].toString()
                                else transactionObjectArray.getJSONObject(i)["FromName"].toString(),
                                id = if (transactionObjectArray.getJSONObject(i)["From"] == DetailsContext.id)
                                    transactionObjectArray.getJSONObject(i)["To"].toString()
                                else transactionObjectArray.getJSONObject(i)["From"].toString(),
                                amount = transactionObjectArray.getJSONObject(i)["Amount"].toString(),
                                time = SplashScreen.dateToString(
                                    transactionObjectArray.getJSONObject(
                                        i
                                    )["TransactionTime"].toString()
                                ),
                                type = if (transactionObjectArray.getJSONObject(i)["From"] == DetailsContext.id)
                                    "Send"
                                else "Received",
                                transactionId =  transactionObjectArray.getJSONObject(i)["TransactionID"].toString(),
                                isGenerated = transactionObjectArray.getJSONObject(i).getBoolean("IsGenerated")
                            )
                        )
                    }
                    StateContext.initAllTransaction(transactions)
                }
                override fun onError(anError: ANError?) {
                    println(anError)
                }

            })

    }

}