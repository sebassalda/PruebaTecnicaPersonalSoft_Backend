package com.jssv.brp_offer_management.offer.attachment;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/offers/{offerId}/attachments")
@RequiredArgsConstructor
public class OfferAttachmentController {
    private final OfferAttachmentService service;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ROLE_RADICADOR') and @moduleSecurity.hasAccess(authentication, 'OFFER_REGISTRATION')")
    public ResponseEntity<OfferAttachmentResponse> upload(@PathVariable Long offerId,
            @RequestPart("file") MultipartFile file, @AuthenticationPrincipal Jwt jwt) {
        OfferAttachmentResponse result = service.upload(offerId, jwt.getClaim("userId"),
                jwt.getClaimAsString("role"), file);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_RADICADOR','ROLE_ANALISTA','ROLE_JEFE','ROLE_ADMINISTRADOR') and @moduleSecurity.hasAccess(authentication, 'OFFER_TRACKING')")
    public List<OfferAttachmentResponse> list(@PathVariable Long offerId, @AuthenticationPrincipal Jwt jwt) {
        return service.list(offerId, jwt.getClaim("userId"), jwt.getClaimAsString("role"));
    }

    @GetMapping("/{attachmentId}")
    @PreAuthorize("hasAnyAuthority('ROLE_RADICADOR','ROLE_ANALISTA','ROLE_JEFE','ROLE_ADMINISTRADOR') and @moduleSecurity.hasAccess(authentication, 'OFFER_TRACKING')")
    public ResponseEntity<FileSystemResource> download(@PathVariable Long offerId,
            @PathVariable Long attachmentId, @AuthenticationPrincipal Jwt jwt) {
        var stored = service.download(offerId, attachmentId, jwt.getClaim("userId"),
                jwt.getClaimAsString("role"));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(stored.filename()).build().toString())
                .body(new FileSystemResource(stored.path()));
    }
}
