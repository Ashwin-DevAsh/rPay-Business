package com.DevAsh.recbusiness.Models

class Contacts(val name: String, val number: String,val id:String,val email:String,var image: Int?=null){
    override fun equals(other: Any?): Boolean {
        return (other as Contacts).id == this.id
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + id.hashCode()
        return result
    }
}
