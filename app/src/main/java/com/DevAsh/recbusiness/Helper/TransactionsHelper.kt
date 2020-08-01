package com.DevAsh.recbusiness.Helper

import com.DevAsh.recbusiness.Context.DetailsContext
import com.DevAsh.recbusiness.Context.StateContext
import com.DevAsh.recbusiness.Models.Merchant
import com.DevAsh.recbusiness.Models.Transaction
import com.DevAsh.recbusiness.SplashScreen
import org.json.JSONArray

object TransactionsHelper {
    fun addTransaction(transactionObjectArray:JSONArray){
        val transactions = ArrayList<Transaction>()
        for (i in 0 until transactionObjectArray!!.length()) {
            val name = if (transactionObjectArray.getJSONObject(i)["From"] == DetailsContext.id)
                transactionObjectArray.getJSONObject(i)["ToName"].toString()
            else transactionObjectArray.getJSONObject(i)["FromName"].toString()
            val number = if (transactionObjectArray.getJSONObject(i)["From"] == DetailsContext.id)
                transactionObjectArray.getJSONObject(i)["To"].toString()
            else transactionObjectArray.getJSONObject(i)["From"].toString()

            val merchant = Merchant(name, "+${number.split("@")[number.split("@").size-1]}","$number")
            if(!transactionObjectArray.getJSONObject(i).getBoolean("IsGenerated"))
                StateContext.addRecentContact(merchant)
            transactions.add(
                0, Transaction(
                    name = name,
                    id = number,
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
}