package com.itcube.Emelyanov;
import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface RetrofitInterface {

    @GET("fetch.php")
    Call<List<DataModel>> fetchData(@Query("user_id") int userId);

    @POST("delete.php")
    Call<Void> deleteData(@Query("id") int id);

    @POST("insert.php")
    Call<Void> createData(@Body DataModel newItem, @Query("user_id") int userId);

    @POST("edit.php")
    Call<Void> editData(@Body DataModel newItem);

    @POST("authenticate.php")
    Call<UserResponse> authenticateUser(@Body UserRequest userRequest);

    @POST("register.php")
    Call<UserResponse> registerUser(@Body UserRequest userRequest);
}
