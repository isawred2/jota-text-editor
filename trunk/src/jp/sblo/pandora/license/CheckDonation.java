package jp.sblo.pandora.license;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import jp.sblo.pandora.billing.BillingService;
import jp.sblo.pandora.license.ILicense;

public class CheckDonation extends Service {
	static final String ACTION="jp.sblo.pandora.jota.license.result";
	static final String EXTRA="data";

	@Override
	public IBinder onBind(Intent arg0) {
        return new ILicense.Stub() {

			@Override
			public void startLicense(long nonce) throws RemoteException {
//				Intent intent = new Intent(ACTION);
//				intent.putExtra(EXTRA,""+nonce);
//				CheckDonation.this.sendBroadcast(intent);

			    BillingService mBillingService = new BillingService();
		        mBillingService.setContext(CheckDonation.this);

		        // Check if billing is supported.
		        if (mBillingService.checkBillingSupported()) {
		            mBillingService.restoreTransactions();
		        }
		        mBillingService.unbind();
			}
        };
	}
}
