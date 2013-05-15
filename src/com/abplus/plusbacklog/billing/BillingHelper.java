package com.abplus.plusbacklog.billing;

/*
 *  play_billingのサンプルにあるIabHelper.javaから、
 *  必要なものを抜粋して、自分なりに再構築したもの
 *  サブスクリプション関連は使わないので、省いている
 */

import android.app.Activity;
import android.app.PendingIntent;
import android.content.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import com.abplus.plusbacklog.R;
import com.android.vending.billing.IInAppBillingService;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Copyright (C) 2013 ABplus Inc. kazhida
 * All rights reserved.
 * Author:  kazhida
 * Created: 2013/05/13 9:08
 */
public class BillingHelper {
    /*
     *  最初に定数とかの宣言
     */

    // Billing response codes
    public static final int BILLING_RESPONSE_RESULT_OK                  = 0;
    public static final int BILLING_RESPONSE_RESULT_USER_CANCELED       = 1;
    public static final int BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE = 3;
    public static final int BILLING_RESPONSE_RESULT_ITEM_UNAVAILABLE    = 4;
    public static final int BILLING_RESPONSE_RESULT_DEVELOPER_ERROR     = 5;
    public static final int BILLING_RESPONSE_RESULT_ERROR               = 6;
    public static final int BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED  = 7;
    public static final int BILLING_RESPONSE_RESULT_ITEM_NOT_OWNED      = 8;

    // IAB Helper error codes
    public static final int HELPER_ERROR_BASE                   = -1000;
    public static final int HELPER_REMOTE_EXCEPTION             = -1001;
    public static final int HELPER_BAD_RESPONSE                 = -1002;
    public static final int HELPER_VERIFICATION_FAILED          = -1003;
    public static final int HELPER_SEND_INTENT_FAILED           = -1004;
    public static final int HELPER_USER_CANCELLED               = -1005;
    public static final int HELPER_UNKNOWN_PURCHASE_RESPONSE    = -1006;
    public static final int HELPER_MISSING_TOKEN                = -1007;
    public static final int HELPER_UNKNOWN_ERROR                = -1008;
    public static final int HELPER_SUBSCRIPTIONS_NOT_AVAILABLE  = -1009;
    public static final int HELPER_INVALID_CONSUMPTION          = -1010;

    // Keys for the responses from InAppBillingService
    public static final String RESPONSE_CODE                     = "RESPONSE_CODE";
    public static final String RESPONSE_GET_SKU_DETAILS_LIST     = "DETAILS_LIST";
    public static final String RESPONSE_BUY_INTENT               = "BUY_INTENT";
    public static final String RESPONSE_INAPP_PURCHASE_DATA      = "INAPP_PURCHASE_DATA";
    public static final String RESPONSE_INAPP_SIGNATURE          = "INAPP_DATA_SIGNATURE";
    public static final String RESPONSE_INAPP_ITEM_LIST          = "INAPP_PURCHASE_ITEM_LIST";
    public static final String RESPONSE_INAPP_PURCHASE_DATA_LIST = "INAPP_PURCHASE_DATA_LIST";
    public static final String RESPONSE_INAPP_SIGNATURE_LIST     = "INAPP_DATA_SIGNATURE_LIST";
    public static final String INAPP_CONTINUATION_TOKEN          = "INAPP_CONTINUATION_TOKEN";

    public static final String ITEM_TYPE_INAPP = "inapp";
    public static final String GET_SKU_DETAILS_ITEM_LIST = "ITEM_ID_LIST";

    /*
     *  クラス変数
     */
    static private Context context = null;
    static private Connection connection = null;
    static private AsyncFlag asyncFlag = null;
    static private String publicKey = "";

    /*
     *  インスタンス変数
     */
    private OnPurchaseFinishedListener purchaseListener;
    private Security security;
    private int requestCode;

    /**
     * コンストラクタ
     * @param aContext      コンテキスト
     */
    public BillingHelper(Context aContext) {
        if (aContext != null) {
            if (context == null) {
                context = aContext;
                publicKey = aContext.getString(R.string.public_key_64);
            }
        } else if (context != null) {
            publicKey = context.getString(R.string.public_key_64);
        }
        if (asyncFlag == null) {
            asyncFlag = new AsyncFlag();
        }
        security = new Security(publicKey);
    }

