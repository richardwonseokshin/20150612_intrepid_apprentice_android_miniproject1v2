package com.intrepid.wonseokshin.twitterscriberetrofit;

import java.util.List;

import retrofit.Callback;
import retrofit.http.GET;
import retrofit.http.Header;

/**
 * Created by wonseokshin on 6/15/15.
 */
public interface TwitterService {
    @GET("/1.1/statuses/home_timeline.json")
    void getHomeTimeline(@Header("Authorization") String authorization, Callback<List<HomeTimeline>> callback);
}
