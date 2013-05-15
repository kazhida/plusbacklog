package com.abplus.plusbacklog;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.BaseAdapter;
import com.abplus.plusbacklog.parsers.*;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Copyright (C) 2013 ABplus Inc. kazhida
 * All rights reserved.
 * Author:  kazhida
 * Created: 2013/05/08 11:47
 */
public class BackLogCache {
    private TimeLine timeLine = null;
    private Comments comments = null;
    private Issue issue = null;
    private User user = new User();
    private Map<String, Drawable> icons = new HashMap<String, Drawable>();
    private BacklogIO backlogIO;
    private Context context;
    private Handler handler = new Handler();

    public interface OnIssueClickListener {
        void onClick(View v, String key);
    }

    public interface ParserBuilder {
        StructParser getParser();
    }

    public interface Parseable {
        void parse(XmlPullParser xpp) throws IOException, XmlPullParserException;
    }

    public interface RootParseable {
        void parse(String response) throws IOException, XmlPullParserException;
    }

    public interface CacheResponseNotify extends BacklogIO.ResponseNotify {
        void success(BaseAdapter adapter);
        void success(Drawable icon);
    }

    private BackLogCache(Activity activity, BacklogIO io) {
        context = activity;
        backlogIO = io;
    }

    static BackLogCache cache = null;

    /**
     * 共有インスタンスの初期化
     *
     * @param activity  メインアクティビティ
     * @param io        通信に使うオブジェクト
     * @return  共有インスタンス
     */
    static public BackLogCache initSharedInstance(Activity activity, BacklogIO io) {
        cache = new BackLogCache(activity, io);
        return cache;
    }

    /**
     * 共有インスタンスの取得
     * @return  共有インスタンス
     */
    static public BackLogCache sharedInstance() {
        return cache;
    }

    /**
     * @return  スペースIDプロパティ
     */
    public String spaceId() {
        return backlogIO.getSpaceId();
    }

    /**
     * @return  ユーザIDプロパティ
     */
    public String userId() {
        return backlogIO.getUserId();
    }

    /**
     * @return  ユーザIDプロパティ(数値)
     */
    public int userIdAsInt() {
        return user.getId();
    }

    public Drawable iconOf(String userId) {
        return icons.get(userId);
    }

    /**
     * 通信に使うオブジェクトの取得
     * @return  初期化に使ったBacklogIOインスタンス
     */
    public BacklogIO getIO() {
        return backlogIO;
    }

    public TimeLine getTimeLine() {
        return timeLine;
    }

    public Comments getComments() {
        return comments;
    }

    public Issue getIssue() {
        return issue;
    }

    private abstract class Responder implements BacklogIO.ResponseNotify {
        CacheResponseNotify notify;

        Responder(CacheResponseNotify notify) {
            this.notify = notify;
        }

        @Override
        public void failed(int code, String response) {
            notify.failed(code, response);
        }

        @Override
        public void error(Exception e) {
            notify.error(e);
        }
    }

    /**
     * ユーザアイコンの取得
     * @param userId    ユーザID
     * @param notify    終了通知インターフェース
     */
    public void getUserIcon(final String userId, CacheResponseNotify notify) {
        Drawable drawable = icons.get(userId);

        if (drawable != null) {
            notify.success(drawable);
        } else {
            backlogIO.getUserIcon(userId, new Responder(notify) {
                @Override
                public void success(int code, final String response) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            final UserIcon icon = new UserIcon();
                            try {
                                icon.parse(response);
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        //  Bitmapを作って、キャッシュに入れる
                                        notify.success(putIcon(userId, icon));
                                    }
                                });
                            } catch (final IOException e) {
                                notify.error(e);
                            } catch (final XmlPullParserException e) {
                                notify.error(e);
                            }
                        }
                    }).start();
                }
            });
        }
    }

    private Drawable createIcon(Bitmap bitmap) {
        if (bitmap == null) {
            return context.getResources().getDrawable(R.drawable.ic_dummy);
        } else {
            BitmapDrawable result = new BitmapDrawable(context.getResources(), bitmap);
            result.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
            return result;
        }
    }

    private Drawable putIcon(String userId, UserIcon userIcon) {
        Drawable drawable = icons.get(userId);

        if (drawable == null) {
            Log.d("userIcon", userIcon.getContentType());

            byte[] data = Base64.decode(userIcon.getData(), Base64.DEFAULT);
            InputStream stream = new ByteArrayInputStream(data);
            Bitmap bitmap = BitmapFactory.decodeStream(stream);
            Log.d("userIcon", "w=" + bitmap.getWidth() + " h=" + bitmap.getHeight());

            drawable = createIcon(bitmap);
            icons.put(userId, drawable);
        }

        return drawable;
    }
}
