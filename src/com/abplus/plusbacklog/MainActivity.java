package com.abplus.plusbacklog;

import android.os.Bundle;
import android.view.*;
import android.view.animation.AlphaAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.*;
import com.example.android.actionbarcompat.ActionBarActivity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends ActionBarActivity {

    private SelectionCache           cache = null;
    private SelectionCache.Project   project = null;
    private SelectionCache.IssueType issueType = null;
    private PriorityAdapter.Priority priority = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        setSpinnerListener(R.id.project_spinner, new ProjectSelectedLister());
        setSpinnerListener(R.id.issue_type_spinner, new IssueTypeSelectedListener());

        setSpinnerAdapter(R.id.priority_spinner, new PriorityAdapter());

        findViewById(R.id.save_config).setOnClickListener(new SaveConfigListener());
    }

    @Override
    protected void onResume() {
        super.onResume();

        //todo:preferenceからアカウント情報を取り出してキャッシュを初期化

        //  プロジェクトを読み込む
        loadProjects();
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
                //todo:バックログに送信
                return true;
        }
        return false;
    }

    private void showConfig() {
        //  2秒かけてぼや〜んとでる。
        View view = findViewById(R.id.config_panel);
        view.setVisibility(View.VISIBLE);
        AlphaAnimation animation = new AlphaAnimation(1.0f, 0.0f);
        animation.setDuration(2000);
        view.startAnimation(animation);
    }

    private void showSpinner(int id) {
        View view = findViewById(id);
        view.setVisibility(View.VISIBLE);
        ScaleAnimation animation = new ScaleAnimation(1.0f, 1.0f, 0.0f, 1.0f);
        animation.setDuration(1000);
        view.startAnimation(animation);
    }

    private void loadProjects() {
        if (cache != null) {
            findViewById(R.id.project_spinner).setVisibility(View.GONE);
            findViewById(R.id.issue_type_spinner).setVisibility(View.GONE);
            findViewById(R.id.priority_spinner).setVisibility(View.GONE);
            cache.loadProjects(new Runnable() {
                @Override
                public void run() {
                    setSpinnerAdapter(R.id.project_spinner, cache.getProjectsAdapter());
                    showSpinner(R.id.project_spinner);
                }
            });
        }
    }

    private void loadIssueTypes() {
        if (cache != null && project != null) {
            findViewById(R.id.issue_type_spinner).setVisibility(View.GONE);
            findViewById(R.id.priority_spinner).setVisibility(View.GONE);
            cache.loadIssueTypes(project, new Runnable() {
                @Override
                public void run() {
                    setSpinnerAdapter(R.id.issue_type_spinner, cache.getIssueTypeAdapter(project));
                    showSpinner(R.id.issue_type_spinner);
                }
            });
        }
    }

    private void resetCache(SelectionCache newCache) {
        if (newCache != null) {
            cache = newCache;
            project = null;
            loadProjects();
        }
    }

    private void setSpinnerAdapter(int spinner_id, BaseAdapter adapter) {
        Spinner spinner = (Spinner)findViewById(spinner_id);
        spinner.setAdapter(adapter);
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
            resetCache(new SelectionCache(MainActivity.this, spaceId(), userId(), password()));
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
