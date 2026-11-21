package com.jssv.brp_offer_management.offer.attachment;

import com.jssv.brp_offer_management.offer.entity.OfferEntity;
import com.jssv.brp_offer_management.offer.repository.OfferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OfferAttachmentService {
    private static final long MAX_SIZE = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of("application/pdf", "image/jpeg", "image/png");
    private final OfferRepository offerRepository;
    private final OfferAttachmentRepository attachmentRepository;

    @Value("${app.attachments.directory:./data/attachments}")
    private String directory;

    @Transactional
    public OfferAttachmentResponse upload(Long offerId, Long userId, String role, MultipartFile file) {
        OfferEntity offer = authorizedOffer(offerId, userId, role);
        if (file == null || file.isEmpty() || file.getSize() > MAX_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Archivo vacío o superior a 10 MB");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo se permiten PDF, JPEG o PNG");
        }
        String original = file.getOriginalFilename();
        if (original == null || original.isBlank() || original.length() > 255
                || original.contains("/") || original.contains("\\") || original.contains("..")
                || original.chars().anyMatch(c -> c < 32)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nombre de archivo inválido");
        }
        String extension = switch (file.getContentType()) {
            case "application/pdf" -> ".pdf";
            case "image/jpeg" -> ".jpg";
            default -> ".png";
        };
        Path saved = null;
        try {
            Path root = Paths.get(directory).toAbsolutePath().normalize();
            Files.createDirectories(root);
            if (Files.isSymbolicLink(root)) {
                throw new IOException("La carpeta de adjuntos no puede ser un enlace simbólico");
            }
            Path target = root.resolve(UUID.randomUUID() + extension);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target);
            }
            saved = target;
            OfferAttachmentEntity entity = new OfferAttachmentEntity();
            entity.setOferta(offer);
            entity.setNombreArchivo(original);
            entity.setRutaLocal(target.toString());
            entity.setFechaCarga(OffsetDateTime.now());
            return OfferAttachmentResponse.from(attachmentRepository.saveAndFlush(entity));
        } catch (IOException | RuntimeException ex) {
            if (saved != null) {
                try { Files.deleteIfExists(saved); } catch (IOException ignored) { }
            }
            if (ex instanceof ResponseStatusException response) throw response;
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible almacenar el adjunto", ex);
        }
    }

    @Transactional(readOnly = true)
    public List<OfferAttachmentResponse> list(Long offerId, Long userId, String role) {
        authorizedOffer(offerId, userId, role);
        return attachmentRepository.findAllByOferta_IdOrderByFechaCargaDesc(offerId)
                .stream().map(OfferAttachmentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public StoredAttachment download(Long offerId, Long attachmentId, Long userId, String role) {
        authorizedOffer(offerId, userId, role);
        OfferAttachmentEntity entity = attachmentRepository.findByIdAndOferta_Id(attachmentId, offerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Adjunto no encontrado"));
        Path root = Paths.get(directory).toAbsolutePath().normalize();
        Path path = Paths.get(entity.getRutaLocal()).toAbsolutePath().normalize();
        if (!path.startsWith(root) || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Archivo no disponible");
        }
        return new StoredAttachment(entity.getNombreArchivo(), path);
    }

    private OfferEntity authorizedOffer(Long offerId, Long userId, String role) {
        if ("RADICADOR".equals(role) || "ROLE_RADICADOR".equals(role)) {
            return offerRepository.findById(offerId)
                    .filter(o -> o.getRadicador().getId().equals(userId))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Oferta no encontrada"));
        }
        return offerRepository.findById(offerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Oferta no encontrada"));
    }

    public record StoredAttachment(String filename, Path path) {}
}
