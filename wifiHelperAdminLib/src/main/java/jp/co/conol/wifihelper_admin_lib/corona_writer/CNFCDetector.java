package jp.co.conol.wifihelper_admin_lib.corona_writer;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.NfcA;

public class CNFCDetector {

    private final NfcAdapter nfcAdapter;
    private final PendingIntent pendingIntent;
    private final IntentFilter[] intentFilters;
    private final String[][] techList;

    public CNFCDetector(Context context) throws NFCToWriteNotAvailableException {
        nfcAdapter = NfcAdapter.getDefaultAdapter(context);
        if (nfcAdapter == null) {
            throw new NFCToWriteNotAvailableException();
        }

        pendingIntent = PendingIntent.getActivity(context, 0,
                new Intent(context, context.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                0);

        intentFilters = new IntentFilter[] {
                new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
        };

        techList = new String[][] {
                new String[] { NfcA.class.getName(), IsoDep.class.getName() },
                new String[] { NfcA.class.getName(), MifareUltralight.class.getName() }
        };
    }

    public void enableForegroundDispatch(Activity activity) {
        nfcAdapter.enableForegroundDispatch(activity, pendingIntent, intentFilters, techList);
    }

    public void disableForegroundDispatch(Activity activity) {
        nfcAdapter.disableForegroundDispatch(activity);
    }

    public CNFCTag getTagFromIntent(Intent intent) {
        if (intent.getAction().equals(NfcAdapter.ACTION_TECH_DISCOVERED)) {
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

}
