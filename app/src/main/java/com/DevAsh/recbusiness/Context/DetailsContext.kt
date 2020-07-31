package com.DevAsh.recbusiness.Context

object DetailsContext {
   var name: String? = null
   var phoneNumber: String? = null
   var email: String? = null
   var password: String? = null
   var token: String? = null
   var fcmToken:String=""
   var id :String=""
   var storeName=""
   var status = ""

   fun setData(name: String, phoneNumber: String, email: String, password: String, token: String,status:String,storeName:String) {
      this.name = name
      this.password = password
      this.email = email
      this.phoneNumber = phoneNumber
      this.token = token
      this.id= "rbusiness@$phoneNumber"
      this.storeName=storeName
      this.status=status
   }

}