package com.DevAsh.recbusiness.Home.Withdraw


import android.Manifest
import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.fingerprint.FingerprintManager
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import androidx.core.app.ActivityCompat
import com.DevAsh.recbusiness.Context.ApiContext
import com.DevAsh.recbusiness.Context.DetailsContext
import com.DevAsh.recbusiness.Context.HelperVariables
import com.DevAsh.recbusiness.Context.StateContext
import com.DevAsh.recbusiness.Database.ExtraValues
import com.DevAsh.recbusiness.Helper.AlertHelper
import com.DevAsh.recbusiness.Helper.PasswordHashing
import com.DevAsh.recbusiness.Helper.TransactionsHelper
import com.DevAsh.recbusiness.Home.Recovery.RecoveryOptions
import com.DevAsh.recbusiness.Home.Transactions.CallBack
import com.DevAsh.recbusiness.Home.Transactions.FingerprintHelper
import com.DevAsh.recbusiness.Home.Transactions.Successful
import com.DevAsh.recbusiness.R
import com.DevAsh.recbusiness.Sync.SocketHelper
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_withdraw_password_prompt.amount
import kotlinx.android.synthetic.main.activity_withdraw_password_prompt.back
import kotlinx.android.synthetic.main.activity_withdraw_password_prompt.done
import kotlinx.android.synthetic.main.activity_withdraw_password_prompt.errorMessage
import kotlinx.android.synthetic.main.activity_withdraw_password_prompt.fingerPrint
import kotlinx.android.synthetic.main.activity_withdraw_password_prompt.forget
import kotlinx.android.synthetic.main.activity_withdraw_password_prompt.loadingScreen
import kotlinx.android.synthetic.main.activity_withdraw_password_prompt.name
import kotlinx.android.synthetic.main.activity_withdraw_password_prompt.number
import kotlinx.android.synthetic.main.activity_withdraw_password_prompt.password
import org.json.JSONObject
import java.io.IOException
import java.security.*
import java.security.cert.CertificateException
import java.text.DecimalFormat
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.NoSuchPaddingException
import javax.crypto.SecretKey

class WithdrawPasswordPrompt : AppCompatActivity() {

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
        setContentView(R.layout.activity_withdraw_password_prompt)

        val extraValues = Realm.getDefaultInstance().where(ExtraValues::class.java).findFirst()

        if (checkLockScreen()) {
            generateKey()
            if (initCipher()) {
                var cryptoObject:FingerprintManager.CryptoObject
                cipher.let {
                    cryptoObject = FingerprintManager.CryptoObject(it)
                }
                val helper = FingerprintHelper(this,object : CallBack {
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


        name.text = HelperVariables.selectedAccount?.bankName
        number.text = HelperVariables.selectedAccount?.accountNumber
        amount.text = HelperVariables.withdrawAmount

        back.setOnClickListener{
            super.onBackPressed()
        }

        forget.setOnClickListener{
            startActivity(Intent(this, RecoveryOptions::class.java))
        }

        done.setOnClickListener { view ->
            if(PasswordHashing.decryptMsg(DetailsContext.password!!)==password.text.toString()){
                hideKeyboardFrom(this,view)
                Handler().postDelayed({
                    transaction()
                },500)

                if(extraValues==null || extraValues.isEnteredPasswordOnce!!){
                    Realm.getDefaultInstance().executeTransactionAsync{
                        val extraValues = ExtraValues(true,StateContext.timeIndex)
                        it.insert(extraValues)
                    }
                }
            }else{
                println(PasswordHashing.decryptMsg(DetailsContext.password!!)+" actual password")
                println(password.text.toString()+" actual password")
                AlertHelper.showError("Invalid Password",this@WithdrawPasswordPrompt)
            }
        }

    }

    fun transaction(){
        if(HelperVariables.needToPay){
            loadingScreen.visibility= View.VISIBLE
            HelperVariables.needToPay =false

            AndroidNetworking.post(ApiContext.apiUrl + ApiContext.paymentPort + "/withdraw")
                .setContentType("application/json; charset=utf-8")
                .addHeaders("jwtToken", DetailsContext.token)
                .addApplicationJsonBody(object{
                    var from = object {
                        var id = DetailsContext.id
                        var name = DetailsContext.storeName
                        var number = DetailsContext.phoneNumber
                        var email = DetailsContext.email
                    }
                    var to = object {
                        var id =  HelperVariables.selectedAccount?.accountNumber
                        var name =  HelperVariables.selectedAccount?.bankName
                        var number =  HelperVariables.selectedAccount?.IFSC
                        var email =  HelperVariables.selectedAccount?.holderName
                    }
                    var amount = HelperVariables.withdrawAmount
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
                                            HelperVariables.selectedUser?.id.toString().replace("+",""))
                                        SocketHelper.socket?.emit("notifyPayment",jsonData)
                                        val balance = response?.getInt("Balance")
                                        StateContext.currentBalance = balance!!
                                        val formatter = DecimalFormat("##,##,##,##,##,##,##,###")
                                        StateContext.setBalanceToModel(formatter.format(balance))
                                        val transactionObjectArray = response.getJSONArray("Transactions")
                                        TransactionsHelper.addTransaction(transactionObjectArray)
                                    }
                                    override fun onError(anError: ANError?) {
                                        AlertHelper.showServerError(this@WithdrawPasswordPrompt)
                                    }
                                })

                        }else{
                            AlertHelper.showAlertDialog(this@WithdrawPasswordPrompt,
                                "Failed !",
                                "your transaction of ${HelperVariables.withdrawAmount} ${HelperVariables.currency} is failed. if any amount debited it will refund soon",
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
                        AlertHelper.showAlertDialog(this@WithdrawPasswordPrompt,
                            "Failed !",
                            "your transaction of ${HelperVariables.withdrawAmount} ${HelperVariables.currency} is failed. if any amount debited it will refund soon",
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
        val intent = Intent(this, Successful::class.java)
        intent.putExtra("type","withdraw")
        intent.putExtra("amount",HelperVariables.withdrawAmount)
        startActivity(intent)
        finish()
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
                Manifest.permission.USE_FINGERPRINT
            ) !=
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



    private fun hideKeyboardFrom(context: Context, view: View) {
        val imm: InputMethodManager =
            context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}