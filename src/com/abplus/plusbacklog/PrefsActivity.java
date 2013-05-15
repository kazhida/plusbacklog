package com.abplus.plusbacklog;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import com.abplus.plusbacklog.billing.BillingHelper;

import static com.abplus.plusbacklog.billing.BillingHelper.*;

/**
 * Copyright (C) 2013 ABplus Inc. kazhida
 * All rights reserved.
 * Author:  kazhida
 * Created: 2013/05/08 12:09
 */
public class PrefsActivity extends Activity {
    private BillingHelper billingHelper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.prefs);

        billingHelper = new BillingHelper();

        findViewById(R.id.save_prefs).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = getPreferences();
                SharedPreferences.Editor editor = prefs.edit();

                editor.putString(getString(R.string.key_space_id), getEditText(R.id.space_id));
                editor.putString(getString(R.string.key_user_id),  getEditText(R.id.user_id));
                editor.putString(getString(R.string.key_password), getEditText(R.id.password));

                editor.commit();

                //  閉じる
                finish();
            }
        });

        findViewById(R.id.billing_no_ad).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchPurchase();
            }
        });
    }

    private void launchPurchase() {
        final View button = findViewById(R.id.billing_no_ad);
        int requestCode = 11021;    //適当な値

        button.setEnabled(false);

        billingHelper.launchPurchaseFlow(this, getString(R.string.sku_no_ad), requestCode, new OnPurchaseFinishedListener() {
            @Override
            public void onPurchaseFinished(BillingHelper.Result result, BillingHelper.Purchase purchase) {
                button.setEnabled(true);

                if (result.isSuccess()) {
                    if (purchase.getSku().equals(getString(R.string.sku_no_ad))) {
                        billingHelper.savePurchaseForNoAd();
                        button.setVisibility(View.GONE);
                    }
                }
            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (! billingHelper.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        final String DEBUG_TAG = "*backlog.no_ad.billing.prefs";

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        setEditText(R.id.space_id, prefs.getString(getString(R.string.key_space_id), ""));
        setEditText(R.id.user_id, prefs.getString(getString(R.string.key_user_id), ""));
        setEditText(R.id.password, prefs.getString(getString(R.string.key_password), ""));

        switch (prefs.getInt(getString(R.string.key_no_ad), 0)) {
            case 0:
                //  確認
                Log.d(DEBUG_TAG, "check");
                billingHelper.queryInventoryAsync(new QueryInventoryFinishedListener() {
                    @Override
                    public void onQueryInventoryFinished(BillingHelper.Result result, BillingHelper.Inventory inventory) {
                        final String DEBUG_TAG = "*backlog.no_ad.billing.prefs";

                        Log.d(DEBUG_TAG, "Query inventory finished.");

                        if (result.isFailure()) {
                            Log.d(DEBUG_TAG, "Query inventory was successful.");
                        } else {
                            Log.d(DEBUG_TAG, "Query inventory was successful.");
                            boolean no_ad = inventory.hasPurchase(getString(R.string.sku_no_ad));
                            if (no_ad) {
                                billingHelper.savePurchaseForNoAd(1);
                                findViewById(R.id.billing_no_ad).setVisibility(View.GONE);
                            } else {
                                billingHelper.savePurchaseForNoAd(-1);
                                findViewById(R.id.billing_no_ad).setVisibility(View.VISIBLE);
                            }
                        }

                    }
                });
                break;
            case 1:
                Log.d(DEBUG_TAG, "no_ad");
                findViewById(R.id.billing_no_ad).setVisibility(View.GONE);
                break;
            default:
                Log.d(DEBUG_TAG, "show_ad");
                findViewById(R.id.billing_no_ad).setVisibility(View.VISIBLE);
                break;
        }
    }

    private void setEditText(int id, String text) {
        EditText edit = (EditText)findViewById(id);
        edit.setText(text);
    }

    private String getEditText(int id) {
        EditText edit = (EditText)findViewById(id);
        return edit.getText().toString().trim();
    }

    private SharedPreferences getPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(this);
    }
}
