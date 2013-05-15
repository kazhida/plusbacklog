package com.abplus.plusbacklog;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.*;
import android.view.animation.ScaleAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.abplus.plusbacklog.billing.BillingHelper;
import com.abplus.plusbacklog.parsers.Components;
import com.abplus.plusbacklog.parsers.IssueTypes;
import com.abplus.plusbacklog.parsers.Projects;
import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private Projects.Project         project = null;
    private IssueTypes.IssueType     issueType = null;
    private Components.Component     component = null;
    private PriorityAdapter.Priority priority = null;
    private String                   savedKey = null;
    private AdView                   adView = null;
    private boolean                  noPrefs = true;
    private BillingHelper            billingHelper = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        setContentView(R.layout.main);

        adView = appendAdView();

        findViewById(R.id.project_spinner).setVisibility(View.GONE);
        findViewById(R.id.issue_attribute_panel).setVisibility(View.GONE);

        setSpinnerListener(R.id.project_spinner, new ProjectSelectedLister());
        setSpinnerListener(R.id.issue_type_spinner, new IssueTypeSelectedListener());
        setSpinnerListener(R.id.component_spinner, new ComponentSelectedListener());
        setSpinnerListener(R.id.priority_spinner, new PrioritySelectedListener());
        setSpinnerAdapter(R.id.priority_spinner, new PriorityAdapter(), 1);
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final String spaceId = prefs.getString(getString(R.string.key_space_id), "");
        final String userId = prefs.getString(getString(R.string.key_user_id), "");
        final String password = prefs.getString(getString(R.string.key_password), "");

        boolean checkInventory = false;
        noPrefs = false;
        savedKey = prefs.getString(getString(R.string.key_project), null);

        if (spaceId.length() == 0 || userId.length() == 0 || password.length() == 0) {
            noPrefs = true;
        } else if (findViewById(R.id.project_spinner).getVisibility() == View.GONE) {
            switch (prefs.getInt(getString(R.string.key_no_ad), 0)) {
                case 0:
                    //  確認
                    checkInventory = true;
                    break;
                case 1:
                    //  adView非表示
                    hideAd();
                    break;
                default:
                    //  adView表示
                    showAd();
                    break;
            }
        }
        //  広告関連の処理
        final boolean needQuery = checkInventory;
        final String DEBUG_TAG = "+backlog.no_ad.billing";

        if (billingHelper == null) {
            billingHelper = new BillingHelper(this);
            Log.d(DEBUG_TAG, "Setup start.");
            billingHelper.startSetup(new BillingHelper.OnSetupFinishedListener() {
                @Override
                public void onSetupFinished(BillingHelper.Result result) {
                    Log.d(DEBUG_TAG, "Setup finished.");

                    if (result.isFailure()) {
                        Log.d(DEBUG_TAG, "Setup failed.");
                    } else if (needQuery) {
                        Log.d(DEBUG_TAG, "Setup successful. Querying inventory.");
                        try {
                            BillingHelper.Inventory inventory = billingHelper.queryInventory(false);
                            Log.d(DEBUG_TAG, "Query inventory was successful.");
                            boolean no_ad = inventory.hasPurchase(getString(R.string.sku_no_ad));
                            if (no_ad) {
                                billingHelper.savePurchaseForNoAd(1);
                                hideAd();
                            } else {
                                billingHelper.savePurchaseForNoAd(-1);
                                showAd();
                            }
                        } catch (BillingHelper.BillingException e) {
                            billingHelper.savePurchaseForNoAd(-1);
                            showAd();
                        }
                    }
                }
            });
        }
        showInit(spaceId, userId, password);
    }


    private void showInit(String spaceId, String userId, String password) {
        if (noPrefs) {
            showPreferences();
        } else {
            BackLogCache cache = BackLogCache.sharedInstance();

            if (cache == null || !spaceId.equals(cache.spaceId()) || !userId.equals(cache.userId())) {
                cache = BackLogCache.initSharedInstance(this, new BacklogIO(spaceId, userId, password));
            }
            resetCache(spaceId, userId, password);

            cache.getUserIcon(userId, new BackLogCache.CacheResponseNotify() {
                @Override
                public void success(BaseAdapter adapter) {
                    //  なにもしない
                }

                @Override
                public void success(Drawable icon) {
                    ImageView view = (ImageView)findViewById(R.id.icon);
                    view.setImageDrawable(icon);
                }

                @Override
                public void success(int code, String response) {
                    //  なにもしない
                }

                @Override
                public void failed(int code, String response) {
                    showError(R.string.cant_load, "ERROR STATUS = " + code);
                }

                @Override
                public void error(Exception e) {
                    showError(R.string.cant_load, e.getLocalizedMessage());
                }
            });
        }
    }

    private void showAd() {
        adView.setVisibility(View.VISIBLE);
        adView.loadAd(new AdRequest());
    }

    private void hideAd() {
        adView.stopLoading();
        adView.setVisibility(View.GONE);
    }

    /**
     * 広告ビューを作って、アクティビティに追加する
     */
    private AdView appendAdView() {
        AdView result = adView;

        if (result == null) {
            result = new AdView(this, AdSize.BANNER, getString(R.string.publisher_id));

            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            result.setLayoutParams(params);

            FrameLayout frame = (FrameLayout)findViewById(R.id.ad_frame);
            frame.addView(result);

            AdRequest adRequest = new AdRequest();

            result.loadAd(adRequest);
        }

        return result;
    }

    @Override
    public void onDestroy() {
        if (adView != null) adView.destroy();
        super.onDestroy();
    }


    @Override
    protected void onPause() {
        super.onPause();

        if (project != null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(getString(R.string.key_project), savedKey);
            editor.commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.menu_config:
                showPreferences();
                return true;
            case R.id.menu_reload:
                loadProjects();
                return true;
            case R.id.menu_post:
                String summary = getEntryText(R.id.summary);
                String description = getEntryText(R.id.description);
                if (summary == null || summary.isEmpty()) {
                    showToast(R.string.summary_is_empty);
                } else  {
                    final ProgressDialog waitDialog = showWait(getString(R.string.sending));
                    BackLogCache cache = BackLogCache.sharedInstance();

                    cache.getIO().createIssue(summary, description,
                            project, issueType, component, priority, new BacklogIO.ResponseNotify() {
                        @Override
                        public void success(int code, String response) {
                            waitDialog.dismiss();
                            Log.d("+backlog.post_issue", response);
                            setEntryText(R.id.summary, null);
                            setEntryText(R.id.description, null);

                            InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            manager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);

                            showToast(R.string.registered_issue);
                        }

                        @Override
                        public void failed(int code, String response) {
                            waitDialog.dismiss();
                            showError(R.string.cant_register, "Error STATUS=" + code);
                        }

                        @Override
                        public void error(Exception e) {
                            waitDialog.dismiss();
                            showError(R.string.cant_register, "Error: " + e.getLocalizedMessage());
                        }
                    });
                }
                return true;
        }
        return false;
    }

    private void showPreferences() {
        Intent intent = new Intent(this, PrefsActivity.class);

        intent.setAction(Intent.ACTION_VIEW);

        startActivity(intent);
    }

    private void showSpinner(int id) {
        //  0.2秒かけてしゅかっとでる
        View view = findViewById(id);
        view.setVisibility(View.VISIBLE);
        ScaleAnimation animation = new ScaleAnimation(1.0f, 1.0f, 0.0f, 1.0f);
        animation.setDuration(200);
        view.startAnimation(animation);
    }

    private void showToast(String msg, int duration) {
        Toast.makeText(this, msg, duration).show();
    }

    private void showToast(int msg_id, int duration) {
        Toast.makeText(this, msg_id, duration).show();
    }

    private void showToast(int msg_id) {
        showToast(msg_id, Toast.LENGTH_SHORT);
    }

    private void showError(int msg_id, String msg) {
        showToast(getString(msg_id) + "  " + msg, Toast.LENGTH_LONG);
    }

    private ProgressDialog showWait(String msg) {
        ProgressDialog result = new ProgressDialog(this);
        result.setMessage(msg);
        result.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        result.show();
        return result;
    }

    private void loadProjects() {
        final String key = project != null ? project.getKey() : savedKey;

        BackLogCache cache = BackLogCache.sharedInstance();
        findViewById(R.id.project_spinner).setVisibility(View.GONE);
        findViewById(R.id.issue_attribute_panel).setVisibility(View.GONE);

        final ProgressDialog waitDialog = showWait(getString(R.string.loading));
        cache.getProjects(new BackLogCache.CacheResponseNotify() {

            @Override
            public void success(int code, String response) {
                waitDialog.dismiss();
                //  ここには来ないはず
            }

            @Override
            public void failed(int code, String response) {
                waitDialog.dismiss();
                showError(R.string.cant_load, "Error STATUS=" + code);
            }

            @Override
            public void error(Exception e) {
                waitDialog.dismiss();
                showError(R.string.cant_load, "Error: " + e.getLocalizedMessage());
            }

            @Override
            public void success(BaseAdapter adapter) {
                waitDialog.dismiss();
                ProjectsAdapter projectsAdapter = (ProjectsAdapter)adapter;
                setSpinnerAdapter(R.id.project_spinner, adapter, projectsAdapter.keyIndexOf(key));
                showSpinner(R.id.project_spinner);
            }

            @Override
            public void success(Drawable icon) {
                waitDialog.dismiss();
                //  ここには来ないはず
            }
        });
    }

    private void loadIssueTypes(final ProgressDialog waitDialog, final Projects.Project project) {
        BackLogCache cache = BackLogCache.sharedInstance();

        cache.getIssueTypes(project, new BackLogCache.CacheResponseNotify() {
            @Override
            public void success(BaseAdapter adapter) {
                setSpinnerAdapter(R.id.issue_type_spinner, adapter, 0);
                loadComponents(waitDialog, project);
            }

            @Override
            public void success(Drawable icon) {
                waitDialog.dismiss();
                //  ここには来ないはず
            }

            @Override
            public void success(int code, String response) {
                waitDialog.dismiss();
                //  ここには来ないはず
            }

            @Override
            public void failed(int code, String response) {
                waitDialog.dismiss();
                showError(R.string.cant_load, "Error STATUS=" + code);
            }

            @Override
            public void error(Exception e) {
                waitDialog.dismiss();
                showError(R.string.cant_load, "Error: " + e.getLocalizedMessage());
            }
        });
    }

    private void loadComponents(final ProgressDialog waitDialog, final Projects.Project project) {
        BackLogCache cache = BackLogCache.sharedInstance();

        cache.getComponents(project, new BackLogCache.CacheResponseNotify() {
            @Override
            public void success(BaseAdapter adapter) {
                setSpinnerAdapter(R.id.component_spinner, adapter, 0);
                waitDialog.dismiss();
                showSpinner(R.id.issue_attribute_panel);
            }

            @Override
            public void success(Drawable icon) {
                waitDialog.dismiss();
                //  ここには来ないはず
            }

            @Override
            public void success(int code, String response) {
                waitDialog.dismiss();
                //  ここには来ないはず
            }

            @Override
            public void failed(int code, String response) {
                waitDialog.dismiss();
                showError(R.string.cant_load, "Error STATUS=" + code);
            }

            @Override
            public void error(Exception e) {
                waitDialog.dismiss();
                showError(R.string.cant_load, "Error: " + e.getLocalizedMessage());
            }
        });
    }

    private void loadAttributes() {
        if (project != null) {
            findViewById(R.id.issue_attribute_panel).setVisibility(View.GONE);
            ProgressDialog waitDialog = showWait(getString(R.string.loading));
            loadIssueTypes(waitDialog, project);
        }
    }

    private void resetCache(String space_id, String user_id, String password) {
        BackLogCache.initSharedInstance(this, new BacklogIO(space_id, user_id, password));
        project = null;
        component = null;
        issueType = null;
        loadProjects();
    }

    private String getEntryText(int id) {
        EditText view = (EditText)findViewById(id);
        return view.getText().toString();
    }

    private void setEntryText(int id, String text) {
        EditText view = (EditText)findViewById(id);
        view.setText(text);
    }

    private void setSpinnerAdapter(int spinner_id, BaseAdapter adapter, int idx) {
        Spinner spinner = (Spinner)findViewById(spinner_id);
        spinner.setAdapter(adapter);
        if (adapter.getCount() > idx && idx >= 0) {
            spinner.setSelection(idx);
        } else if (! adapter.isEmpty()) {
            spinner.setSelection(0);
        }
    }

    private void setSpinnerListener(int spinner_id, AdapterView.OnItemSelectedListener listener) {
        Spinner spinner = (Spinner)findViewById(spinner_id);
        spinner.setOnItemSelectedListener(listener);
    }

    private class PriorityAdapter extends BaseAdapter {

        private class Priority implements BacklogIO.IdHolder {
            int id;
            String name;

            public int getId() {
                return id;
            }

            public String getName() {
                return name;
            }

            Priority(int id, String name) {
                this.id = id;
                this.name = name;
            }
        }

        private List<Priority> items = new ArrayList<Priority>();

        PriorityAdapter() {
            super();

            items.add(new Priority(2, getString(R.string.priority_high)));
            items.add(new Priority(3, getString(R.string.priority_middle)));
            items.add(new Priority(4, getString(R.string.priority_low)));
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView result = (TextView)convertView;

            if (result == null) {
                result = (TextView)getLayoutInflater().inflate(R.layout.spinner_item, null);
            }
            Priority priority = items.get(position);
            result.setText(priority.getName());
            result.setTag(priority);

            return result;
        }
    }

    private class ProjectSelectedLister implements AdapterView.OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            project = (Projects.Project)view.getTag();
            savedKey = project.getKey();
            loadAttributes();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            savedKey = null;
            project = null;
        }
    }

    private class IssueTypeSelectedListener implements AdapterView.OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            issueType = (IssueTypes.IssueType)view.getTag();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }

    private class ComponentSelectedListener implements AdapterView.OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            component = (Components.Component)view.getTag();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }

    private class PrioritySelectedListener implements AdapterView.OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            priority = (PriorityAdapter.Priority)view.getTag();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }
}
