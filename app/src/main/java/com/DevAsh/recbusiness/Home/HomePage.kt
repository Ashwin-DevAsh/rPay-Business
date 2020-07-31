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
import androidx.recyclerview.widget.RecyclerView
import com.DevAsh.recbusiness.Context.DetailsContext
import com.DevAsh.recbusiness.Context.StateContext
import com.DevAsh.recbusiness.Context.TransactionContext
import com.DevAsh.recbusiness.Context.UiContext.colors
import com.DevAsh.recbusiness.Home.Transactions.AddMoney
import com.DevAsh.recbusiness.Home.Transactions.Contacts
import com.DevAsh.recbusiness.Home.Transactions.SendMoney
import com.DevAsh.recbusiness.Home.Transactions.SingleObjectTransaction
import com.DevAsh.recbusiness.Models.Merchant
import com.DevAsh.recbusiness.R
import com.DevAsh.recbusiness.Sync.SocketHelper
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.iid.FirebaseInstanceId
import com.synnapps.carouselview.CarouselView
import com.synnapps.carouselview.ImageListener
import kotlinx.android.synthetic.main.activity_home_page.*


class HomePage : AppCompatActivity() {

    var context: Context = this

    lateinit var carouselView: CarouselView
    var sampleImages = intArrayOf(
        R.drawable.banner_1,
        R.drawable.banner_2
    )

//    lateinit var peopleViewAdapter: PeopleViewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_page)


//        merchantHolder.layoutManager = GridLayoutManager(context, 3)
//        recent.layoutManager = GridLayoutManager(context, 3)

        val bottomDown: Animation = AnimationUtils.loadAnimation(
            context,
            R.anim.button_down
        )
        val bottomUp: Animation = AnimationUtils.loadAnimation(
            context,
            R.anim.button_up
        )

        val hiddenPanel = findViewById<CardView>(R.id.scanContainer)


        scroller.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            if (scrollY >500) {
                if(hiddenPanel.visibility==View.VISIBLE){
                    hiddenPanel.startAnimation(bottomDown)
                    hiddenPanel.visibility=View.GONE
                }

            }
            if(scrollY < 500){
                if(hiddenPanel.visibility==View.GONE){
                    hiddenPanel.visibility=View.VISIBLE
                    hiddenPanel.startAnimation(bottomUp)
                }
            }
        }

//        merchantHolder.adapter = MerchantViewAdapter(arrayListOf(
//            Merchant("Hut Cafe","@hutcafe","@hutcafe",R.drawable.hut_cafe),
//            Merchant("Tamil cafe","@tamilcafe","@tamilcafe",R.drawable.tamil_cafe),
//            Merchant("Rec Mart","@recmart","@recmart",R.drawable.rec_mart),
//            Merchant("A2Z","@a2z","@a2z",R.drawable.ug),
//            Merchant("CCD","@ccd","@ccd",R.drawable.ccd),
//            Merchant("Rec bill","@recbill","@recbill",R.drawable.rec_bill),
//            Merchant("Fine Payment","@finepayment","@finepayment",R.drawable.fine),
//            Merchant("zerox","@zerox","@zerox",R.drawable.xrox),
//            Merchant("More","1234567890","1234567890",R.drawable.more)
//            ),context,BottomSheetMerchant(context))
//        peopleViewAdapter = PeopleViewAdapter(arrayListOf(),this,BottomSheetPeople(context))
//        recent.adapter = peopleViewAdapter

        val imageListener =
            ImageListener { position, imageView ->
                imageView.setImageResource(sampleImages[position])
                imageView.scaleType=ImageView.ScaleType.CENTER_INSIDE
                imageView.adjustViewBounds=true
                imageView.setBackgroundColor(Color.WHITE)
            }

        carouselView = findViewById(R.id.carouselView)
        carouselView.pageCount = sampleImages.size
        carouselView.setImageListener(imageListener)

