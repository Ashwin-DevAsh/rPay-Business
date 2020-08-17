package com.DevAsh.recbusiness.Models



class Transaction(
    var contacts: Contacts,
    var time: String,
    var amount: String,
    var type: String,
    var transactionId:String,
    var isGenerated:Boolean,
    var timeStamp:Any,
    var isWithdraw:Boolean
){
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Transaction) return false

        if (contacts != other.contacts) return false
        if (time != other.time) return false
        if (amount != other.amount) return false
        if (type != other.type) return false
        if (transactionId != other.transactionId) return false
        if (isGenerated != other.isGenerated) return false
        if (isWithdraw != other.isWithdraw) return false

        return true
    }
    override fun hashCode(): Int {
        var result = contacts.hashCode()
        result = 31 * result + time.hashCode()
        result = 31 * result + amount.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + transactionId.hashCode()
        result = 31 * result + isGenerated.hashCode()
        result = 31 * result + isWithdraw.hashCode()
        return result
    }
}