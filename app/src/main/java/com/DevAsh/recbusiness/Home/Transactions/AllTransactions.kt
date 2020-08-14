package com.DevAsh.recbusiness.Home.Transactions


import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.DevAsh.recbusiness.Context.LoadProfileCallBack
import com.DevAsh.recbusiness.Context.StateContext
import com.DevAsh.recbusiness.Context.HelperVariables
import com.DevAsh.recbusiness.Context.UiContext
import com.DevAsh.recbusiness.Context.UiContext.colors
import com.DevAsh.recbusiness.Models.Transaction
import com.DevAsh.recbusiness.R
import kotlinx.android.synthetic.main.activity_all_transactions.*
import kotlinx.android.synthetic.main.activity_all_transactions.activity
import kotlinx.android.synthetic.main.widget_listtile_transaction.view.*
import kotlin.collections.ArrayList


class AllTransactions : AppCompatActivity() {
    lateinit var context: Context
    lateinit var activityAdapter: AllActivityAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_transactions)


        context = this


        val transactionObserver = Observer<ArrayList<Transaction>> {updatedList->
         activityAdapter.updateList(updatedList)
        }

        StateContext.model.allTransactions.observe(this,transactionObserver)
        activityAdapter =
            AllActivityAdapter(
                StateContext.model.allTransactions.value!!,
                context
            )
        Handler().postDelayed({
            activity.layoutManager = LinearLayoutManager(context)
            activity.adapter = activityAdapter
            mainContent.visibility=View.VISIBLE
        },700)
    }

}

class AllActivityAdapter(private var items : ArrayList<Transaction>, val context: Context) : RecyclerView.Adapter<AllActivityViewHolder>() {



    var colorIndex = 0

    var colorMap = HashMap<String,String>()

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AllActivityViewHolder {
        return AllActivityViewHolder(
            LayoutInflater.from(context)
                .inflate(R.layout.widget_listtile_transaction, parent, false),
            context
        )
    }

    override fun onBindViewHolder(holder: AllActivityViewHolder, position: Int) {
        holder.title.text = items[position].contacts.name
        holder.subtitle.text = items[position].time
        holder.badge.text = items[position].contacts.name[0].toString()

        if (items[position].contacts.name.startsWith("+")) {
            holder.badge.text = items[position].contacts.name.subSequence(1, 3)
            holder.badge.textSize = 18F
        }

        holder.item = items[position]

        try {
            holder.badge.setBackgroundColor(Color.parseColor(colorMap[items[position].contacts.id]))
            holder.color = colorMap[items[position].contacts.id]

        }catch (e:Throwable){
            holder.badge.setBackgroundColor(Color.parseColor(colors[colorIndex]))
            colorMap[items[position].contacts.id] = colors[colorIndex]
            holder.color = colors[colorIndex]
            colorIndex = (colorIndex+1)%colors.size
        }

        UiContext.loadProfileImage(context,items[position].contacts.id,object: LoadProfileCallBack {
            override fun onSuccess() {
                holder.badge.visibility=View.GONE
                holder.profile.visibility = View.VISIBLE

                if(!items[position].contacts.id.contains("rpay")){
                    holder.profile.setBackgroundColor( context.resources.getColor(R.color.textDark))
                    holder.profile.setColorFilter(Color.WHITE,  android.graphics.PorterDuff.Mode.SRC_IN)
                    holder.profile.setPadding(35,35,35,35)
                }

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
            holder.additionalInfo.text= "+${items[position].amount} ${HelperVariables.currency}"
            holder.badge.setBackgroundColor(context.getColor(R.color.highlightButton))
            holder.color = "#035aa6"

        }else if(items[position].isWithdraw){

            holder.badge.text = "RC"
            holder.logo.visibility = View.VISIBLE
            holder.logo.setBackgroundColor(context.resources.getColor(R.color.textDark))
            holder.logo.setImageDrawable(context.resources.getDrawable(R.drawable.bank_symbol))
            holder.additionalInfo.setTextColor(Color.parseColor("#8B008B"))
            holder.additionalInfo.setBackgroundColor(Color.parseColor("#258B008B"))
            holder.additionalInfo.text= "-${items[position].amount} ${HelperVariables.currency}"
            holder.badge.setBackgroundColor(context.getColor(R.color.highlightButton))
            holder.color = "#035aa6"
        }else if(items[position].type=="Received"){
            holder.additionalInfo.setTextColor(Color.parseColor("#1b5e20"))
            holder.additionalInfo.setBackgroundColor(Color.parseColor("#151b5e20"))
            holder.additionalInfo.text= "+${items[position].amount} ${HelperVariables.currency}"
        }else if(items[position].type=="Send"){
            holder.additionalInfo.setTextColor(Color.parseColor("#d50000"))
            holder.additionalInfo.setBackgroundColor(Color.parseColor("#15d50000"))
            holder.additionalInfo.text= "-${items[position].amount} ${HelperVariables.currency}"
        }
    }

    fun updateList(updatedList : ArrayList<Transaction>){
        this.items = updatedList
        notifyDataSetChanged()
    }
}

class AllActivityViewHolder (view: View,context: Context,var item:Transaction?=null,var color:String?=null) : RecyclerView.ViewHolder(view) {
    val title = view.findViewById(R.id.title) as TextView
    val subtitle = view.findViewById(R.id.subtitle) as TextView
    val badge = view.findViewById(R.id.badge) as TextView
    val additionalInfo = view.findViewById(R.id.additionalInfo) as TextView
    val logo = view.findViewById<ImageView>(R.id.logo)
    val profile = view.profile


    init {
        view.setOnClickListener{
             HelperVariables.selectedTransaction = item
             HelperVariables.avatarColor = color!!
             context.startActivity(Intent(context,TransactionDetails::class.java))
        }
    }
}


