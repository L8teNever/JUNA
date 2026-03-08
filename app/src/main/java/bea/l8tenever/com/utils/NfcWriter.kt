package bea.l8tenever.com.utils

import android.app.Activity
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.util.Log

class NfcWriter(private val activity: Activity) : NfcAdapter.ReaderCallback {

    private var nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)
    private var pendingUri: String? = null
    private var onResult: ((Boolean, String) -> Unit)? = null

    fun startWriting(uri: String, callback: (Boolean, String) -> Unit) {
        if (nfcAdapter == null) {
            callback(false, "NFC wird von diesem Gerät nicht unterstützt.")
            return
        }
        if (nfcAdapter?.isEnabled == false) {
            callback(false, "Bitte schalte NFC in den Einstellungen ein.")
            return
        }
        pendingUri = uri
        onResult = callback

        val flags = NfcAdapter.FLAG_READER_NFC_A or 
                    NfcAdapter.FLAG_READER_NFC_B or 
                    NfcAdapter.FLAG_READER_NFC_F or 
                    NfcAdapter.FLAG_READER_NFC_V or 
                    NfcAdapter.FLAG_READER_NFC_BARCODE
        
        nfcAdapter?.enableReaderMode(activity, this, flags, null)
    }

    fun stopWriting() {
        pendingUri = null
        onResult = null
        nfcAdapter?.disableReaderMode(activity)
    }

    override fun onTagDiscovered(tag: Tag?) {
        val uri = pendingUri ?: return
        val resultCallback = onResult ?: return
        
        if (tag == null) {
            resultCallback(false, "Fehler beim Lesen des Tags.")
            stopTimerAndDisable()
            return
        }

        try {
            val ndefMessage = NdefMessage(arrayOf(NdefRecord.createUri(uri)))
            val ndef = Ndef.get(tag)

            if (ndef != null) {
                ndef.connect()
                if (!ndef.isWritable) {
                    resultCallback(false, "Dieser NFC Tag ist schreibgeschützt.")
                    return
                }
                if (ndef.maxSize < ndefMessage.toByteArray().size) {
                    resultCallback(false, "Nicht genug Speicherplatz auf dem NFC Tag.")
                    return
                }
                ndef.writeNdefMessage(ndefMessage)
                ndef.close()
                resultCallback(true, "NFC Tag erfolgreich beschrieben!")
            } else {
                val formatable = NdefFormatable.get(tag)
                if (formatable != null) {
                    formatable.connect()
                    formatable.format(ndefMessage)
                    formatable.close()
                    resultCallback(true, "NFC Tag erfolgreich formatiert und beschrieben!")
                } else {
                    resultCallback(false, "Dieser NFC Tag wird nicht unterstützt.")
                }
            }
        } catch (e: Exception) {
            Log.e("NfcWriter", "Fehler beim Beschreiben des NFC Tags", e)
            resultCallback(false, "Fehler beim Schreiben auf den Tag: ${e.message}")
        } finally {
            stopTimerAndDisable()
        }
    }

    private fun stopTimerAndDisable() {
        activity.runOnUiThread {
            stopWriting()
        }
    }
}
