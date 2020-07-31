package com.DevAsh.recbusiness.Home.Transactions

import android.Manifest.permission.USE_FINGERPRINT
import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.fingerprint.FingerprintManager
import android.os.*
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.DevAsh.recbusiness.Context.ApiContext
import com.DevAsh.recbusiness.Context.DetailsContext
import com.DevAsh.recbusiness.Context.StateContext
import com.DevAsh.recbusiness.Context.TransactionContext
import com.DevAsh.recbusiness.Context.TransactionContext.needToPay
import com.DevAsh.recbusiness.Database.ExtraValues
import com.DevAsh.recbusiness.Database.RealmHelper
import com.DevAsh.recbusiness.Helper.AlertHelper
import com.DevAsh.recbusiness.Helper.PasswordHashing
import com.DevAsh.recbusiness.Home.Recovery.RecoveryOptions
import com.DevAsh.recbusiness.Models.Merchant
import com.DevAsh.recbusiness.Models.Transaction
import com.DevAsh.recbusiness.R
import com.DevAsh.recbusiness.SplashScreen
import com.DevAsh.recbusiness.Sync.SocketHelper
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_amount_prompt.back
import kotlinx.android.synthetic.main.activity_amount_prompt.done
import kotlinx.android.synthetic.main.activity_password_prompt.*
import org.json.JSONObject
import java.io.IOException
import java.security.*
import java.security.cert.CertificateException
import java.text.DecimalFormat
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.NoSuchPaddingException
import javax.crypto.SecretKey
import kotlin.collections.ArrayList


class PasswordPrompt : AppCompatActivity() {

