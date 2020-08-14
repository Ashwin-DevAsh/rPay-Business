package com.DevAsh.recbusiness.Models

class BankAccount(val holderName:String, val bankName:String, val IFSC:String, val accountNumber: String){
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BankAccount) return false
        if (holderName != other.holderName) return false
        if (bankName != other.bankName) return false
        if (IFSC != other.IFSC) return false
        if (accountNumber != other.accountNumber) return false
        return true
    }

    override fun hashCode(): Int {
        var result = holderName.hashCode()
        result = 31 * result + bankName.hashCode()
        result = 31 * result + IFSC.hashCode()
        result = 31 * result + accountNumber.hashCode()
        return result
    }
}
