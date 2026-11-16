package com.toolrental.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.toolrental.exception.ApiException;

/**
 * Сохраняет загруженные фото в папку uploads.
 */
@Service
public class FileStorageService {

    private final Path folder;

    public FileStorageService(@Value("${app.upload-dir}") String uploadDir) {
        this.folder = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    // Возвращает имя, под которым файл сохранён
    public String save(MultipartFile file) {
        String type = file.getContentType();
        String extension;
        if ("image/jpeg".equals(type)) {
            extension = ".jpg";
        } else if ("image/png".equals(type)) {
            extension = ".png";
        } else {
            throw ApiException.badRequest("Допустимы только фото в формате JPEG или PNG");
        }
        String fileName = UUID.randomUUID() + extension;
        try {
            Files.createDirectories(folder);
            file.transferTo(folder.resolve(fileName));
        } catch (IOException e) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "Не удалось сохранить файл");
        }
        return fileName;
    }

    public void delete(String fileName) {
        try {
            Files.deleteIfExists(folder.resolve(fileName));
        } catch (IOException e) {
            // Файл не удалился — не страшно, запись в БД всё равно удаляется
        }
    }
}
