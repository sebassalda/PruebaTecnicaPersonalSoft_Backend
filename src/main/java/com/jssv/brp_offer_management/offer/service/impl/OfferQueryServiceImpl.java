package com.jssv.brp_offer_management.offer.service.impl;

import com.jssv.brp_offer_management.offer.dto.response.*;
import com.jssv.brp_offer_management.offer.entity.*;
import com.jssv.brp_offer_management.offer.repository.OfferRepository;
import com.jssv.brp_offer_management.offer.service.OfferQueryService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;

import com.jssv.brp_offer_management.offer.repository.OfferPaymentRepository;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OfferQueryServiceImpl implements OfferQueryService {

    private final OfferRepository offerRepository;
    private final OfferPaymentRepository offerPaymentRepository;

    private void validateUserContext(
            Long authenticatedUserId,
            String authenticatedRole
    ) {
        if (authenticatedUserId == null
                || authenticatedUserId <= 0) {
            throw new AccessDeniedException(
                    "No se pudo identificar al usuario autenticado"
            );
        }

        if (authenticatedRole == null
                || !switch (authenticatedRole) {
            case "RADICADOR", "ANALISTA",
                 "JEFE", "ADMINISTRADOR" -> true;
            default -> false;
        }) {
            throw new AccessDeniedException(
                    "El usuario no tiene un rol autorizado"
            );
        }
    }

    @Override
    public Page<OfferSummaryResponse> findAll(
            Pageable pageable,
            Long authenticatedUserId,
            String authenticatedRole
    ) {
        validateUserContext(authenticatedUserId, authenticatedRole);

        Page<OfferEntity> offers;

        if ("RADICADOR".equals(authenticatedRole)) {
            offers = offerRepository.findAllByRadicadorId(
                    authenticatedUserId,
                    pageable
            );
        } else {
            offers = offerRepository.findAllWithReferences(pageable);
        }

        return offers.map(this::toSummary);
    }

    @Override
    public OfferDetailResponse findById(
            Long offerId,
            Long authenticatedUserId,
            String authenticatedRole
    ) {
        validateUserContext(authenticatedUserId, authenticatedRole);

        if (offerId == null || offerId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El identificador de la oferta no es válido"
            );
        }

        Optional<OfferEntity> result;

        if ("RADICADOR".equals(authenticatedRole)) {
            result = offerRepository.findWithDetailsByIdAndRadicador_Id(
                    offerId,
                    authenticatedUserId
            );
        } else {
            result = offerRepository.findWithDetailsById(offerId);
        }

        OfferEntity offer = result.orElseThrow(
                () -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "La oferta solicitada no existe"
                )
        );

        return toDetail(offer);
    }


    private OfferSummaryResponse toSummary(OfferEntity offer) {

        return new OfferSummaryResponse(
                offer.getId(),
                offer.getBrp().getId(),
                offer.getBrp().getCodigo(),
                offer.getValorOferta(),
                offer.getPorcentajeDescuento(),
                offer.getEstado(),
                offer.getRadicador().getId(),
                offer.getAnalista() != null
                        ? offer.getAnalista().getId()
                        : null,
                offer.getFechaRadicacion()
        );
    }


    private OfferDetailResponse toDetail(OfferEntity offer) {

        List<OfferPaymentEntity> payments =
                offerPaymentRepository.findAllByOfferId(offer.getId());

        Map<Long, List<OfferPaymentEntity>> paymentsByApplicant =
                payments.stream()
                        .collect(Collectors.groupingBy(
                                payment -> payment.getOferente().getId()
                        ));

        List<OfferApplicantResponse> applicants =
                offer.getOferentes()
                        .stream()
                        .map(applicant -> toApplicant(
                                applicant,
                                paymentsByApplicant.getOrDefault(
                                        applicant.getId(),
                                        List.of()
                                )
                        ))
                        .toList();

        return new OfferDetailResponse(
                offer.getId(),
                offer.getBrp().getId(),
                offer.getBrp().getCodigo(),
                offer.getValorOferta(),
                offer.getPorcentajeDescuento(),
                offer.getEstado(),
                offer.getRadicador().getId(),
                offer.getAnalista() != null
                        ? offer.getAnalista().getId()
                        : null,
                offer.getFechaRadicacion(),
                offer.getFechaActualizacion(),
                offer.getFechaEscalamiento(),
                offer.getFechaDevolucion(),
                offer.getFechaFinalizacion(),
                applicants
        );
    }


    private OfferApplicantResponse toApplicant(
            OfferApplicantEntity applicant,
            List<OfferPaymentEntity> applicantPayments
    ) {

        List<OfferPaymentResponse> payments =
                applicantPayments.stream()
                        .map(this::toPayment)
                        .toList();

        return new OfferApplicantResponse(
                applicant.getId(),
                applicant.getTipoIdentificacion(),
                applicant.getNumeroIdentificacion(),
                applicant.getNombre(),
                applicant.getPorcentajeParticipacion(),
                applicant.getEmail(),
                applicant.getTelefono(),
                payments
        );
    }


    private OfferPaymentResponse toPayment(
            OfferPaymentEntity payment
    ) {

        return new OfferPaymentResponse(
                payment.getId(),
                payment.getTipoPago(),
                payment.getValor(),
                payment.getFechaPago(),
                payment.getEntidadFinanciera()
        );
    }
}
