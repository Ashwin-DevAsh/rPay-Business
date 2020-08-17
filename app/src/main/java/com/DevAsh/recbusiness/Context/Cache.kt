package com.DevAsh.recbusiness.Context

import com.DevAsh.recbusiness.Home.Transactions.TransactionsAdapter

object Cache {
    val singleObjectTransactionCache = HashMap<String,TransactionsAdapter>()
}