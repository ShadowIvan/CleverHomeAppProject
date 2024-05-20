package com.itcube.Emelyanov;
import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface RetrofitInterface {

    @GET("Android.php")
    Call<RoomsResponse> fetchData(
            @Query("operation") String operation
    );

    @GET("Android.php")
    Call<Void> deleteData(
            @Query("operation") String operation,
            @Query("id") int ID // Ensure this matches the server parameter
    );

    @GET("Android.php")
    Call<Void> CreateData(
            @Query("operation") String operation,
            @Query("name") String Name,
            @Query("room") int Room,
            @Query("info") String Info
    );
}


