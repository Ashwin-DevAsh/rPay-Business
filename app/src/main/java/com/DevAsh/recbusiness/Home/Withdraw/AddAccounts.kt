package com.DevAsh.recbusiness.Home.Withdraw

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.DevAsh.recbusiness.Context.ApiContext
import com.DevAsh.recbusiness.Context.DetailsContext
import com.DevAsh.recbusiness.Context.StateContext
import com.DevAsh.recbusiness.Database.RealmHelper
import com.DevAsh.recbusiness.Helper.AlertHelper
import com.DevAsh.recbusiness.Models.BankAccount
import com.DevAsh.recbusiness.R
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_add_accounts.*
import kotlinx.android.synthetic.main.activity_add_accounts.cancel
import kotlinx.android.synthetic.main.activity_add_accounts.done
import kotlinx.android.synthetic.main.activity_add_accounts.mainContent
import org.json.JSONObject


class AddAccounts : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_accounts)

        RealmHelper.init(this)

        done.setOnClickListener {
            addAccount(it)
        }

        cancel.setOnClickListener {
            onBackPressed()
        }
    }

    private fun addAccount(it:View){
       if(!validate()){
          return
       }

        if(isAccountExist()){
            AlertHelper.showError("Account Already exist",this)
            return
        }

        hideKeyboardFrom(context = this,view = it)
        Handler().postDelayed({
            mainContent.visibility= View.INVISIBLE
        },500)


        val holderName = holderName.text.toString()
        val accountNumber =  accountNumber.text.toString()
        val ifsc =  ifsc.text.toString()
        val bankName = bankName.text.toString()

        AndroidNetworking.post(ApiContext.apiUrl+ ApiContext.profilePort+"/addBankAccountMerchant")
            .addHeaders("token", DetailsContext.token)
            .addBodyParameter(object{
                var id = DetailsContext.id
                var holderName = holderName
                var accountNumber = accountNumber
                var ifsc = ifsc
                var bankName = bankName
            })
            .setPriority(Priority.IMMEDIATE)
            .build()
            .getAsJSONObject(object: JSONObjectRequestListener {
                override fun onResponse(response: JSONObject?) {
                    mainContent.visibility= View.VISIBLE
                    if(response?.getString("message")=="done"){
                           completed()
                    }else{
                        AlertHelper.showAlertDialog(
                            this@AddAccounts,
                            "Failed !", "There is some issue with our server",
                            object : AlertHelper.AlertDialogCallback {
                                override fun onDismiss() {
                                    mainContent.visibility = View.VISIBLE
                                }

                                override fun onDone() {
                                    mainContent.visibility = View.VISIBLE
                                }

                            }
                        )
                    }
                }

                override fun onError(anError: ANError?) {
                    mainContent.visibility= View.VISIBLE
                    AlertHelper.showAlertDialog(
                        this@AddAccounts,
                        "Failed !",
                        "There is some issue with our server",
                        object : AlertHelper.AlertDialogCallback {
                            override fun onDismiss() {
                                mainContent.visibility = View.VISIBLE
                            }

                            override fun onDone() {
                                mainContent.visibility = View.VISIBLE
                            }
                        }
                    )
                }
            })
    }

    private fun completed(){

        StateContext.addBankAccounts(
          BankAccount(
                holderName.text.toString(),
                bankName.text.toString(),
                ifsc.text.toString(),
                accountNumber.text.toString()
            )
        )
        showToast()
        Realm.getDefaultInstance().executeTransactionAsync{
            val bankAccounts = com.DevAsh.recbusiness.Database.BankAccount(
                holderName.text.toString(),
                bankName.text.toString(),
                ifsc.text.toString(),
                accountNumber.text.toString()
            )
            it.insert(bankAccounts)
        }




    }

    private fun showToast(){
        AlertHelper.showAlertDialog(
            this@AddAccounts,
            "Successful !",
            "Your Bank Account Added Successfully",
            object : AlertHelper.AlertDialogCallback {
                override fun onDismiss() {
                    mainContent.visibility = View.VISIBLE
                    onBackPressed()
                }
                override fun onDone() {
                    mainContent.visibility = View.VISIBLE
                    onBackPressed()
                }
            }
        )
    }

    private fun validate():Boolean{
        if ( holderName.text.isEmpty() || bankName.text.isEmpty() || accountNumber.text.isEmpty() || ifsc.text.isEmpty()){
            AlertHelper.showError("Invalid Credential",this)
            return false
        }

        if (holderName.text.length<3){
            AlertHelper.showError("Invalid Holder Name",this)
            return false
        }

        if (bankName.text.length<3){
            AlertHelper.showError("Invalid Bank Name",this)
            return false
        }
        if (bankName.text.length<3){
            AlertHelper.showError("Invalid Bank Name",this)
            return false
        }
        if (accountNumber.text.length<5){
            AlertHelper.showError("Invalid Account Number",this)
            return false
        }

        if(ifsc.text.length!=11){
            AlertHelper.showError("Invalid IFSC Code",this)
            return false
        }

        return true
    }

    private fun isAccountExist():Boolean{
        val accountList: ArrayList<BankAccount>? = StateContext.model.bankAccounts.value ?: return false
        for( i in accountList!!){
            if(i.accountNumber==accountNumber.text.toString() && i.IFSC==ifsc.text.toString()){
                return true
            }
        }
        return false
    }

    private fun hideKeyboardFrom(context: Context, view: View) {
        val imm: InputMethodManager =
            context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

}