    var context: Context = this
    private lateinit var keyStore: KeyStore
    private lateinit var keyGenerator: KeyGenerator
    private val KEY_NAME = ApiContext.qrKey
    lateinit var cipher:Cipher
    lateinit var  fingerprintManager:FingerprintManager
    lateinit var keyguardManager:KeyguardManager
    var isTooManyAttempts = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password_prompt)
        RealmHelper.init(context)

        val extraValues = Realm.getDefaultInstance().where(ExtraValues::class.java).findFirst()


        if (checkLockScreen()) {

            generateKey()
            if (initCipher()) {
                var cryptoObject:FingerprintManager.CryptoObject
                cipher.let {
                    cryptoObject = FingerprintManager.CryptoObject(it)
                }
                val helper = FingerprintHelper(this,object :CallBack{
                    override fun onSuccess(){
                        if(extraValues==null || !extraValues.isEnteredPasswordOnce!!){
                            animateBell("Enter password to enable fingerprint")
                        }else{
                            startVibrate(100)
                            fingerPrint.setColorFilter(Color.parseColor("#4BB543"))
                            errorMessage.setTextColor(Color.parseColor("#4BB543"))
                            errorMessage.text = "Authentication success !"
                            Handler().postDelayed({
                                transaction()
                            },200)
                        }
                    }

                    override fun onFailed(){
                        startVibrate(200)
                        animateBell()
                    }

                    override fun onDirtyRead(){
                        startVibrate(200)
                        animateBell("Could'nt process fingerprint")
                    }

                    override fun onTooManyAttempt() {
                        isTooManyAttempts=true
                        handler.removeCallbacksAndMessages(1)
                        fingerPrint.setColorFilter(Color.GRAY)
                        errorMessage.setTextColor(Color.GRAY)
                        errorMessage.text = "Fingerprint disabled"
                    }
                })

                if (fingerprintManager != null && cryptoObject != null) {
                    helper.startAuth(fingerprintManager, cryptoObject)
                }
            }
        }



        badge.setBackgroundColor(Color.parseColor(TransactionContext.avatarColor))
        badge.text = TransactionContext.selectedUser?.name?.substring(0,1)
        type.text = "Paying to ${TransactionContext.selectedUser?.name}"
        selectedUserName.text = TransactionContext.selectedUser?.number
        amount.text = TransactionContext.amount

        back.setOnClickListener{
            super.onBackPressed()
        }

        forget.setOnClickListener{
            startActivity(Intent(this, RecoveryOptions::class.java))
        }


        done.setOnClickListener{v->
            if(PasswordHashing.decryptMsg(DetailsContext.password!!)==password.text.toString()){
                hideKeyboardFrom(context,v)
                Handler().postDelayed({
                    transaction()
                },500)

                if(extraValues==null || extraValues.isEnteredPasswordOnce!!){
                    Realm.getDefaultInstance().executeTransactionAsync{
                        val extraValues = ExtraValues(true)
                        it.insert(extraValues)
                    }
                }
            }else{
                println(PasswordHashing.decryptMsg(DetailsContext.password!!)+" actual password")
                println(password.text.toString()+" actual password")
//                AlertHelper.showAlertDialog(this,"Incorrect Password !","The password you entered is incorrect, kindly check your password")
                AlertHelper.showError("Invalid Password",this@PasswordPrompt)
            }

        }


    }

    fun transaction(){
        if(needToPay){
            loadingScreen.visibility= View.VISIBLE
            needToPay=false

            AndroidNetworking.post(ApiContext.apiUrl + ApiContext.paymentPort + "/pay")
                .setContentType("application/json; charset=utf-8")
                .addHeaders("jwtToken", DetailsContext.token)
                .addApplicationJsonBody(object{
                    var from = DetailsContext.id
                    var to = TransactionContext.selectedUser?.id.toString().replace("+","")
                    var amount = TransactionContext.amount
                    var toName = TransactionContext.selectedUser?.name.toString()
                    var fromName = DetailsContext.name
                })
                .setPriority(Priority.IMMEDIATE)
                .build()
                .getAsJSONObject(object : JSONObjectRequestListener {
                    override fun onResponse(response: JSONObject?) {
                        if(response?.get("message")=="done"){
                            transactionSuccessful()
                            AndroidNetworking.get(ApiContext.apiUrl + ApiContext.paymentPort + "/getMyState?id=${DetailsContext.id}")
                                .addHeaders("jwtToken",DetailsContext.token)
                                .setPriority(Priority.IMMEDIATE)
                                .build()
                                .getAsJSONObject(object: JSONObjectRequestListener {
                                    override fun onResponse(response: JSONObject?) {
                                        val jsonData = JSONObject()
                                        jsonData.put("to",
                                            TransactionContext.selectedUser?.id.toString().replace("+",""))
                                        SocketHelper.socket?.emit("notifyPayment",jsonData)
                                        val balance = response?.getInt("Balance")
                                        StateContext.currentBalance = balance!!
                                        val formatter = DecimalFormat("##,##,##,##,##,##,##,###")
                                        StateContext.setBalanceToModel(formatter.format(balance))
                                        val transactionObjectArray = response?.getJSONArray("Transactions")
                                        val transactions = ArrayList<Transaction>()
                                        for (i in 0 until transactionObjectArray!!.length()) {
                                            transactions.add(
                                                0, Transaction(
                                                    name = if (transactionObjectArray.getJSONObject(i)["From"] == DetailsContext.id)
                                                        transactionObjectArray.getJSONObject(i)["ToName"].toString()
                                                    else transactionObjectArray.getJSONObject(i)["FromName"].toString(),
                                                    id = if (transactionObjectArray.getJSONObject(i)["From"] == DetailsContext.id)
                                                        transactionObjectArray.getJSONObject(i)["To"].toString()
                                                    else transactionObjectArray.getJSONObject(i)["From"].toString(),
                                                    amount = transactionObjectArray.getJSONObject(i)["Amount"].toString(),
                                                    time = SplashScreen.dateToString(
                                                        transactionObjectArray.getJSONObject(
                                                            i
                                                        )["TransactionTime"].toString()
                                                    ),
                                                    type = if (transactionObjectArray.getJSONObject(i)["From"] == DetailsContext.id)
                                                        "Send"
                                                    else "Received",
                                                    transactionId =  transactionObjectArray.getJSONObject(i)["TransactionID"].toString(),
                                                    isGenerated = transactionObjectArray.getJSONObject(i).getBoolean("IsGenerated")
                                                )
                                            )
                                        }
                                        StateContext.initAllTransaction(transactions)
                                    }
                                    override fun onError(anError: ANError?) {
                                        AlertHelper.showServerError(this@PasswordPrompt)
                                    }

                                })

                        }else{
                            AlertHelper.showAlertDialog(this@PasswordPrompt,
                                "Failed !",
                                "your transaction of ${TransactionContext.amount} ${TransactionContext.currency} is failed. if any amount debited it will refund soon",
                                object: AlertHelper.AlertDialogCallback {
                                    override fun onDismiss() {
                                        loadingScreen.visibility=View.INVISIBLE
                                        onBackPressed()
                                    }

                                    override fun onDone() {
                                        loadingScreen.visibility=View.INVISIBLE
                                        onBackPressed()
                                    }
                                }
                            )
                        }
                    }

                    override fun onError(anError: ANError?) {
                        loadingScreen.visibility= View.VISIBLE
                        AlertHelper.showAlertDialog(this@PasswordPrompt,
                            "Failed !",
                            "your transaction of ${TransactionContext.amount} ${TransactionContext.currency} is failed. if any amount debited it will refund soon",
                            object: AlertHelper.AlertDialogCallback {
                                override fun onDismiss() {
                                    loadingScreen.visibility=View.INVISIBLE
                                    onBackPressed()
                                }

                                override fun onDone() {
                                    loadingScreen.visibility=View.INVISIBLE
                                    onBackPressed()
                                }
                            }
                        )
                    }

                })
        }
    }


    fun transactionSuccessful(){
        val intent = Intent(this,Successful::class.java)
        intent.putExtra("type","transaction")
        intent.putExtra("amount",TransactionContext.amount)
        Handler().postDelayed({
            addRecent()
        },0)
        startActivity(intent)
        finish()
    }

    private fun addRecent(){
        val account = Merchant(TransactionContext.selectedUser?.name!!,TransactionContext.selectedUser?.number!!,TransactionContext.selectedUser?.id!!)
        StateContext.addRecentContact(account)
    }

    private fun hideKeyboardFrom(context: Context, view: View) {
        val imm: InputMethodManager =
            context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun checkLockScreen(): Boolean {
        keyguardManager = getSystemService(Context.KEYGUARD_SERVICE)
                as KeyguardManager
         fingerprintManager = getSystemService(Context.FINGERPRINT_SERVICE)
                as FingerprintManager
        if (!keyguardManager.isKeyguardSecure) {
           AlertHelper.showError(
                "Lock screen security not enabled",
               this)
            return false
        }

        if (ActivityCompat.checkSelfPermission(this,
                USE_FINGERPRINT) !=
            PackageManager.PERMISSION_GRANTED) {
          AlertHelper.showError(
                "Permission not enabled (Fingerprint)",
              this)

            return false
        }

        if (!fingerprintManager.hasEnrolledFingerprints()) {
            AlertHelper.showError(
                "No fingerprint registered, please register",
                this)
            return false
        }
        return true
    }

    private fun generateKey() {
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore")
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(
                "Failed to get KeyGenerator instance", e)
        } catch (e: NoSuchProviderException) {
            throw RuntimeException("Failed to get KeyGenerator instance", e)
        }

        try {
            keyStore.load(null)
            keyGenerator.init(
                KeyGenParameterSpec.Builder(KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(
                        KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build())
            keyGenerator.generateKey()
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        } catch (e: InvalidAlgorithmParameterException) {
            throw RuntimeException(e)
        } catch (e: CertificateException) {
            throw RuntimeException(e)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    private fun initCipher(): Boolean {

        try {
            cipher = Cipher.getInstance(
                KeyProperties.KEY_ALGORITHM_AES + "/"
                        + KeyProperties.BLOCK_MODE_CBC + "/"
                        + KeyProperties.ENCRYPTION_PADDING_PKCS7)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("Failed to get Cipher", e)
        } catch (e: NoSuchPaddingException) {
            throw RuntimeException("Failed to get Cipher", e)
        }

        try {
            keyStore.load(null)
            val key = keyStore.getKey(KEY_NAME, null) as SecretKey
            cipher.init(Cipher.ENCRYPT_MODE, key)
            return true
        } catch (e: KeyPermanentlyInvalidatedException) {
            return false
        } catch (e: KeyStoreException) {
            throw RuntimeException("Failed to init Cipher", e)
        } catch (e: CertificateException) {
            throw RuntimeException("Failed to init Cipher", e)
        } catch (e: UnrecoverableKeyException) {
            throw RuntimeException("Failed to init Cipher", e)
        } catch (e: IOException) {
            throw RuntimeException("Failed to init Cipher", e)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("Failed to init Cipher", e)
        } catch (e: InvalidKeyException) {
            throw RuntimeException("Failed to init Cipher", e)
        }
    }

    private fun startVibrate(time:Long){
        val v: Vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(time, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            //deprecated in API 26
            v.vibrate(time)
        }
    }

    val handler= Handler()

    fun animateBell(message:String="Authentication Failed") {

        val imgBell: ImageView = findViewById<View>(R.id.fingerPrint) as ImageView
        imgBell.setColorFilter(Color.RED)
        errorMessage.text=message
        errorMessage.visibility=View.VISIBLE
        errorMessage.setTextColor(Color.RED)

        val shake: Animation = AnimationUtils.loadAnimation(this, R.anim.shakeanimation)
        imgBell.animation = shake
        imgBell.startAnimation(shake)

        handler.postDelayed({
            if(!isTooManyAttempts && !message.startsWith("Enter")){
                imgBell.setColorFilter(resources.getColor(R.color.textDark))
                errorMessage.text="Touch the fingerprint sensor"
                errorMessage.setTextColor(resources.getColor(R.color.textDark))
             }
        },1500)
    }
}

class FingerprintHelper(private val appContext: Activity, private val callBack: CallBack) : FingerprintManager.AuthenticationCallback() {

    private lateinit var cancellationSignal: CancellationSignal

    fun startAuth(manager: FingerprintManager,
                  cryptoObject: FingerprintManager.CryptoObject) {

        cancellationSignal = CancellationSignal()

        if (ActivityCompat.checkSelfPermission(appContext,
                USE_FINGERPRINT) !=
            PackageManager.PERMISSION_GRANTED) {
            return
        }
        manager.authenticate(cryptoObject, cancellationSignal, 0, this, null)
    }

    override fun onAuthenticationError(errMsgId: Int, errString: CharSequence) {
        callBack.onTooManyAttempt()
    }

    override fun onAuthenticationHelp(helpMsgId: Int,helpString: CharSequence) {
        callBack.onDirtyRead()

    }

    override fun onAuthenticationFailed() {
        callBack.onFailed()
    }

    override fun onAuthenticationSucceeded(
        result: FingerprintManager.AuthenticationResult) {
        callBack.onSuccess()
    }
}

interface CallBack{
    fun onSuccess()
    fun onFailed()
    fun onDirtyRead()
    fun onTooManyAttempt()
}