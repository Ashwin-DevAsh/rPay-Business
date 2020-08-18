package com.DevAsh.recbusiness.Sync

import android.content.Context
import android.content.Intent
import com.DevAsh.recbusiness.Context.ApiContext
import com.DevAsh.recbusiness.Context.DetailsContext
import com.DevAsh.recbusiness.Context.StateContext
import com.DevAsh.recbusiness.Helper.TransactionsHelper
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
    var context: Context?=null
    var socketIntent : Intent?=null

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
            context?.stopService(socketIntent)
            println("disconnecting...")
        }

        socket?.on("receivedPayment"){
            getMyState()
        }



    }

    fun updateProfilePicture(){
        socket?.emit("updateProfilePicture",JSONObject( mapOf(
            "id" to DetailsContext.id
        )))
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
                    TransactionsHelper.addTransaction(transactionObjectArray)
                }
                override fun onError(anError: ANError?) {
                    println(anError)
                }

            })

    }

}