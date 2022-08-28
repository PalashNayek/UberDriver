package com.palash.uberdriver.Utils;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.text.TextUtils;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class LocationUtils {
    public static String getAddressFromLocation(Context context, Location location) {
        StringBuilder result = new StringBuilder();
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        List<Address> addressList;

        try {
            addressList = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addressList != null && addressList.size() > 0) {
                if (addressList.get(0).getLocality() != null && !TextUtils.isEmpty(addressList.get(0).getLocality())) {
                    //If address have city name field
                    result.append(addressList.get(0).getLocality());
                } else if (addressList.get(0).getSubAdminArea() != null && !TextUtils.isEmpty(addressList.get(0).getSubAdminArea())) {
                    //If don't have city field, we look for subAdminArea
                    result.append(addressList.get(0).getSubAdminArea());
                }else if (addressList.get(0).getAdminArea() != null && !TextUtils.isEmpty(addressList.get(0).getAdminArea())) {
                    //If don't have sub-admin-area
                    result.append(addressList.get(0).getAdminArea());
                }else {
                    //If don't have admin-area, we look for country
                    result.append(addressList.get(0).getCountryName());
                }
                //final result, apply country code
                result.append("_").append(addressList.get(0).getCountryCode());
            }
            return result.toString();
        } catch (IOException e) {
            return result.toString();
        }
    }
}
