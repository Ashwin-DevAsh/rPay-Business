package com.DevAsh.recbusiness.Database

import io.realm.DynamicRealm
import io.realm.RealmMigration

open class Migrations : RealmMigration {
    override fun migrate(realm: DynamicRealm, oldVersion: Long, newVersion: Long) {
        val schema = realm.schema

        println(oldVersion)
        when (oldVersion) {
            3L -> {
                val credentials = schema.get("Credentials")
                credentials!!.addField("test", String::class.java).transform{ obj ->
                    obj.set("test","hello")

                }
            }
            4L -> {
                val credentials = schema.get("Credentials")
                credentials!!.removeField("test")

                val stateLedger = schema.create("StateLedger")
                stateLedger.addField("id", String::class.java).transform { obj->obj.set("id","null") }
                stateLedger.addField("amount", Integer::class.java).transform { obj->obj.set("amount","null") }

            }
            5L -> {
                val checkPoint = schema.create("CheckPoint")
                checkPoint.addField("transactionCheckPoint",Integer::class.java)
                checkPoint.addField("usersCheckPoint",Integer::class.java)

            }
            6L -> {
                val checkPoint = schema.get(("CheckPoint"))
                checkPoint!!.removeField("transactionCheckPoint")
                checkPoint.removeField("usersCheckPoint")
                checkPoint.addField("checkPoint",Integer::class.java)
            }
            7L -> {
                val checkPoint = schema.create("RecentContacts")
                checkPoint.addField("name",String::class.java)
                checkPoint.addField("number",String::class.java)
                checkPoint.addField("freq",Integer::class.java)

            }
            8L -> {
                schema.remove("RecentContacts")
            }
            9L -> {
                val extraValues= schema.create("ExtraValues")
                extraValues.addField("isEnteredPasswordOnce",Boolean::class.javaObjectType)
            }
            10L ->{
                schema.get("ExtraValues")?.addField("timeIndex",Integer::class.javaObjectType)?.transform {
                    obj -> obj.set("timeIndex",0)
                }
            }
            11L -> {
                val bankAccounts= schema.create("BankAccount")
                bankAccounts.addField("holderName",String::class.javaObjectType)
                bankAccounts.addField("bankName",String::class.javaObjectType)
                bankAccounts.addField("IFSC",String::class.javaObjectType)
                bankAccounts.addField("accountNumber",String::class.javaObjectType)
            }
            12L->{
                val ledger = schema.create("Ledger")
                ledger.addField("id",String::class.javaObjectType)
                ledger.addField("balance",String::class.javaObjectType)
            }
        }

    }

    override fun hashCode(): Int {
        return Migrations::class.java.hashCode()
    }

    override fun equals(`object`: Any?): Boolean {
        return if (`object` == null) {
            false
        } else `object` is Migrations
    }
}