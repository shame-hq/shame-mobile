package com.shame.tracker;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface ApiService {
    @GET("rest/v1/cigarette_logs")
    Call<List<ShameEntry>> getLogs(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Query("select") String select);
}
