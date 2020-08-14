package com.DevAsh.recbusiness.Database;

import io.realm.RealmObject;

public class BankAccount extends RealmObject {

    public BankAccount(){}

    public BankAccount(String holderName, String bankName, String IFSC, String accountNumber) {
        this.holderName = holderName;
        this.bankName = bankName;
        this.IFSC = IFSC;
        this.accountNumber = accountNumber;
    }

    public String holderName;
    public String bankName;
    public String IFSC;
    public String accountNumber;

}
