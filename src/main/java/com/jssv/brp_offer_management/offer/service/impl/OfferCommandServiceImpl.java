package com.jssv.brp_offer_management.offer.service.impl;

import com.jssv.brp_offer_management.brp.entity.BrpEntity;
import com.jssv.brp_offer_management.brp.repository.BrpRepository;
import com.jssv.brp_offer_management.offer.dto.request.*;
import com.jssv.brp_offer_management.offer.entity.*;
import com.jssv.brp_offer_management.offer.repository.OfferRepository;
import com.jssv.brp_offer_management.offer.service.OfferCommandService;
import com.jssv.brp_offer_management.security.entity.UserEntity;
import com.jssv.brp_offer_management.security.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class OfferCommandServiceImpl implements OfferCommandService {

    private static final BigDecimal ONE_HUNDRED =
            new BigDecimal("100.0000");

    private final OfferRepository offerRepository;
    private final BrpRepository brpRepository;
    private final UserRepository userRepository;

    public OfferCommandServiceImpl(
            OfferRepository offerRepository,
            BrpRepository brpRepository,
            UserRepository userRepository
    ) {
        this.offerRepository = offerRepository;
        this.brpRepository = brpRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public Long createOffer(
            OfferCreateRequest request,
            Long radicadorId
    ) {
        BrpEntity brp = brpRepository.findLatestById(request.brpId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "El BRP no existe o no corresponde a su versión vigente"
                ));

        if (!"DISPONIBLE".equals(brp.getEstado())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El BRP no se encuentra disponible para recibir ofertas"
            );
        }

        BigDecimal precioBrp = brp.getPrecio();

        if (precioBrp == null
                || precioBrp.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El BRP no tiene un precio válido"
            );
        }

        UserEntity radicador = userRepository.findById(radicadorId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "El usuario radicador no existe"
                ));

        validateOffer(request);

        BigDecimal descuento = precioBrp
                .subtract(request.valorOferta())
                .multiply(ONE_HUNDRED)
                .divide(precioBrp, 4, RoundingMode.HALF_UP);

        OfferEntity oferta = new OfferEntity();
        oferta.setBrp(brp);
        oferta.setValorOferta(request.valorOferta());
        oferta.setPorcentajeDescuento(descuento);
        oferta.setEstado("RADICADO");
        oferta.setRadicador(radicador);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        oferta.setFechaRadicacion(now);
        oferta.setFechaActualizacion(now);

        for (OfferApplicantRequest applicantRequest
                : request.oferentes()) {

            OfferApplicantEntity oferente =
                    new OfferApplicantEntity();

            oferente.setTipoIdentificacion(
                    applicantRequest.tipoIdentificacion()
            );
            oferente.setNumeroIdentificacion(
                    applicantRequest.numeroIdentificacion()
            );
            oferente.setNombre(applicantRequest.nombre());
            oferente.setPorcentajeParticipacion(
                    applicantRequest.porcentajeParticipacion()
            );
            oferente.setEmail(applicantRequest.email());
            oferente.setTelefono(applicantRequest.telefono());

            for (OfferPaymentRequest paymentRequest
                    : applicantRequest.pagos()) {

                OfferPaymentEntity pago = new OfferPaymentEntity();

                pago.setTipoPago(paymentRequest.tipoPago());
                pago.setValor(paymentRequest.valor());
                pago.setFechaPago(paymentRequest.fechaPago());
                pago.setEntidadFinanciera(
                        paymentRequest.entidadFinanciera()
                );

                oferente.addPago(pago);
            }

            oferta.addOferente(oferente);
        }

        return offerRepository.saveAndFlush(oferta).getId();
    }

    private void validateOffer(OfferCreateRequest request) {

        List<OfferApplicantRequest> oferentes = request.oferentes();

        if (oferentes == null
                || oferentes.isEmpty()
                || oferentes.size() > 5) {
            throw badRequest(
                    "La oferta debe tener entre 1 y 5 oferentes"
            );
        }

        if (request.valorOferta() == null
                || request.valorOferta().compareTo(BigDecimal.ZERO) <= 0) {
            throw badRequest(
                    "El valor de la oferta debe ser mayor que cero"
            );
        }

        BigDecimal participacionTotal = BigDecimal.ZERO;
        BigDecimal pagosTotales = BigDecimal.ZERO;

        for (OfferApplicantRequest oferente : oferentes) {

            if (oferente == null
                    || oferente.porcentajeParticipacion() == null) {
                throw badRequest(
                        "Los oferentes deben tener una participación válida"
                );
            }

            BigDecimal participacion =
                    oferente.porcentajeParticipacion();

            if (participacion.compareTo(BigDecimal.ZERO) <= 0
                    || participacion.compareTo(ONE_HUNDRED) > 0) {
                throw badRequest(
                        "La participación debe ser mayor que cero y máximo 100"
                );
            }

            participacionTotal =
                    participacionTotal.add(participacion);

            List<OfferPaymentRequest> pagos = oferente.pagos();

            if (pagos == null
                    || pagos.isEmpty()
                    || pagos.size() > 6) {
                throw badRequest(
                        "Cada oferente debe registrar entre 1 y 6 pagos"
                );
            }

            BigDecimal pagosOferente = BigDecimal.ZERO;

            for (OfferPaymentRequest pago : pagos) {

                if (pago == null
                        || pago.valor() == null
                        || pago.valor().compareTo(BigDecimal.ZERO) <= 0) {
                    throw badRequest(
                            "Todos los pagos deben tener un valor positivo"
                    );
                }

                if (!"EFECTIVO".equals(pago.tipoPago())
                        && !"CREDITO".equals(pago.tipoPago())) {
                    throw badRequest(
                            "El tipo de pago debe ser EFECTIVO o CREDITO"
                    );
                }

                if ("CREDITO".equals(pago.tipoPago())
                        && (pago.entidadFinanciera() == null
                        || pago.entidadFinanciera().isBlank())) {
                    throw badRequest(
                            "Los pagos a crédito requieren entidad financiera"
                    );
                }

                pagosOferente = pagosOferente.add(pago.valor());
            }

            BigDecimal valorEsperado = request.valorOferta()
                    .multiply(participacion)
                    .divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);

            if (pagosOferente.compareTo(valorEsperado) != 0) {
                throw badRequest(
                        "Los pagos del oferente "
                                + oferente.numeroIdentificacion()
                                + " no corresponden a su participación"
                );
            }

            pagosTotales = pagosTotales.add(pagosOferente);
        }

        if (participacionTotal.compareTo(ONE_HUNDRED) != 0) {
            throw badRequest(
                    "La suma de las participaciones debe ser exactamente 100%"
            );
        }

        if (pagosTotales.compareTo(request.valorOferta()) != 0) {
            throw badRequest(
                    "La suma de todos los pagos debe coincidir con el valor de la oferta"
            );
        }
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }
}
