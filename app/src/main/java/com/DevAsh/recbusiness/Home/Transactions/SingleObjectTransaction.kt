package com.DevAsh.recbusiness.Home.Transactions

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SmoothScroller
import com.DevAsh.recbusiness.Context.*
import com.DevAsh.recbusiness.Helper.AlertHelper
import com.DevAsh.recbusiness.Helper.NotificationObserver
import com.DevAsh.recbusiness.Helper.PaymentObserver
import com.DevAsh.recbusiness.Helper.TransactionsHelper
import com.DevAsh.recbusiness.Models.Contacts
import com.DevAsh.recbusiness.Models.Message
import com.DevAsh.recbusiness.Models.Transaction
import com.DevAsh.recbusiness.R
import com.DevAsh.recbusiness.SplashScreen
import com.DevAsh.recbusiness.Sync.SocketHelper

import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONArrayRequestListener
import com.androidnetworking.interfaces.JSONObjectRequestListener
import kotlinx.android.synthetic.main.activity_single_object_transaction.*

import org.json.JSONArray
import org.json.JSONObject
import java.lang.Exception
import java.sql.Timestamp

class SingleObjectTransaction : AppCompatActivity() {

    var allActivityAdapter: TransactionsAdapter?=null
    private lateinit var badge: TextView
    lateinit var context: Context
    lateinit var smoothScroller:SmoothScroller

