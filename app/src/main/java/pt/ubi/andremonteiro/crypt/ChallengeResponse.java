//
//  Author:
//       Ben Rush
//
//  Copyright (c) 2014 Ben Rush
//
//  This program is free software; you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation; either version 2 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program; if not, write to the Free Software
//  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
//
//  This code was adapted from code produced by Yubico, Inc available:
//		https://github.com/Yubico/yubitotp-android
//

package pt.ubi.andremonteiro.crypt;


import java.io.IOException;

import android.widget.Toast;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.IsoDep;
import android.preference.PreferenceManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;

public class ChallengeResponse {


    private byte[] challenge;
    private int slot = 2;

    private static final byte SLOT_CHAL_HMAC1 = 0x30;
    private static final byte SLOT_CHAL_HMAC2 = 0x38;
    private static final byte CHAL_BYTES = 0x40; // 64
    private static final byte RESP_BYTES = 20;

    private static final byte[] selectCommand = { 0x00, (byte) 0xA4, 0x04,
            0x00, 0x07, (byte) 0xA0, 0x00, 0x00, 0x05, 0x27, 0x20, 0x01, 0x00 };
    private static final byte[] chalCommand = { 0x00, 0x01, SLOT_CHAL_HMAC2,
            0x00, CHAL_BYTES };

    private AlertDialog swipeDialog;

    private Activity act;

    public ChallengeResponse(Activity a, byte[] cha)
    {
        act=a;
        challenge=cha;
    }

    public void challengeYubiKey() {
        AlertDialog.Builder challengeDialog = new AlertDialog.Builder(act);
        challengeDialog.setTitle("Challenging...");
        challengeDialog.setMessage("Swipe your Yubikey!");
        swipeDialog = challengeDialog.show();
        enableDispatch();
    }

    private void enableDispatch() {
        Intent intent = new Intent();

        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent tagIntent = PendingIntent.getActivity(
                act, 0, intent, 0);

        IntentFilter iso = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(act);
        if (adapter == null) {
            Toast.makeText(act, "no nfc", Toast.LENGTH_LONG).show();
            return;
        }
        if (adapter.isEnabled()) {
            // register for foreground dispatch so we'll receive tags according to our intent filters
            adapter.enableForegroundDispatch(
                    act, tagIntent, new IntentFilter[]{iso},
                    new String[][]{new String[]{IsoDep.class.getName()}}
            );
        } else {
            AlertDialog.Builder dialog = new AlertDialog.Builder(act);
            dialog.setTitle("nfc off");
            dialog.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Intent settings = new Intent(android.provider.Settings.ACTION_NFC_SETTINGS);
                    act.startActivity(settings);
                    dialog.dismiss();
                }
            });
            dialog.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            dialog.show();
        }
        System.out.println("GETS HERE 1");
        startIntent(intent);
    }

    private void disableDispatch()
    {
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(act);
        if(adapter != null) {
            adapter.disableForegroundDispatch(act);
        }

    }

    String result=null;

    protected void startIntent(Intent intent) {
        System.out.println("INTENT STARTED");
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag != null) {
            IsoDep isoTag = IsoDep.get(tag);
            try {
                isoTag.connect();
                byte[] resp = isoTag.transceive(selectCommand);
                int length = resp.length;
                if (resp[length - 2] == (byte) 0x90 && resp[length - 1] == 0x00)
                    doChallengeYubiKey(isoTag, slot, challenge, intent);
                else {
                    Toast.makeText(act, "tag error", Toast.LENGTH_LONG)
                            .show();
                    result="canceled";
                }

                isoTag.close();
            } catch (TagLostException e) {
                Toast.makeText(act, "lost tag", Toast.LENGTH_LONG)
                        .show();
                result="canceled";
            } catch (IOException e) {
                Toast.makeText(act, "tag error: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
                result="canceled";
            }
        }
        else result="canceled";
        System.out.println("INTENT FINISHED");
    }

    private void doChallengeYubiKey(IsoDep isoTag, int slot, byte[] challenge, Intent intent) throws IOException {
        if (challenge == null || challenge.length != CHAL_BYTES)
            return;
        System.out.println("STARTING CHALLENGE");
        byte[] apdu = new byte[chalCommand.length + CHAL_BYTES];
        System.arraycopy(chalCommand, 0, apdu, 0, chalCommand.length);
        if (slot == 1)
            apdu[2] = SLOT_CHAL_HMAC1;
        System.arraycopy(challenge, 0, apdu, chalCommand.length, CHAL_BYTES);

        byte[] respApdu = isoTag.transceive(apdu);
        if (respApdu.length == 22 && respApdu[20] == (byte) 0x90
                && respApdu[21] == 0x00) {
            // Get the secret
            byte[] resp = new byte[RESP_BYTES];
            System.arraycopy(respApdu, 0, resp, 0, RESP_BYTES);
            Intent data = intent;
            data.putExtra("response", resp);
            result="ok"; // This is where the result gets sent back
        } else {
            Toast.makeText(act, "challenge failed", Toast.LENGTH_LONG)
                    .show();
            result="canceled";
        }
    }
    /*
    @Override
    protected void onResume()
    {
        super.onResume();

        /*Intent closeIntent = new Intent("close main activity");
        sendBroadcast(closeIntent);	//Having the main activity running can cause problems using the back button*/
