package com.DevAsh.recbusiness.Home

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.DevAsh.recbusiness.Context.DetailsContext
import com.DevAsh.recbusiness.Context.StateContext
import com.DevAsh.recbusiness.Context.TransactionContext
import com.DevAsh.recbusiness.Context.UiContext
import com.DevAsh.recbusiness.Home.Transactions.AddMoney
import com.DevAsh.recbusiness.Home.Transactions.SendMoney
import com.DevAsh.recbusiness.Home.Transactions.TransactionDetails
import com.DevAsh.recbusiness.Models.Merchant
import com.DevAsh.recbusiness.Models.Transaction
import com.DevAsh.recbusiness.R
import com.DevAsh.recbusiness.Sync.SocketHelper
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.android.synthetic.main.activity_home_page.*
import kotlinx.android.synthetic.main.set_time_bottomsheet.view.*


class HomePage : AppCompatActivity() {

    var context: Context = this

    var time = mapOf(
        0 to "Past 1 Hour",
        1 to "Past 3 Hour",
        2 to "Past 5 Hour",
        3 to "Today",
        4 to "Past 3 days",
        5 to "Past 7 days",
        6 to "Past 1 month",
        7 to "Past 6 month"
    )

    lateinit var recentPaymentsAdapter: RecentPaymentsAdapter




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_page)

        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    println("Failed . . . .")
                    return@OnCompleteListener
                } else {
                    println("success . . . .")
                    DetailsContext.fcmToken = task.result?.token!!
                    SocketHelper.connect()
                }
            })

        recentPaymentsAdapter = RecentPaymentsAdapter(arrayListOf(),this)
        recentPayments.layoutManager=LinearLayoutManager(this)
        recentPayments.adapter = recentPaymentsAdapter

        loadObservers()

        balance.setOnClickListener{
            startActivity(Intent(this,AllTransactions::class.java))
        }

        profile.setOnClickListener{
            startActivity(Intent(context,Profile::class.java))
        }

        sendMoney.setOnClickListener{
            startActivity(Intent(context, SendMoney::class.java))
        }

        buyMoney.setOnClickListener{
            startActivity(Intent(context, AddMoney::class.java))
        }



        setTime.setOnClickListener{
             openTimeSheet()
        }

        updatePayments()

    }

   private fun updatePayments(index:Int = StateContext.timeIndex){
       timeline.text = time[index]
   }

    private fun loadObservers(){
        val balanceObserver = Observer<String> { currentBalance ->
            balance.text = currentBalance
        }



        val paymentsObserver = Observer<ArrayList<Transaction>> {updatedList->
            val updateListTemp = arrayListOf<Transaction>()
            for(i in updatedList){
                if(i.type=="Received" && !i.isGenerated){
                   updateListTemp.add(i)
                }
                if(updateListTemp.size==5){
                    break
                }
            }
            if(updateListTemp.size>0){
                noPayments.visibility=View.GONE
                recentPaymentsContainer.visibility = View.VISIBLE
            }else{
                noPayments.visibility=View.VISIBLE
                recentPaymentsContainer.visibility = View.GONE
            }
            recentPaymentsAdapter.updateList(updateListTemp)
            recentPayments.smoothScrollToPosition(0)
        }

        greetings.text=(getText())
        StateContext.model.currentBalance.observe(this,balanceObserver)
        StateContext.model.allTransactions.observe(this,paymentsObserver)
    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
         if(requestCode==0){
                if(grantResults[0]==PackageManager.PERMISSION_GRANTED ){
                    startActivity(Intent(context, SendMoney::class.java))
                }
        }else if(requestCode==1){
            if(grantResults[0]==PackageManager.PERMISSION_GRANTED ){
                startActivity(Intent(context, QrScanner::class.java))
            }
        }
    }

    private fun getText():String{
        val name = DetailsContext.storeName
        if(name.length>10){
            val splitedText = name.split(" ")
            if(splitedText.size>1){
                val firstName = splitedText[0]
                val secondName = splitedText[1]
                if(firstName.length<3){
                    return secondName
                }
                return firstName
            }else{
                return splitedText[0]
            }
        }else{
            return name
        }
    }


    override fun onBackPressed() {
        val startMain = Intent(Intent.ACTION_MAIN)
        startMain.addCategory(Intent.CATEGORY_HOME)
        startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(startMain)
    }

    private fun openTimeSheet(){
        val mBottomSheetDialog = BottomSheetDialog(context)
        val sheetView: View = LayoutInflater.from(context).inflate(R.layout.set_time_bottomsheet, null)

        fun updateBackground(view: View, index:Int){
            if(StateContext.timeIndex==index){
                view.background=getDrawable(R.color.colorPrimary)
            }else{
                view.background=getDrawable(R.color.white)
            }
        }

        fun setBackground(){
            updateBackground( sheetView.oneHour,0)
            updateBackground( sheetView.threeHour,1)
            updateBackground( sheetView.fiveHour,2)
            updateBackground( sheetView.today,3)
            updateBackground( sheetView.threeDay,4)
            updateBackground( sheetView.sevenDay,5)
            updateBackground( sheetView.oneMonth,6)
            updateBackground( sheetView.sixMonth,7)
        }

        setBackground()


        sheetView.oneHour.setOnClickListener{
            StateContext.timeIndex=0
            updatePayments(0)
            mBottomSheetDialog.cancel()
            setBackground()
        }
        sheetView.threeHour.setOnClickListener{
            StateContext.timeIndex=1
            updatePayments(1)
            mBottomSheetDialog.cancel()
            setBackground()

        }
        sheetView.fiveHour.setOnClickListener{
            StateContext.timeIndex=2
            updatePayments(2)
            mBottomSheetDialog.cancel()
            setBackground()

        }
        sheetView.today.setOnClickListener{
            StateContext.timeIndex=3
            updatePayments(3)
            mBottomSheetDialog.cancel()
            setBackground()

        }
        sheetView.threeDay.setOnClickListener{
            StateContext.timeIndex=4
            updatePayments(4)
            mBottomSheetDialog.cancel()
            setBackground()
        }
        sheetView.sevenDay.setOnClickListener{
            StateContext.timeIndex=5
            updatePayments(5)
            mBottomSheetDialog.cancel()
            setBackground()
        }
        sheetView.oneMonth.setOnClickListener{
            StateContext.timeIndex=6
            updatePayments(6)
            mBottomSheetDialog.cancel()
            setBackground()
        }
        sheetView.sixMonth.setOnClickListener{
            StateContext.timeIndex=7
            updatePayments(7)
            mBottomSheetDialog.cancel()
            setBackground()
        }



        mBottomSheetDialog.setContentView(sheetView)
        mBottomSheetDialog.show()


    }
}

