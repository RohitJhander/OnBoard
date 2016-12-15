package com.example.rohit.onboardapplication;

import android.content.SharedPreferences;

/**
 * Created by rohit on 13/12/16.
 */

public class Constants {
    public static long SCAN_PERIOD = 3000;    // milliseconds
    public static long connectionTime = 1200; // milliseconds
    public static String sendData = "Rohit";    // data send to device
    public static int writeToServiceIndex = 2;  // write to service 3
    public static int writeToCharacterisiticIndex = 0; // write to characeristic 1
    public static String noBusFoundMsg = "no bus found";
}
