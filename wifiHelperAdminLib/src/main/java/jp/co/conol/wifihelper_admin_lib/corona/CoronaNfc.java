package jp.co.conol.wifihelper_admin_lib.corona;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.os.PatternMatcher;
import android.util.Log;

import org.json.JSONObject;

import java.io.IOException;

import jp.co.conol.wifihelper_admin_lib.Util;
import jp.co.conol.wifihelper_admin_lib.corona.corona_reader.CNFCReaderException;
import jp.co.conol.wifihelper_admin_lib.corona.corona_reader.CNFCReaderTag;
import jp.co.conol.wifihelper_admin_lib.corona.corona_writer.CNFCT2Tag;
import jp.co.conol.wifihelper_admin_lib.corona.corona_writer.CNFCTag;
import jp.co.conol.wifihelper_admin_lib.device_manager.SendLogAsyncTask;

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

        intentFilters = new IntentFilter[] {
                actionNdef
        };

        techList = new String[][] {
                new String[] { NfcA.class.getName(), IsoDep.class.getName() },
                new String[] { NfcA.class.getName(), MifareUltralight.class.getName() }
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
        } catch (IOException |FormatException e) {
            throw new CNFCReaderException(e);
        } finally {
            try {
                ndef.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        NdefRecord[] records = msg.getRecords();
        for (NdefRecord rec: records) {
            CNFCReaderTag t = CNFCReaderTag.get(rec);
            if (t != null) {

                // ログの送信
                if(Util.Network.isConnected(context)) {
                    String test[] = {"45 07 a2 cf 15 3a 2a", "2014-10-10T13:50:40", "test", "test"};
                    new SendLogAsyncTask(new SendLogAsyncTask.AsyncCallback() {
                        @Override
                        public void onSuccess(JSONObject responseJson) {
                            Log.d("SendLogSuccess", responseJson.toString());
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Log.d("SendLogFailure", e.toString());
                        }
                    }).execute(test);
                }

                return t;
            }
        }

        throw new CNFCReaderException("Not Corona tag");
    }
}