class RecentPaymentsAdapter(private var items : List<Transaction>, val context: Context) : RecyclerView.Adapter<RecentActivityViewHolder>() {

    private var colorIndex = 0

    private var colorMap = HashMap<String,String>()
    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentActivityViewHolder {
        return RecentActivityViewHolder(LayoutInflater.from(context).inflate(R.layout.widget_listtile_transactions_home, parent, false),context)
    }

    override fun onBindViewHolder(holder: RecentActivityViewHolder, position: Int) {
        holder.title.text = items[position].name
        holder.subtitle.text = items[position].time
        holder.badge.text = items[position].name[0].toString()

        if (items[position].name.startsWith("+")) {
            holder.badge.text = items[position].name.subSequence(1, 3)
            holder.badge.textSize = 18F
        }

        holder.item = items[position]

        try {
            holder.badge.setBackgroundColor(Color.parseColor(colorMap[items[position].id]))
            holder.color = colorMap[items[position].id]

        }catch (e:Throwable){
            holder.badge.setBackgroundColor(Color.parseColor(UiContext.colors[colorIndex]))
            colorMap[items[position].id] = UiContext.colors[colorIndex]
            holder.color = UiContext.colors[colorIndex]
            colorIndex = (colorIndex+1)% UiContext.colors.size
        }



        if(items[position].isGenerated){

            holder.badge.text = "RC"
            holder.logo.visibility = View.VISIBLE
            holder.title.text="Added to wallet"
            holder.badge.textSize = 14F
            holder.additionalInfo.setTextColor(Color.parseColor("#ff9100"))
            holder.additionalInfo.setBackgroundColor(Color.parseColor("#25ff9100"))
            holder.additionalInfo.text= "+${items[position].amount} ${TransactionContext.currency}"
            holder.badge.setBackgroundColor(context.getColor(R.color.highlightButton))
            holder.color = "#035aa6"

        }else if(items[position].type=="Received"){
            holder.additionalInfo.setTextColor(Color.parseColor("#1b5e20"))
            holder.additionalInfo.setBackgroundColor(Color.parseColor("#151b5e20"))
            holder.additionalInfo.text= "+${items[position].amount} ${TransactionContext.currency}"
        }else if(items[position].type=="Send"){
            holder.additionalInfo.setTextColor(Color.parseColor("#d50000"))
            holder.additionalInfo.setBackgroundColor(Color.parseColor("#15d50000"))
            holder.additionalInfo.text= "-${items[position].amount} ${TransactionContext.currency}"
        }
    }

    fun updateList(updatedList : ArrayList<Transaction>){
        this.items = updatedList
        notifyDataSetChanged()

    }


}

class RecentActivityViewHolder (view: View,context: Context,var item:Transaction?=null,var color:String?=null) : RecyclerView.ViewHolder(view) {
    val title = view.findViewById(R.id.title) as TextView
    val subtitle = view.findViewById(R.id.subtitle) as TextView
    val badge = view.findViewById(R.id.badge) as TextView
    val additionalInfo = view.findViewById(R.id.additionalInfo) as TextView
    val logo = view.findViewById<ImageView>(R.id.logo)

    init {
        view.setOnClickListener{
            TransactionContext.selectedTransaction = item
            TransactionContext.avatarColor = color!!
            context.startActivity(Intent(context, TransactionDetails::class.java))
        }
    }
}
