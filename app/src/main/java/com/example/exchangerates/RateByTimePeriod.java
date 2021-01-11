package com.example.exchangerates;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class RateByTimePeriod {
    @SerializedName("rates")
    @Expose
    private Map<String, Map<String, String>> rates;
    @SerializedName("start_at")
    @Expose
    private String start_at;
    @SerializedName("base")
    @Expose
    private String base;
    @SerializedName("end_at")
    @Expose
    private String end_at;


    public Map<String, Map<String, String>> getRates() {
        return rates;
    }

    public void setRates(Map<String, Map<String, String>> rates) {
        this.rates = rates;
    }

    public String getStart_at() {
        return start_at;
    }

    public void setStart_at(String start_at) {
        this.start_at = start_at;
    }

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }

    public String getEnd_at() {
        return end_at;
    }

    public void setEnd_at(String end_at) {
        this.end_at = end_at;
    }

}
