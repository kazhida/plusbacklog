package com.abplus.plusbacklog;

import android.os.Handler;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

/**
 * Copyright (C) 2013 ABplus Inc. kazhida
 * All rights reserved.
 * Author:  kazhida
 * Created: 2013/04/06 13:52
 */
public class BacklogIO {
    private String space_id;
    private String user_id;
    private String password;
    private Handler handler = new Handler();

    interface ResponseNotify {
        void success(int code, String response);
        void failed(int code, String response);
        void error(Exception e);
    }

    BacklogIO(String space_id, String user_id, String password) {
        this.space_id = space_id;
        this.user_id = user_id;
        this.password = password;
    }

//    public String getSpaceId() {
//        return space_id;
//    }
//
//    public String getUserId() {
//        return user_id;
//    }
//
//    public String getPassword() {
//        return password;
//    }

    public void post(final String request, final ResponseNotify notify) {
        final HttpPost req = new HttpPost("https://" + space_id + ".backlog.jp/XML-RPC");
        final DefaultHttpClient http = new DefaultHttpClient();

        http.getCredentialsProvider().setCredentials(
                new AuthScope(req.getURI().getHost(), req.getURI().getPort()),
                new UsernamePasswordCredentials(user_id, password)
        );

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(10);   //ProgressDialogのために、ちょっとだけスリープ
                    req.setEntity(new StringEntity(request));
                    HttpResponse response = http.execute(req);

                    final int code = response.getStatusLine().getStatusCode();
                    final String entity = EntityUtils.toString(response.getEntity());

                    if (200 <= code && code < 400) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                notify.success(code, entity);
                            }
                        });
                    } else {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                notify.failed(code, entity);
                            }
                        });
                    }
                } catch (Exception e) {
                    final Exception exception = e;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            notify.error(exception);
                        }
                    });
                }
            }
        }).start();
    }

    public void createIssue(String summary, String description,
                            int projectId, int issueTypeId, int priorityId,
                            ResponseNotify notify) {
        StringBuilder xml = new StringBuilder();

        xml.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        xml.append("<methodCall>");
        xml.append("<methodName>backlog.createIssue</methodName>");
        xml.append("<params>");
        xml.append("<param>");
        xml.append("<value>");
        xml.append("<struct>");

        xml.append("<member>");
        xml.append("<name>projectId</name>");
        xml.append("<value><int>").append(projectId).append("</int></value>");
        xml.append("</member>");

        xml.append("<member>");
        xml.append("<name>summary</name>");
        xml.append("<value><string>").append(summary).append("</string></value>");
        xml.append("</member>");

        if (description != null && ! description.isEmpty()) {
            xml.append("<member>");
            xml.append("<name>description</name>");
            xml.append("<value><string>").append(description).append("</string></value>");
            xml.append("</member>");
        }

        xml.append("<member>");
        xml.append("<name>issueTypeId</name>");
        xml.append("<value><int>").append(issueTypeId).append("</int></value>");
        xml.append("</member>");

        xml.append("<member>");
        xml.append("<name>priorityId</name>");
        xml.append("<value><int>").append(priorityId).append("</int></value>");
        xml.append("</member>");

        xml.append("</value>");
        xml.append("</param>");
        xml.append("</params>");
        xml.append("</methodCall>");

        post(xml.toString(), notify);
    }

    public void loadProjects(ResponseNotify notify) {
        StringBuilder xml = new StringBuilder();

        xml.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        xml.append("<methodCall>");
        xml.append("<methodName>backlog.getProjects</methodName>");
        xml.append("<params />");
        xml.append("</methodCall>");

        post(xml.toString(), notify);
    }

    public void loadIssueTypes(int projectId, ResponseNotify notify) {
        StringBuilder xml = new StringBuilder();

        xml.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        xml.append("<methodCall>");
        xml.append("<methodName>backlog.getIssueTypes</methodName>");
        xml.append("<params>");
        xml.append("<param>");
        xml.append("<value>");
        xml.append("<int>");
        xml.append(projectId);
        xml.append("</int>");
        xml.append("");
        xml.append("</value>");
        xml.append("</param>");
        xml.append("</params>");
        xml.append("</methodCall>");

        post(xml.toString(), notify);
    }
}
