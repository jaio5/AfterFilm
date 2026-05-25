package alicanteweb.pelisapp.service;

import alicanteweb.pelisapp.entity.User;
import alicanteweb.pelisapp.repository.UserRepository;
import alicanteweb.pelisapp.service.image.ImageStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserProfileImageService {

    private static final String AVATAR_SUBFOLDER = "avatars";
    private static final long DEFAULT_MAX_AVATAR_BYTES = 5L * 1024L * 1024L;

    private final UserRepository userRepository;
    private final ImageStorage imageStorage;
    private final ImageUrlService imageUrlService;

    @Value("${app.users.profile-image.max-size-bytes:5242880}")
    private long maxAvatarBytes;

    @Transactional
    public String updateProfileImage(String username, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Debes subir una imagen o GIF");
        }
        long maxBytes = maxAvatarBytes > 0 ? maxAvatarBytes : DEFAULT_MAX_AVATAR_BYTES;
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException("La foto de perfil no puede superar " + (maxBytes / 1024 / 1024) + " MB");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        byte[] bytes = readBytes(file);
        ImageKind kind = detectImageKind(bytes);
        validateDeclaredContentType(file.getContentType(), kind);

        String filename = "user_" + user.getId() + "_" + System.currentTimeMillis() + "." + kind.extension();
        try {
            String storedPath = imageStorage.saveImage(new ByteArrayInputStream(bytes), filename, AVATAR_SUBFOLDER);
            user.setProfileImagePath(storedPath);
            userRepository.save(user);
            return imageUrlService.localImageUrlIfAvailable(storedPath);
        } catch (IOException e) {
            throw new IllegalArgumentException("No se pudo guardar la foto de perfil");
        }
    }

    public String profileImageUrl(User user) {
        if (user == null) {
            return null;
        }
        return imageUrlService.localImageUrlIfAvailable(user.getProfileImagePath());
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("No se pudo leer el archivo");
        }
    }

    private ImageKind detectImageKind(byte[] bytes) {
        if (bytes == null || bytes.length < 12) {
            throw new IllegalArgumentException("El archivo no es una imagen válida");
        }

        if ((bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff) {
            return ImageKind.JPEG;
        }
        if ((bytes[0] & 0xff) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e && bytes[3] == 0x47
                && bytes[4] == 0x0d && bytes[5] == 0x0a && bytes[6] == 0x1a && bytes[7] == 0x0a) {
            return ImageKind.PNG;
        }
        if (startsWithAscii(bytes, "GIF87a") || startsWithAscii(bytes, "GIF89a")) {
            return ImageKind.GIF;
        }
        if (startsWithAscii(bytes, "RIFF") && bytes[8] == 0x57 && bytes[9] == 0x45
                && bytes[10] == 0x42 && bytes[11] == 0x50) {
            return ImageKind.WEBP;
        }

        throw new IllegalArgumentException("Solo se permiten fotos JPG, PNG, WEBP o GIF");
    }

    private boolean startsWithAscii(byte[] bytes, String prefix) {
        if (bytes.length < prefix.length()) {
            return false;
        }
        for (int i = 0; i < prefix.length(); i++) {
            if (bytes[i] != prefix.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private void validateDeclaredContentType(String contentType, ImageKind kind) {
        if (contentType == null || contentType.isBlank()) {
            return;
        }
        String clean = contentType.toLowerCase(Locale.ROOT);
        if ("application/octet-stream".equals(clean)) {
            return;
        }
        if (kind == ImageKind.JPEG && ("image/jpg".equals(clean) || "image/pjpeg".equals(clean))) {
            return;
        }
        if (kind == ImageKind.WEBP && "image/x-webp".equals(clean)) {
            return;
        }
        if (!clean.equals(kind.contentType())) {
            throw new IllegalArgumentException("El tipo del archivo no coincide con una imagen válida");
        }
    }

    private enum ImageKind {
        JPEG("jpg", "image/jpeg"),
        PNG("png", "image/png"),
        WEBP("webp", "image/webp"),
        GIF("gif", "image/gif");

        private final String extension;
        private final String contentType;

        ImageKind(String extension, String contentType) {
            this.extension = extension;
            this.contentType = contentType;
        }

        String extension() {
            return extension;
        }

        String contentType() {
            return contentType;
        }
    }
}
