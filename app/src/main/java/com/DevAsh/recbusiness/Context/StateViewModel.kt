package com.DevAsh.recbusiness.Context

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.DevAsh.recbusiness.Models.BankAccount
import com.DevAsh.recbusiness.Models.Contacts
import com.DevAsh.recbusiness.Models.Merchant
import com.DevAsh.recbusiness.Models.Transaction

class StateViewModel: ViewModel() {

    var currentBalance = MutableLiveData<String>()
    var allTransactions = MutableLiveData<ArrayList<Transaction>>()
    var recentContacts = MutableLiveData<ArrayList<Contacts>>()
    var bankAccounts = MutableLiveData<ArrayList<BankAccount>>()

}
