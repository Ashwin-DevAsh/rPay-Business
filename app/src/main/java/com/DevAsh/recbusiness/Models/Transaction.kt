package com.DevAsh.recbusiness.Models



class Transaction(
    var contacts: Contacts,
    var time: String,
    var amount: String,
    var type: String,
    var transactionId:String,
    var isGenerated:Boolean,
    var timeStamp:Any
)