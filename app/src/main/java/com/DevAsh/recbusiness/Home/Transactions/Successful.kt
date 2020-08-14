package com.DevAsh.recbusiness.Home.Transactions

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.DevAsh.recbusiness.Context.HelperVariables
import com.DevAsh.recbusiness.Home.HomePage
import com.DevAsh.recbusiness.R
import kotlinx.android.synthetic.main.activity_successfull.*
import kotlinx.android.synthetic.main.confirm_sheet.done


class Successful : AppCompatActivity() {
    lateinit var type:String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_successfull)

        type = intent.getStringExtra("type")!!
        val amount = intent.getStringExtra("amount")
        if(type=="addMoney"){
            message.text = "The amount $amount ${HelperVariables.currency}s has been successfully added in your wallet"
        }else{
            image.setImageDrawable(getDrawable(R.drawable.transaction_successful))
            message.text = "Your transaction of $amount ${HelperVariables.currency}s has been successfully completed"
        }

        val ring: MediaPlayer = MediaPlayer.create(this, R.raw.success)
        ring.start()

        done.setOnClickListener{
            onBackPressed()
        }
    }

    override fun onBackPressed() {
        if(type=="addMoney"){
            try{
                startActivity(Intent(this,HomePage::class.java))
                finish()
            }catch (e:Throwable){
                println(e)
            }

        }else{
            super.onBackPressed()
        }

    }
}