    var needToScroll = false
    var transaction = ArrayList<ObjectTransactions>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SocketHelper.connect()
        setContentView(R.layout.activity_single_object_transaction)
        context=this
        badge = findViewById(R.id.badge)
        handelSocket()
        avatarContainer.setBackgroundColor(Color.parseColor(HelperVariables.avatarColor))
        Cache.socketListnerCache[this] = HelperVariables.selectedUser!!.id
        smoothScroller = object : LinearSmoothScroller(context) {
            override fun getVerticalSnapPreference(): Int {
                return SNAP_TO_END
            }
            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                return 250f / displayMetrics.densityDpi
            }
        }

        loadAvatar()



        badge.text = HelperVariables.selectedUser!!.name[0].toString()

        try {
            if(!intent.getBooleanExtra("openSingleObjectTransactions",false)){
                allActivityAdapter = Cache.singleObjectTransactionCache[HelperVariables.selectedUser!!.id]!!
                val layoutManager=LinearLayoutManager(context,LinearLayoutManager.VERTICAL,false)
                layoutManager.stackFromEnd=true
                transactionContainer.layoutManager = layoutManager
                transactionContainer.adapter = allActivityAdapter
                transaction = allActivityAdapter!!.items
                Handler().postDelayed({getData()},0)
                loadingScreen.visibility=View.INVISIBLE
            }else{
                throw Exception()
            }

        }catch (e:Throwable){
            e.printStackTrace()
            getData()
        }

        if (HelperVariables.selectedUser!!.name.startsWith("+")) {
            badge.text = HelperVariables.selectedUser!!.name.subSequence(1, 3)
            badge.textSize = 18F
        }
        name.text = HelperVariables.selectedUser!!.name
        number.text = HelperVariables.selectedUser!!.number

        back.setOnClickListener{
            super.onBackPressed()
        }

        sendMessage.setOnClickListener{
            try{
                sendMessage()
            }catch (e:Throwable){
                e.printStackTrace()
            }
        }

        TransactionsHelper.notificationObserver[HelperVariables.selectedUser!!.id] = object:
            NotificationObserver {
            override fun check() {
                try {
                    Handler().postDelayed({
                        getData()
                    },0)
                }catch (e:Throwable){
                    e.printStackTrace()
                }
            }
        }

        pay.setOnClickListener{
            TransactionsHelper.paymentObserver=object : PaymentObserver {
                override fun update(transaction: Transaction) {
                    val objectTransactions=ObjectTransactions(transaction=transaction)
                    needToScroll=true
                    if(!this@SingleObjectTransaction.transaction.contains(objectTransactions)){
                        this@SingleObjectTransaction.transaction.add(objectTransactions)
                        allActivityAdapter?.updateList(this@SingleObjectTransaction.transaction,transactionContainer)
                    }
                }

            }
            startActivity(Intent(context,AmountPrompt::class.java))
        }
    }

    override fun onResume() {
        if(needToScroll){
            needToScroll=false
            smoothScroller.targetPosition = transaction.size
            (transactionContainer.layoutManager as RecyclerView.LayoutManager).startSmoothScroll(smoothScroller)
        }
        super.onResume()
    }



    private fun loadAvatar(){
        UiContext.loadProfileImage(context,HelperVariables.selectedUser?.id!!,object:LoadProfileCallBack {
            override fun onSuccess() {
                if(!HelperVariables.selectedUser?.id!!.contains("rpay")){
                    profile.setBackgroundColor( context.resources.getColor(R.color.textDark))
                    profile.setColorFilter(Color.WHITE,  android.graphics.PorterDuff.Mode.SRC_IN)
                    profile.setPadding(35,35,35,35)
                }
                avatarContainer.visibility=View.GONE
                profile.visibility = View.VISIBLE
            }

            override fun onFailure() {
                avatarContainer.visibility= View.VISIBLE
                profile.visibility = View.GONE
            }
        },profile)
    }

    fun sendMessageToSocket(message:String){
        SocketHelper.socket?.emit(
            "notifyMessage",
            JSONObject(
                mapOf(
                    "from"  to JSONObject(
                        mapOf(
                            "id" to DetailsContext.id ,
                            "name" to DetailsContext.storeName,
                            "number" to DetailsContext.phoneNumber,
                            "email" to DetailsContext.email
                        )
                    ),
                    "to" to JSONObject(
                        mapOf(
                            "id" to HelperVariables.selectedUser?.id ,
                            "name" to HelperVariables.selectedUser?.name,
                            "number" to  HelperVariables.selectedUser?.number,
                            "email" to HelperVariables.selectedUser?.email
                        )
                    ),
                    "message" to message,
                    "messageTime" to  Timestamp(System.currentTimeMillis()).toString()
                )
            )
        )
    }

    private fun handelSocket(){
        try{
            SocketHelper.socket?.on("receivedMessage") { it->
                val messageData = it[0] as JSONObject
                if( Cache.socketListnerCache[this]==messageData.getJSONObject("from")["id"]){
                    val objectTransactions =  ObjectTransactions(
                        message = Message(
                            contacts = HelperVariables.selectedUser,
                            message = messageData.getString("message"),
                            type = "Received",
                            time = messageData.getString("messageTime")
                        ))
                    if (!transaction.contains(objectTransactions)){
                        transaction.add(
                            objectTransactions
                        )
                        allActivityAdapter?.updateList(transaction,transactionContainer)

                    }

                    smoothScroller.targetPosition = transaction.size
                    (transactionContainer.layoutManager as RecyclerView.LayoutManager).startSmoothScroll(smoothScroller)

                }


            }

            SocketHelper.socket?.on("receivedSingleObjectTransaction") { it->
                val transactionData = it[0] as JSONObject
                val from = transactionData.getJSONObject("from")
                val to = transactionData.getJSONObject("to")
                val isSend = TransactionsHelper.isSend(DetailsContext.id, from.getString("id"))
                val name = if (isSend) to.getString("name") else from.getString("name")
                val number = if (isSend) to.getString("number") else from.getString("number")
                val email = if (isSend) to.getString("email") else from.getString("email")
                val id = if (isSend) to.getString("id") else from.getString("id")
                val contacts = Contacts(name, number,id,email)
                if(
                    from["id"]==DetailsContext.id && to["id"]==Cache.socketListnerCache[this] ||
                    to["id"]==DetailsContext.id && from["id"]==Cache.socketListnerCache[this]
                ){
                    val transactionObject = Transaction(
                        contacts = contacts,
                        amount = transactionData["amount"].toString(),
                        time =(if (isSend)
                            "Paid  "
                        else "Received  ")+ SplashScreen.dateToString(
                            transactionData["transactionTime"].toString()
                        ),
                        type = if (isSend)
                            "Send"
                        else "Received",
                        transactionId =  transactionData["transactionID"].toString(),
                        isGenerated = false,
                        isWithdraw = false,
                        timeStamp =  transactionData["transactionTime"].toString()
                    )
                    val objectTransactions=ObjectTransactions( transaction = transactionObject   )
                    if(!transaction.contains(objectTransactions)){
                        transaction.add(
                            objectTransactions
                        )
                        allActivityAdapter?.updateList(transaction,transactionContainer)

                    }
                    smoothScroller.targetPosition = transaction.size
                    (transactionContainer.layoutManager as RecyclerView.LayoutManager).startSmoothScroll(smoothScroller)

                }
            }
        }catch (e:Throwable){
            smoothScroller.targetPosition = transaction.size
            (transactionContainer.layoutManager as RecyclerView.LayoutManager).startSmoothScroll(smoothScroller)
            e.printStackTrace()
        }
    }

    private fun sendMessage(){
        val messageText = messageEditText.text.toString()
        if (messageText.isNotEmpty()){
            transaction.add(
                ObjectTransactions(
                    message = Message(
                        contacts = HelperVariables.selectedUser,
                        message = messageText,
                        type = "Send",
                        time = Timestamp(System.currentTimeMillis()).toString()
                    ))
            )

            messageEditText.setText("")
            allActivityAdapter?.updateList(transaction,transactionContainer)
            smoothScroller.targetPosition = transaction.size
            (transactionContainer.layoutManager as RecyclerView.LayoutManager).startSmoothScroll(smoothScroller)


            AndroidNetworking.post(ApiContext.apiUrl + ApiContext.paymentPort + "/sendMessage")
                .setContentType("application/json; charset=utf-8")
                .addHeaders("jwtToken", DetailsContext.token)
                .addApplicationJsonBody(object{
                    var from = object{
                        var id = DetailsContext.id
                        var name = DetailsContext.storeName
                        var number = DetailsContext.phoneNumber
                        var email = DetailsContext.email
                    }
                    var to = object {
                        var id =  HelperVariables.selectedUser?.id
                        var name =  HelperVariables.selectedUser?.name
                        var number =  HelperVariables.selectedUser?.number
                        var email =  HelperVariables.selectedUser?.email
                    }
                    var message = messageText
                })
                .setPriority(Priority.IMMEDIATE)
                .build()
                .getAsJSONObject(object :JSONObjectRequestListener{
                    override fun onResponse(response: JSONObject?) {
                        sendMessageToSocket(messageText)
                    }

                    override fun onError(anError: ANError?) {
                        println(anError?.errorCode)
                    }

                })
        }


    }


    private fun getData(){
        Handler().postDelayed({
            AndroidNetworking.get(
                ApiContext.apiUrl
                        + ApiContext.paymentPort
                        + "/getTransactionsBetweenObjects?id1=${DetailsContext.id}&id2=${HelperVariables.selectedUser!!.id.replace("+","")}")
                .addHeaders("jwtToken", DetailsContext.token)
                .setPriority(Priority.IMMEDIATE)
                .build()
                .getAsJSONArray(object: JSONArrayRequestListener {
                    override fun onResponse(response: JSONArray?) {

                        val transactions = ArrayList<ObjectTransactions>()
                        val transactionObjectArray = response!!
                        for (i in 0 until transactionObjectArray.length()) {
                            val message = response.getJSONObject(i).getJSONObject("MessageData")
                            val transaction = response.getJSONObject(i).getJSONObject("TransactionData")
                            try{
                                val from = transaction.getJSONObject("From")
                                val to = transaction.getJSONObject("To")
                                val isSend = TransactionsHelper.isSend(DetailsContext.id, from.getString("Id"))
                                val name = if (isSend) to.getString("Name") else from.getString("Name")
                                val number = if (isSend) to.getString("Number") else from.getString("Number")
                                val email = if (isSend) to.getString("Email") else from.getString("Email")
                                val id = if (isSend) to.getString("Id") else from.getString("Id")
                                val contacts = Contacts(name, number,id,email)
                                transactions.add(
                                    ObjectTransactions( transaction =   Transaction(
                                        contacts = contacts,
                                        amount = transaction["Amount"].toString(),
                                        time =(if (isSend)
                                            "Paid  "
                                        else "Received  ")+ SplashScreen.dateToString(
                                            transaction["TransactionTime"].toString()
                                        ),
                                        type = if (isSend)
                                            "Send"
                                        else "Received",
                                        transactionId =  transaction["TransactionID"].toString(),
                                        isGenerated = transaction.getBoolean("IsGenerated"),
                                        isWithdraw = transaction.getBoolean("IsWithdraw"),
                                        timeStamp = transaction.getString("TimeStamp")
                                    )
                                    ))
                            }catch (e:Throwable){
                                e.printStackTrace()
                                val from = message.getJSONObject("From")
                                val to = message.getJSONObject("To")
                                val isSend = TransactionsHelper.isSend(DetailsContext.id, from.getString("Id"))
                                val name = if (isSend) to.getString("Name") else from.getString("Name")
                                val number = if (isSend) to.getString("Number") else from.getString("Number")
                                val email = if (isSend) to.getString("Email") else from.getString("Email")
                                val id = if (isSend) to.getString("Id") else from.getString("Id")
                                val contacts = Contacts(name, number,id,email)

                                transactions.add(
                                    ObjectTransactions( message =   Message(
                                        contacts = contacts,
                                        message = message["Message"].toString(),
                                        time =(if (isSend)
                                            "Paid  "
                                        else "Received  ")+ SplashScreen.dateToString(
                                            message["MessageTime"].toString()
                                        ),
                                        type = if (isSend)
                                            "Send"
                                        else "Received"
                                    )
                                    ))
                            }
                        }

                        if(transactions.size!=allActivityAdapter?.itemCount) {
                            transaction = transactions
                            val layoutManager =  LinearLayoutManager(context,LinearLayoutManager.VERTICAL,false)
                            layoutManager.stackFromEnd=true
                            transactionContainer.layoutManager =layoutManager
                            allActivityAdapter = TransactionsAdapter(transaction, context)
                            transactionContainer.adapter = allActivityAdapter

                            Cache.singleObjectTransactionCache[HelperVariables.selectedUser!!.id.replace("+","")] = allActivityAdapter!!
                            Handler().postDelayed({
                                loadingScreen.visibility = View.INVISIBLE
                            },300)
                        }
                    }

                    override fun onError(anError: ANError?) {
                        AlertHelper.showServerError(this@SingleObjectTransaction)
                    }

                })
        },0)
    }
}

