package com.DevAsh.recbusiness.Database;

import io.realm.RealmObject;

public class StateLedger extends RealmObject {

    public String id;
    public Integer amount;

    public StateLedger(){}

    public StateLedger(String id, Integer amount){
        this.id=id;
        this.amount=amount;
    }
}