//        startService(Intent(this, SocketService::class.java))
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

        val balanceObserver = Observer<String> { currentBalance ->
             balance.text = currentBalance
        }

        val recentContactsObserver = Observer<ArrayList<Merchant>> {recentContacts->
            println("Updating...")
            val newList = try{
                ArrayList(recentContacts.subList(0,8))
            }catch (e:Throwable){
                ArrayList(recentContacts.subList(0,recentContacts.size))
            }
            if(newList.size==8){
                newList.add(Merchant("More","","",R.drawable.more))
            }

//            peopleViewAdapter.updateList(ArrayList(newList))
//            if(recentContacts.size!=0){
//                info.visibility = View.GONE
//                recent.visibility = View.VISIBLE
//            }else{
//                info.visibility = View.VISIBLE
//                recent.visibility = View.GONE
//            }
        }

        fun getText():String{
            var name = DetailsContext.storeName
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

        greetings.text=(getText())
        StateContext.model.currentBalance.observe(this,balanceObserver)
        StateContext.model.recentContacts.observe(this,recentContactsObserver)




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

        scan.setOnClickListener{
            val permissions = arrayOf(android.Manifest.permission.CAMERA)
            if(packageManager.checkPermission(android.Manifest.permission.CAMERA,context.packageName)==PackageManager.PERMISSION_GRANTED ){
                startActivity(Intent(context,
                    QrScanner::class.java))
            }else{
                ActivityCompat.requestPermissions(this, permissions,1)
            }

        }

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

    override fun onBackPressed() {
        val startMain = Intent(Intent.ACTION_MAIN)
        startMain.addCategory(Intent.CATEGORY_HOME)
        startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(startMain)
    }

//    class BottomSheetMerchant(val context:Context):BottomSheet{
//        val mBottomSheetDialog = BottomSheetDialog(context)
//        val sheetView: View = LayoutInflater.from(context).inflate(R.layout.all_merchants, null)
//        init {
//            val merchantContainer = sheetView.findViewById<RecyclerView>(R.id.merchantContainer)
//            merchantContainer.layoutManager = GridLayoutManager(context, 3)
//            merchantContainer.adapter = MerchantViewAdapter(arrayListOf(
//                Merchant("Hut Cafe","@hutcafe","@hutcafe",R.drawable.hut_cafe),
//                Merchant("Tamil cafe","@tamilcafe","@tamilcafe",R.drawable.tamil_cafe),
//                Merchant("Rec Mart","@recmart","@recmart",R.drawable.rec_mart),
//                Merchant("A2Z","@a2z","@a2z",R.drawable.ug),
//                Merchant("CCD","@ccd","@ccd",R.drawable.ccd),
//                Merchant("Rec bill","@recbill","@recbill",R.drawable.rec_bill),
//                Merchant("Fine Payment","@finepayment","@finepayment",R.drawable.fine),
//                Merchant("zerox","@zerox","@zerox",R.drawable.xrox),
//                Merchant("Hut Cafe","@hutcafe","@hutcafe",R.drawable.hut_cafe),
//                Merchant("Tamil cafe","@tamilcafe","@tamilcafe",R.drawable.tamil_cafe),
//                Merchant("Rec Mart","@recmart","@recmart",R.drawable.rec_mart),
//                Merchant("A2Z","@a2z","@a2z",R.drawable.ug),
//                Merchant("CCD","@ccd","@ccd",R.drawable.ccd),
//                Merchant("Rec bill","@recbill","@recbill",R.drawable.rec_bill),
//                Merchant("Fine Payment","@finepayment","@finepayment",R.drawable.fine),
//                Merchant("zerox","@zerox","@zerox",R.drawable.xrox)
//            ),context,this)
//            mBottomSheetDialog.setContentView(sheetView)
//        }
//        override fun openBottomSheet(){
//            mBottomSheetDialog.show()
//        }
//
//        override fun closeBottomSheet() {
//            mBottomSheetDialog.cancel()
//        }
//    }



//    class BottomSheetPeople(val context:Context):BottomSheet{
//        val mBottomSheetDialog = BottomSheetDialog(context)
//        val sheetView: View = LayoutInflater.from(context).inflate(R.layout.all_merchants, null)
//        init {
//            val peopleContainer = sheetView.findViewById<RecyclerView>(R.id.merchantContainer)
//            val title = sheetView.findViewById<TextView>(R.id.title)
//            title.text="Peoples"
//            peopleContainer.layoutManager = GridLayoutManager(context, 3)
//            peopleContainer.adapter = PeopleViewAdapter(StateContext.model.recentContacts.value!!,context,this)
//            mBottomSheetDialog.setContentView(sheetView)
//        }
//        override fun openBottomSheet(){
//            mBottomSheetDialog.show()
//        }
//
//        override fun closeBottomSheet() {
//            mBottomSheetDialog.cancel()
//        }
//    }





}

