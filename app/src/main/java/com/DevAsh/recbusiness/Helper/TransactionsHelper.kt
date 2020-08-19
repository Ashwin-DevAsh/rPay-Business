package com.DevAsh.recbusiness.Helper

import com.DevAsh.recbusiness.Context.DetailsContext
import com.DevAsh.recbusiness.Context.StateContext
import com.DevAsh.recbusiness.Models.Contacts
import com.DevAsh.recbusiness.Models.Merchant
import com.DevAsh.recbusiness.Models.Transaction
import com.DevAsh.recbusiness.SplashScreen
import org.json.JSONArray

object TransactionsHelper {
    var paymentObserver:PaymentObserver?=null
    fun addTransaction(transactionObjectArray:JSONArray){
        val transactions = ArrayList<Transaction>()
        for (i in 0 until transactionObjectArray.length()) {

            val from = transactionObjectArray.getJSONObject(i).getJSONObject("frommetadata")
            val to = transactionObjectArray.getJSONObject(i).getJSONObject("tometadata")
            val isSend = isSend(DetailsContext.id,from.getString("Id"))

            val name = if (isSend) to.getString("Name") else from.getString("Name")
            val number = if (isSend) to.getString("Number") else from.getString("Number")
            val email = if (isSend) to.getString("Email") else from.getString("Email")
            val id = if (isSend) to.getString("Id") else from.getString("Id")

            val contacts = Contacts(name, number,id,email)
            val transaction = Transaction(
                contacts=contacts,
                amount = transactionObjectArray.getJSONObject(i)["amount"].toString(),
                time = SplashScreen.dateToString(
                    transactionObjectArray.getJSONObject(
                        i
                    )["transactiontime"].toString()
                ),
                type = if (isSend)
                    "Send"
                else "Received",
                transactionId =  transactionObjectArray.getJSONObject(i)["transactionid"].toString(),
                isGenerated = transactionObjectArray.getJSONObject(i).getBoolean("isgenerated"),
                timeStamp = transactionObjectArray.getJSONObject(i).get("timestamp"),
                isWithdraw = transactionObjectArray.getJSONObject(i).getBoolean("iswithdraw")
            )

            transactions.add(0, transaction)


        }
        StateContext.initAllTransaction(transactions)
    }

    fun isSend(myId:String,fromId:String):Boolean = myId == fromId

}

interface PaymentObserver{
    fun update(transaction: Transaction)
}
