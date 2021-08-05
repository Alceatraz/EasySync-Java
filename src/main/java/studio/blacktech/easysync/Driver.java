package studio.blacktech.easysync;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class Driver {

    private static int mkdirs;
    private static int upload;
    private static int update;
    private static int delete;
    private static int deldir;
    private static long size;

    private SFTPClient sftpClient;

    private String CLIENT_PATH;
    private String SERVER_PATH;

    private boolean deleteMode;

    public static void main(String[] args) {
        long a = System.currentTimeMillis();
        try {
            new Driver().main();
        } catch (Exception exception) {
            exception.printStackTrace();
            throw new RuntimeException();
        }
        long b = System.currentTimeMillis();
        System.out.println("累计耗时 " + (b - a) / 1000);
        System.out.println("创建目录 " + mkdirs);
        System.out.println("上传文件 " + upload);
        System.out.println("更新文件 " + update);
        System.out.println("删除目录 " + deldir);
        System.out.println("删除文件 " + delete);
        System.out.println("上传流量 " + size / 1000000D + "MB");
    }

    public void main() throws IOException {

        File CONFIG = Paths.get("config.properties").toFile();

        Properties properties = new Properties();

        try (InputStream inputStream = new FileInputStream(CONFIG)) {
            properties.load(inputStream);
        }

        this.CLIENT_PATH = properties.getProperty("easy-sync.path.client");
        this.SERVER_PATH = properties.getProperty("easy-sync.path.server");

        String HOST = properties.getProperty("easy-sync.server.host");
        String PORT = properties.getProperty("easy-sync.server.port");
        String USERNAME = properties.getProperty("easy-sync.username");
        String KEY_PATH = properties.getProperty("easy-sync.key.path");
        String KEY_HASH = properties.getProperty("easy-sync.key.hash");
        String DELETE_MODE = properties.getProperty("easy-sync.mode.delete");

        this.deleteMode = Boolean.parseBoolean(DELETE_MODE);

        SSHClient sshClient = new SSHClient();
        sshClient.addHostKeyVerifier(KEY_HASH);
        sshClient.connect(HOST, Integer.parseInt(PORT));
        sshClient.authPublickey(USERNAME, KEY_PATH);

        this.sftpClient = sshClient.newSFTPClient();

        try {
            this.sync("");
        } finally {
            this.sftpClient.close();
            sshClient.disconnect();
        }
    }


    private void sync(String relativePath) {

        File folder = Paths.get(this.CLIENT_PATH + relativePath).toFile();

        File[] files = folder.listFiles();

        if (files == null) {
            files = new File[0];
        }

        String serverRelativePath = this.SERVER_PATH + (relativePath.isEmpty() ? "\\" : relativePath + "\\");

        List<RemoteResourceInfo> list;

        try {
            list = this.sftpClient.ls(serverRelativePath);
        } catch (IOException ioException) {
            throw new RuntimeException(ioException);
        }

        Map<String, RemoteResourceInfo> remoteList = new HashMap<>();

        list.forEach(item -> remoteList.put(item.getName(), item));

        for (File file : files) {

            String fileName = file.getName();

            String clientFilePath = file.getAbsolutePath();
            String serverFilePath = serverRelativePath + fileName;

            RemoteResourceInfo remoteResourceInfo = remoteList.remove(fileName);

            if (file.isDirectory()) {

                if (remoteResourceInfo == null) {
                    System.out.println("MKDIRS " + serverFilePath);
                    try {
                        this.sftpClient.mkdir(serverFilePath);
                    } catch (IOException ioException) {
                        throw new RuntimeException("MKDIRS " + serverFilePath, ioException);
                    }
                    mkdirs++;
                }

                this.sync(relativePath + "\\" + fileName);

            } else {

                boolean needDelete = false;
                boolean needUpload = false;

                if (remoteResourceInfo == null) {
                    System.out.println("UPLOAD " + serverFilePath);
                    needUpload = true;
                    upload++;
                } else {
                    long clientMtime = file.lastModified() / 1000;
                    long remoteMtime = remoteResourceInfo.getAttributes().getMtime();
                    if (clientMtime > remoteMtime) {
                        needDelete = true;
                        needUpload = true;
                        System.out.println("UPDATE " + clientFilePath);
                        update++;
                    } else {
                        System.out.println("IGNORE " + clientFilePath);
                    }
                }

                if (this.deleteMode && needDelete) {
                    try {
                        this.sftpClient.rm(serverFilePath);
                    } catch (IOException ioException) {
                        throw new RuntimeException(ioException);
                    }
                }

                if (needUpload) {
                    size = size + file.length();
                    try {
                        this.sftpClient.put(clientFilePath, serverFilePath);
                    } catch (IOException ioException) {
                        throw new RuntimeException(ioException);
                    }
                }

            }
        }

        if (!this.deleteMode) return;

        for (Map.Entry<String, RemoteResourceInfo> entry : remoteList.entrySet()) {

            var k = entry.getKey();
            var v = entry.getValue();

            String serverFileName = serverRelativePath + k;

            if (v.isDirectory()) {
                this.delete(k);
            } else {
                System.out.println("DELETE " + serverFileName);
                try {
                    this.sftpClient.rm(serverFileName);
                    delete++;
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        }
    }


    private void delete(String path) {

        List<RemoteResourceInfo> list;

        String serverRelativePath = this.SERVER_PATH + "\\" + path;

        try {
            list = this.sftpClient.ls(serverRelativePath);
        } catch (IOException ioException) {
            throw new RuntimeException(ioException);
        }

        for (RemoteResourceInfo info : list) {

            String fileName = info.getName();
            String serverFileName = serverRelativePath + "\\" + fileName;

            if (info.isDirectory()) {

                System.out.println("DELDIR " + serverFileName);

                this.delete(path + "\\" + fileName);

            } else {

                System.out.println("DELETE " + serverFileName);

                try {
                    this.sftpClient.rm(serverFileName);
                } catch (IOException ioException) {
                    throw new RuntimeException(ioException);
                }
                delete++;
            }
        }

        try {
            this.sftpClient.rmdir(serverRelativePath);
        } catch (IOException ioException) {
            throw new RuntimeException(serverRelativePath, ioException);
        }

        deldir++;
    }
}