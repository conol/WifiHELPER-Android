package jp.co.conol.wifihelper_lib.cuona.cuona_writer;

import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.util.Log;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class CuonaWritableT2 extends CuonaWritableTag {

    private static final int T2_DEVICE_ID_LENGTH = 9;

    private MifareUltralight mul;

    public CuonaWritableT2(MifareUltralight mul) {
        this.mul = mul;
    }

    public void writeJSON(String json) throws IOException {
        Log.i("nfc", "T2 detected");

        if (!mul.isConnected()) {
            mul.connect();
        }

        byte[] deviceId = mul.readPages(0);
        HexUtils.logd("readPages(0)", deviceId);
        deviceId = Arrays.copyOf(deviceId, T2_DEVICE_ID_LENGTH);

        mul.close();

        byte[] jsonData = ("JSON" + json).getBytes(StandardCharsets.UTF_8);

        NdefRecord rec = CuonaNDEF.createRecord(deviceId, jsonData);
        NdefMessage msg = new NdefMessage(rec);

        Ndef ndef = Ndef.get(mul.getTag());
        if (ndef == null) {
            throw new IOException("Cannot get Ndef");
        }

        if (!ndef.isConnected()) {
            ndef.connect();
        }

        if (ndef.isWritable()) {

            try {
                ndef.writeNdefMessage(msg);
            } catch (FormatException e) {
                throw new IOException(e);
            }
            Log.i("nfc", "Tag written!");

        } else {
            throw new IOException("Tag is not writable");
        }

    }

}