class TransactionsAdapter(var items : ArrayList<ObjectTransactions>, val context: Context) : RecyclerView.Adapter<TransactionsViewHolder>() {

    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemViewType(position: Int): Int = position


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionsViewHolder {
        return if(items[viewType].transaction==null){
            TransactionsViewHolder(LayoutInflater.from(context).inflate(R.layout.widget_message, parent, false),context)
        }else{
            TransactionsViewHolder(LayoutInflater.from(context).inflate(R.layout.widget_transactions, parent, false),context)
        }
    }

    override fun onBindViewHolder(holder: TransactionsViewHolder, position: Int) {

        if(position==0){
            holder.topMargin?.visibility=View.VISIBLE
        }else{
            holder.topMargin?.visibility=View.GONE
        }
        if(items[position].transaction!=null){
            println(items[position].transaction?.type+"  =>  "+items[position].transaction?.time)

            holder.amount?.text = "${items[position].transaction?.amount}"
            holder.time?.text = items[position].transaction?.time
            holder.item = items[position].transaction
            if(items[position].transaction?.type=="Received"){
                holder.container?.gravity = Gravity.START
                holder.contentWidget?.background= context.getDrawable(R.drawable.transaction_received_ripple)
            }
        }else{
            holder.message?.text = items[position].message?.message
            if(items[position].message?.type=="Received"){
                holder.container?.gravity = Gravity.START
                holder.contentWidget?.background= context.getDrawable(R.drawable.transaction_received_ripple)
            }
        }
    }

