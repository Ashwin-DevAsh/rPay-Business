package com.DevAsh.recbusiness.Home.Transactions
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.DevAsh.recbusiness.Helper.AlertHelper
import com.DevAsh.recbusiness.R
import kotlinx.android.synthetic.main.activity_add_money.*



class AddMoney : AppCompatActivity(){
    var amount:String =""
    var context = this
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_add_money)
        done.setOnClickListener{v->
            val amountEditText = (findViewById<EditText>(R.id.amount))
            amount = amountEditText.text.toString()
            try{
                if(amount !="" && Integer.parseInt(amount)>0){
                    val intent = Intent(context,AddingOptions::class.java)
                    intent.putExtra("amount",amount)
                    startActivity(intent)

                }else{
                    AlertHelper.showError("Invalid Amount", this)
                }
            }catch (e:java.lang.Exception){
                AlertHelper.showError("Invalid Amount", this)
            }

        }

        back.setOnClickListener{
            onBackPressed()
        }
        cancel.setOnClickListener{
            onBackPressed()

        }
    }







}