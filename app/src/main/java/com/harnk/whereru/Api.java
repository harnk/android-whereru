package com.harnk.whereru;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.HttpVersion;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.params.CoreProtocolPNames;

/**
 * Created by scottnull on 11/21/15.
 */
public class Api {

    private static final String BASE_URL = "https://my.api.com/";
    private static AsyncHttpClient aSyncClient;
    private static String USER_AGENT = "Our Custom User Agent";

    static {

        // setup asynchronous client
        aSyncClient = new AsyncHttpClient();
        aSyncClient.setUserAgent(USER_AGENT);
        aSyncClient.getHttpClient().getParams().setParameter(ClientPNames.ALLOW_CIRCULAR_REDIRECTS, false);
        aSyncClient.getHttpClient().getParams().setParameter(ClientPNames.HANDLE_REDIRECTS, false);
        aSyncClient.getHttpClient().getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
    }

    public static void get(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        aSyncClient.get(getAbsoluteUrl(url), params, responseHandler);
    }

    public static void post(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        aSyncClient.post(getAbsoluteUrl(url), params, responseHandler);
    }

    public static void put(String url, AsyncHttpResponseHandler responseHandler) {
        aSyncClient.put(null, getAbsoluteUrl(url), null, "application/json", responseHandler);
    }

    public static void delete(String url, AsyncHttpResponseHandler responseHandler) {
        aSyncClient.delete(null, getAbsoluteUrl(url), responseHandler);
    }

    private static String getAbsoluteUrl(String relativeUrl) {
        return BASE_URL + relativeUrl;
    }

}