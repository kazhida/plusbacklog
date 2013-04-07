package com.abplus.plusbacklog;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.*;
import android.view.animation.AlphaAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.*;
import com.example.android.actionbarcompat.ActionBarActivity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends ActionBarActivity {

    private BacklogIO                backlog = null;
    private SelectionCache           cache = null;
    private SelectionCache.Project   project = null;
    private SelectionCache.IssueType issueType = null;
    private PriorityAdapter.Priority priority = null;

    private final String PREF_NAME    = "backlog_prefs";
    private final String KEY_SPACE_ID = "space_id";
    private final String KEY_USER_ID  = "user_id";
    private final String KEY_PASSWORD = "password";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        setSpinnerListener(R.id.project_spinner, new ProjectSelectedLister());
        setSpinnerListener(R.id.issue_type_spinner, new IssueTypeSelectedListener());
        setSpinnerListener(R.id.priority_spinner, new PrioritySelectedListener());
        setSpinnerAdapter(R.id.priority_spinner, new PriorityAdapter());

        findViewById(R.id.save_config).setOnClickListener(new SaveConfigListener());
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String space_id = prefs.getString(KEY_SPACE_ID, null);
        String user_id = prefs.getString(KEY_USER_ID, null);
        String password = prefs.getString(KEY_PASSWORD, null);

        if (space_id == null || user_id == null || password == null) {
            showConfig();
        } else {
            //  プロジェクトを読み込む
            setEntryText(R.id.space_id, space_id);
            setEntryText(R.id.user_id, user_id);
            setEntryText(R.id.password, password);
            resetCache(space_id, user_id, password);
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
            case R.id.menu_post:
                if (backlog != null) {
                    String summary = getEntryText(R.id.summary);
                    String description = getEntryText(R.id.description);
                    final ProgressDialog waitDialog = new ProgressDialog(this);
                    waitDialog.setMessage(getText(R.string.sending));
                    waitDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    // プログレスダイアログを表示
                    waitDialog.show();
                    backlog.createIssue(summary, description,
                            project.getId(), issueType.getId(), priority.getId(), new BacklogIO.ResponseNotify() {
                        @Override
                        public void success(int code, String response) {
                            waitDialog.dismiss();
                            showToast(R.string.registered_issue);
                        }
                        @Override
                        public void failed(int code, String response) {
                            waitDialog.dismiss();
                            showToast("Error STATUS=" + code, Toast.LENGTH_LONG);
                        }
                        @Override
                        public void error(Exception e) {
                            waitDialog.dismiss();
                            showToast("Error: " + e.getLocalizedMessage(), Toast.LENGTH_LONG);
                        }
                    });
                }
                return true;
        }
        return false;
    }

    private void showConfig() {
        //  2秒かけてぼや〜んとでる。
        View view = findViewById(R.id.config_panel);
        view.setVisibility(View.VISIBLE);
        AlphaAnimation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(2000);
        view.startAnimation(animation);
        findViewById(R.id.main_panel).setVisibility(View.GONE);
    }

    private void showSpinner(int id) {
        //  1秒かけてしゅかっとでる
        View view = findViewById(id);
        view.setVisibility(View.VISIBLE);
        ScaleAnimation animation = new ScaleAnimation(1.0f, 1.0f, 0.0f, 1.0f);
        animation.setDuration(1000);
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

    private void loadProjects() {
        if (cache != null) {
            findViewById(R.id.project_spinner).setVisibility(View.GONE);
            findViewById(R.id.issue_attribute_panel).setVisibility(View.GONE);
            final ProgressDialog waitDialog = new ProgressDialog(this);
            waitDialog.setMessage(getText(R.string.sending));
            waitDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            cache.loadProjects(new SelectionCache.CacheNotify() {
                @Override
                public void success() {
                    waitDialog.dismiss();
                    setSpinnerAdapter(R.id.project_spinner, cache.getProjectsAdapter());
                    showSpinner(R.id.project_spinner);
                }
                @Override
                public void failed() {
                    waitDialog.dismiss();
                    showToast(R.string.cant_load);
                }
            });
        }
    }

    private void loadIssueTypes() {
        if (cache != null && project != null) {
            if (project.hasCache()) {
                findViewById(R.id.issue_attribute_panel).setVisibility(View.VISIBLE);
                setSpinnerAdapter(R.id.issue_type_spinner, cache.getIssueTypeAdapter(project));
            } else {
                findViewById(R.id.issue_attribute_panel).setVisibility(View.GONE);
                final ProgressDialog waitDialog = new ProgressDialog(this);
                waitDialog.setMessage(getText(R.string.sending));
                waitDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                cache.loadIssueTypes(project, new SelectionCache.CacheNotify() {
                    @Override
                    public void success() {
                        waitDialog.dismiss();
                        setSpinnerAdapter(R.id.issue_type_spinner, cache.getIssueTypeAdapter(project));
                        showSpinner(R.id.issue_attribute_panel);
                    }
                    @Override
                    public void failed() {
                        waitDialog.dismiss();
                        showToast(R.string.cant_load);
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

    private void setSpinnerAdapter(int spinner_id, BaseAdapter adapter) {
        Spinner spinner = (Spinner)findViewById(spinner_id);
        spinner.setAdapter(adapter);
        if (! adapter.isEmpty()) {
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

        private class Priority {
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
            loadIssueTypes();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }

    private class IssueTypeSelectedListener implements AdapterView.OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            issueType = (SelectionCache.IssueType)view.getTag();
            showSpinner(R.id.priority_spinner);
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