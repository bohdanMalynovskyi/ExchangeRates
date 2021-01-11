package com.example.exchangerates;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface JSONPlaceHolderApi {
    @GET("/latest")
    public Call<RateByBaseCurrency> getRateByBaseCurrency(
                @Query("base") String base
    );

//    @GET("/latest")
//    public Call<ResponseBody> getRateByBaseCurrencyJSON(
//            @Query("base") String base
//    );

    @GET("/history")
    public Call<RateByTimePeriod> getRateByTimePeriod (
            @Query("start_at") String start_at,
            @Query("end_at") String end_at,
            @Query("symbols") String currency,
            @Query("base") String base
    );
}
