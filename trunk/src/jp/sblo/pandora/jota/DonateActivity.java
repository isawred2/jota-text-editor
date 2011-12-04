package jp.sblo.pandora.jota;

import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import jp.sblo.pandora.billing.BillingService;
import jp.sblo.pandora.billing.Consts;
import jp.sblo.pandora.billing.PurchaseObserver;
import jp.sblo.pandora.billing.ResponseHandler;
import jp.sblo.pandora.billing.BillingService.RequestPurchase;
import jp.sblo.pandora.billing.BillingService.RestoreTransactions;
import jp.sblo.pandora.billing.Consts.PurchaseState;
import jp.sblo.pandora.billing.Consts.ResponseCode;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class DonateActivity extends AboutActivity  {

    private static final String TAG = "Donate";

    public static final String DONATION_COUNTER = "donationcounter";
    private DonatePurchaseObserver mDonatePurchaseObserver;
    private Handler mHandler;

    private BillingService mBillingService;
//    private Button mBuyButton;

    private static final int DIALOG_CANNOT_CONNECT_ID = 1;
    private static final int DIALOG_BILLING_NOT_SUPPORTED_ID = 2;
    private ProgressDialog mProgressDialog;

    /** An array of product list entries for the products that can be purchased. */
    private static final String [] CATALOG = new String[] {
        "jotatexteditordonation_managed",
        "android.test.purchased",
        "android.test.canceled",
        "android.test.refunded",
        "android.test.item_unavailable",
    };

    /**
     * A {@link PurchaseObserver} is used to get callbacks when Android Market sends
     * messages to this application so that we can update the UI.
     */
    private class DonatePurchaseObserver extends PurchaseObserver {
        public DonatePurchaseObserver(Handler handler) {
            super(DonateActivity.this, handler);
        }

        @Override
        public void onBillingSupported(boolean supported) {
            if (Consts.DEBUG) {
                Log.i(TAG, "supported: " + supported);
            }
            if (supported) {
//                mBuyButton.setEnabled(true);
            } else {
                showDialog(DIALOG_BILLING_NOT_SUPPORTED_ID);
            }
        }

        @Override
        public void onPurchaseStateChange(PurchaseState purchaseState, String itemId,
                final String orderId, long purchaseTime, String developerPayload) {
            if (Consts.DEBUG) {
                Log.i(TAG, "onPurchaseStateChange() itemId: " + itemId + " " + purchaseState);
            }
            try{
                mProgressDialog.dismiss();
            }catch(Exception e){}
            mProgressDialog = null;

            if (purchaseState == PurchaseState.PURCHASED) {
                final int index;
                if ( isDebuggable() ){
                    index = 1;
                }else{
                    index = 0;
                }
                if ( itemId.equals(CATALOG[index]) ){
                    donatedAction(orderId);
                }
            }
        }

        @Override
        public void onRequestPurchaseResponse(RequestPurchase request, ResponseCode responseCode) {
            if (Consts.DEBUG) {
                Log.d(TAG, request.mProductId + ": " + responseCode);
            }
            if (responseCode == ResponseCode.RESULT_OK) {
                if (Consts.DEBUG) {
                    Log.i(TAG, "purchase was successfully sent to server");
                }
                mProgressDialog = new ProgressDialog(DonateActivity.this);
                mProgressDialog.setMessage(getString(R.string.spinner_message));
                mProgressDialog.setIndeterminate(true);
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                mProgressDialog.setCancelable(true);
                mProgressDialog.show();
            } else if (responseCode == ResponseCode.RESULT_USER_CANCELED) {
                if (Consts.DEBUG) {
                    Log.i(TAG, "user canceled purchase");
                }
            } else {
                if (Consts.DEBUG) {
                    Log.i(TAG, "purchase failed");
                }
            }
        }

        @Override
        public void onRestoreTransactionsResponse(RestoreTransactions request,
                ResponseCode responseCode) {
            if (responseCode == ResponseCode.RESULT_OK) {
                if (Consts.DEBUG) {
                    Log.d(TAG, "completed RestoreTransactions request");
                }
            } else {
                if (Consts.DEBUG) {
                    Log.d(TAG, "RestoreTransactions error: " + responseCode);
                }
            }
        }
    }

    private void donatedAction(final String orderId)
    {
        // purchased
        // Update the shared preferences so that we don't perform
        // a RestoreTransactions again.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(DonateActivity.this);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putInt(DONATION_COUNTER, 1);
        edit.commit();

        View view = getLayoutInflater().inflate(R.layout.subscribe, null);
        final EditText edtnickname = (EditText)view.findViewById(R.id.nickname);

        new AlertDialog.Builder(DonateActivity.this)
        .setView( view )
        .setIcon(R.drawable.icon)
        .setTitle(R.string.app_name)
//        .setMessage(R.string.label_thankyou)
        .setCancelable(true)
        .setPositiveButton(R.string.label_subscribe, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                String nickname = edtnickname.getText().toString();

                if ( nickname.length()>0 ){

                    Intent it = new Intent();
                    it.setAction(Intent.ACTION_SENDTO );
                    int mill = (int)(System.currentTimeMillis() / 1000 / 60 /60 );
                    try{
                        it.setData(Uri.parse("mailto:" + getString(R.string.label_mail_summary)
                                + "?subject=Subcribe Jota Text Editor(" + orderId + ")"
                                + "&body=nickname:%20" + URLEncoder.encode(nickname, "utf-8")
                                ));
                        startActivity(it);
                        Toast.makeText(DonateActivity.this , R.string.toast_send_mail, Toast.LENGTH_LONG).show();

                    }catch(Exception e){}
                    finish();
                }
            }
        })
        .setNegativeButton(R.string.label_close, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        })
        .show();

    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final int index;
        if ( isDebuggable() ){
            index = 1;
        }else{
            index = 0;
        }

        mjsobj.mProcBilling = new Runnable() {
            @Override
            public void run() {
                if (!mBillingService.requestPurchase(CATALOG[index], null)) {
                    showDialog(DIALOG_BILLING_NOT_SUPPORTED_ID);
                }
            }
        };

        mjsobj.mProcConfirm1 = new Runnable() {
            @Override
            public void run() {
                final String orderId = mjsobj.mOrderid.trim();
                if ( checkOrderid(orderId) ){
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            donatedAction(orderId);
                        }
                    });
                }
            }
        };

        mjsobj.mProcConfirm2 = new Runnable() {
            @Override
            public void run() {
                if (!mBillingService.restoreTransactions()) {
                    showDialog(DIALOG_BILLING_NOT_SUPPORTED_ID);
                }
            }
        };

        mHandler = new Handler();
        mDonatePurchaseObserver = new DonatePurchaseObserver(mHandler);
        mBillingService = new BillingService();
        mBillingService.setContext(this);

        // Check if billing is supported.
        if (!mBillingService.checkBillingSupported()) {
            showDialog(DIALOG_CANNOT_CONNECT_ID);
        }
    }

    /**
     * Called when this activity becomes visible.
     */
    @Override
    protected void onStart() {
        super.onStart();
        ResponseHandler.register(mDonatePurchaseObserver);
    }

    /**
     * Called when this activity is no longer visible.
     */
    @Override
    protected void onStop() {
        super.onStop();
        ResponseHandler.unregister(mDonatePurchaseObserver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBillingService.unbind();
    }


    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case DIALOG_CANNOT_CONNECT_ID:
            return createDialog(R.string.cannot_connect_title,
                    R.string.cannot_connect_message);
        case DIALOG_BILLING_NOT_SUPPORTED_ID:
            return createDialog(R.string.billing_not_supported_title,
                    R.string.billing_not_supported_message);
        default:
            return null;
        }
    }

    private Dialog createDialog(int titleId, int messageId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(titleId)
            .setIcon(android.R.drawable.stat_sys_warning)
            .setMessage(messageId)
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
        return builder.create();
    }

    public boolean isDebuggable() {
        PackageManager manager = getPackageManager();
        ApplicationInfo appInfo = null;
        try {
            appInfo = manager.getApplicationInfo(getPackageName(), 0);
        } catch (NameNotFoundException e) {
            return false;
        }
        if ((appInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) == ApplicationInfo.FLAG_DEBUGGABLE)
            return true;
        return false;
    }

    boolean checkOrderid( String orderId )
    {
        MessageDigest md;
        String result="";
        try {
            md = MessageDigest.getInstance("SHA1");
            md.reset();
            md.update(orderId.getBytes());
            md.update(orderId.getBytes());
            md.update(orderId.getBytes());
            byte[] digest = md.digest();

            result += "";
            for( byte b : digest )
            {
                result += String.format("%02x",b );
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        }

        for( String  hashedId : mOrderids ){
            if ( result.equals(hashedId) ){
                return true;
            }
        }
        return false;
    }


    static private final String[] mOrderids = new String[] {
        "6acbd91e24e0111bf7d0beb984fcb9cb0864434b",
        "d3c38e9167afc3883e0fbcfbca68c38664d5c6bc",
        "3081dc116f32d92009b278a5cee5d717e761ae12",
        "f1852ffdbb659aea6b6a2e021e38ae7bbf95207c",
        "814b81f17284e81f7e37e97a5932f36dbcfa437e",
        "1af79e624fb9b54198783bf4413ed877b8f4d5a2",
        "f184c7c07cc4706a8b12d90a2cb2d5ef7457dab0",
        "ced075ac2c578490ee44cc8df5fe7d134954490d",
        "d158e14298d9fb139c841bcae31c15bd20dd9814",
        "d3e5327a8dc486f91fc06cc36605e6c9b3deeee0",
        "7581412ca100960214e0442e6e17c08bee4662cd",
        "844c196493dd22da14128e0ab7aa28186aa13382",
        "2f57df6b6b914556f31447b9a81699e45e0a0494",
        "a55b931cf769481424d06a2cdc2ba5926f42fa92",
        "1545e435b2c3902f41a683da0e28ce74c662c1a1",
        "b45e60e6fbd8573575244fbe61cf3279bb29c69a",
        "3356cd568fc9057d6a536a15cc9729a541e00b70",
        "f5fc78f4123f1f900d754c499a522e90b7bd69bb",
        "08eac9ea806126de487a289e1fd4d4d2a11d6208",
        "12ec5769f34ad912589eb5d91071463649496956",
        "75a23ac395c1530d3dbdc7c460c6ba486f2dfd5a",
        "7072071e2dec3bbf9da8d8c2f82b0c19e9b80ccd",
        "83d1503d01f13ec4a25b11094c71a42620dac26f",
        "25495a532f83e76c2f98611de84672c20eab6e7c",
        "26edc34139e1e7c93da9635c139ab02266332894",
        "c7d19e27f345d01ab5757bbcd3d4afdc782155f7",
        "14e792941f04cca396fb02332b8124782e0c60be",
        "13897fccc0bb5e235e2af4f63a4e0d2c538c4acf",
        "3c9be4e819200416f14d59e1bb9bc35d07df2cfe",
        "7023ee2db7c1f086dc9c3ed780d5e5e76d3a47ea",
        "1de3595f1229845bbdf4ff74ae477152849fa1cc",
        "2ad6ec9e77b9f970f45fef0be4effe9be329d56e",
        "0bd63705763e00851178f965ebd2d52e42768b64",
        "6d73b63ed489fd7dd7b924147c4a608c430ee670",
        "ba0019c78d2cfd30fc36c7ee156ecba2798e45af",
        "81e0a12bdc2c3fe125c01d656b7e26ffe398fc2f",
        "069dff97da2e5d1115771ab29e565f97a5dbd9b8",
        "2a0425509a336f33d892e21f2e16eb5ee8db199d",
        "0fae1b10bc2122de973470b772c684027fc7c234",
        "cca4d78f4cf294ccb088a91cab07c5d3f7555196",
        "6cae0dc62f6fa21439d74a282412bd88b821f48e",
        "dd765c328fa036f91d42113e11edc7ba9f833443",
        "91e8d934b319a41ad1f2f720ec0e2aff3c8b473c",
        "97a04ceea731108b1feeca84c7b48d5a36619c9f",
        "adac6769d27633ef86d728bc66c606a4780fc279",
        "2267e9935b5713f4a5aa38038cf896dd634db6d3",
        "e133f5af3ed54e9d4dcdf6424b5143a303ac8d42",
        "1d2b8504bb4d9d12cc4ebd75158bb3b4f239bfce",
        "845ebd07e698f346abc6dd1f02ffef6d63150887",
        "9e53bec2b951abca4e174401a6984260f71d6a15",
        "779d929e1e66130d3b5a92d02db9f7efd2b17d55",
        "4434e56c764dc65ad1cad132a3836b75dd4a2c56",
        "c5628882cc8954016bd89c00605ef482a81dea62",
        "deb097c15fec1e166c1c5abc28e37c8954758d88",
        "303e79b6e13a0540f034b3639411560e6a80cfcd",
        "4e18b95dd4df74e6d08660ef88cc23f054a24273",
        "ffba3498b010178c80f4a103416e30c8a7d4e844",
        "a75d7515d90a100e10a659d7b37267520aae9791",
        "7a9c3600417a706a50ceea37a27da88ac2cb6599",
        "26a8c0119ef945ce89fd1b03851e7ecfc378d3c4",
        "4e25e91b47b34148bf063cd8daae69033bb0a9ad",
        "eaf4056d1af924d6138baf7ece0379a487c5139f",
        "956c62f1edfb816d96534936c60d4a0f053c6522",
        "de2e1b690c2e39d3ebfbbf100b21ee0e1b28f642",
        "28765315c1d7eec2cbe564111719c676539cd65f",
        "ac99b3411d15ac159fe07ff14f841154f6e25f3a",
        "88550149b8479c8017e87e6c9da88fd4d4054938",
        "08cdc393b6e5ad5df0e164c9b80933ced2fce337",
        "1c01a9d415d076e5b958e181ca423f9d7cc9eb5c",
        "3f7e721fc1ed43aaa5c92d62882b3a2b0d99b44f",
        "b2893649048e8cf6234052036321984172ab4d4c",
        "3eb9aa35cfa12cedd0af955f95e08cc55ee2d4cd",
        "ac86beb9a39747682e315f06c85267dff635ce06",
        "693ae185f0c3ffb88e59b75163798e8593b17f14",
        "646bf9c1b5c58606cb3e232267fce69b1e52b33b",
        "6a7289eb167cc2c263e1dc932f21b6281f415ccd",
        "1a5e1ecb162bb6ec5fb3eb9696a3d5727fdc6ab6",
        "7eeb143faa1ca01af8d5e4cc8fde0129fcf5f297",
        "11981a8710b1c958047a4cba0d7b98405efad9ca",
        "dc595c074a84fe74ee8fbeb942072c551a1e6afa",
        "9324f9ec05e62a63b1e15475f4ff666d2676dcac",
        "bf8a18d2ff8803574fc912af98727182b85809c4",
        "9e8573b46e805532fc8df76f155c0b178a4c3e4a",
        "9691b3b2038c15d631a56c41e15304870720d4c3",
        "e66ea2e34ffb6e5ec1388d0be255b73e5316315a",
        "c6cbaf4b22d89dedf5c87fc4f3d832d0469235c2",
        "22a8cbc9ee14334769bdc9a158a2d990e7935990",
        "53e0aeb0fb4c79a2ad6fa66bf6bcb53143179d00",
        "31c8a8975a2bcbb24ecaf5544a0a258f1d210268",
        "c2f48e774bf3bc99507d405f9769c80e2cd7777d",
        "db5d26dfa402efffb8adacdeb6a6a1ccaa362e4b",
        "99db39e1fdb06ba42f683555e337a0f7f607b6fd",
        "3096c2b98e13d70fc86043898a85e2de5461d331",
        "508502a2ddee8e3c34ed6fdb6249418a2338f627",
        "ed967aea56ab8a39ec2fe946c6017bce51f51290",
        "168525fef05cff1046f56eed15036dfc5b15b43f",
        "92addfaf840fd9e6fd327f2480674b71323ab4a3",
        "70f7df8cad2dfd6e500b113ee4ae6abe9f71de59",
        "6dbe760f3a3d237acbc0e08209ee900940d9c891",
        "7c1ae6680e293cc824206c4aa6efd2ddf1a260ee",
        "8342c46ec1e30c7d447eab2c080725285a2ce437",
        "a81e0339cbac418b0affc52974f89ce1ff1bf79b",
        "e4d9ad104a89e39cce6c9284c070c31c9fc19c24",
        "1f474e2d02e5c7936abb3a9cb48cab422e6247ca",
        "de290ea8bb39dbc71aefe234bc4aa007adb66289",
        "5a7fa1d1fb4acce0cd9b44227a30da31b8934f59",
        "e1724a05e022f1d33da7887de98b173883904623",
        "712cfdc6168c81e97c8c274cbe1804d0da0a9aa0",
        "3eb985c001ceb4d311367326e5a99eb302141a0b",
        "e35da007b3ce5a174a6dfe6655cffa49c56fb0de",
        "129e78de977f01c9af3e694074af5df2cf16cf1e",
        "4f689865597ca79a8ff931fca95216d31568a97a",
        "74f72a18f44c440b069a055e7de808c38c37b671",
        "eb7d8af5f7b10e2a8d6494c697c7f28c260df40d",
        "67a932f510d23566113d6583a13f27365dfe6ba8",
        "224b3deea6ee6b178bf00b109dd62cc5a6078a82",
        "91950528a6c09d38af1cc6c802058faed004b1f3",
        "485189ef91168ba26000ddbea0be8ccfbdee17dc",
        "51db3d71d36134ab3265e208fa54b349b54024bb",
        "8affeaa01a5d8dc464c64e7aa779705519a8b71f",
        "b373ce18026c3fc4cb4a90d8a5a64e9ba8d01669",
        "bb96f7334969b49d38d559f885a53ed755c4404a",
        "619da0c2cc7f3b07a9c7deecbf273a4d9e2cde19",
        "933eb7d2a2cd1024f12b5cc16179f0c75f274d2b",
        "f5399980573be6ea17d136bbfcfac3654e50f50f",
        "62ec1ec8ec66a48f6522737267d6e497b91d7a32",
        "a07a5683dc79ab89bec04dc1211bb129720178bb",
        "1313610c621c95f7c8b5c6d3347444944a14de0e",
        "f0b559509df7bf9ce1f9801448caff31248bfaa0",
        "24fa89e63140bf03d8f05916dd3e5ec8c5e8bc9e",
        "132561ebf96f529fbedd6723efc4d37b2dca3d3e",
        "472de9dcc07fdcacf6d9a015aa2c2a3b47679e2f",
        "0dd1783fa6b726b30fcc60620d1c9905154a1172",
        "cc7130621f589cadbb0afb23c3e01c7adfe0016e",
        "21ee4a70908e64171eaddc81117ae5616f203f3f",
        "5ce34e45d27d4197f77007ee55f1d0069b835a73",
        "9a53bec8672c96c264ee903c650187cc2d0b5ec0",
        "fabde61b4064b8d3593928827ee66dde2343ba86",
        "e1841370b2e6d424031121337daaade3eb216265",
        "b3ada4f7ed5957c6a5154cf795494d19577a4c5e",
        "61bce50d8e59c9a36340abdcc2020148c7391c57",
        "28f8cdefbf0793518f78ff30e3a3b6f6e5e8e148",
        "c69866dc43937ffda7130f8a843a013c2f562a54",
        "29a37fab98b96ef7c2bd363eb764625f9c870f30",
        "a05b5eca5589d74656cb0627f37d2fa43bbf939e",
        "af100106695ac746b593c0937607ca7c558dfc99",
        "a27e7a89133c5f796936776b357d4ab492f7c512",
        "7f05a687122b1315009408441c110d99fa1ff9a1",
        "9508f97c5404aaeaccf7bcf19e0cf1570d538f7b",
        "65df92e8175c24163ffd642d40e49baf55cc9d11",
        "ff67cbead6ba9cb026a837b80fab261b36ecfc1f",
        "9644d0a2fe969677012515918e7e2fd217b86898",
        "bfee3fb388e9a5b67da744cdcb21298ad8c04070",
        "b669085150749e1d41a5722a965ff744a72aee2b",
        "cb24a5bd408d68fdc3d6a750f504fd8a2221de5b",
        "3bba4e5efb28db3132053384fe952c0bc22066f7",
        "72b5b148f02693ec242502ba664eba12780c3558",
        "6f279b12cab63db1ef25f5be2813b03e64bbaa10",
        "b8e8c190db0da16fe61ae054f038336785bb4acd",
        "fa9c867dbe34bd756e5ea7db540d6a0012743bcc",
        "e2fc1b8dd38ee667668d229dc8115d420bcb8c65",
        "c2775a9f30307bdac1d51c4a40b2cf95959c1ec6",
        "8efbb644b37a9ee8bd55bf5466db5bf16293b39b",
        "7f7d73c14e7d53b54b66df426b262ed0aea2baee",
        "d72d658eab8733fac878bb18afb1a197ddbec865",
        "ffad5d99df02ccfb5e4636a4ebaed87f8ecf0006",
        "ff6c71a9bc2514aedd9b97ddf867e4b183afd8aa",
        "46e52f6fed6afe9a1495e124d670b49d28a8c49f",
        "6d0402d4f843dca3cbe022c6f3a69fc43480682c",
        "08e3f1377b9d46790857d60f3125e46ac405346f",
        "ca46c78066c280581caa9691428501f276751b04",
        "f281312b5db32bae2bf41eb5bc100bb08c455a8d",
        "4f254dde50c709ea22360bf919d53f5962a4bd25",
        "80a92a0cc991a24243ee7dea589f16cd0f996965",
        "7cdf1f2d4cc1cce712ae4b172deef114076cd40b",
        "42b395ccaca6edf846918b8c1af0d4c6997f9ecf",
        "ca879c9f3cee0ef9055e11a7b2abc3b275d4a91e",
        "39b3381821b7ec64e1f89c9b1d30c3b2c7373813",
        "45fb10880f53707e1bc3fce04d1dd581bead8914",
        "832de141a253d4d5d60e63dc9d2a0e91a486048f",
        "7967c52d431a5355a94ac4ec876f20cdbd174ead",
        "6d1ac5dd2e308210eac2932d59c3e61ff8bfc196",
        "ab511233352baab43d7dc8298003a347f7f65a0c",
        "ae57a7479b745ea14fa19ea2e640cf21e227532e",
        "130c6f6fcc2504c42ecae942be2ae4ee39fffec4",
        "7e50a08891d739fbfbd5211a74597de533e48ade",
        "b96a58c2c0e6301d1bb5e8c6fe907a410f25be55",
        "d2fa290f915e099aec1b67b2fe4527829d2ed2ab",
        "3cc4bf0e6766dd89b72034e1a450d3afd1f8abfe",
        "3a99a9b1d1ffe47ee47fc562262aa9e2fc5aa4e4",
        "e0e9a0ee2e188eff7b1112b808e823205b4c6cc5",
        "87aa9d6ad7017c5aecf4379776578db8864f7814",
        "cfe35bc3842d20f35dc6499f119dd2ca2d223422",
        "42b3231cfa31259b4526665665bf76bfdb743364",
        "a4095732bbaa02d03f7f0884d4ae7805e76fd234",
        "24fcdeb39280ec098e4a4e06db8360571f9f76c1",
        "451d516b79c796c2eb157ce5d82fb6913837ff94",
        "ca3e57350f7ff63e6c29504a497a330174908da8",
        "1490a5f6e5d2c6ae75865484dfbcfa9cb10c8cdc",
        "288fd6a9a39fb0ef678c99d6fec2abcf023e6055",
        "084848c5a7fd137b9d38125f343de880429038af",
        "fef85aab322bb424bcc1424e92dad9832e038bde",
        "175f79839a3b464cc7fc03ad23f8752cf34fd33c",
        "daabb49b6ca34d0fdd206924a9bfe37cdb7c319b",
        "ae38785f9c632e4fd764a9c7f78f05bdbec9674c",
        "ed8e3e9f42107b9e3f5c55085f71c2365695b8ee",
        "377c7a3709c173b19f59352827667579d5fdd66e",
        "3db53cee21e13614a451c596862f99edb8eaface",
        "f2d12f42c33dc08b350f7b5c12f0e0c214960ab3",
        "630228c0c674940925f12ecb512b097bed197dc5",
        "8f772d02689533783b5fc7fbf02ffd373c39cbf1",
        "5ae59f8794b397243d8db4f8ac7e54379ae0eb1e",
        "badddc3350678265ace4534e7748aeee28ceeea1",
        "4cfaa063dda83fe12044eb04f2289b6a1b6daabb",
        "e135a679dacc79d5a5fbe896927080de552ecdbd",
        "4f5b2676b6bfaa0e3bb93716d100a238699eb7eb",
        "ca195e8d8c36819709ed0a70d3b0a35e250cb2a5",
        "2184b42676664a9ea717529712a1d8cfd5d49497",
        "8239df10e4a9be4d2c1509eb53563352321a3c83",
        "e7796dd2ac7bdfb56e2428896bdea3bb2bbe087d",
        "6871b41ea55fea8adec6bd4096b13905e1e23fc9",
        "831d8b03d9e4ed3910190b4e1c0996b8f969e198",
        "52dee4fd3a0d9ee46dc13e2966b04da33cd46f1f",
        "5beb7d912b03bb2c65c4e6fb2e6a6898edd73f41",
        "a1fcf14087bd19a6a1044bf19779e09211c92f25",
        "d1cd8948534ea83bcc25bb9ff8ae6bef84d30369",
        "e65d21bb7bed57f94496285fd7bbec82c4f3a954",
        "b2baac86372145e9abd1195c5e31046d3309ad43",
        "05f3abe5e5f5f8f9d6c83ac064a6e9ddbd61ca6b",
        "336ea13530a8a0d78186936f2de0033a437b325d",
        "82f78e9f64f43d1579e044dbe8f1f9bd50cc30c1",
        "b85522f7211635adaa3b0ec3a92faedbbb87b65b",
        "7c5733c728fa5a9ca896e4d3efb06164a70045fb",
        "2c4e7b3dbbb1ca5d7d15acdec04d12151e4d5940",
        "24c377efc8cd3efd3e1a26ed5f48453431a258a4",
        "1ab8a0eef8a656e22a0dae6e12709da24d0e9db9",
        "20cf39873b6899721efa70cf284d7ac49f82f156",
        "bc8615075b239604a8ed726f3bbea32839d8666d",
        "d0f13fc28a5bf516f9252ae2db620aef999cd4c1",
        "106c981f53d4202d53fea2b4f0a56f69c74d3bd1",
        "4dff4347489c1727d304a723f40c536d33826c43",
        "9aed9edfe07147e66b1a85b84ef474c264862d40",
        "209f56c71c7b8638b5bc7e7af3549926c9d6d139",
        "443375250ba7308da8d74d8638a51c9191468a06",
        "9044c9ee2bb37948c349d0d1d5a64cffd0d51b5c",
        "989257da77930e97039d2e03c092156947d8bc71",
        "e4ac5f290a0654620d22181450c0077ff1483fee",
        "76f4607bfd0d180fd3e4d992931895ccc523a56b",
        "b8d008aa6a184e8bb5bf0aade680ac0e41e77764",
        "185f33c152c9b2ecc30889cf929a24acdd376af6",
        "a1b9b724c79102566291776b674e460d95f7912b",
        "f71c85ad75930b2d0efbb6f0d8ce5fbe66e8a8ae",
        "b916e1f8e247829c0f83b9610c1e1db64a55d013",
        "c74d729c337fe5cec60887f80ff88ff1056e1148",
        "0cb3c5fb8360034321d27754462c3297b9824137",
        "2efa7b196bee252f627a73ddadc9696f4db568bd",
        "a3697f74ec51a610ccfd4023a887f15be4bfad1d",
        "fca775611b227be1f4c7919c20eff1ba9d786b80",
        "bd13e4c75a94414e1798973e22c1dc5045a875ed",
        "e5f462b5802026e878d30ca55aab468db0a511e4",
        "695cf690cb6b6a3e54e71c6c77876effcae3c400",
        "6c2d83b667648af790d4e5965d0de534fa1c4301",
        "924ddda358c07de2b29405cbbb78d5af388916a7",
        "a2c17c42df5f2d7307fb3f75e9c86532b07b3f3f",
        "43f2c23e843b415e60dc153aebc2c76c7dcca345",
        "ab976675249ab676049704e1e72247a0d250456f",
        "142d1ae53e5383e19c6442a6cd485f8291fc00bf",
        "c8ef041af5d76f97bc296ff12277bfb4639b0f5b",
        "b227f997cfc49650039e945dcbc230ebcd5ce793",
        "7ea942b0a0f5539e739ee24735b0ccc169bd056c",
        "b44a594c744532ee310434375d55fd6db2e1348e",
        "897d76f4aa532cb1d981028451a0998fe5af2089",
        "bf98b2679ba4aede603feae2090afd256adbbec1",
        "69057d46625f1760e6a9d43d3458662b15d4a8aa",
        "89a875ff75b51c5f76649d4170ab805169f8f0de",
        "1629c050d367426899a65716d383f1fab5cedf6a",
        "f8fab69ab21c27549756497f96da7152c0942764",
        "ab4ec9c46682828860076185adc08b51084eb650",
        "1414755e4cfe7fc0003243fb104bf34c1eaaa419",
        "2f6e21dfd0b2bc85c5eaefff367cf40ac75d9b12",
        "2ce845b63eaaea8582d6c3b1762681cb062d1d23",
        "38941b5c563b0dbd7fe74e82bf4ada6a674a5b45",
        "06060d5cdc09e78454fe36ae81b7e9e833f92a6c",
        "78ae7e78f47a405ca9defff618a66a66c0f53cd5",
        "d29ec1b0514bca18776bb0db8e005c79622451c9",
        "56378c5f7b6edc5636f754e8c1ce50b8c8ee2a5d",
        "df8cdda1c7f4e5d83e3e41c798d89dcbb08b9ff4",
        "ae4357995beaf70319f006fe45d7fa25eb08aea8",
        "b501125663283a76fc0f45113267edb1fe1bdd05",
        "e86edef9e47c1b1ae6d683d62ff0d83a24366cf3",
        "5f00fbea0b62fe9221381615269a6cc012689870",
        "0c0dc4b4a4744f920c9dc664060c4199d9d81e6b",
        "c63bcd21270616f16f90be078173d0815fa1345f",
        "6053b20a6a7d46ccfcb93e8ee09842edb1d16774",
        "0b0c21d92422169f7e456e64a7d5bb51c271c5d7",
        "24b610468418550187a6e162d3e5c45cb5ba3f4a",
        "7c6de47c065e6caf692ade3c5717784fea36c178",
        "7f0b0796aa41a302c91d2d8d30fe84b191b7129a",
        "c8fa4d0b46a4da128bbe93239068942db0d17fb1",
        "2964e376d8af342b1f9d1916bd9bb72f7a4e2b0e",
        "7251ff3f885d3742755b2fee7e7bb9047dd44f59",
        "687d70aa6c3ea00f8a91a37b3e76b25651c15dce",
        "4dc339cec4d812c26141f35f03d97dcd7a938762",
        "776df6b4ea70160ba69bd11315e399a06e2fa345",
        "c11a6e81750c9e83813d722e412f8c81b228914b",
    };

}
