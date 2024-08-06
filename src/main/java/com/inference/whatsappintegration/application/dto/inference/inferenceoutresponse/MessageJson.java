package com.inference.whatsappintegration.application.dto.inference.inferenceoutresponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.inference.whatsappintegration.util.enums.EnumBotStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MessageJson {

    @JsonProperty("correo")
    private String email;

    @JsonProperty("consulta")
    private String clientRequest;

    @JsonProperty("motivo")
    private String request;

    @JsonProperty("canal")
    private String channel;

    @JsonProperty("botStatus")
    private EnumBotStatus botStatus;

    @JsonProperty("nodeCode")
    private String nodeCode;

    @JsonProperty("scordServOption")
    private String servOption;

    @JsonProperty("opcionMarcada")
    private String option;

    @JsonProperty("tipoOpcion")
    private String optionType;

    @JsonProperty("campania")
    private String campaign;

    @JsonProperty("skill")
    private String agentSkill;

    @JsonProperty("client_telefonoIngresado")
    private String clientPhone;

    @JsonProperty("client_filial")
    private String clientFilial;

    @JsonProperty("client_dniRuc")
    private String clientDNI;

    @JsonProperty("client_nombre")
    private String clientName;

    @JsonProperty("client_apellido")
    private String clientLastName;

    @JsonProperty("client_email")
    private String clientMail;

    @JsonProperty("client_idAbonado")
    private String clientIdSubscriber;

    @JsonProperty("client_claseServicio")
    private String clientServiceClass;

    @JsonProperty("client_categorias")
    private String clientCategory;

    @JsonProperty("client_idServicio")
    private String clientIdService;

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

    @JsonProperty("client_tarifa")
    private String clientRate;

}
