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

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.View;
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

public class Challenge extends Activity {


    private byte[] challenge;
    private int slot=2;

    private static final byte SLOT_CHAL_HMAC1 = 0x30;
    private static final byte SLOT_CHAL_HMAC2 = 0x38;
    private static final byte CHAL_BYTES = 0x40; // 64
    private static final byte RESP_BYTES = 20;

    private static final byte[] selectCommand = { 0x00, (byte) 0xA4, 0x04,
            0x00, 0x07, (byte) 0xA0, 0x00, 0x00, 0x05, 0x27, 0x20, 0x01, 0x00 };
    private static final byte[] chalCommand = { 0x00, 0x01, SLOT_CHAL_HMAC2,
            0x00, CHAL_BYTES };

    private AlertDialog swipeDialog;


    public Challenge()
    {

    }


    @Override
    protected void onResume()
    {
        super.onResume();

        /*Intent closeIntent = new Intent("close main activity");
        sendBroadcast(closeIntent);	//Having the main activity running can cause problems using the back button*/

        Intent intent = getIntent();
        challenge = intent.getByteArrayExtra("challenge");
        slot = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString("pref_Slot","2"));
        if (challenge == null) {
            System.out.println("NULL CHALLENGE"); return;
        }
        else if (challenge.length != CHAL_BYTES) {
            System.out.println("length inc"); return;
        }
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

    private void challengeYubiKey() {
        AlertDialog.Builder challengeDialog = new AlertDialog.Builder(this);
        challengeDialog.setTitle("challenging...");
        challengeDialog.setMessage("swipe!");
        swipeDialog = challengeDialog.show();
        enableDispatch();
    }

    private void doChallengeYubiKey(IsoDep isoTag, int slot, byte[] challenge) throws IOException {
        if (challenge == null || challenge.length != CHAL_BYTES)
            return;
        System.out.println("Challenging");
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
            System.out.println(new String(resp));
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
                    Challenge.this.startActivity(settings);
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
    }


}
