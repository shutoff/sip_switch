package ru.shutoff.sip_switch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

public class Phone extends BroadcastReceiver {

    private static final String
            SIP_CALL_OPTIONS = "sip_call_options",
            SIP_ALWAYS = "SIP_ALWAYS",
            SIP_ADDRESS_ONLY = "SIP_ADDRESS_ONLY";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null)
            return;
        String action = intent.getAction();
        if (action == null)
            return;
        if (action.equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
            final TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            final GsmCellLocation location = (GsmCellLocation) telephony.getCellLocation();
            int lac = 0;
            if (location != null)
                lac = location.getLac();
            String number = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
            if (number == null)
                number = "";
            boolean use_sip = false;
            if (number.length() > 7) {
                use_sip = true;
                if (!telephony.isNetworkRoaming()) {
                    if ((lac >= 7801) && (lac <= 7815))
                        use_sip = false;
                    if ((lac >= 4701) && (lac <= 4715))
                        use_sip = false;
                }
                if (!use_sip) {
                    String zone = "";
                    if (number.substring(0, 1).equals("8"))
                        zone = number.substring(1, 4);
                    if (number.substring(0, 2).equals("+7"))
                        zone = number.substring(2, 5);
                    int area_code = 0;
                    try {
                        area_code = Integer.parseInt(zone);
                    } catch (Exception ex) {
                        // ignore
                    }
                    use_sip = true;
                    for (int code : local_codes) {
                        if (code == area_code) {
                            use_sip = false;
                            break;
                        }
                    }
                }
            }
            try {
                Settings.System.putString(context.getContentResolver(),
                        SIP_CALL_OPTIONS, use_sip ? SIP_ALWAYS : SIP_ADDRESS_ONLY);
            } catch (Exception ex) {
                // ignore
            }
        }
    }

    static int[] local_codes = {
            812, 921, 929, 931, 911, 981, 903, 905, 906, 909, 960, 961, 962,
            963, 964, 965, 966, 967, 968, 904, 950, 951, 952, 953, 991, 901
    };

    static public void appendLog(String text) {
        File logFile = Environment.getExternalStorageDirectory();
        logFile = new File(logFile, "phone.log");
        try {
            if (!logFile.exists())
                logFile.createNewFile();
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            Date d = new Date();
            buf.append(d.toLocaleString());
            buf.append(" ");
            buf.append(text);
            buf.newLine();
            buf.close();
        } catch (IOException e) {
        }
    }

    static public void print(Throwable ex) {
        appendLog("Error: " + ex.toString());
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        String s = sw.toString();
        appendLog(s);
    }

}
