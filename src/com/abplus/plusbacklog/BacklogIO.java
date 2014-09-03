package com.abplus.plusbacklog;

import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
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

    private final String DEBUG_TAG = "+backlog.io";

    interface ResponseNotify {
        void success(int code, String response);
        void failed(int code, String response);
        void error(Exception e);
    }

    public interface IdHolder {
        int getId();
    }

    public interface KeyHolder {
        String getKey();
    }

    public interface NameHolder {
        String getName();
    }


    BacklogIO(String space_id, String user_id, String password) {
        this.space_id = space_id;
        this.user_id = user_id;
        this.password = password;
    }

    public String getSpaceId() {
        return space_id;
    }

    public String getUserId() {
        return user_id;
    }

    public String getPassword() {
        return password;
    }

    public void post(final String request, final ResponseNotify notify) {
        final HttpPost httpPost = new HttpPost("https://" + space_id + ".backlog.jp/XML-RPC");
        final DefaultHttpClient http = new DefaultHttpClient();

        httpPost.addHeader("Content-Type", "text/xml");

        http.getCredentialsProvider().setCredentials(
                new AuthScope(httpPost.getURI().getHost(), httpPost.getURI().getPort()),
                new UsernamePasswordCredentials(user_id, password)
        );

        Log.d(DEBUG_TAG + ".request", request);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(10);   //ProgressDialogのために、ちょっとだけスリープ
                    httpPost.setEntity(new StringEntity(request, HTTP.UTF_8));
                    HttpResponse response = http.execute(httpPost);

                    for (Header header : response.getAllHeaders()) {
                        Log.d(DEBUG_TAG + ".response_header", header.toString());
                    }

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
                            IdHolder project, IdHolder issueType, IdHolder component, IdHolder priority,
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
        xml.append("<value><int>").append(project.getId()).append("</int></value>");
        xml.append("</member>");

        xml.append("<member>");
        xml.append("<name>summary</name>");
        xml.append("<value><string>").append(TextUtils.htmlEncode(summary)).append("</string></value>");
        xml.append("</member>");

        if (description != null && ! description.isEmpty()) {
            xml.append("<member>");
            xml.append("<name>description</name>");
            xml.append("<value><string>").append(TextUtils.htmlEncode(description)).append("</string></value>");
            xml.append("</member>");
        }

        xml.append("<member>");
        xml.append("<name>issueTypeId</name>");
        xml.append("<value><int>").append(issueType.getId()).append("</int></value>");
        xml.append("</member>");

        if (component != null) {
            xml.append("<member>");
            xml.append("<name>component</name>");
            xml.append("<value><int>").append(component.getId()).append("</int></value>");
            xml.append("</member>");
        }

        xml.append("<member>");
        xml.append("<name>priorityId</name>");
        xml.append("<value><int>").append(priority.getId()).append("</int></value>");
        xml.append("</member>");

        xml.append("</struct>");
        xml.append("</value>");
        xml.append("</param>");
        xml.append("</params>");
        xml.append("</methodCall>");

        post(xml.toString(), notify);
    }

    public void getProjects(ResponseNotify notify) {
        post(   "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "<methodCall>" +
                "<methodName>backlog.getProjects</methodName>" +
                "<params />" + "</methodCall>",
                notify);
    }

    public void getIssueTypes(int projectId, ResponseNotify notify) {
        post(   "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "<methodCall>" +
                "<methodName>backlog.getIssueTypes</methodName>" +
                "<params>" +
                "<param>" +
                "<value>" +
                "<int>" + projectId + "</int>" +
                "</value>" +
                "</param>" +
                "</params>" +
                "</methodCall>",
                notify);
    }

    public void getComponents(int projectId, ResponseNotify notify) {
        post(   "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "<methodCall>" +
                "<methodName>backlog.getComponents</methodName>" +
                "<params>" +
                "<param>" +
                "<value>" +
                "<int>" + projectId + "</int>" +
                "</value>" +
                "</param>" +
                "</params>" +
                "</methodCall>",
                notify);
    }

    public void getUsers(int projectId, ResponseNotify notify) {
        post(   "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "<methodCall>" +
                "<methodName>backlog.getUsers</methodName>" +
                "<params>" +
                "<param>" +
                "<value>" +
                "<int>" + projectId + "</int>" +
                "</value>" +
                "</param>" +
                "</params>" +
                "</methodCall>",
                notify);
    }

    public void getUserIcon(String userId, ResponseNotify notify) {
        post(   "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "<methodCall>" +
                "<methodName>backlog.getUserIcon</methodName>" +
                "<params>" +
                "<param>" +
                "<value>" +
                "<string>" + userId + "</string>" +
                "</value>" +
                "</param>" +
                "</params>" +
                "</methodCall>",
                notify);
    }
}
