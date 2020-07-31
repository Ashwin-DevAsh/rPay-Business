package com.DevAsh.recbusiness.Home.ViewModels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.DevAsh.recbusiness.Models.Merchant
import com.DevAsh.recbusiness.Models.Transaction

class BalanceViewModel: ViewModel() {

    var currentBalance = MutableLiveData<String>()
    var allTransactions = MutableLiveData<ArrayList<Transaction>>()
    var recentContacts = MutableLiveData<ArrayList<Merchant>>()
}
