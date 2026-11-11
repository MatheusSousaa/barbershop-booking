package com.barbershop.service;

import com.barbershop.exception.BusinessException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;

@Service
@Slf4j
public class ServicePhotoStorage {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final long MAX_IMAGE_PIXELS = 20_000_000;

    @Getter
    private final Path servicesDirectory;

    public ServicePhotoStorage(
            @Value("${barbershop.upload.directory:./uploads}") String uploadDirectory) {
        this.servicesDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize().resolve("services");
    }

    public StoredPhoto store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Selecione uma foto para enviar.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("A foto deve ter no máximo 5 MB.");
        }
        if (file.getContentType() == null ||
                !(file.getContentType().equalsIgnoreCase("image/jpeg") ||
                        file.getContentType().equalsIgnoreCase("image/png"))) {
            throw new BusinessException("Envie uma imagem JPG ou PNG.");
        }

        try {
            byte[] content = file.getBytes();
            String extension = validateImage(content, file.getContentType());
            String fileName = UUID.randomUUID().toString().replace("-", "") + extension;
            Files.createDirectories(servicesDirectory);
            Files.write(servicesDirectory.resolve(fileName), content, StandardOpenOption.CREATE_NEW);
            return new StoredPhoto(fileName, "/uploads/services/" + fileName);
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível salvar a foto do serviço.", exception);
        }
    }

    public void delete(String photoUrl) {
        if (photoUrl == null || !photoUrl.matches("/uploads/services/[a-f0-9]{32}\\.(jpg|png)")) {
            return;
        }
        String fileName = photoUrl.substring(photoUrl.lastIndexOf('/') + 1);
        try {
            Files.deleteIfExists(servicesDirectory.resolve(fileName).normalize());
        } catch (IOException exception) {
            log.warn("Não foi possível remover a foto substituída {}", fileName, exception);
        }
    }

    private String validateImage(byte[] content, String contentType) throws IOException {
        try {
            try (ImageInputStream imageInput = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
                if (imageInput == null) {
                    throw new BusinessException("O arquivo enviado não é uma imagem JPG ou PNG válida.");
                }
                Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
                if (!readers.hasNext()) {
                    throw new BusinessException("O arquivo enviado não é uma imagem JPG ou PNG válida.");
                }

                ImageReader reader = readers.next();
                try {
                    reader.setInput(imageInput, true, true);
                    String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                    boolean isJpeg = format.equals("jpeg") || format.equals("jpg");
                    boolean isPng = format.equals("png");
                    if (!(isJpeg && contentType.equalsIgnoreCase("image/jpeg")) &&
                            !(isPng && contentType.equalsIgnoreCase("image/png"))) {
                        throw new BusinessException("O conteúdo da imagem não corresponde ao formato enviado.");
                    }

                    int width = reader.getWidth(0);
                    int height = reader.getHeight(0);
                    if (width <= 0 || height <= 0 || (long) width * height > MAX_IMAGE_PIXELS) {
                        throw new BusinessException("A imagem excede o limite de resolução permitido.");
                    }
                    reader.read(0);
                    return isJpeg ? ".jpg" : ".png";
                } finally {
                    reader.dispose();
                }
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new BusinessException("O arquivo enviado não é uma imagem JPG ou PNG válida.");
        }
    }

    public record StoredPhoto(String fileName, String url) {}
}
