package com.DevAsh.recbusiness.Home

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.DevAsh.recbusiness.Context.*
import com.DevAsh.recbusiness.Database.ExtraValues
import com.DevAsh.recbusiness.Home.Transactions.AddMoney
import com.DevAsh.recbusiness.Home.Transactions.SendMoney
import com.DevAsh.recbusiness.Home.Transactions.TransactionDetails
import com.DevAsh.recbusiness.Models.Transaction
import com.DevAsh.recbusiness.MyStore.MyStoreHome
import com.DevAsh.recbusiness.R
import com.DevAsh.recbusiness.Sync.SocketHelper
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.iid.FirebaseInstanceId
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_home_page.*
import kotlinx.android.synthetic.main.activity_profile.*
import kotlinx.android.synthetic.main.set_time_bottomsheet.view.*
import kotlinx.android.synthetic.main.widget_listtile_transaction.view.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


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
    lateinit var extraValues: ExtraValues



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_page)

        extraValues = try{
            Realm.getDefaultInstance().where(ExtraValues::class.java).findFirst()!!
        }catch (e:Throwable){
            println(e)
            ExtraValues()
        }
        StateContext.timeIndex = extraValues.timeIndex
        timeline.text = time[StateContext.timeIndex]

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

        loadProfilePicture()

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

        viewPayments.setOnClickListener{
             BottomSheetPeople(context, updatePaymentsListener()).openBottomSheet()
        }

        scan.setOnClickListener{
            val permissions = arrayOf(android.Manifest.permission.CAMERA)
            if(packageManager.checkPermission(android.Manifest.permission.CAMERA,context.packageName)==PackageManager.PERMISSION_GRANTED ){
                startActivity(Intent(context, QrScanner::class.java))
            }else{
                ActivityCompat.requestPermissions(this, permissions,1)
            }
        }

        qrCode.setOnClickListener{
            startActivity(Intent(context, DisplayQrcode::class.java))
        }

        myStore.setOnClickListener{
            startActivity(Intent(this,MyStoreHome::class.java))
        }

        hideButton()

    }

    private fun hideButton(){
        val hiddenPanel = findViewById<CardView>(R.id.scanContainer)
        val bottomDown: Animation = AnimationUtils.loadAnimation(
            context,
            R.anim.button_down
        )
        val bottomUp: Animation = AnimationUtils.loadAnimation(
            context,
            R.anim.button_up
        )
        scroller.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            if (scrollY >200) {
                if(hiddenPanel.visibility==View.VISIBLE){
                    hiddenPanel.startAnimation(bottomDown)
                    hiddenPanel.visibility=View.GONE
                }

            }
            if(scrollY < 200){
                if(hiddenPanel.visibility==View.GONE){
                    hiddenPanel.visibility=View.VISIBLE
                    hiddenPanel.startAnimation(bottomUp)
                }
            }
        }
    }

   private fun updatePayments(index:Int = StateContext.timeIndex){
       timeline.text = time[index]
   }

    private fun loadObservers(){
        val balanceObserver = Observer<String> { currentBalance ->
            balance.text = currentBalance
        }

        val paymentsObserver = Observer<ArrayList<Transaction>> {updatedList->
          updatePaymentsListener(updatedList)
        }

        greetings.text=(getText())
        StateContext.model.currentBalance.observe(this,balanceObserver)
        StateContext.model.allTransactions.observe(this,paymentsObserver)
    }

    private fun updatePaymentsListener(updatedList:ArrayList<Transaction> = StateContext.model.allTransactions.value!!):ArrayList<Transaction>{
        val updateListTemp = arrayListOf<Transaction>()
        for(i in updatedList){
            if(i.type=="Received" && !i.isGenerated){
                if( filterWithTimeStamp(i.timeStamp.toString()))
                  updateListTemp.add(i)
                else
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


        if(updateListTemp.size<5){
            recentPaymentsAdapter.updateList(updateListTemp)
        }else{
            recentPaymentsAdapter.updateList(ArrayList(updateListTemp.subList(0,5)))

        }


        recentPayments.smoothScrollToPosition(0)
        return updateListTemp
    }

    private fun filterWithTimeStamp(timestampString: String):Boolean{

        val compareTime = mapOf(
            0 to Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)),
            1 to Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(3)),
            2 to Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(5)),
            3 to Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)),
            4 to Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(3)),
            5 to Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)),
            6 to Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)),
            7 to Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30*6))
        )

        val time = timestampString.replace("T"," ").substring(0,timestampString.lastIndex)
        val parser = SimpleDateFormat("yyyy-MM-dd HH:mm")
        val transactionTime: Date = parser.parse(time)
        if(transactionTime.after(compareTime[StateContext.timeIndex])){
            return true
        }
        return false
    }


    private fun loadProfilePicture(){
        UiContext.loadProfileImage(
            context,
            DetailsContext.id,
            object :LoadProfileCallBack{
                override fun onSuccess() {
                    profile.background=resources.getDrawable(R.drawable.image_avatar)
                    profile.setPadding(35,35,35,35)
                }

                override fun onFailure() {

                }
            },
            profile,
            R.drawable.profile
        )
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

    override fun onResume() {
        if(UiContext.isProfilePictureChanged){
            println("Uploaded")
            UiContext.UpdateImage(profile)
            UiContext.isProfilePictureChanged=false
        }
        super.onResume()
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

        fun onClick(index: Int){
            StateContext.timeIndex=index
            Realm.getDefaultInstance().executeTransactionAsync{realm->
                val extraValues = realm.where(ExtraValues::class.java).findFirst()
                if(extraValues!=null ){
                  extraValues.timeIndex=index
                }else{
                    val newExtraValues = ExtraValues()
                    newExtraValues.timeIndex = StateContext.timeIndex
                    realm.insert(newExtraValues)
                }
            }
            updatePayments(index)
            mBottomSheetDialog.cancel()
            setBackground()
            updatePaymentsListener()
        }

        setBackground()


        sheetView.oneHour.setOnClickListener{
          onClick(0)
        }
        sheetView.threeHour.setOnClickListener{
            onClick(1)


        }
        sheetView.fiveHour.setOnClickListener{
            onClick(2)

        }
        sheetView.today.setOnClickListener{
            onClick(3)


        }
        sheetView.threeDay.setOnClickListener{
            onClick(4)

        }
        sheetView.sevenDay.setOnClickListener{
            onClick(5)

        }
        sheetView.oneMonth.setOnClickListener{
            onClick(6)

        }
        sheetView.sixMonth.setOnClickListener{
            onClick(7)

        }

        mBottomSheetDialog.setContentView(sheetView)
        mBottomSheetDialog.show()


    }



}

