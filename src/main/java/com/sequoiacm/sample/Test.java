package com.sequoiacm.sample;

import org.apache.log4j.Logger;
import org.bson.BSONObject;

public class Test {
    private static final Logger logger = Logger.getLogger(Test.class);

    public static void main(String[] args) throws Exception {
        String gatewayurl = "172.26.114.227:8080";
        String siteName = "rootsite"; // must be lowercase!!
        String userName = "admin";
        String password = "admin";
        String workspaceName = "ws_default";

        Sample s = new Sample(gatewayurl, siteName);
        String sessionId = null;
        try {
            // login
            sessionId = s.login(userName, password);
            logger.info("login success:sessionId=" + sessionId);

            // create file
            String fileId = s.createFile(sessionId, "fileName7", workspaceName);
            logger.info("create file success:fileId=" + fileId);

            // get file info by fileId (latestVersion:-1.-1)
            BSONObject fileInfo = s.getFileInfo(sessionId, workspaceName, fileId, -1, -1);
            logger.info("get file success:fileInfo=" + fileInfo);

            // download file ((latestVersion:-1.-1))
            s.downloadFileContent(sessionId, workspaceName, fileId, -1, -1, "./localFilePath.data");
            logger.info("download file content success:path=./localFilePath.data");

            // create batch
            BSONObject batchInfo = s.createBatch(sessionId, workspaceName, "batchName");
            logger.info("create batch success:batchInfo=" + batchInfo);

            String batchId = (String) batchInfo.get("id");

            // batch attach file
            s.batchAttachFile(sessionId, workspaceName, batchId, fileId);
            logger.info("attch file success:fileId=" + fileId);

            batchInfo = s.getBatchInfo(sessionId, workspaceName, batchId);
            logger.info("get batch:batchInfo=" + batchInfo);

            s.batchDetachFile(sessionId, workspaceName, batchId, fileId);
            logger.info("detach file success:fileId=" + fileId);

            s.deleteBatch(sessionId, workspaceName, batchId);
            logger.info("delete batch success:batchId=" + batchId);

            // delete file by id
            s.deleteFile(sessionId, fileId, workspaceName);
            logger.info("delete file success:fileId=" + fileId);
        }
        finally {
            if (sessionId != null) {
                s.logout(sessionId);
            }
            s.close();
        }
    }
}