class MerchantViewAdapter(private var items : ArrayList<Merchant>, val context: Context,val openBottomSheetCallback: BottomSheet?) : RecyclerView.Adapter<MerchantViewHolder>() {

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MerchantViewHolder {
        return MerchantViewHolder(LayoutInflater.from(context).inflate(R.layout.merchant_avatar, parent, false),context,openBottomSheetCallback)
    }

    override fun onBindViewHolder(holder: MerchantViewHolder, position: Int) {
        holder.title.text = items[position].name
        holder.merchant = items[position]

        if(items[position].image!=null){
            holder.badge.setImageDrawable(context.getDrawable(items[position].image!!))
        }

    }
    fun updateList(updatedList : ArrayList<Merchant>){
        this.items = updatedList
        notifyDataSetChanged()
    }
}

class MerchantViewHolder(view: View, context: Context,bottomSheetCallback: BottomSheet?) : RecyclerView.ViewHolder(view) {
    val title = view.findViewById(R.id.title) as TextView
    val badge = view.findViewById(R.id.avatar) as ImageView
    lateinit var merchant:Merchant


    init {

        view.setOnClickListener{
            if(merchant.name=="More"){
                  bottomSheetCallback?.openBottomSheet()
            }else{
                bottomSheetCallback?.closeBottomSheet()
                TransactionContext.selectedUser= Contacts(merchant.name,merchant.phoneNumber,merchant.id)
                TransactionContext.avatarColor = "#035aa6"
                context.startActivity(
                    Intent(context, SingleObjectTransaction::class.java)
                )

            }
        }
    }
}



//class PeopleViewAdapter(private var items : ArrayList<Merchant>, val context: Context,val openBottomSheetCallback: BottomSheet?) : RecyclerView.Adapter<PeopleViewHolder>() {
//
//
//    override fun getItemCount(): Int {
//        return items.size
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PeopleViewHolder {
//        return PeopleViewHolder(LayoutInflater.from(context).inflate(R.layout.people_avatar, parent, false),context,openBottomSheetCallback)
//    }
//
//    override fun onBindViewHolder(holder: PeopleViewHolder, position: Int) {
//        holder.title.text = items[position].name
//        holder.people = items[position]
//
//
//            holder.title.text = items[position].name
//            holder.badge.text = items[position].name[0].toString()
//            holder.badge.setBackgroundColor(Color.parseColor(colors[position%colors.size]))
//            holder.color = colors[position%colors.size]
//            if(items[position].name=="More" ){
//                holder.avatar.visibility = View.VISIBLE
//                holder.badge.visibility=View.GONE
//            }else{
//                holder.avatar.visibility = View.GONE
//                holder.badge.visibility=View.VISIBLE
//            }
//
//        if (items[position].name.startsWith("+")) {
//                holder.badge.text = items[position].name.subSequence(1, 3)
//                holder.badge.textSize = 18F
//            }
//
//    }
//
//    fun updateList(updatedList : ArrayList<Merchant>){
//        this.items = updatedList
//        notifyDataSetChanged()
//    }
//}

//class PeopleViewHolder (view: View, context: Context,val openBottomSheetCallback: BottomSheet?) : RecyclerView.ViewHolder(view) {
//    val title = view.findViewById(R.id.title) as TextView
//    val badge = view.findViewById(R.id.badge) as TextView
//    val avatar = view.findViewById(R.id.avatar) as ImageView
//    lateinit var color: String
//    lateinit var people: Merchant
//
//    init {
//        view.setOnClickListener{
//            if(people.name=="More"){
//                val imageView = ImageView(context)
//                imageView.setImageDrawable(context.getDrawable(R.drawable.more))
//                openBottomSheetCallback?.openBottomSheet()
//
//
//            }else{
//                openBottomSheetCallback?.closeBottomSheet()
//                TransactionContext.selectedUser= Contacts(people.name,people.phoneNumber,people.id)
//                TransactionContext.avatarColor = color
//                context.startActivity(
//                    Intent(context, SingleObjectTransaction::class.java)
//                )
//            }
//
//        }
//    }
//}

interface BottomSheet{
    fun openBottomSheet()
    fun closeBottomSheet()
}
