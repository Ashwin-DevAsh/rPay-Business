package com.DevAsh.recbusiness.Context

import com.DevAsh.recbusiness.Home.Transactions.TransactionsAdapter

object Cache {
    val singleObjecttransactionCache = HashMap<String,TransactionsAdapter>()
}