/*
        Intent intent = getIntent();
        challenge = intent.getByteArrayExtra("challenge");
        slot = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString("pref_Slot","2"));
        if (challenge == null)  return;
        else if (challenge.length != CHAL_BYTES) return;
        else  challengeYubiKey();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag != null) {
            IsoDep isoTag = IsoDep.get(tag);
            try {
                isoTag.connect();
                byte[] resp = isoTag.transceive(selectCommand);
                int length = resp.length;
                if (resp[length - 2] == (byte) 0x90 && resp[length - 1] == 0x00)
                    doChallengeYubiKey(isoTag, slot, challenge);
                else {
                    Toast.makeText(this, "tag error", Toast.LENGTH_LONG)
                            .show();
                    setResult(RESULT_CANCELED,intent);
                }

                isoTag.close();
            } catch (TagLostException e) {
                Toast.makeText(this, "lost tag", Toast.LENGTH_LONG)
                        .show();
                setResult(RESULT_CANCELED,intent);
            } catch (IOException e) {
                Toast.makeText(this, "tag error: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
                setResult(RESULT_CANCELED,intent);
            }
        }
        else setResult(RESULT_CANCELED,intent);
        finish();
    }


    @Override
    protected void onPause()
    {
        super.onPause();
        if (swipeDialog != null)
        {
            swipeDialog.dismiss();
            swipeDialog = null;
        }
        disableDispatch();
    }

    private void disableDispatch()
    {
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
        if(adapter != null) {
            adapter.disableForegroundDispatch(this);
        }

    }



    private void doChallengeYubiKey(IsoDep isoTag, int slot, byte[] challenge) throws IOException {
        if (challenge == null || challenge.length != CHAL_BYTES)
            return;

        byte[] apdu = new byte[chalCommand.length + CHAL_BYTES];
        System.arraycopy(chalCommand, 0, apdu, 0, chalCommand.length);
        if (slot == 1)
            apdu[2] = SLOT_CHAL_HMAC1;
        System.arraycopy(challenge, 0, apdu, chalCommand.length, CHAL_BYTES);

        byte[] respApdu = isoTag.transceive(apdu);
        if (respApdu.length == 22 && respApdu[20] == (byte) 0x90
                && respApdu[21] == 0x00) {
            // Get the secret
            byte[] resp = new byte[RESP_BYTES];
            System.arraycopy(respApdu, 0, resp, 0, RESP_BYTES);
            Intent data = getIntent();
            data.putExtra("response", resp);
            setResult(RESULT_OK,data); // This is where the result gets sent back
        } else {
            Toast.makeText(this, "challenge failed", Toast.LENGTH_LONG)
                    .show();
            setResult(RESULT_CANCELED,getIntent());
        }
    }

    private void enableDispatch()
    {
        Intent intent = getIntent();
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent tagIntent = PendingIntent.getActivity(
                this, 0, intent, 0);

        IntentFilter iso = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
        if(adapter == null) {
            Toast.makeText(this, "no nfc", Toast.LENGTH_LONG).show();
            return;
        }
        if(adapter.isEnabled()) {
            // register for foreground dispatch so we'll receive tags according to our intent filters
            adapter.enableForegroundDispatch(
                    this, tagIntent, new IntentFilter[] {iso},
                    new String[][] { new String[] { IsoDep.class.getName() } }
            );
        } else {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle("nfc off");
            dialog.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Intent settings = new Intent(android.provider.Settings.ACTION_NFC_SETTINGS);
                    ChallengeResponse.this.startActivity(settings);
                    dialog.dismiss();
                }
            });
            dialog.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            dialog.show();
        }
    }*/


}
