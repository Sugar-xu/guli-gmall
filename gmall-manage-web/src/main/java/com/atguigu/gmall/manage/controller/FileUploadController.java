package com.atguigu.gmall.manage.controller;

import org.apache.commons.lang3.StringUtils;
import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@CrossOrigin
public class FileUploadController {

    @Value("${fileServer.url}")
    private String fileUrl;

    //http://localhost:8082/fileUpload
    @RequestMapping("fileUpload")
    public String fileUpload(MultipartFile file) throws IOException, MyException {
        String configFile = this.getClass().getResource("/tracker.conf").getFile();
        ClientGlobal.init(configFile);
        TrackerClient trackerClient = new TrackerClient();
        TrackerServer trackerServer = trackerClient.getConnection();
        StorageClient storageClient = new StorageClient(trackerServer, null);
        //获取上传文件名
        String orginalFilename = file.getOriginalFilename();
        //获取文件后缀名
        String extName = StringUtils.substringAfterLast(orginalFilename, ".");
        //获取本地文件
        //String[] upload_file = storageClient.upload_file(orginalFilename, "jpg", null);
        String[] upload_file = storageClient.upload_file(file.getBytes(), extName, null);
        String imgUrl = fileUrl;
        for (int i = 0; i < upload_file.length; i++) {
            String path = upload_file[i];
            imgUrl += "/" + path;
        }

        return imgUrl;
    }
}