    fun updateList(updatedList : ArrayList<ObjectTransactions>,view:RecyclerView){
        this.items = updatedList
        try{
            notifyItemInserted(updatedList.size)
            view.scrollToPosition(updatedList.size+1)
        }catch (e:Throwable){
            e.printStackTrace()
        }
    }
}

class TransactionsViewHolder (view: View,context: Context,var item:Transaction?=null,var color:String?=null) : RecyclerView.ViewHolder(view) {

    var amount:TextView? = null
    var time:TextView? = null
    var message:TextView?=null
    var container:RelativeLayout?=view.findViewById(R.id.container) as RelativeLayout
    var contentWidget:RelativeLayout?=view.findViewById(R.id.contentWidget) as RelativeLayout
    var topMargin:LinearLayout?=view.findViewById(R.id.topMargin)

    init {
        try {
            amount = view.findViewById(R.id.amount) as TextView
            time = view.findViewById(R.id.time) as TextView
        }catch (e:Throwable){
            message = view.findViewById(R.id.message) as TextView
        }
        view.setOnClickListener{
            if (item!=null){
                HelperVariables.selectedTransaction = item
                context.startActivity(Intent(context,TransactionDetails::class.java))
            }
        }
    }
}

class ObjectTransactions(var transaction:Transaction?=null, var message: Message?=null) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ObjectTransactions) return false

        if (transaction != other.transaction) return false
        if (message != other.message) return false

        return true
    }

    override fun hashCode(): Int {
        var result = transaction?.hashCode() ?: 0
        result = 31 * result + (message?.hashCode() ?: 0)
        return result
    }
}