package main.services;

import main.entities.FileChange;
import main.repositories.FileChangeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FileChangeService {
    private final FileChangeRepository fileChangeRepository;

    @Autowired
    public FileChangeService(FileChangeRepository fileChangeRepository) {
        this.fileChangeRepository = fileChangeRepository;
    }

    public void saveFile(FileChange fileChange) {
        fileChangeRepository.save(fileChange);
    }
}
