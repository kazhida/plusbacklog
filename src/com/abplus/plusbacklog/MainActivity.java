package com.abplus.plusbacklog;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.view.animation.AlphaAnimation;
import android.view.animation.ScaleAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private BacklogIO                backlog = null;
    private SelectionCache           cache = null;
    private SelectionCache.Project   project = null;
    private SelectionCache.IssueType issueType = null;
    private SelectionCache.Component component = null;
    private PriorityAdapter.Priority priority = null;
    private String                   savedKey = null;
    private AdView                   adView = null;

    private final String PREF_NAME    = "backlog_prefs";
    private final String KEY_SPACE_ID = "space_id";
    private final String KEY_USER_ID  = "user_id";
    private final String KEY_PASSWORD = "password";
    private final String KEY_PROJECT  = "curr_project";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        adView = appendAdView();

        findViewById(R.id.project_spinner).setVisibility(View.GONE);
        findViewById(R.id.issue_attribute_panel).setVisibility(View.GONE);

        setSpinnerListener(R.id.project_spinner, new ProjectSelectedLister());
        setSpinnerListener(R.id.issue_type_spinner, new IssueTypeSelectedListener());
        setSpinnerListener(R.id.component_spinner, new ComponentSelectedListener());
        setSpinnerListener(R.id.priority_spinner, new PrioritySelectedListener());
        setSpinnerAdapter(R.id.priority_spinner, new PriorityAdapter(), 1);

        findViewById(R.id.save_config).setOnClickListener(new SaveConfigListener());
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String space_id = prefs.getString(KEY_SPACE_ID, null);
        String user_id = prefs.getString(KEY_USER_ID, null);
        String password = prefs.getString(KEY_PASSWORD, null);

        savedKey = prefs.getString(KEY_PROJECT, null);

        if (space_id == null || user_id == null || password == null) {
            showConfig();
        } else if (findViewById(R.id.project_spinner).getVisibility() == View.GONE) {
            //  プロジェクトを読み込む
            setEntryText(R.id.space_id, space_id);
            setEntryText(R.id.user_id, user_id);
            setEntryText(R.id.password, password);
            resetCache(space_id, user_id, password);
        }
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
            SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_PROJECT, project.getKey());
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
                showConfig();
                return true;
            case R.id.menu_reload:
                loadProjects();
                return true;
            case R.id.menu_post:
                String summary = getEntryText(R.id.summary);
                String description = getEntryText(R.id.description);
                if (summary == null || summary.isEmpty()) {
                    showToast(R.string.summary_is_empty);
                } else if (backlog != null) {
                    final ProgressDialog waitDialog = showWait(getString(R.string.sending));
                    backlog.createIssue(summary, description,
                            project, issueType, component, priority, new BacklogIO.ResponseNotify() {
                        @Override
                        public void success(int code, String response) {
                            waitDialog.dismiss();
                            Log.d("+backlog.post_issue", response);
                            setEntryText(R.id.summary, null);
                            setEntryText(R.id.description, null);
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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            View view = findViewById(R.id.config_panel);
            if (view.getVisibility() == View.VISIBLE) {
                view.setVisibility(View.GONE);
                findViewById(R.id.main_panel).setVisibility(View.VISIBLE);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void showConfig() {
        //  1秒かけてぼや〜んとでる。
        View view = findViewById(R.id.config_panel);
        view.setVisibility(View.VISIBLE);
        AlphaAnimation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(1000);
        view.startAnimation(animation);
        findViewById(R.id.main_panel).setVisibility(View.GONE);
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

//    private void showToast(String msg) {
//        showToast(msg, Toast.LENGTH_SHORT);
//    }

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
        if (cache != null) {
            final String key = project != null ? project.getKey() : savedKey;
            findViewById(R.id.project_spinner).setVisibility(View.GONE);
            findViewById(R.id.issue_attribute_panel).setVisibility(View.GONE);
            final ProgressDialog waitDialog = showWait(getString(R.string.loading));
            cache.loadProjects(new BacklogIO.ResponseNotify() {
                @Override
                public void success(int code, String response) {
                    waitDialog.dismiss();
                    SelectionCache.ProjectsAdapter adapter = cache.getProjectsAdapter();
                    setSpinnerAdapter(R.id.project_spinner, adapter, adapter.keyIndexOf(key));
                    showSpinner(R.id.project_spinner);
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
    }

    private void loadAttributes() {
        if (cache != null && project != null) {
            if (project.hasCache()) {
                findViewById(R.id.issue_attribute_panel).setVisibility(View.VISIBLE);
                setSpinnerAdapter(R.id.issue_type_spinner, cache.getIssueTypesAdapter(project), 0);
            } else {
                findViewById(R.id.issue_attribute_panel).setVisibility(View.GONE);
                final ProgressDialog waitDialog = showWait(getString(R.string.loading));
                final boolean[] flags = new boolean[2];
                flags[0] = false;
                flags[1] = false;
                cache.loadIssueTypes(project, new BacklogIO.ResponseNotify() {
                    @Override
                    public void success(int code, String response) {
                        flags[0] = true;
                        setSpinnerAdapter(R.id.issue_type_spinner, cache.getIssueTypesAdapter(project), 0);
                        if (flags[0] && flags[1]) {
                            waitDialog.dismiss();
                            showSpinner(R.id.issue_attribute_panel);
                        }
                    }
                    @Override
                    public void failed(int code, String response) {
                        flags[0] = true;
                        if (flags[0] && flags[1]) {
                            waitDialog.dismiss();
                        }
                        showError(R.string.cant_load, "Error STATUS=" + code);
                    }
                    @Override
                    public void error(Exception e) {
                        flags[0] = true;
                        if (flags[0] && flags[1]) {
                            waitDialog.dismiss();
                        }
                        showError(R.string.cant_load, "Error: " + e.getLocalizedMessage());
                    }
                });
                cache.loadComponents(project, new BacklogIO.ResponseNotify() {
                    @Override
                    public void success(int code, String response) {
                        flags[1] = true;
                        setSpinnerAdapter(R.id.component_spinner, cache.getComponentsAdapter(project), 0);
                        if (flags[0] && flags[1]) {
                            waitDialog.dismiss();
                            showSpinner(R.id.issue_attribute_panel);
                        }
                    }
                    @Override
                    public void failed(int code, String response) {
                        flags[1] = true;
                        if (flags[0] && flags[1]) {
                            waitDialog.dismiss();
                        }
                        showError(R.string.cant_load, "Error STATUS=" + code);
                    }
                    @Override
                    public void error(Exception e) {
                        flags[1] = true;
                        if (flags[0] && flags[1]) {
                            waitDialog.dismiss();
                        }
                        showError(R.string.cant_load, "Error: " + e.getLocalizedMessage());
                    }
                });
            }
        }
    }

    private void resetCache(String space_id, String user_id, String password) {
        backlog = new BacklogIO(space_id, user_id, password);
        cache = new SelectionCache(this, backlog);
        project = null;
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

    private class SaveConfigListener implements View.OnClickListener {

        private String getText(int id) {
            EditText edit = (EditText)findViewById(id);
            return edit.getText().toString();
        }

        private String spaceId() {
            return getText(R.id.space_id);
        }

        private String userId() {
            return getText(R.id.user_id);
        }

        private String password() {
            return getText(R.id.password);
        }

        @Override
        public void onClick(View v) {
            if (getCurrentFocus() != null) {
                InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                manager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }

            findViewById(R.id.config_panel).setVisibility(View.GONE);
            findViewById(R.id.main_panel).setVisibility(View.VISIBLE);

            SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_SPACE_ID, spaceId());
            editor.putString(KEY_USER_ID, userId());
            editor.putString(KEY_PASSWORD, password());
            editor.commit();

            resetCache(spaceId(), userId(), password());
        }
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
            project = (SelectionCache.Project)view.getTag();
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
            issueType = (SelectionCache.IssueType)view.getTag();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }

    private class ComponentSelectedListener implements AdapterView.OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            component = (SelectionCache.Component)view.getTag();
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
