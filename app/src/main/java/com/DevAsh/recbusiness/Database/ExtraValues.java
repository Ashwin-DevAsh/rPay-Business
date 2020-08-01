package com.DevAsh.recbusiness.Database;

import io.realm.RealmObject;

public class ExtraValues extends RealmObject {
    public Boolean isEnteredPasswordOnce = false;

    public Integer getTimeIndex() {
        return timeIndex;
    }

    public void setTimeIndex(Integer timeIndex) {
        this.timeIndex = timeIndex;
    }

    public Integer timeIndex = 0;

    public ExtraValues(){}

    public ExtraValues(Boolean isEnteredPasswordOnce){
        this.isEnteredPasswordOnce = isEnteredPasswordOnce;
    }
}
