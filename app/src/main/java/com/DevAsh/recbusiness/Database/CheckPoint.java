package com.DevAsh.recbusiness.Database;

import io.realm.RealmObject;

public class CheckPoint extends RealmObject {

    public Integer checkPoint;

    public CheckPoint(){}

    public CheckPoint(Integer checkPoint){
        this.checkPoint=checkPoint;
    }

}
