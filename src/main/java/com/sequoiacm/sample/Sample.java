package com.sequoiacm.sample;

import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.bson.BSONObject;

public class Sample implements Closeable {
    private static final Logger logger = Logger.getLogger(Sample.class);

    public static final String AUTHORIZATION = "x-auth-token";
    public static final String NO_AUTH_SESSION_ID = "-1";

    private String gatewayUrl;
    private String siteName;
    private CloseableHttpClient httpClient;

    public Sample(String gatewayUrl, String siteName) {
        this.gatewayUrl = gatewayUrl;
        this.siteName = siteName;
        this.httpClient = HttpClients.createDefault();
    }

    // return session id.
    public String login(String user, String password) throws Exception {
        HttpPost request = new HttpPost("http://" + gatewayUrl + "/login");

        List<NameValuePair> params = new ArrayList<>(2);
        params.add(new BasicNameValuePair("username", user));
        params.add(new BasicNameValuePair("password", password));

        String sessionId = HttpClientUtil.sendRequestWithHeaderResponse(httpClient,
                NO_AUTH_SESSION_ID, request, params, AUTHORIZATION);
        return sessionId;
    }

    // return file id.
    public String createFile(String sessionId, String fileName, String wsName)
            throws Exception {
        String uri = "http://" + gatewayUrl + "/" + siteName + "/api/v1/files?workspace_name="
                + encode(wsName);
        HttpPost postFile = new HttpPost(uri);

        MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
        // file meta
        entityBuilder.addTextBody("description", "{\"name\":\"" + fileName + "\"}",
                ContentType.APPLICATION_JSON);
        // file data
        entityBuilder.addBinaryBody("file", "This is File Content. for Restful Sample".getBytes(),
                ContentType.MULTIPART_FORM_DATA, "");
        postFile.setEntity(entityBuilder.build());

        BSONObject resp = HttpClientUtil.sendRequestWithJsonResponse(httpClient, sessionId,
                postFile);
        BSONObject file = (BSONObject) resp.get("file");
        return (String)file.get("id");
    }

    public BSONObject getFileInfo(String sessionId, String workspace_name, String fileId,
            int majorVersion, int minorVersion) throws Exception {
        String uri = "http://" + gatewayUrl + "/" + siteName + "/api/v1/files/id/" + fileId
                + "?workspace_name=" + encode(workspace_name) + "&major_version=" + majorVersion
                + "&minor_version=" + minorVersion;
        HttpHead request = new HttpHead(uri);
        String resp = HttpClientUtil.sendRequestWithHeaderResponse(httpClient, sessionId, request,
                "file");
        return (BSONObject) org.bson.util.JSON.parse(resp);
    }

    public void downloadFileContent(String sessionId, String workspace_name, String fileId,
            int majorVersion, int minorVersion, String downloadPath) throws Exception {
        String uri = "http://" + gatewayUrl + "/" + siteName + "/api/v1/files/" + fileId
                + "?workspace_name=" + encode(workspace_name) + "&major_version=" + majorVersion
                + "&minor_version=" + minorVersion;
        HttpGet request = new HttpGet(uri);
        CloseableHttpResponse response = HttpClientUtil.sendRequestWithHttpResponse(httpClient,
                sessionId, request);
        InputStream is = null;
        FileOutputStream fos = null;
        try {
            is = response.getEntity().getContent();
            fos = new FileOutputStream(downloadPath);
            byte[] buf = new byte[1024];
            while (true) {
                int readLen = is.read(buf, 0, 1024);
                if (readLen <= -1) {
                    break;
                }
                fos.write(buf, 0, readLen);
            }
        }
        finally {
            HttpClientUtil.closeResource(fos);
            HttpClientUtil.closeResource(is);
            HttpClientUtil.closeResource(response);
        }
    }

    public void deleteFile(String sessionId, String fileId, String wsName) throws Exception {
        String uri = "http://" + gatewayUrl + "/" + siteName + "/api/v1/files/" + fileId
                + "?workspace_name=" + encode(wsName) + "&is_physical=true";
        HttpDelete request = new HttpDelete(uri);
        HttpClientUtil.sendRequest(httpClient, sessionId, request);
    }

    public void logout(String sessionId) throws Exception {
        String uri = "http://" + gatewayUrl + "/logout";
        HttpPost request = new HttpPost(uri);
        try {
            HttpClientUtil.sendRequest(httpClient, sessionId, request);
        }
        catch (Exception e) {
            logger.warn("logout failed:sessionId=" + sessionId, e);
        }
    }

    public BSONObject createBatch(String sessionId, String wsName, String batchName)
            throws Exception {
        String uri = "http://" + gatewayUrl + "/" + siteName + "/api/v1/batches?workspace_name" + "="
                + encode(wsName);
        HttpPost request = new HttpPost(uri);

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("description", "{name:'" + batchName + "'}"));

        BSONObject resp = HttpClientUtil.sendRequestWithJsonResponse(httpClient, sessionId, request,
                params);
        BSONObject batchObj = (BSONObject) resp.get("batch");
        return batchObj;
    }

    public void batchAttachFile(String sessionId, String workspaceName, String batchId,
            String fileId) throws Exception {
        String uri = "http://" + gatewayUrl + "/" + siteName + "/api/v1/batches/" + batchId
                + "/attachfile";
        HttpPost request = new HttpPost(uri);

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("workspace_name", workspaceName));
        params.add(new BasicNameValuePair("file_id", fileId));

        HttpClientUtil.sendRequest(httpClient, sessionId, request, params);
    }

    public void batchDetachFile(String sessionId, String workspaceName, String batchId,
            String fileId) throws Exception {
        String uri = "http://" + gatewayUrl + "/" + siteName + "/api/v1/batches/" + batchId
                + "/detachfile";
        HttpPost request = new HttpPost(uri);

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("workspace_name", workspaceName));
        params.add(new BasicNameValuePair("file_id", fileId));

        HttpClientUtil.sendRequest(httpClient, sessionId, request, params);
    }

    public void deleteBatch(String sessionId, String workspaceName, String batchID)
            throws Exception {
        String uri = "http://" + gatewayUrl + "/" + siteName + "/api/v1/batches/" + batchID
                + "?workspace_name=" + encode(workspaceName);
        HttpDelete request = new HttpDelete(uri);
        HttpClientUtil.sendRequest(httpClient, sessionId, request);
    }

    public BSONObject getBatchInfo(String sessionId, String workspaceName, String batchId)
            throws Exception {
        String uri = "http://" + gatewayUrl + "/" + siteName + "/api/v1/batches/" + batchId
                + "?workspace_name=" + encode(workspaceName);
        HttpGet request = new HttpGet(uri);
        BSONObject resp = HttpClientUtil.sendRequestWithJsonResponse(httpClient, sessionId,
                request);
        return (BSONObject) resp.get("batch");
    }

    @Override
    public void close() {
        try {
            httpClient.close();
        }
        catch (Exception e) {
            logger.warn("close httpclient failed", e);
        }
    }

    private String encode(String url) throws Exception {
        if (url == null || url.length() == 0) {
            return "";
        }
        return URLEncoder.encode(url, HttpClientUtil.CHARSET_UTF8);
    }

}
