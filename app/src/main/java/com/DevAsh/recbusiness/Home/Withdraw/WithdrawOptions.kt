package com.DevAsh.recbusiness.Home.Withdraw

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.DevAsh.recbusiness.Context.HelperVariables
import com.DevAsh.recbusiness.Context.StateContext
import com.DevAsh.recbusiness.Home.BottomSheet
import com.DevAsh.recbusiness.Home.Withdraw.AccountDetails
import com.DevAsh.recbusiness.Home.Withdraw.AddAccounts
import com.DevAsh.recbusiness.Models.BankAccount
import com.DevAsh.recbusiness.R
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.activity_withdraw_options.*
import kotlinx.android.synthetic.main.bank_accounts.view.*
import kotlinx.android.synthetic.main.widget_accounts.view.*

class WithdrawOptions : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_withdraw_options)

        toBank.setOnClickListener{
            val totalAccounts = StateContext.model.bankAccounts.value?.size
            if(totalAccounts!=null && totalAccounts!=0){
              BottomSheetAccounts(this).openBottomSheet()
            }else{
                startActivity(Intent(this, AddAccounts::class.java))
            }
        }
    }
}

class BottomSheetAccounts(val context:Context): BottomSheet {
    private val mBottomSheetDialog = BottomSheetDialog(context)
    private val sheetView: View = LayoutInflater.from(context).inflate(R.layout.bank_accounts, null)
    private val bankAccounts = if(StateContext.model.bankAccounts.value!=null)
        StateContext.model.bankAccounts.value
    else arrayListOf()
    init {
        val accountsContainer = sheetView.findViewById<RecyclerView>(R.id.accountsContainer)
        sheetView.addAccounts.visibility = View.GONE
        accountsContainer.layoutManager = LinearLayoutManager(context)
        accountsContainer.adapter = AccountsViewAdapter(
            bankAccounts!!,
            context,this)
        mBottomSheetDialog.setContentView(sheetView)
    }
    override fun openBottomSheet(){
        mBottomSheetDialog.show()
    }

    override fun closeBottomSheet() {
        mBottomSheetDialog.cancel()
    }
}

class AccountsViewAdapter(
    private var items : ArrayList<BankAccount>,
    val context: Context,
    private val openBottomSheetCallback: BottomSheet?)
    : RecyclerView.Adapter<AccountsViewHolder>() {

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountsViewHolder {
        return AccountsViewHolder(LayoutInflater.from(context).inflate(R.layout.widget_accounts, parent, false),context,openBottomSheetCallback)
    }

    override fun onBindViewHolder(holder: AccountsViewHolder, position: Int) {
        holder.account= items[position]
        holder.accountName.text = items[position].bankName
        holder.accountNumber.text = items[position].accountNumber
    }
}

class AccountsViewHolder (view: View, context: Context, private val openBottomSheetCallback: BottomSheet?) : RecyclerView.ViewHolder(view){

    var account: BankAccount?=null
    val accountName = view.accountName
    val accountNumber = view.accountNumber

    init {
        view.setOnClickListener{
            openBottomSheetCallback?.closeBottomSheet()
            HelperVariables.selectedAccount = account
            var intent = Intent(context, AccountDetails::class.java)
            intent.putExtra("isWithdraw",true)
            context.startActivity(intent)
        }

        view.edit.setOnClickListener{
            openBottomSheetCallback?.closeBottomSheet()
        }
    }
}
