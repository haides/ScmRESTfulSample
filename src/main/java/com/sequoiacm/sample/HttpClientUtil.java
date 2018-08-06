package com.sequoiacm.sample;

import java.io.Closeable;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.bson.BSONObject;
import org.bson.util.JSON;

public class HttpClientUtil {
    private static final Logger logger = Logger.getLogger(HttpClientUtil.class);
    public static final String CHARSET_UTF8 = "utf-8";

    private static CloseableHttpResponse sendRequest(CloseableHttpClient client,
            HttpRequestBase request) throws Exception {
        CloseableHttpResponse response = client.execute(request);
        handleException(response);
        return response;
    }

    public static void sendRequest(CloseableHttpClient client, String sessionId,
            HttpRequestBase request) throws Exception {
        CloseableHttpResponse response = sendRequestWithHttpResponse(client, sessionId, request);
        closeResource(response);
    }

    private static void handleException(CloseableHttpResponse response) throws Exception {
        int httpStatusCode = response.getStatusLine().getStatusCode();

        // 2xx Success
        if (httpStatusCode >= 200 && httpStatusCode < 300) {
            return;
        }

        try {
            int errcode = httpStatusCode;
            String resp = getErrorResponse(response);
            throw new Exception("httpStatusCode=" + errcode + ", errorRep=" + resp);
        }
        finally {
            closeResource(response);
        }
    }

    private static String getErrorResponse(CloseableHttpResponse response) throws Exception {
        String error = null;

        HttpEntity entity = response.getEntity();
        if (null != entity) {
            try {
                error = EntityUtils.toString(entity);
            }
            catch (IOException e) {
                throw new Exception("an error occurs when read http entity");
            }
        }
        else {
            Header header = response.getFirstHeader("X-SCM-ERROR");
            if (header != null) {
                error = header.getValue();
            }
        }

        return error;
    }

    public static String sendRequestWithHeaderResponse(CloseableHttpClient client, String sessionId,
            HttpEntityEnclosingRequestBase request, List<NameValuePair> params, String keyName)
                    throws Exception {
        CloseableHttpResponse response = sendRequestWithHttpResponse(client, sessionId, request,
                params);
        try {
            String value = response.getFirstHeader(keyName).getValue();
            return URLDecoder.decode(value, CHARSET_UTF8);
        }
        finally {
            closeResource(response);
        }
    }

    public static String sendRequestWithHeaderResponse(CloseableHttpClient client, String sessionId,
            HttpRequestBase request, String keyName) throws Exception {
        CloseableHttpResponse response = sendRequestWithHttpResponse(client, sessionId, request);
        try {
            String value = response.getFirstHeader(keyName).getValue();
            return URLDecoder.decode(value, CHARSET_UTF8);
        }
        finally {
            closeResource(response);
        }
    }

    public static CloseableHttpResponse sendRequestWithHttpResponse(CloseableHttpClient client,
            String sessionId, HttpRequestBase request) throws Exception {
        if (!Sample.NO_AUTH_SESSION_ID.equals(sessionId)) {
            request.setHeader(Sample.AUTHORIZATION, sessionId);
        }

        return sendRequest(client, request);
    }

    public static BSONObject sendRequestWithJsonResponse(CloseableHttpClient client,
            String sessionId, HttpRequestBase request) throws Exception {
        CloseableHttpResponse response = sendRequestWithHttpResponse(client, sessionId, request);
        try {
            String resp = EntityUtils.toString(response.getEntity(), CHARSET_UTF8);
            return (BSONObject) JSON.parse(resp);
        }
        finally {
            closeResource(response);
        }
    }

    public static BSONObject sendRequestWithJsonResponse(CloseableHttpClient client,
            String sessionId, HttpEntityEnclosingRequestBase request, List<NameValuePair> params)
                    throws Exception {
        CloseableHttpResponse response = sendRequestWithHttpResponse(client, sessionId, request,
                params);
        try {
            String resp = EntityUtils.toString(response.getEntity(), CHARSET_UTF8);
            return (BSONObject) JSON.parse(resp);
        }
        finally {
            closeResource(response);
        }
    }

    public static void sendRequest(CloseableHttpClient client, String sessionId,
            HttpEntityEnclosingRequestBase request, List<NameValuePair> params) throws Exception {
        CloseableHttpResponse response = sendRequestWithHttpResponse(client, sessionId, request,
                params);
        closeResource(response);
    }

    public static void closeResource(Closeable res) {
        if (res != null) {
            try {
                res.close();
            }
            catch (Exception e) {
                logger.warn("close HttpResponse failed:res=" + res, e);
            }
        }
    }

    public static CloseableHttpResponse sendRequestWithHttpResponse(CloseableHttpClient client,
            String sessionId, HttpEntityEnclosingRequestBase request, List<NameValuePair> params)
                    throws Exception {
        if (!Sample.NO_AUTH_SESSION_ID.equals(sessionId)) {
            request.setHeader(Sample.AUTHORIZATION, sessionId);
        }

        if (params != null) {
            HttpEntity entity = new UrlEncodedFormEntity(params, CHARSET_UTF8);
            request.setEntity(entity);
        }

        return sendRequest(client, request);
    }
}