    public BillingHelper() {
        this(null);
    }

    /**
     * このクラスで使用する内部クラス
     */


    /**
     * Callback for setup process. This listener's {@link #onSetupFinished} method is called
     * when the setup process is complete.
     */
    public interface OnSetupFinishedListener {
        public void onSetupFinished(Result result);
    }

    /**
     * Callback that notifies when a purchase is finished.
     */
    public interface OnPurchaseFinishedListener {
        /**
         * Called to notify that an in-app purchase finished. If the purchase was successful,
         * then the sku parameter specifies which item was purchased. If the purchase failed,
         * the sku and extraData parameters may or may not be null, depending on how far the purchase
         * process went.
         *
         * @param result The result of the purchase.
         * @param info The purchase information (null if purchase failed)
         */
        public void onPurchaseFinished(Result result, Purchase info);
    }

    /**
     * Listener that notifies when an inventory query operation completes.
     */
    public interface QueryInventoryFinishedListener {
        /**
         * Called to notify that an inventory query operation completed.
         *
         * @param result The result of the operation.
         * @param inventory The inventory.
         */
        public void onQueryInventoryFinished(Result result, Inventory inventory);
    }

    /**
     * このクラス用の例外
     */
    public class BillingException extends Exception {
        Result result;

        public BillingException(Result result) {
            this(result, null);
        }
        public BillingException(int response, String message) {
            this(new Result(response, message));
        }
        public BillingException(Result result, Exception cause) {
            super(result.getMessage(), cause);
            this.result = result;
        }
        public BillingException(int response, String message, Exception cause) {
            this(new Result(response, message), cause);
        }

        public Result getResult() {
            return result;
        }
    }

    /**
     * ServerConnection
     */
    private class Connection implements ServiceConnection {
        IInAppBillingService billingService;
        OnSetupFinishedListener listener;

        Connection(OnSetupFinishedListener listener) {
            this.listener = listener;
        }

        public Bundle getBuyIntent(String sku, String extraData) throws RemoteException {
            return billingService.getBuyIntent(3, context.getPackageName(), sku, ITEM_TYPE_INAPP, extraData);
        }

        public Bundle getPurchases(String continueToken) throws RemoteException {
            return billingService.getPurchases(3, context.getPackageName(), ITEM_TYPE_INAPP, continueToken);
        }

        public Bundle getSkuDetails(Bundle querySkus) throws RemoteException {
            return billingService.getSkuDetails(3, context.getPackageName(), ITEM_TYPE_INAPP, querySkus);
        }


        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            billingService =  IInAppBillingService.Stub.asInterface(service);
            String packageName = context.getPackageName();
            try {
                // check for in-app billing v3 support
                int response = billingService.isBillingSupported(3, packageName, ITEM_TYPE_INAPP);
                if (response != BILLING_RESPONSE_RESULT_OK) {
                    if (listener != null) {
                        listener.onSetupFinished(new Result(response,
                                "Error checking for billing v3 support."));
                    }
                    return;
                }
            } catch (RemoteException e) {
                if (listener != null) {
                    listener.onSetupFinished(new Result(HELPER_REMOTE_EXCEPTION,
                            "RemoteException while setting up in-app billing."));
                }
                e.printStackTrace();
                return;
            }

            if (listener != null) {
                listener.onSetupFinished(new Result(BILLING_RESPONSE_RESULT_OK, "Setup successful."));
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            billingService = null;
        }
    }

    /**
     * Represents the result of an in-app billing operation.
     * A result is composed of a response code (an integer) and possibly a
     * message (String). You can get those by calling
     * {@link #getResponse} and {@link #getMessage()}, respectively. You
     * can also inquire whether a result is a success or a failure by
     * calling {@link #isSuccess()} and {@link #isFailure()}.
     */
    public class Result {
        private int response;
        private String message;

        public Result(int response, String message) {
            this.response = response;

            if (message == null || message.trim().length() == 0) {
                this.message = getResponseDesc(response);
            }
            else {
                this.message = message + " (response: " + getResponseDesc(response) + ")";
            }
        }
        public int getResponse() {
            return response;
        }
        public String getMessage() {
            return message;
        }
        public boolean isSuccess() {
            return response == BILLING_RESPONSE_RESULT_OK;
        }
        public boolean isFailure() {
            return !isSuccess();
        }
        public String toString() {
            return "Result: " + getMessage();
        }

    }

