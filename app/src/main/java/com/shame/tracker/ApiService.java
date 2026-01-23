package com.shame.tracker;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

import retrofit2.http.POST;
import retrofit2.http.Body;

public interface ApiService {
    @GET("rest/v1/cigarette_logs")
    Call<List<ShameEntry>> getLogs(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Query("select") String select);

    @POST("rest/v1/cigarette_logs")
    Call<Void> addLog(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Header("Content-Type") String contentType,
            @Header("Prefer") String prefer,
            @Body ShameEntry log);
}
