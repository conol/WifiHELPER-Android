package jp.co.conol.wifihelper_admin_lib.corona;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.os.Build;
import android.os.PatternMatcher;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import jp.co.conol.wifihelper_admin_lib.Util;
import jp.co.conol.wifihelper_admin_lib.corona.corona_reader.CNFCReaderException;
import jp.co.conol.wifihelper_admin_lib.corona.corona_reader.CNFCReaderTag;
import jp.co.conol.wifihelper_admin_lib.corona.corona_writer.CNFCT2Tag;
import jp.co.conol.wifihelper_admin_lib.corona.corona_writer.CNFCTag;
import jp.co.conol.wifihelper_admin_lib.device_manager.GetLocation;
import jp.co.conol.wifihelper_admin_lib.device_manager.SendLogAsyncTask;

import static android.content.Context.LOCATION_SERVICE;

/**
 * Created by Masafumi_Ito on 2017/10/11.
 */

public class CoronaNfc {

    private Context context;
    private final NfcAdapter nfcAdapter;
    private final PendingIntent pendingIntent;
    private final IntentFilter[] intentFilters;
    private final String[][] techList;

    public CoronaNfc(Context context) throws NFCNotAvailableException {
        this.context = context;
        nfcAdapter = NfcAdapter.getDefaultAdapter(context);
        if (nfcAdapter == null) {
            throw new NFCNotAvailableException();
        }

        pendingIntent = PendingIntent.getActivity(context, 0,
                new Intent(context, context.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                0);

        IntentFilter actionNdef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        actionNdef.addDataScheme("vnd.android.nfc");
        actionNdef.addDataPath("/conol.co.jp:cnfc_bt_manu_data", PatternMatcher.PATTERN_LITERAL);

        intentFilters = new IntentFilter[]{
                actionNdef
        };

        techList = new String[][]{
                new String[]{NfcA.class.getName(), IsoDep.class.getName()},
                new String[]{NfcA.class.getName(), MifareUltralight.class.getName()}
        };
    }

    public boolean isEnable() {
        return nfcAdapter.isEnabled();
    }

    public void enableForegroundDispatch(Activity activity) {
        nfcAdapter.enableForegroundDispatch(activity, pendingIntent, intentFilters, techList);
    }

    public void disableForegroundDispatch(Activity activity) {
        nfcAdapter.disableForegroundDispatch(activity);
    }

    public CNFCTag getWriteTagFromIntent(Intent intent) {
        // GPSの許可を確認（ログ送信用）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return null;
            }
        }

        if (intent.getAction().equals(NfcAdapter.ACTION_NDEF_DISCOVERED)
                || intent.getAction().equals(NfcAdapter.ACTION_TECH_DISCOVERED)) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag != null) {
                /*
                IsoDep isoDep = IsoDep.get(tag);
                if (isoDep != null) {
                    return new CNFCT4Tag(isoDep);
                }
                */
                MifareUltralight mul = MifareUltralight.get(tag);
                if (mul != null) {
                    return new CNFCT2Tag(mul);
                }
            }
        }
        return null;
    }

    public CNFCReaderTag getReadTagFromIntent(Intent intent) throws CNFCReaderException {
        // GPSの許可がなければwifiに接続できないため、事前に確認（ログ送信にも使用）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return null;
            }
        }

        if (!intent.getAction().equals(NfcAdapter.ACTION_NDEF_DISCOVERED) &&
                !intent.getAction().equals(NfcAdapter.ACTION_TECH_DISCOVERED)) {
            return null;
        }

        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag == null) {
            return null;
        }

        Ndef ndef = Ndef.get(tag);
        if (ndef == null) {
            throw new CNFCReaderException("Not NDEF tag");
        }

        NdefMessage msg;
        try {
            ndef.connect();
            msg = ndef.getNdefMessage();
        } catch (IOException | FormatException e) {
            throw new CNFCReaderException(e);
        } finally {
            try {
                ndef.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        NdefRecord[] records = msg.getRecords();
        for (NdefRecord rec : records) {
            CNFCReaderTag t = CNFCReaderTag.get(rec);
            if (t != null) {

                // 位置情報の取得
                GetLocation location = new GetLocation(context);
                String locationInfo = "";
                if(location.getCurrentLocation() != null) {
                    locationInfo = String.valueOf(location.getCurrentLocation().getLatitude()) + "," + String.valueOf(location.getCurrentLocation().getLongitude());
                }

                // デバイスIDに空白を入れる
                // TODO 16進数でなくなるとこの部分削除
                String str = t.getChipIdString();
                StringBuilder sb = new StringBuilder(str);
                sb.insert(12," ");
                sb.insert(10," ");
                sb.insert(8," ");
                sb.insert(6," ");
                sb.insert(4," ");
                sb.insert(2," ");

                // ログの送信
                if(Util.Network.isConnected(context)) {
                    String log[] = {sb.toString().toLowerCase(), locationInfo, "Read"};
                    new SendLogAsyncTask(new SendLogAsyncTask.AsyncCallback() {
                        @Override
                        public void onSuccess(JSONObject responseJson) {
                            Log.d("SendLogSuccess", responseJson.toString());
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Log.d("SendLogFailure", e.toString());
                        }
                    }).execute(log);
                }

                return t;
            }
        }

        throw new CNFCReaderException("Not Corona tag");
    }
}
