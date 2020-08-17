package com.DevAsh.recbusiness.Models

class Message(
    var contacts: Contacts?=null,
    var time: String?=null,
    var message: String,
    var type: String

) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Message) return false

        if (contacts != other.contacts) return false
        if (time != other.time) return false
        if (message != other.message) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = contacts?.hashCode() ?: 0
        result = 31 * result + (time?.hashCode() ?: 0)
        result = 31 * result + message.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }
}