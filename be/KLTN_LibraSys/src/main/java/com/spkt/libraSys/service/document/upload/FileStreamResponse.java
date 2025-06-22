package com.spkt.libraSys.service.document.upload;

import org.springframework.core.io.Resource;

public class FileStreamResponse {

    private Resource file;
    private String fileType;

    public FileStreamResponse(Resource file, String fileType) {
        this.file = file;
        this.fileType = fileType;
    }

    public Resource getFile() {
        return file;
    }

    public void setFile(Resource file) {
        this.file = file;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
}
