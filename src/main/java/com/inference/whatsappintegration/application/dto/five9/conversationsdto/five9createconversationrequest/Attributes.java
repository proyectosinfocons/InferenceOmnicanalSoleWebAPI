package com.inference.whatsappintegration.application.dto.five9.conversationsdto.five9createconversationrequest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Attributes {

    @JsonProperty("Question")
    private String requirementProgramming;

    @JsonProperty("Custom.external_history")
    private String customExternalHistory;

    @JsonProperty("canal")
    private String channel;

    @JsonProperty("skill_area")
    private String skillArea;

    @JsonProperty("chat_sessionId")
    private String chatSessionId;

    @JsonProperty("client_scordService")
    private String clientScoreService;

    @JsonProperty("client_dniRuc")
    private String clientDniRuc;

    @JsonProperty("client_filial")
    private String clientBranch;

    @JsonProperty("client_nombre")
    private String clientFirstName;

    @JsonProperty("client_apellido")
    private String clientLastName;

    @JsonProperty("client_email")
    private String clientEmail;

    @JsonProperty("client_idAbonado")
    private String clientSubscriberId;

    @JsonProperty("client_claseServicio")
    private String clientServiceClass;

    @JsonProperty("client_categorias")
    private String clientCategories;

    @JsonProperty("client_idServicio")
    private String clientServiceId;

    @JsonProperty("client_departamento")
    private String clientDepartment;

    @JsonProperty("client_direccion")
    private String clientAddress;

    @JsonProperty("client_planContratado")
    private String clientContractedPlan;

    @JsonProperty("client_provincia")
    private String clientProvince;

    @JsonProperty("client_distrito")
    private String clientDistrict;

    @JsonProperty("client_plano")
    private String clientPlan;

    @JsonProperty("client_estadoServicio")
    private String clientServiceStatus;

    @JsonProperty("client_fechaInstalacion")
    private String clientInstallationDate;

    @JsonProperty("client_saldoEntero")
    private String clientWholeBalance;

    @JsonProperty("client_saldoDecimal")
    private String clientDecimalBalance;

    @JsonProperty("client_fechaVencimiento")
    private String clientDueDate;

    @JsonProperty("client_codClientePago")
    private String clientPaymentCode;

    @JsonProperty("client_anioNacimiento")
    private String clientBirthYear;

    @JsonProperty("client_telefonoIngresado")
    private String clientEnteredPhone;

    @JsonProperty("client_tarifa")
    private String clientRate;

    @JsonProperty("conversationId")
    private String conversationId;

}
