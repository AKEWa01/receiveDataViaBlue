package com.example.aphiwat.blutoothakewa;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GraphQL {
    private OkHttpClient client = new OkHttpClient();
    private MediaType media_type = MediaType.parse("application/graphql");

    private String url = "https://stratos.watchsmart.space/graphql/";
    private String auth_token;
    private String postBody;

    private Queue<String> value = new LinkedList<String>();
    private int max_value;
    private int size_package;
    private boolean is_successful = true;
    private boolean is_failure = false;
    private boolean can_send = false;

    public GraphQL(String auth_token) {
        setToken(auth_token);
    }

    public Call call(String postBody) {
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(media_type, postBody))
                .addHeader("Authorization", "jwt " + auth_token)
                .addHeader("Content-Type", "application/graphql")
                .build();

        return client.newCall(request);
    }

    public void post() {
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(media_type, postBody))
                .addHeader("Authorization", "jwt " + auth_token)
                .addHeader("Content-Type", "application/graphql")
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                is_successful = false;
                is_failure = true;
                call.cancel();
            }

            @Override
            public void onResponse(Call call, Response response) {
                if (response.isSuccessful()) {
                    is_successful = true;
                    is_failure = false;
                } else {
                    is_successful = false;
                    is_failure = true;
                }
            }
        });
    }

    public void makeBody() {
        int size;
        if (value.size() <= (int) (1.5 * size_package)) {
            size = value.size();
        } else {
            size = size_package;
        }

        int i = 0;
        postBody = "mutation{";
        while (i < size) {
            postBody += "j" + i + ":" + value.poll() + "{status}";
            ++i;
        }
        postBody += "}";
    }

    public void send() {
        if (value.size() >= max_value) {
            can_send = true;
        }

        if (can_send) {
            if (is_successful) {
                if (value.size() == 0) {
                    is_successful = true;
                    can_send = false;
                } else {
                    makeBody();
                    post();
                    is_successful = false;
                }
            } else if (is_failure) {
                is_failure = false;
                post();
            }
        }
    }

    public void setToken(String token) {
        auth_token = token;
    }

    public boolean isSend() {
        return can_send;
    }

    public void setSizePerRound(int limit) {
        size_package = limit;
    }

    public void setMaxTimes(int time) {
        max_value = time * size_package;
    }

    public void setMaxSize(int size) {
        max_value = size;
    }

    public void addQueue(String query) {
        value.add(query);
    }

    public Queue<String> getQueue() {
        return value;
    }
}