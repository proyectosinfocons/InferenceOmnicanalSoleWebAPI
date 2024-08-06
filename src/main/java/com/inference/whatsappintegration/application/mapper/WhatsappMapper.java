package com.inference.whatsappintegration.application.mapper;

import com.inference.whatsappintegration.application.dto.five9.conversationsdto.five9createconversationrequest.Attributes;
import com.inference.whatsappintegration.application.dto.five9.conversationsdto.five9createconversationrequest.Contact;
import com.inference.whatsappintegration.application.dto.five9.conversationsdto.five9createconversationrequest.Five9CreateConversationRequest;
import com.inference.whatsappintegration.application.dto.five9.five9tokenrequest.Five9TokenRequest;
import com.inference.whatsappintegration.application.dto.five9.five9tokenresponse.ApiUrl;
import com.inference.whatsappintegration.application.dto.five9.five9tokenresponse.DataCenter;
import com.inference.whatsappintegration.application.dto.five9.five9tokenresponse.Five9TokenResponse;
import com.inference.whatsappintegration.application.dto.whatsapp.whatappoutrequest.*;
import com.inference.whatsappintegration.application.dto.whatsapp.whatsappinrequest.WhatsappIncomingMessageRequestDTO;
import com.inference.whatsappintegration.application.dto.whatsapp.whatsappoldinrequest.WhatsappOldIncomingMessageRequestDTO;
import com.inference.whatsappintegration.domain.model.Conversation;
import com.inference.whatsappintegration.domain.model.TransferAgentInformation;
import com.inference.whatsappintegration.infrastructure.persistence.entity.Five9Session;
import com.inference.whatsappintegration.infrastructure.persistence.entity.Sessions;
import com.inference.whatsappintegration.infrastructure.persistence.entity.WhatsappDailyConversationHistory;
import com.inference.whatsappintegration.util.Constants;
import com.inference.whatsappintegration.util.Utils;
import com.inference.whatsappintegration.util.enums.EnumConversationStatus;
import com.inference.whatsappintegration.util.enums.EnumWhatsappContentType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class WhatsappMapper {

    @Value("${property.server.five9.callback.ip}")
    private String serverNgrokValue;

    @Value("${property.five9.default.campaign.option}")
    private String defaultFive9Campaign;

    @Value("${property.five9.default.skill.option.widget}")
    private String defaultSkillFive9Widget;

    @Value("${property.five9.default.skill.option.whatsapp}")
    private String defaultSkillFive9Whatsapp;

    @Value("${property.five9.default.skill.option.facebook}")
    private String defaultSkillFacebook;


    @Value("${property.redis.defaultExpirationTime}")
    private long defaultExpirationTime;

    public Sessions whatsappReceiveDTOtoSession(WhatsappIncomingMessageRequestDTO whatsappIncomingMessageRequestDTO){
        return Sessions.builder().identifier(whatsappIncomingMessageRequestDTO.getSubscriber().getIdentifier() + "-"
                + whatsappIncomingMessageRequestDTO.getSubject())
                .digitalChannelType(Constants.WHATSAPP_CHANNEL)
                .ani(whatsappIncomingMessageRequestDTO.getSubscriber().getIdentifier())
                .subjectId(whatsappIncomingMessageRequestDTO.getSubjectId().toString())
                .expiration(defaultExpirationTime).build();
    }

    public Sessions whatsappReceiveOldDTOtoSession(WhatsappOldIncomingMessageRequestDTO whatsappOldIncomingMessageRequestDTO){
        return Sessions.builder().identifier(whatsappOldIncomingMessageRequestDTO.getAddress() + "-"
                        + whatsappOldIncomingMessageRequestDTO.getImSubject())
                .digitalChannelType(Constants.WHATSAPP_CHANNEL)
                .ani(whatsappOldIncomingMessageRequestDTO.getAddress())
                .subjectId(whatsappOldIncomingMessageRequestDTO.getImSubjectId().toString())
                .expiration(defaultExpirationTime).build();
    }

    public List<WhatsappMessage> conversationToWhatsappTextMessage(Conversation conversation){
        List<WhatsappMessage> listWhatsAppMessages = new ArrayList<>();
        String textMessage = conversation.getStatus() == EnumConversationStatus.TRANSFER_AGENT ?
                conversation.getMessageText() :
                conversation.getMessageResponse();

        textMessage = Utils.replaceSpecialCharacters(textMessage);
        List<String> listTextMessages = Utils.splitSpecialMessageLinks(textMessage);

        for (String textMessageIterate : listTextMessages) {
            WhatsappContent whatsappContent = createWhatsappContent(textMessageIterate);
            listWhatsAppMessages.add(buildWhatsAppmessage(conversation, whatsappContent));
        }
        return listWhatsAppMessages;
    }

    public WhatsappDailyConversationHistory conversationClientToWhatsappHistory(Conversation conversation,
                                                                                Sessions incomingSession){
        return WhatsappDailyConversationHistory.builder().conversationId(conversation.getConversationId())
                .ani(conversation.getChannelType().equals(Constants.WHATSAPP_CHANNEL) ? conversation.getClientId() :
                        Constants.EMPTY_STRING)
                .sender(Constants.CLIENT_DEFAULT_IDENTIFIER)
                .receiver(incomingSession.getChannelType() == Constants.CHANNEL_TYPE_FIVE9_AGENT ?
                        Constants.FIVE9_RECEIVER : Constants.INFERENCE_BOT_RECEIVER)
                .type(Constants.INBOUND_TYPE)
                .content(conversation.getMessageText())
                .channel(conversation.getChannelType())
                .build();
    }

    public WhatsappDailyConversationHistory conversationBotToWhatsappHistory(Conversation conversation){
        String messageBotResponse = conversation.getStatus() == EnumConversationStatus.TRANSFER_AGENT ?
                conversation.getMessageText() : conversation.getMessageResponse();

        return WhatsappDailyConversationHistory.builder().conversationId(conversation.getConversationId())
                .sender(Constants.INFERENCE_BOT_RECEIVER)
                .receiver(Constants.CLIENT_DEFAULT_IDENTIFIER)
                .ani(conversation.getChannelType().equals(Constants.WHATSAPP_CHANNEL) ? conversation.getClientId() :
                        Constants.EMPTY_STRING)
                .type(Constants.OUTBOUND_TYPE)
                .content(messageBotResponse)
                .channel(conversation.getChannelType())
                .nodeCode(conversation.getNodeCode() != null ? conversation.getNodeCode() : Constants.EMPTY_STRING)
                .build();
    }

    public Five9Session five9TokenResponseToFive9Session(Five9TokenResponse five9TokenResponse, Sessions incomingSession){
        Optional<DataCenter> dataCenterOpt = five9TokenResponse.getMetadata().getDataCenters().stream().findFirst();
        Optional<ApiUrl> apiUrlOpt = dataCenterOpt.flatMap(dataCenter -> dataCenter.getApiUrls().stream().findFirst());

        return Five9Session.builder().tokenId(five9TokenResponse.getTokenId())
                .farmId(five9TokenResponse.getContext().getFarmId())
                .host(apiUrlOpt.map(ApiUrl::getHost).orElse(Constants.DEFAULT_FIVE9_API_HOST))
                .identifier(incomingSession.getIdentifier())
                .build();
    }

    public Five9CreateConversationRequest five9TokenResponseToCreateConversationRequest(Conversation conversation, Five9TokenResponse five9TokenResponse){
        String clientRequest = conversation.getTransferAgentInformation() != null ? conversation
                .getTransferAgentInformation().getClientRequest() : conversation.getMessageText();
        String email = getEmailFromConversation(conversation);

        return Five9CreateConversationRequest.builder()
                .campaignName(Optional.ofNullable(conversation.getTransferAgentInformation())
                        .map(TransferAgentInformation::getCampaign)
                        .orElse(defaultFive9Campaign))
                .tenantId(Integer.parseInt(five9TokenResponse.getOrgId()))
                .type(conversation.getChannel())
                .priority(Constants.DEFAULT_FIVE9_PRIORITY)
                .callbackUrl(serverNgrokValue.concat(Constants.DEFAULT_FIVE9_CONTEXT_PATH))
                .contact(
                        Contact.builder().number1(conversation.getChannel().equals(Constants.WHATSAPP_CHANNEL) ?
                                        conversation.getClientId() : null)
                                .firstName(conversation.getClientName())
                                .email(email)
                                .socialAccountImageUrl(Constants.DEFAULT_FIVE9_WHATSAPP_IMAGE_PROFILE)
                                .socialAccountProfileUrl(Constants.DEFAULT_FIVE9_WHATSAPP_IMAGE_PROFILE)
                                .build()
                )
                .attributes(
                        Attributes.builder()
                                .requirementProgramming(clientRequest)
                                .channel(conversation.getChannel().equals(Constants.WIDGET_CHANNEL_TYPE) ?
                                Constants.WIDGET_CHANNEL : conversation.getChannel())
                                .skillArea(Optional.ofNullable(conversation.getTransferAgentInformation())
                                        .map(TransferAgentInformation::getAgentSkill)
                                        .orElse(getSkillTransfer(conversation))
                                )
                                .clientScoreService(
                                        Optional.ofNullable(conversation.getTransferAgentInformation())
                                                .map(TransferAgentInformation::getClientScoreService)
                                                .orElse(Constants.EMPTY_STRING)
                                )
                                .chatSessionId(Optional.ofNullable(conversation.getTransferAgentInformation())
                                        .map(TransferAgentInformation::getChatSessionId)
                                        .orElse(Constants.EMPTY_STRING)
                                )
                                .clientDniRuc(
                                        Optional.ofNullable(conversation.getTransferAgentInformation())
                                                .map(TransferAgentInformation::getClientDniRuc)
                                                .orElse(Constants.EMPTY_STRING)
                                )
                                .clientBranch(
                                        Optional.ofNullable(conversation.getTransferAgentInformation())
                                                .map(TransferAgentInformation::getClientBranch)
                                                .orElse(Constants.EMPTY_STRING)
                                )
                                .clientFirstName(
                                        Optional.ofNullable(conversation.getTransferAgentInformation())
                                                .map(TransferAgentInformation::getClientFirstName)
                                                .orElse(Constants.EMPTY_STRING)
                                )
                                .clientLastName(
                                        Optional.ofNullable(conversation.getTransferAgentInformation())
                                                .map(TransferAgentInformation::getClientLastName)
                                                .orElse(Constants.EMPTY_STRING)
                                )
                                .clientEmail(
                                        Optional.ofNullable(conversation.getTransferAgentInformation())
                                                .map(TransferAgentInformation::getClientEmail)
                                                .orElse(Constants.EMPTY_STRING)
                                )
                                .clientSubscriberId(
                                        Optional.ofNullable(conversation.getTransferAgentInformation())
                                                .map(TransferAgentInformation::getClientSubscriberId)
                                                .orElse(Constants.EMPTY_STRING)
                                )
                                .clientServiceClass(
                                        Optional.ofNullable(conversation.getTransferAgentInformation())
                                                .map(TransferAgentInformation::getClientServiceClass)
                                                .orElse(Constants.EMPTY_STRING)
                                )
                                .clientCategories(
                                        Optional.ofNullable(conversation.getTransferAgentInformation())
                                                .map(TransferAgentInformation::getClientCategories)
                                                .orElse(Constants.EMPTY_STRING)
                                )
                                .clientServiceId(
                                        Optional.ofNullable(conversation.getTransferAgentInformation())
                                                .map(TransferAgentInformation::getClientServiceId)
                                                .orElse(Constants.EMPTY_STRING)
                                )
                                .clientDepartment(
                                        Optional.ofNullable(conversation.getTransferAgentInformation())
                                                .map(TransferAgentInformation::getClientDepartment)
                                                .orElse(Constants.EMPTY_STRING)
                                )
                                .clientAddress(
                                        Optional.ofNullable(conversation.getTransferAgentInformation())
                                                .map(TransferAgentInformation::getClientAddress)
                                                .orElse(Constants.EMPTY_STRING)
                                )
                                .clientContractedPlan(
                                        Optional.ofNullable(conversation.getTransferAgentInformation())
                                                .map(TransferAgentInformation::getClientContractedPlan)
                                                .orElse(Constants.EMPTY_STRING)
                                )
                                .clientProvince(
                                        Optional.ofNullable(conversation.getTransferAgentInformation())
                                                .map(TransferAgentInformation::getClientProvince)
                                                .orElse(Constants.EMPTY_STRING)
                                )
                                .clientDistrict(
                                        Optional.ofNullable(conversation.getTransferAgentInformation())
                                                .map(TransferAgentInformation::getClientDistrict)
                                                .orElse(Constants.EMPTY_STRING)
                                )
                                .clientPlan(
                                        Optional.ofNullable(conversation.getTransferAgentInformation())
                                                .map(TransferAgentInformation::getClientPlan)
                                                .orElse(Constants.EMPTY_STRING)
                                )
                                .clientServiceStatus(
                                        Optional.ofNullable(conversation.getTransferAgentInformation())
                                                .map(TransferAgentInformation::getClientServiceStatus)
                                                .orElse(Constants.EMPTY_STRING)
                                )
                                .clientInstallationDate(
                                        Optional.ofNullable(conversation.getTransferAgentInformation())
                                                .map(TransferAgentInformation::getClientInstallationDate)
                                                .orElse(Constants.EMPTY_STRING)
                                )
                                .clientWholeBalance(
                                        Optional.ofNullable(conversation.getTransferAgentInformation())
                                                .map(TransferAgentInformation::getClientWholeBalance)
                                                .orElse(Constants.EMPTY_STRING)
                                )
                                .clientDecimalBalance(
                                        Optional.ofNullable(conversation.getTransferAgentInformation())
                                                .map(TransferAgentInformation::getClientDecimalBalance)
                                                .orElse(Constants.EMPTY_STRING)
                                )
                                .clientDueDate(
                                        Optional.ofNullable(conversation.getTransferAgentInformation())
                                                .map(TransferAgentInformation::getClientDueDate)
                                                .orElse(Constants.EMPTY_STRING)
                                )
                                .clientPaymentCode(
                                        Optional.ofNullable(conversation.getTransferAgentInformation())
                                                .map(TransferAgentInformation::getClientPaymentCode)
                                                .orElse(Constants.EMPTY_STRING)
                                )
                                .clientBirthYear(
                                        Optional.ofNullable(conversation.getTransferAgentInformation())
                                                .map(TransferAgentInformation::getClientBirthYear)
                                                .orElse(Constants.EMPTY_STRING)
                                )
                                .clientEnteredPhone(
                                        Optional.ofNullable(conversation.getTransferAgentInformation())
                                                .map(TransferAgentInformation::getClientEnteredPhone)
                                                .orElse(Constants.EMPTY_STRING)
                                )
                                .clientRate(
                                        Optional.ofNullable(conversation.getTransferAgentInformation())
                                                .map(TransferAgentInformation::getClientRate)
                                                .orElse(Constants.EMPTY_STRING)
                                )
                                .conversationId(conversation.getConversationId())
                                .build()

                ).build();
    }

    private static String getEmailFromConversation(Conversation conversation) {
        String email = Constants.DEFAULT_EMPTY_EMAIL;

        if (conversation.getTransferAgentInformation() != null
                && !conversation.getTransferAgentInformation().getEmail().equals(Constants.EMPTY_STRING)) {
            email = conversation.getTransferAgentInformation().getEmail();
        } else if (conversation.getChannel().equals(Constants.WIDGET_CHANNEL_TYPE)) {
            email = String.format(Constants.DEFAULT_WIDGET_EMAIL, conversation.getWidgetId());
        } else if (conversation.getChannel().equals(Constants.WHATSAPP_CHANNEL)){
            email = conversation.getClientId() + Constants.DEFAULT_EMPTY_EMAIL_SUFIX;
        } else if (conversation.getChannel().equals(Constants.FACEBOOK_CHANNEL)) {
            email = String.format(Constants.DEFAULT_FACEBOOK_EMAIL, conversation.getClientId());
        }
        return email;
    }

    public Five9TokenRequest createFive9TokenRequest (){
        return Five9TokenRequest.builder().tenantName(Constants.TENANT_NAME).build();
    }

    private WhatsappContent createWhatsappContent(String textMessage) {
        WhatsappContent.WhatsappContentBuilder whatsappContentBuilder = WhatsappContent.builder();
        String matchingContentType = Utils.findMatchingContentType(textMessage);

        if (matchingContentType != null) {
            EnumWhatsappContentType contentType = EnumWhatsappContentType.fromFieldName(matchingContentType);
            Attachment attachment = Attachment.builder()
                    .url(Utils.extractUrlFromMessage(textMessage)).build();

            whatsappContentBuilder.contentType(contentType.getContentType()).attachment(attachment);
        } else {
            whatsappContentBuilder.contentType(EnumWhatsappContentType.TEXT.getContentType()).text(textMessage);
        }

        return whatsappContentBuilder.build();
    }

    private WhatsappMessage buildWhatsAppmessage(Conversation conversation, WhatsappContent whatsappContent){
        return WhatsappMessage.builder().requestId(Utils.generateRandomUUID())
                .cascadeId(conversation.getCascadeId())
                .subscriberFilter(
                        SubscriberFilter.builder().address(conversation.getClientId())
                                .type(Constants.SUBSCRIBE_FILTER_TYPE_PHONE).build()
                )
                .content(Content.builder().whatsappContent(whatsappContent).build())
                .build();
    }

    public String getSkillTransfer(Conversation conversation){
        if (conversation.getChannel().equals(Constants.WIDGET_CHANNEL_TYPE)){
            return defaultSkillFive9Widget;
        } else if (conversation.getChannel().equals(Constants.FACEBOOK_CHANNEL)){
            return defaultSkillFacebook;
        } else {
            return defaultSkillFive9Whatsapp;
        }
    }
}
