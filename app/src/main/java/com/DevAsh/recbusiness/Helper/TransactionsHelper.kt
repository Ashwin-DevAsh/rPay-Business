package com.DevAsh.recbusiness.Helper

import com.DevAsh.recbusiness.Context.DetailsContext
import com.DevAsh.recbusiness.Context.StateContext
import com.DevAsh.recbusiness.Models.Contacts
import com.DevAsh.recbusiness.Models.Merchant
import com.DevAsh.recbusiness.Models.Transaction
import com.DevAsh.recbusiness.SplashScreen
import org.json.JSONArray

object TransactionsHelper {
    fun addTransaction(transactionObjectArray:JSONArray){
        val transactions = ArrayList<Transaction>()
        for (i in 0 until transactionObjectArray.length()) {

            val from = transactionObjectArray.getJSONObject(i).getJSONObject("From")
            val to = transactionObjectArray.getJSONObject(i).getJSONObject("To")
            val isSend = isSend(DetailsContext.id,from.getString("Id"))

            val name = if (isSend) to.getString("Name") else from.getString("Name")
            val number = if (isSend) to.getString("Number") else from.getString("Number")
            val email = if (isSend) to.getString("Email") else from.getString("Email")
            val id = if (isSend) to.getString("Id") else from.getString("Id")

            val contacts = Contacts(name, number,id,email)
            val transaction = Transaction(
                contacts=contacts,
                amount = transactionObjectArray.getJSONObject(i)["Amount"].toString(),
                time = SplashScreen.dateToString(
                    transactionObjectArray.getJSONObject(
                        i
                    )["TransactionTime"].toString()
                ),
                type = if (isSend)
                    "Send"
                else "Received",
                transactionId =  transactionObjectArray.getJSONObject(i)["TransactionID"].toString(),
                isGenerated = transactionObjectArray.getJSONObject(i).getBoolean("IsGenerated"),
                timeStamp = transactionObjectArray.getJSONObject(i).get("TimeStamp"),
                isWithdraw = transactionObjectArray.getJSONObject(i).getBoolean("IsWithdraw")
            )

            transactions.add(0, transaction)


        }
        StateContext.initAllTransaction(transactions)
    }

    fun isSend(myId:String,fromId:String):Boolean = myId == fromId

}