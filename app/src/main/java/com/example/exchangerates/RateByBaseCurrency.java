package com.example.exchangerates;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Map;


public class RateByBaseCurrency {
    @SerializedName("rates")
    @Expose
    private Map<String, String> rates;
    @SerializedName("base")
    @Expose
    private String baseCurrency;
    @SerializedName("date")
    @Expose
    private String date;

    public Map<String, String> getRates() {
        return rates;
    }

    public void setRates(Map<String, String> rates) {
        this.rates = rates;
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public void setBaseCurrency(String baseCurrency) {
        this.baseCurrency = baseCurrency;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}

