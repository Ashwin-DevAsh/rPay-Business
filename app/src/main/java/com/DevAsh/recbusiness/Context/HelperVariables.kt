package com.DevAsh.recbusiness.Context


import com.DevAsh.recbusiness.Models.BankAccount
import com.DevAsh.recbusiness.Models.Contacts
import com.DevAsh.recbusiness.Models.Transaction

object HelperVariables {
    var allUsers = ArrayList<Contacts>()
    var selectedUser:Contacts?=null
    var amount:String?=null
    var needToPay = false

    var avatarColor = "#035aa6"
    var currency = "RC"

    var selectedTransaction:Transaction?=null

    var openTransactionPage = false

    var selectedAccount: BankAccount?=null

    var withdrawAmount = ""

}