    /**
     * Represents an in-app billing purchase.
     */
    public class Purchase {
        String orderId;
        String packageName;
        String sku;
        long purchaseTime;
        int purchaseState;
        String developerPayload;
        String token;
        String originalJson;
        String signature;

        public Purchase(String jsonPurchaseInfo, String signature) throws JSONException {
            originalJson = jsonPurchaseInfo;

            JSONObject o = new JSONObject(originalJson);
            orderId          = o.optString("orderId");
            packageName      = o.optString("packageName");
            sku              = o.optString("productId");
            purchaseTime     = o.optLong("purchaseTime");
            purchaseState    = o.optInt("purchaseState");
            developerPayload = o.optString("developerPayload");
            token            = o.optString("token", o.optString("purchaseToken"));

            this.signature = signature;
        }

        public String getItemType() {
            return ITEM_TYPE_INAPP;
        }
        public String getOrderId() {
            return orderId;
        }
        public String getPackageName() {
            return packageName;
        }
        public String getSku() {
            return sku;
        }
        public long getPurchaseTime() {
            return purchaseTime;
        }
        public int getPurchaseState() {
            return purchaseState;
        }
        public String getDeveloperPayload() {
            return developerPayload;
        }
        public String getToken() {
            return token;
        }
        public String getOriginalJson() {
            return originalJson;
        }
        public String getSignature() {
            return signature;
        }

        @Override
        public String toString() { return "PurchaseInfo(type:" + ITEM_TYPE_INAPP + "):" + originalJson; }
    }

    public class SkuDetails {
        String sku;
        String type;
        String price;
        String title;
        String description;
        String json;

        public SkuDetails(String jsonSkuDetails) throws JSONException {
            json = jsonSkuDetails;

            JSONObject o = new JSONObject(json);
            sku         = o.optString("productId");
            type        = o.optString("type");
            price       = o.optString("price");
            title       = o.optString("title");
            description = o.optString("description");
        }

