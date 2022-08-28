package com.palash.uberdriver.Remote;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface IGoogleAPI
{
    //Remember you must enable billing for your project to use this api
    @GET("maps/api/directions/json")
    Observable<String> getDirections(
            @Query("mode") String mode,
            @Query("transit_routing_preference") String transit_routing,
            @Query("origin") String from,
            @Query("destination") String to,
            @Query("key") String key
    );
}
