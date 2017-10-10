package jp.co.conol.wifihelper_admin_lib.corona.corona_writer;

import android.nfc.tech.IsoDep;
import android.util.Log;

import java.io.IOException;

public class CNFCT4Tag extends CNFCTag {

    private final static byte[] NDEFTagAppSelectAPDU = new byte[] {
            (byte) 0x00, // CLA
            (byte) 0xA4, // INS
            (byte) 0x04, (byte) 0x00, // P1 P2
            (byte) 0x07, // Lc
            (byte) 0xD2, (byte) 0x76, (byte) 0x00, (byte) 0x00,
            (byte) 0x85, (byte) 0x01, (byte) 0x01, // data
    };

    private IsoDep dep;

    public CNFCT4Tag(IsoDep dep) {
        this.dep = dep;
    }

    private void selectNDEFTagApp() throws IOException {
        if (!dep.isConnected()) {
            dep.connect();
        }

        HexUtils.logd("T4 send", NDEFTagAppSelectAPDU);
        byte[] ans = dep.transceive(NDEFTagAppSelectAPDU);
        HexUtils.logd("T4 recv", ans);

    }

    @Override
    public void writeServiceID(byte[] serviceId) throws IOException {
        Log.i("nfc", "T4 detected");
        selectNDEFTagApp();

        // TODO: incomplete
    }
}
