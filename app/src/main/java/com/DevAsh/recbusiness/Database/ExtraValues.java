package com.DevAsh.recbusiness.Database;

import io.realm.RealmObject;

public class ExtraValues extends RealmObject {
    public Boolean isEnteredPasswordOnce = false;

    public ExtraValues(){}

    public ExtraValues(Boolean isEnteredPasswordOnce){
        this.isEnteredPasswordOnce = isEnteredPasswordOnce;
    }
}
