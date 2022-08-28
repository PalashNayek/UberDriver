package com.palash.uberdriver.Service;

import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.palash.uberdriver.Common;
import com.palash.uberdriver.Model.EventBus.DriverRequestReceived;
import com.palash.uberdriver.Utils.UserUtils;

import org.greenrobot.eventbus.EventBus;

import java.util.Map;
import java.util.Random;

public class MyFirebaseMessagingService extends FirebaseMessagingService
{
    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        if (FirebaseAuth.getInstance().getCurrentUser()!=null)
        {
            UserUtils.updateToken(this,s);
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

       /* // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d("TAG2", "Message data payload: " + remoteMessage.getData());

            if (true) {
                Log.d("TAG3", "scheduleJob() ");
            } else {
                Log.d("TAG4", "handleNow() ");
            }

        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.d("TAG5", "Message Notification Body: " + remoteMessage.getNotification().getTitle()+" "+remoteMessage.getNotification().getBody());
            Map<String, String> dataRecv= remoteMessage.getData();


            Log.d("NotificationTest",""+dataRecv);

            if (dataRecv!=null)
            {
                Common.showNotification(this,new Random().nextInt(),
                        remoteMessage.getNotification().getTitle(),
                        remoteMessage.getNotification().getBody(),
                        null);
            }
        }*/ //this is my code.......................

        Map<String, String> dataRecv=remoteMessage.getData();
        Log.d("NotificationTest",""+dataRecv);

        if (dataRecv!=null)
        {
            if (dataRecv.get(Common.NOTI_TITLE).equals(Common.REQUEST_DRIVER_TITLE))
            {
                DriverRequestReceived driverRequestReceived = new DriverRequestReceived();
                driverRequestReceived.setKey(dataRecv.get(Common.RIDER_KEY));
                driverRequestReceived.setPickupLocation(dataRecv.get(Common.RIDER_PICKUP_LOCATION));
                driverRequestReceived.setPickupLocationString(dataRecv.get(Common.RIDER_PICKUP_LOCATION_STRING));
                driverRequestReceived.setDestinationLocation(dataRecv.get(Common.RIDER_DESTINATION));
                driverRequestReceived.setDestinationLocationString(dataRecv.get(Common.RIDER_DESTINATION_STRING));

                EventBus.getDefault().postSticky(driverRequestReceived);

            }else {
                Common.showNotification(this, new Random().nextInt(),
                        dataRecv.get(Common.NOTI_TITLE),
                        dataRecv.get(Common.NOTI_CONTENT),
                        null);
            }
        }
    }
}
