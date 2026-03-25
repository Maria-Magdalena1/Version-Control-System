package main.services;

import main.entities.Document;
import main.entities.DocumentFile;
import main.repositories.DocumentFileRepository;
import main.repositories.FileChangeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DocumentFileService {

    private final DocumentFileRepository documentFileRepository;
    private final FileChangeRepository fileChangeRepository;

    @Autowired
    public DocumentFileService(DocumentFileRepository documentFileRepository, FileChangeRepository fileChangeRepository) {
        this.documentFileRepository = documentFileRepository;
        this.fileChangeRepository = fileChangeRepository;
    }

    //public DocumentFile uploadFile(String sourcePath, Document document) {
    //}
}
