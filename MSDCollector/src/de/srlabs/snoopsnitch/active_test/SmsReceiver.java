package de.srlabs.snoopsnitch.active_test;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import de.srlabs.snoopsnitch.util.Constants;
import de.srlabs.snoopsnitch.util.MsdLog;

/**
 * @author Andreas Schildbach
 */
public abstract class SmsReceiver extends BroadcastReceiver {
	private static final String TAG = "msd-active-test-service-sms-receiver";

	@Override
	public void onReceive(final Context context, final Intent intent) {

		final Bundle extras = intent.getExtras();

		boolean swallowSms = false;

		if (extras != null) {
			final Object[] pdus = (Object[]) extras.get("pdus");

			for (final Object pdu : pdus) {

				final SmsMessage sms = SmsMessage.createFromPdu((byte[]) pdu);

				String originatingAddress = sms.getOriginatingAddress();
				if(!originatingAddress.startsWith("+"))
					originatingAddress = "+" + originatingAddress;
				MsdLog.i(TAG, "Received SMS from number " + originatingAddress);
				MsdLog.i(TAG, sms.getMessageBody());
				if (Constants.CALL_NUMBER.equals(originatingAddress) || Constants.CALLBACK_NUMBER.equals(originatingAddress)) {
					MsdLog.i(TAG, "SMS Sender number matched verified, swallowing SMS");
					swallowSms = true;
				} else if(sms.getMessageBody().contains("GSMmap Test SMS")){
					// sms.getOriginatingAddress() sometimes returns the string
					// "SMS" instead of the real number, so detect the GSMmap
					// Test SMS via the message contents
					MsdLog.i(TAG, "SMS contains 'GSMmap Test SMS', swallowing SMS");
					swallowSms = true;
				}
				if(swallowSms)
					onReceiveSms(sms);
			}
		}

		// we don't want our spam to appear in the user's inbox
		if (swallowSms) abortBroadcast();
	}

	protected abstract void onReceiveSms(SmsMessage sms);
}