class BottomSheetPeople(val context:Context,transactions:ArrayList<Transaction>):BottomSheet{
    private val mBottomSheetDialog = BottomSheetDialog(context)
    private val sheetView: View = LayoutInflater.from(context).inflate(R.layout.payments_bottom_sheet, null)
    init {
        val peopleContainer = sheetView.findViewById<RecyclerView>(R.id.recentPayments)
        peopleContainer.adapter = RecentPaymentsAdapter(transactions,context,this)
        peopleContainer.layoutManager = LinearLayoutManager(context)
        mBottomSheetDialog.setContentView(sheetView)
    }
    override fun openBottomSheet(){
        mBottomSheetDialog.show()
    }

    override fun closeBottomSheet() {
        mBottomSheetDialog.cancel()
    }
}



class RecentPaymentsAdapter(var items : List<Transaction>, val context: Context, private val openSheet: BottomSheet?=null) : RecyclerView.Adapter<RecentActivityViewHolder>() {


    private var colorIndex = 0

    private var colorMap = HashMap<String,String>()
    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentActivityViewHolder {
        return RecentActivityViewHolder(LayoutInflater.from(context).inflate(R.layout.widget_listtile_transactions_home, parent, false),context,openSheet = openSheet)
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

        UiContext.loadProfileImage(context,items[position].id,object: LoadProfileCallBack {
            override fun onSuccess() {
                holder.badge.visibility=View.GONE
                holder.profile.visibility = View.VISIBLE

            }

            override fun onFailure() {
                holder.badge.visibility= View.VISIBLE
                holder.profile.visibility = View.GONE

            }

        },holder.profile)


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

class RecentActivityViewHolder (view: View,context: Context,var item:Transaction?=null,var color:String?=null,openSheet: BottomSheet?=null) : RecyclerView.ViewHolder(view) {
    val title = view.findViewById(R.id.title) as TextView
    val subtitle = view.findViewById(R.id.subtitle) as TextView
    val badge = view.findViewById(R.id.badge) as TextView
    val additionalInfo = view.findViewById(R.id.additionalInfo) as TextView
    val logo = view.findViewById<ImageView>(R.id.logo)
    val profile = view.profile

    init {
        view.setOnClickListener{
            TransactionContext.selectedTransaction = item
            TransactionContext.avatarColor = color!!
            context.startActivity(Intent(context, TransactionDetails::class.java))
            openSheet?.closeBottomSheet()
        }
    }
}

interface BottomSheet{
    fun openBottomSheet()
    fun closeBottomSheet()
}