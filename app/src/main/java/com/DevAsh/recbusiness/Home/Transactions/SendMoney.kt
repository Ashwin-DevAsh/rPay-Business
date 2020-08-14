package com.DevAsh.recbusiness.Home.Transactions

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.DevAsh.recbusiness.Context.*
import com.DevAsh.recbusiness.Context.UiContext.colors
import com.DevAsh.recbusiness.Helper.AlertHelper
import com.DevAsh.recbusiness.Models.Contacts
import com.DevAsh.recbusiness.R
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONArrayRequestListener
import com.jacksonandroidnetworking.JacksonParserFactory
import kotlinx.android.synthetic.main.activity_send_money.*
import org.json.JSONArray
import java.util.*
import kotlin.collections.ArrayList


class SendMoney : AppCompatActivity() {
    var userAdapter: UserAdapter = UserAdapter(HelperVariables.allUsers,this)
    lateinit var context:Context
    lateinit var searchHandler: Handler
    lateinit var usersContainer: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_money)
        AndroidNetworking.initialize(applicationContext)
        AndroidNetworking.setParserFactory(JacksonParserFactory())

        context=this


        usersContainer = findViewById(R.id.usersContainer)

        Handler().postDelayed({
            getAllUsers()
        },0)

        back.setOnClickListener{
            super.onBackPressed()
        }




        search.addTextChangedListener(object:TextWatcher{
            override fun afterTextChanged(s: Editable?) {
                try {
                    searchHandler.removeCallbacksAndMessages("")
                } catch (e: Exception) {

                }
                searchHandler = Handler()
                searchHandler.postDelayed({
                    val updatedList = ArrayList<Contacts>()
                    for(i in HelperVariables.allUsers) {
                        if(i.name.toLowerCase(Locale.ROOT).contains((s.toString().toLowerCase(Locale.ROOT)))
                            || i.number.contains((s.toString().toLowerCase(Locale.ROOT)))
                        ) {
                            updatedList.add(i)
                        }
                    }
                    userAdapter.updateList(updatedList)
                    if(updatedList.isEmpty()){
                        peopleHeading.visibility=INVISIBLE
                    }else{
                        peopleHeading.visibility=VISIBLE
                    }
                },0)

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

        })


    }

    override fun onResume(){
        if(HelperVariables.allUsers.isEmpty()){
            peopleHeading.visibility=INVISIBLE
        }else{
            peopleHeading.visibility=VISIBLE
        }
        super.onResume()
    }


    private fun getAllUsers(){
        HelperVariables.allUsers.clear()
        AndroidNetworking.get(ApiContext.apiUrl+ApiContext.registrationPort+"/getUsers")
            .setPriority(Priority.IMMEDIATE)
            .build()
            .getAsJSONArray(object :JSONArrayRequestListener{
                override fun onResponse(response: JSONArray?) {
                      if(response!=null)
                      for(i in 0 until response.length()){
                          val user = Contacts(
                               response.getJSONObject(i)["name"].toString()
                              ,"+"+response.getJSONObject(i)["number"].toString()
                              ,response.getJSONObject(i)["id"].toString()
                              ,response.getJSONObject(i)["email"].toString()
                          )
                          if(user.id!=DetailsContext.id) HelperVariables.allUsers.add(user)
                      }
                        usersContainer.layoutManager = LinearLayoutManager(context)
                        userAdapter = UserAdapter(HelperVariables.allUsers,context)
                        usersContainer.adapter = userAdapter

                        if(HelperVariables.allUsers.isEmpty()){
                            peopleHeading.visibility= INVISIBLE
                        }else{
                            peopleHeading.visibility= VISIBLE

                        }
                        mainContent.visibility= VISIBLE
                }

                override fun onError(anError: ANError?) {
                    AlertHelper.showServerError(this@SendMoney)
                }

            })

    }
}


class UserAdapter(private var items : ArrayList<Contacts>, val context: Context) : RecyclerView.Adapter<ViewHolder>() {

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.widget_listtile, parent, false),context)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.title.text = items[position].name
        holder.subtitle.text = items[position].number
        holder.badge.text = items[position].name[0].toString()
        holder.avatarContainer.setBackgroundColor(Color.parseColor(colors[position%colors.size]))

        holder.color = colors[position%colors.size]

        holder.contact = items[position]

        UiContext.loadProfileImage(context,items[position].id,object: LoadProfileCallBack {
            override fun onSuccess() {
                holder.avatarContainer.visibility=View.GONE
                holder.profileImage.visibility = VISIBLE

            }

            override fun onFailure() {
                holder.avatarContainer.visibility=VISIBLE
                holder.profileImage.visibility = View.GONE

            }

        },holder.profileImage)

        if (items[position].name.startsWith("+")) {
            holder.badge.text = items[position].name.subSequence(1, 3)
            holder.badge.textSize = 18F
        }
    }

    fun updateList(updatedList : ArrayList<Contacts>){
         this.items = updatedList
         notifyDataSetChanged()
    }
}

class ViewHolder (view: View,context: Context) : RecyclerView.ViewHolder(view) {
    val title = view.findViewById(R.id.title) as TextView
    val subtitle = view.findViewById(R.id.subtitle) as TextView
    val badge = view.findViewById(R.id.badge) as TextView
    val avatarContainer = view.findViewById(R.id.avatarContainer) as RelativeLayout
    val profileImage: ImageView = view.findViewById(R.id.profile)
    lateinit var color: String

    var contact: Contacts?=null


    init {
        view.setOnClickListener{
           HelperVariables.avatarColor = color
           HelperVariables.selectedUser= Contacts(contact?.name!!,contact?.number!!,contact?.id!!,contact?.email!!)
           startActivity(context,Intent(context,SingleObjectTransaction::class.java),null)
        }
    }
}


