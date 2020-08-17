package com.DevAsh.recbusiness.Context

import android.app.Activity
import com.DevAsh.recbusiness.Home.Transactions.TransactionsAdapter

object Cache {
    val singleObjectTransactionCache = HashMap<String,TransactionsAdapter>()
    val socketListnerCache = HashMap<Activity,String>()
}