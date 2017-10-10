package jp.co.conol.wifihelper_admin_lib.corona_reader;

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
import android.nfc.tech.Ndef;

import java.io.IOException;

public class CNFCReader {

    private final NfcAdapter nfcAdapter;
    private final PendingIntent pendingIntent;
    private final IntentFilter[] intentFilters;
    private final String[][] techList;

    public CNFCReader(Context context) throws NFCToReadNotAvailableException {
        nfcAdapter = NfcAdapter.getDefaultAdapter(context);
        if (nfcAdapter == null) {
            throw new NFCToReadNotAvailableException();
        }

        pendingIntent = PendingIntent.getActivity(context, 0,
                new Intent(context, context.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                0);

        intentFilters = new IntentFilter[] {
                new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
        };

        techList = new String[][] {
                new String[] { Ndef.class.getName() }
        };
    }

    public void enableForegroundDispatch(Activity activity) {
        nfcAdapter.enableForegroundDispatch(activity, pendingIntent, intentFilters, techList);
    }

    public void disableForegroundDispatch(Activity activity) {
        nfcAdapter.disableForegroundDispatch(activity);
    }

    public CNFCReaderTag getTagFromIntent(Intent intent) throws CNFCReaderException {
        if (!intent.getAction().equals(NfcAdapter.ACTION_TECH_DISCOVERED)) {
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
                return t;
            }
        }

        throw new CNFCReaderException("Not Corona tag");
    }

}