        public String getSku() {
            return sku;
        }
        public String getType() {
            return type;
        }
        public String getPrice() {
            return price;
        }
        public String getTitle() {
            return title;
        }
        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return "SkuDetails:" + json;
        }
    }

    public class Inventory {
        Map<String, SkuDetails> skuMap = new HashMap<String, SkuDetails>();
        Map<String, Purchase> purchaseMap = new HashMap<String, Purchase>();

        Inventory() { }

        public SkuDetails getSkuDetails(String sku) {
            return skuMap.get(sku);
        }
        public Purchase getPurchase(String sku) {
            return purchaseMap.get(sku);
        }
        public boolean hasPurchase(String sku) {
            return purchaseMap.containsKey(sku);
        }
        public boolean hasDetails(String sku) {
            return skuMap.containsKey(sku);
        }

        public void erasePurchase(String sku) {
            if (purchaseMap.containsKey(sku)) purchaseMap.remove(sku);
        }

        List<String> getAllOwnedSkus() {
            return new ArrayList<String>(purchaseMap.keySet());
        }

        List<String> getAllOwnedSkus(String itemType) {
            List<String> result = new ArrayList<String>();
            for (Purchase p : purchaseMap.values()) {
                if (p.getItemType().equals(itemType)) result.add(p.getSku());
            }
            return result;
        }

        List<Purchase> getAllPurchases() {
            return new ArrayList<Purchase>(purchaseMap.values());
        }

        void addSkuDetails(SkuDetails d) {
            skuMap.put(d.getSku(), d);
        }

        void addPurchase(Purchase p) {
            purchaseMap.put(p.getSku(), p);
        }
    }

    /**
     * Async
     */
    private class AsyncFlag {
        boolean inProgress = false;
        String  operation = "";

        synchronized void start(String operation) {
            if (inProgress) {
                String cannot = "Can't start async operation (" + operation + ")";
                String because = " because another async operation(" + this.operation + ") is in progress.";
                throw new IllegalStateException(cannot + because);
            }
            this.operation = operation;
            inProgress = true;
        }

        synchronized void end() {
            operation = "";
            inProgress = false;
        }
    }

    /**
     * Security-related methods. For a secure implementation, all of this code
     * should be implemented on a server that communicates with the
     * application on the device. For the sake of simplicity and clarity of this
     * example, this code is included here and is executed on the device. If you
     * must verify the purchases on the phone, you should obfuscate this code to
     * make it harder for an attacker to replace the code with stubs that treat all
     * purchases as verified.
     */
    public class Security {
        private static final String TAG = "IABUtil/Security";
        private static final String KEY_FACTORY_ALGORITHM = "RSA";
        private static final String SIGNATURE_ALGORITHM = "SHA1withRSA";
        private PublicKey key;

        Security(String base64PublicKey) {
            key = generatePublicKey(base64PublicKey);
        }

        /**
         * Verifies that the data was signed with the given signature, and returns
         * the verified purchase. The data is in JSON format and signed
         * with a private key. The data also contains the PurchaseState
         * and product ID of the purchase.
         * @param signedData the signed JSON string (signed, not encrypted)
         * @param signature the signature for the data, signed with the private key
         */
        public boolean verifyPurchase(String signedData, String signature) {
            if (signedData == null) {
                Log.e(TAG, "data is null");
                return false;
            }

            if (TextUtils.isEmpty(signature) || verify(signedData, signature)) {
                return true;
            } else {
                Log.w(TAG, "signature does not match data.");
                return false;
            }
        }

        /**
         * Generates a PublicKey instance from a string containing the
         * Base64-encoded public key.
         *
         * @param encodedPublicKey Base64-encoded public key
         * @throws IllegalArgumentException if encodedPublicKey is invalid
         */
        private PublicKey generatePublicKey(String encodedPublicKey) {
            try {
                byte[] decodedKey = Base64.decode(encodedPublicKey);
                KeyFactory keyFactory = KeyFactory.getInstance(KEY_FACTORY_ALGORITHM);
                return keyFactory.generatePublic(new X509EncodedKeySpec(decodedKey));
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            } catch (InvalidKeySpecException e) {
                Log.e(TAG, "Invalid key specification.");
                throw new IllegalArgumentException(e);
            } catch (Base64DecoderException e) {
                Log.e(TAG, "Base64 decoding failed.");
                throw new IllegalArgumentException(e);
            }
        }

        /**
         * Verifies that the signature from the server matches the computed
         * signature on the data.  Returns true if the data is correctly signed.
         *
         * @param signedData signed data from server
         * @param signature server signature
         * @return true if the data and signature match
         */
        private boolean verify(String signedData, String signature) {
            Signature sig;
            try {
                sig = Signature.getInstance(SIGNATURE_ALGORITHM);
                sig.initVerify(key);
                sig.update(signedData.getBytes());
                if (!sig.verify(Base64.decode(signature))) {
                    Log.e(TAG, "Signature verification failed.");
                    return false;
                }
                return true;
            } catch (NoSuchAlgorithmException e) {
                Log.e(TAG, "NoSuchAlgorithmException.");
            } catch (InvalidKeyException e) {
                Log.e(TAG, "Invalid key specification.");
            } catch (SignatureException e) {
                Log.e(TAG, "Signature exception.");
            } catch (Base64DecoderException e) {
                Log.e(TAG, "Base64 decoding failed.");
            }
            return false;
        }
    }

    /**
     * Starts the setup process. This will start up the setup process asynchronously.
     * You will be notified through the listener when the setup process is complete.
     * This method is safe to call from a UI thread.
     *
     * @param listener The listener to notify when the setup process is complete.
     */
    public void startSetup(final OnSetupFinishedListener listener) {
        if (connection == null) {
            connection = new Connection(listener);
            Intent intent = new Intent("com.android.vending.billing.InAppBillingService.BIND");

            if (! context.getPackageManager().queryIntentServices(intent, 0).isEmpty()) {
                context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
            } else if (listener != null) {
                String message = context.getString(R.string.billing_service_unavailable);
                Result result = new Result(BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE, message);
                listener.onSetupFinished(result);
            }
        }
    }

    /**
     * Dispose of object, releasing resources. It's very important to call this
     * method when you are done with this object. It will release any resources
     * used by it such as connection connections. Naturally, once the object is
     * disposed of, it can't be used again.
     */
    public void dispose() {
        if (connection != null) {
            if (context != null) context.unbindService(connection);
            connection = null;
            purchaseListener = null;
        }
    }


    public void launchPurchaseFlow(Activity activity, String sku, int requestCode, OnPurchaseFinishedListener listener) {
        launchPurchaseFlow(activity, sku, requestCode, listener, "");
    }

    public void launchPurchaseFlow(Activity activity, String sku, int requestCode,
                                   OnPurchaseFinishedListener listener, String extraData) {
        checkSetupDone("launchPurchaseFlow");
        asyncFlag.start("launchPurchaseFlow");
        Result result;

        try {
            Bundle buyIntentBundle = connection.getBuyIntent(sku, extraData);
            int response = getResponseCodeFromBundle(buyIntentBundle);
            if (response != BILLING_RESPONSE_RESULT_OK) {
                result = new Result(response, "Unable to buy item");
                if (listener != null) listener.onPurchaseFinished(result, null);
            } else {
                PendingIntent intent = buyIntentBundle.getParcelable(RESPONSE_BUY_INTENT);
                this.requestCode = requestCode;
                purchaseListener = listener;
                activity.startIntentSenderForResult(intent.getIntentSender(), requestCode, new Intent(), 0, 0, 0);
            }
        } catch (IntentSender.SendIntentException e) {
            result = new Result(HELPER_SEND_INTENT_FAILED, "Failed to send intent.");
            if (listener != null) listener.onPurchaseFinished(result, null);
        } catch (RemoteException e) {
            result = new Result(HELPER_REMOTE_EXCEPTION, "Remote exception while starting purchase flow");
            if (listener != null) listener.onPurchaseFinished(result, null);
        }
    }

    /**
     * Handles an activity result that's part of the purchase flow in in-app billing. If you
     * are calling {@link #launchPurchaseFlow}, then you must call this method from your
     * Activity's {@link android.app.Activity@onActivityResult} method. This method
     * MUST be called from the UI thread of the Activity.
     *
     * @param requestCode The requestCode as you received it.
     * @param resultCode The resultCode as you received it.
     * @param data The data (Intent) as you received it.
     * @return Returns true if the result was related to a purchase flow and was handled;
     *     false if the result was not related to a purchase, in which case you should
     *     handle it normally.
     */
    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        Result result;
        if (requestCode != this.requestCode) return false;

        checkSetupDone("handleActivityResult");

        asyncFlag.end();

        if (data == null) {
            result = new Result(HELPER_BAD_RESPONSE, "Null data in IAB result");
            if (purchaseListener != null) {
                purchaseListener.onPurchaseFinished(result, null);
            }
            return true;
        }

        int responseCode = getResponseCodeFromIntent(data);
        String purchaseData = data.getStringExtra(RESPONSE_INAPP_PURCHASE_DATA);
        String dataSignature = data.getStringExtra(RESPONSE_INAPP_SIGNATURE);

        if (resultCode == Activity.RESULT_OK && responseCode == BILLING_RESPONSE_RESULT_OK) {

            if (purchaseData == null || dataSignature == null) {
                result = new Result(HELPER_UNKNOWN_ERROR, "IAB returned null purchaseData or dataSignature");
                if (purchaseListener != null) {
                    purchaseListener.onPurchaseFinished(result, null);
                }
                return true;
            }

            Purchase purchase;
            try {
                purchase = new Purchase(purchaseData, dataSignature);
                String sku = purchase.getSku();

                if (! security.verifyPurchase(purchaseData, dataSignature)) {
                    result = new Result(HELPER_VERIFICATION_FAILED, "Signature verification failed for sku " + sku);
                    if (purchaseListener != null) {
                        purchaseListener.onPurchaseFinished(result, purchase);
                    }
                    return true;
                }
            } catch (JSONException e) {
                e.printStackTrace();
                result = new Result(HELPER_BAD_RESPONSE, "Failed to parse purchase data.");
                if (purchaseListener != null) {
                    purchaseListener.onPurchaseFinished(result, null);
                }
                return true;
            }

            if (purchaseListener != null) {
                purchaseListener.onPurchaseFinished(new Result(BILLING_RESPONSE_RESULT_OK, "Success"), purchase);
            }
        }
        else if (resultCode == Activity.RESULT_OK) {
            if (purchaseListener != null) {
                result = new Result(responseCode, "Problem purchashing item.");
                purchaseListener.onPurchaseFinished(result, null);
            }
        }
        else if (resultCode == Activity.RESULT_CANCELED) {
            result = new Result(HELPER_USER_CANCELLED, "User canceled.");
            if (purchaseListener != null) {
                purchaseListener.onPurchaseFinished(result, null);
            }
        }
        else {
            result = new Result(HELPER_UNKNOWN_PURCHASE_RESPONSE, "Unknown purchase response.");
            if (purchaseListener != null) purchaseListener.onPurchaseFinished(result, null);
        }
        return true;
    }

    public Inventory queryInventory(boolean querySkuDetails, List<String> moreItem) throws BillingException {
        checkSetupDone("queryInventory");

        try {
            Inventory inv = new Inventory();
            int r = queryPurchases(inv);

            if (r != BILLING_RESPONSE_RESULT_OK) {
                throw new BillingException(r, "Error refreshing inventory (querying owned items).");
            }
            if (querySkuDetails) {
                r = querySkuDetails(inv, moreItem);
                if (r != BILLING_RESPONSE_RESULT_OK) {
                    throw new BillingException(r, "Error refreshing inventory (querying prices of items).");
                }
            }

            return inv;
        } catch (RemoteException e) {
            throw new BillingException(HELPER_REMOTE_EXCEPTION, "Remote exception while refreshing inventory.", e);
        } catch (JSONException e) {
            throw new BillingException(HELPER_BAD_RESPONSE, "Error parsing JSON response while refreshing inventory.", e);
        }
    }
    public Inventory queryInventory(boolean querySkuDetails) throws BillingException {
        return queryInventory(querySkuDetails, null);
    }


    /**
     * Asynchronous wrapper for inventory query. This will perform an inventory
     * query as described in {@link #queryInventory}, but will do so asynchronously
     * and call back the specified listener upon completion. This method is safe to
     * call from a UI thread.
     *
     * @param querySkuDetails as in {@link #queryInventory}
     * @param moreSkus as in {@link #queryInventory}
     * @param listener The listener to notify when the refresh operation completes.
     */
    public void queryInventoryAsync(final boolean querySkuDetails,
                                    final List<String> moreSkus,
                                    final QueryInventoryFinishedListener listener) {
        final Handler handler = new Handler();
        checkSetupDone("queryInventory");
        asyncFlag.start("refresh inventory");
        (new Thread(new Runnable() {
            public void run() {
                Result result = new Result(BILLING_RESPONSE_RESULT_OK, "Inventory refresh successful.");
                Inventory inv = null;
                try {
                    inv = queryInventory(querySkuDetails, moreSkus);
                } catch (BillingException ex) {
                    result = ex.getResult();
                }

                asyncFlag.end();

                final Result result_f = result;
                final Inventory inv_f = inv;
                handler.post(new Runnable() {
                    public void run() {
                        listener.onQueryInventoryFinished(result_f, inv_f);
                    }
                });
            }
        })).start();
    }

    public void queryInventoryAsync(QueryInventoryFinishedListener listener) {
        queryInventoryAsync(true, null, listener);
    }

    public void queryInventoryAsync(boolean querySkuDetails, QueryInventoryFinishedListener listener) {
        queryInventoryAsync(querySkuDetails, null, listener);
    }

    void checkSetupDone(String operation) {
        if (connection == null) {
            throw new IllegalStateException("IAB helper is not set up. Can't perform operation: " + operation);
        }
    }

    // Workaround to bug where sometimes response codes come as Long instead of Integer
    int getResponseCodeFromBundle(Bundle b) {
        Object o = b.get(RESPONSE_CODE);
        if (o == null) {
            return BILLING_RESPONSE_RESULT_OK;
        } else if (o instanceof Integer) {
            return (Integer)o;
        } else if (o instanceof Long) {
            return (int)((Long)o).longValue();
        } else {
            throw new RuntimeException("Unexpected type for bundle response code: " + o.getClass().getName());
        }
    }

    // Workaround to bug where sometimes response codes come as Long instead of Integer
    int getResponseCodeFromIntent(Intent i) {
        return getResponseCodeFromBundle(i.getExtras());
    }

    int queryPurchases(Inventory inv) throws JSONException, RemoteException {
        boolean verificationFailed = false;
        String continueToken = null;

        do {
            Bundle ownedItems = connection.getPurchases(continueToken);

            int response = getResponseCodeFromBundle(ownedItems);
            if (response != BILLING_RESPONSE_RESULT_OK) {
                return response;
            }
            if (!ownedItems.containsKey(RESPONSE_INAPP_ITEM_LIST)
                    || !ownedItems.containsKey(RESPONSE_INAPP_PURCHASE_DATA_LIST)
                    || !ownedItems.containsKey(RESPONSE_INAPP_SIGNATURE_LIST)) {
                return HELPER_BAD_RESPONSE;
            }

            ArrayList<String> purchaseDataList = ownedItems.getStringArrayList(RESPONSE_INAPP_PURCHASE_DATA_LIST);
            ArrayList<String> signatureList = ownedItems.getStringArrayList(RESPONSE_INAPP_SIGNATURE_LIST);

            for (int i = 0; i < purchaseDataList.size(); ++i) {
                String purchaseData = purchaseDataList.get(i);
                String signature = signatureList.get(i);
                if (security.verifyPurchase(purchaseData, signature)) {
                    Purchase purchase = new Purchase(purchaseData, signature);
                    inv.addPurchase(purchase);
                } else {
                    verificationFailed = true;
                }
            }

            continueToken = ownedItems.getString(INAPP_CONTINUATION_TOKEN);
        } while (!TextUtils.isEmpty(continueToken));

        return verificationFailed ? HELPER_VERIFICATION_FAILED : BILLING_RESPONSE_RESULT_OK;
    }

    int querySkuDetails(Inventory inv, List<String> moreSkus) throws RemoteException, JSONException {
        ArrayList<String> skuList = new ArrayList<String>();
        skuList.addAll(inv.getAllOwnedSkus(ITEM_TYPE_INAPP));
        if (moreSkus != null) skuList.addAll(moreSkus);

        if (skuList.size() == 0) {
            return BILLING_RESPONSE_RESULT_OK;
        }

        Bundle querySkus = new Bundle();
        querySkus.putStringArrayList(GET_SKU_DETAILS_ITEM_LIST, skuList);
        Bundle skuDetails = connection.getSkuDetails(querySkus);

        if (!skuDetails.containsKey(RESPONSE_GET_SKU_DETAILS_LIST)) {
            int response = getResponseCodeFromBundle(skuDetails);
            if (response != BILLING_RESPONSE_RESULT_OK) {
                return response;
            }
            else {
                return HELPER_BAD_RESPONSE;
            }
        }

        ArrayList<String> responseList = skuDetails.getStringArrayList(
                RESPONSE_GET_SKU_DETAILS_LIST);

        for (String thisResponse : responseList) {
            SkuDetails d = new SkuDetails(thisResponse);
            inv.addSkuDetails(d);
        }
        return BILLING_RESPONSE_RESULT_OK;
    }

    public static String getResponseDesc(int code) {
        String[] iab_msgs = (
                "0:OK/1:User Canceled/2:Unknown/" +
                "3:Billing Unavailable/4:Item unavailable/" +
                "5:Developer Error/6:Error/7:Item Already Owned/" +
                "8:Item not owned").split("/");
        String[] iabhelper_msgs = (
                "0:OK/-1001:Remote exception during initialization/" +
                "-1002:Bad response received/" +
                "-1003:Purchase signature verification failed/" +
                "-1004:Send intent failed/" +
                "-1005:User cancelled/" +
                "-1006:Unknown purchase response/" +
                "-1007:Missing token/" +
                "-1008:Unknown error/" +
                "-1009:Subscriptions not available/" +
                "-1010:Invalid consumption attempt").split("/");

        if (code <= HELPER_ERROR_BASE) {
            int index = HELPER_ERROR_BASE - code;
            if (0 <= index && index < iabhelper_msgs.length) {
                return iabhelper_msgs[index];
            } else {
                return String.valueOf(code) + ":Unknown IAB Helper Error";
            }
        } else if (0 <= code && code < iab_msgs.length) {
            return iab_msgs[code];
        } else {
            return String.valueOf(code) + ":Unknown";
        }
    }

    public void savePurchaseForNoAd(int value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(context.getString(R.string.key_no_ad), value);
        editor.commit();
    }

    public void savePurchaseForNoAd() {
        savePurchaseForNoAd(1);
    }
}

