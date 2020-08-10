package com.DevAsh.recbusiness.Context

import com.DevAsh.recbusiness.Home.ViewModels.BalanceViewModel
import com.DevAsh.recbusiness.Models.Contacts
import com.DevAsh.recbusiness.Models.Merchant
import com.DevAsh.recbusiness.Models.Transaction

object StateContext {

    val model: BalanceViewModel = BalanceViewModel()
    var currentBalance = 0
    var timeIndex=0

    init {
        model.allTransactions.value = ArrayList()
    }

    fun setBalanceToModel(amount: String){
         model.currentBalance.value = "$amount ${TransactionContext.currency}"
    }

    fun initAllTransaction(initList:ArrayList<Transaction>){
        model.allTransactions.value = ArrayList(initList)
    }

    fun initRecentContact(arrayList:ArrayList<Contacts>){
        model.recentContacts.value=arrayList
    }

    fun addRecentContact(contact: Contacts){
        val temp:ArrayList<Contacts> = if(model.recentContacts.value!=null) model.recentContacts.value!! else ArrayList()
        if(!temp.contains(contact)){
            temp.add(0,contact)
            model.recentContacts.value=temp
        }else{
            temp.remove(contact)
            temp.add(0,contact)
            model.recentContacts.value=temp
        }
    }

    fun addTransaction(transaction: Transaction){
        val temp = model.allTransactions.value
        temp?.add(0,transaction)
        model.allTransactions.value=temp
    }

    fun getBalance():String{
        return model.currentBalance.value!!
    }

//    fun addFakeTransactions(){
//        val transactions = ArrayList<Transaction>()
//        setBalanceToModel("34,60,000")
//        transactions.addAll(
//            arrayOf(
//                Transaction("+6663529XXXXX", "993088909", "Jul 9 , 3:45 AM", "16,000", "Send","",false),
//                Transaction("+6399419XXXXX", "993088909", "Jul 7 , 5:00 AM", "17,200", "Send","",false),
//                Transaction("+6399503XXXXX", "993088909", "Jul 5 , 3:20 AM", "15,000", "Send","",false),
//                Transaction("+6399503XXXXX", "993088909", "Jul 3 , 4:00 AM", "15,500", "Send","",false),
//                Transaction("+6392570XXXXX", "993088909", "Jun 20 , 10:30 PM", "17,000", "Send","",false),
//                Transaction("USA Project", "993088909", "Jun 4 , 12:15 AM", "20,00,000", "Received","",false),
//                Transaction("+6397567XXXXX", "993088909", "May 30 , 3:24 AM", "28,000", "Send","",false),
//                Transaction("Food order", "993088909", "May 31 , 1:12 PM", "900", "Send","",false),
//                Transaction("+6397570XXXXX", "993088909", "May 31 , 3:35 AM", "11,000", "Send","",false),
//                Transaction("+6391830XXXXX", "993088909", "May 30 , 1:18 PM", "17,500", "Send","",false),
//                Transaction("+6399419XXXXX", "993088909", "May 27 , 11:52 PM", "7,000", "Send","",false),
//                Transaction("David", "993088909", "May 27 , 3:02 PM", "2,20,000", "Received","",false),
//                Transaction("U.S Dealer", "993088909", "May 23 , 8:02 PM", "1,20,000", "Send","",false),
//                Transaction("App client", "993088909", "May 21 , 11:00 PM", "3,50,000", "Received","",false),
//                Transaction("Bank", "9930 8890 9780 1247", "May 15 , 11:00 PM", "65,000", "Send","",false),
//                Transaction("+6399503XXXXX", "+639950357286", "May 5 , 9:30 PM", "11,750", "Send","",false),
//                Transaction("Flight Book", "+06836547237", "April 15 , 12:30 AM", "39,000", "Send","",false),
//                Transaction("Tobin", "+176814322900", "March 15 , 12:30 AM", "11,000", "Received","",false),
//                Transaction("David", "+819885437880", "March 10 , 12:30 AM", "20,000", "Send","",false)
//            )
//        )
//        model.allTransactions.value=transactions
//    }

}