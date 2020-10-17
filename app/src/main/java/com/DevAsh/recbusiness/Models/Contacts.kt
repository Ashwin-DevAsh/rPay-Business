package com.DevAsh.recbusiness.Models

import android.content.Intent

class Contacts(val name: String, val number: String,val id:String,val email:String,var image: Int?=null){
    override fun equals(other: Any?): Boolean {
        return (other as Contacts).id == this.id
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + id.hashCode()
        return result
    }


    fun putIntent(intent: Intent){
        intent.putExtra("name",name)
        intent.putExtra("number",number)
        intent.putExtra("id",id)
        intent.putExtra("email",email)
    }

    override fun toString(): String {
        return "Contacts(name='$name', number='$number', id='$id', email='$email', image=$image)"
    }

    companion object {
        fun fromIntent(intent: Intent):Contacts{
            return Contacts(
                intent.getStringExtra("name")!!,
                intent.getStringExtra("number")!!,
                intent.getStringExtra("id")!!,
                intent.getStringExtra("email")!!
            )

        }
    }
}
