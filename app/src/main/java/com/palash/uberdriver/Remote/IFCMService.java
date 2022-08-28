package com.palash.uberdriver.Remote;

import com.palash.uberdriver.Model.FCMResponse;
import com.palash.uberdriver.Model.FCMSendData;

import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface IFCMService
{
    @Headers({
            "Content-Type:application/json",
            "Authorization:key=AAAAyCFFX8A:APA91bHRCs3rq3meANOtX-yuUU39hiSS51C9NiOd6FT_uRn0RO6F1t6wC1XPOnhIWGZse-5GRFvKRw0dSqJlFN778d7xbwWq6i20Ub4OPlMJw3PXophUiC_OmfwejXjF7WMGsaMaWvRG"
    })
    @POST("fcm/send")
    Observable<FCMResponse> sendNotification(@Body FCMSendData body);

}
