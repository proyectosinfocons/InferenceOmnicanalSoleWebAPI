package com.inference.whatsappintegration.application.mapper;

import com.inference.whatsappintegration.application.dto.facebook.facebookinrequest.FacebookInRequest;
import com.inference.whatsappintegration.application.dto.facebook.facebookinrequest.Messaging;
import com.inference.whatsappintegration.application.dto.five9.conversationsdto.five9acknowledgemessagerequest.AcknowledgeMessage;
import com.inference.whatsappintegration.application.dto.five9.conversationsdto.five9acknowledgemessagerequest.Five9AcknowledgeMessageRequest;
import com.inference.whatsappintegration.application.dto.five9.conversationsdto.five9conversationsendmessagerequest.SendConversationMessageRequest;
import com.inference.whatsappintegration.application.dto.five9.conversationseventsdto.conversationmessageeventrequest.ConversationMessageEventRequest;
import com.inference.whatsappintegration.application.dto.inference.inferenceoutrequest.InferenceOutRequest;
import com.inference.whatsappintegration.application.dto.inference.inferenceoutrequest.MessageJson;
import com.inference.whatsappintegration.application.dto.inference.inferenceoutresponse.InferenceOutResponse;
import com.inference.whatsappintegration.application.dto.webwidget.webwidgetinrequest.WebWidgetIncomingMessageRequestDTO;
import com.inference.whatsappintegration.application.dto.whatsapp.whatsappinrequest.Attachment;
import com.inference.whatsappintegration.application.dto.whatsapp.whatsappinrequest.Location;
import com.inference.whatsappintegration.application.dto.whatsapp.whatsappinrequest.WhatsappIncomingMessageRequestDTO;
import com.inference.whatsappintegration.application.dto.whatsapp.whatsappoldinrequest.WhatsappOldIncomingMessageRequestDTO;
import com.inference.whatsappintegration.domain.model.Conversation;
import com.inference.whatsappintegration.domain.model.TransferAgentInformation;
import com.inference.whatsappintegration.infrastructure.config.WhatsappSubjectProperties;
import com.inference.whatsappintegration.infrastructure.persistence.entity.ConversationCountSummary;
import com.inference.whatsappintegration.infrastructure.persistence.entity.Sessions;
import com.inference.whatsappintegration.infrastructure.persistence.entity.WhatsappDailyConversationHistory;
import com.inference.whatsappintegration.util.Constants;
import com.inference.whatsappintegration.util.Utils;
import com.inference.whatsappintegration.util.enums.EnumAcknowledgeType;
import com.inference.whatsappintegration.util.enums.EnumConversationStatus;
import com.inference.whatsappintegration.util.enums.EnumSummaryStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ConversationMapper {

    @Value("${property.inference.bot.api.key.main}")
    private String apiKeyInferenceBotMain;

    @Value("${property.inference.bot.task.key.main}")
    private String taskKeyMain;

    @Value("${property.inference.bot.api.key.webwidget}")
    private String apiKeyInferenceBotWebWidget;

    @Value("${property.inference.bot.task.key.webwidget}")
    private String taskKeWebWidget;

    @Value("${property.inference.bot.task.key.facebook}")
    private String taskKeFacebook;

    private WhatsappSubjectProperties whatsappSubjectProperties;

    public ConversationMapper(WhatsappSubjectProperties whatsappSubjectProperties){
        this.whatsappSubjectProperties = whatsappSubjectProperties;
    }


    public Conversation whatsappIncomingRequestToConversation(Sessions session,
                                                              WhatsappIncomingMessageRequestDTO whatsappIncomingMessageRequestDTO){
        String messageText = Utils.processWhatsAppType(whatsappIncomingMessageRequestDTO);
        return Conversation.builder().conversationId(session.getConversationId())
                .clientId(whatsappIncomingMessageRequestDTO.getSubscriber().getIdentifier())
                .sessionId(Optional.ofNullable(session.getSessionId()).orElse(Constants.EMPTY_STRING))
                .imSubject(whatsappIncomingMessageRequestDTO.getSubject())
                .clientName(whatsappIncomingMessageRequestDTO.getUserInfo().getUserName())
                .messageText(messageText)
                .messageAttachment(Optional.ofNullable(whatsappIncomingMessageRequestDTO.getMessageContent().getAttachment())
                        .map(Attachment::getUrl).orElse(null))
                .location(whatsappIncomingMessageRequestDTO.getMessageContent().getLocation())
                .subjectId(session.getSubjectId())
                .cascadeId(whatsappSubjectProperties.getCascade().get(session.getSubjectId()))
                .channel(Constants.WHATSAPP_CHANNEL)
                .channelType(Constants.WHATSAPP_CHANNEL)
                .status(EnumConversationStatus.IN_PROGRESS)
                .build();
    }

    public Conversation whatsappOldIncomingRequestToConversation(Sessions session,
                                                                 WhatsappOldIncomingMessageRequestDTO whatsappOldIncomingMessageRequestDTO){
        String messageText = Utils.processOldWhatsAppType(whatsappOldIncomingMessageRequestDTO);
        return Conversation.builder().conversationId(session.getConversationId())
                .clientId(whatsappOldIncomingMessageRequestDTO.getAddress())
                .sessionId(Optional.ofNullable(session.getSessionId()).orElse(Constants.EMPTY_STRING))
                .imSubject(whatsappOldIncomingMessageRequestDTO.getImSubject())
                .clientName(whatsappOldIncomingMessageRequestDTO.getUserName())
                .messageText(messageText)
                .messageAttachment(whatsappOldIncomingMessageRequestDTO.getAttachmentUrl())
                .location(whatsappOldIncomingMessageRequestDTO.getContentType().equals(Constants.WHATSAPP_CONTENT_TYPE_LOCATION) ?
                        Location.builder().longitude(whatsappOldIncomingMessageRequestDTO.getLongitude())
                        .latitude(whatsappOldIncomingMessageRequestDTO.getLatitude()).build() : null
                )
                .subjectId(session.getSubjectId())
                .cascadeId(whatsappSubjectProperties.getCascade().get(session.getSubjectId()))
                .channel(Constants.WHATSAPP_CHANNEL)
                .channelType(Constants.WHATSAPP_CHANNEL)
                .status(EnumConversationStatus.IN_PROGRESS)
                .build();

    }

    public Conversation widgetIncomingRequestToConversation(Sessions incomingSession,
                                                            WebWidgetIncomingMessageRequestDTO webWidgetIncomingMessageRequestDTO) {
        return Conversation.builder().conversationId(incomingSession.getConversationId())
                .clientId(webWidgetIncomingMessageRequestDTO.getSessionId())
                .sessionId(Optional.ofNullable(incomingSession.getSessionId()).orElse(Constants.EMPTY_STRING))
                .imSubject(webWidgetIncomingMessageRequestDTO.getSubject())
                .clientName(Constants.WIDGET_NAME_DEFAULT)
                .messageText(webWidgetIncomingMessageRequestDTO.getContentMessage())
                .widgetId(webWidgetIncomingMessageRequestDTO.getSessionId())
                .messageAttachment(null)
                .location(null)
                .status(EnumConversationStatus.IN_PROGRESS)
                .channel(Constants.WIDGET_CHANNEL_TYPE)
                .channelType(Constants.WIDGET_SUBJECT_DEFAULT)
                .build();
    }

    public Conversation facebookMessageEventRequestToConversation(Sessions incomingSession,
                                                                  FacebookInRequest facebookInRequest){
         Messaging messaging = facebookInRequest.getEntry().get(Constants.DEFAULT_META_ENTRY_START).getMessaging()
                 .get(Constants.DEFAULT_META_ENTRY_START);
        return Conversation.builder().conversationId(incomingSession.getConversationId())
                .clientId(messaging.getSender().getId())
                .sessionId(Optional.ofNullable(incomingSession.getSessionId()).orElse(Constants.EMPTY_STRING))
                .imSubject(Constants.FACEBOOK_CHANNEL)
                .clientName(Constants.WIDGET_NAME_DEFAULT)
                .messageText(messaging.getMessage().getText() != null ? messaging.getMessage().getText() :
                        Constants.EMPTY_STRING)
                .attachmentList(messaging.getMessage().getAttachments() != null &&
                        !messaging.getMessage().getAttachments().isEmpty() ? messaging.getMessage().getAttachments() : null)
                .status(EnumConversationStatus.IN_PROGRESS)
                .channel(Constants.FACEBOOK_CHANNEL)
                .channelType(Constants.FACEBOOK_CHANNEL)
                .build();
    }

    public InferenceOutRequest conversationToInferenceOutRequestNewConversation(
            Conversation conversation){
        return InferenceOutRequest.builder().apiKey(conversation.getChannel().equals(Constants.WIDGET_CHANNEL_TYPE) ?
                        apiKeyInferenceBotWebWidget : apiKeyInferenceBotMain)
                .taskKey(getTaskKeyByChannel(conversation))
                .sessionId(Optional.ofNullable(conversation.getSessionId()).orElse(Constants.EMPTY_STRING))
                .text(conversation.getMessageText())
                .userKey(Constants.EMPTY_STRING)
                .messageJson(
                        MessageJson.builder().userId(Constants.EMPTY_STRING)
                                .idRecipient(conversation.getClientId())
                                .channel(conversation.getChannel())
                                .clientName(conversation.getClientName())
                                .agent(Constants.EMPTY_STRING).build()
                )
                .build();
    }

    private String getTaskKeyByChannel(Conversation conversation){
        if (conversation.getChannel().equals(Constants.WIDGET_CHANNEL_TYPE)){
            return taskKeWebWidget;
        } else if (conversation.getChannel().equals(Constants.FACEBOOK_CHANNEL)){
            return taskKeFacebook;
        } else {
            return taskKeyMain;
        }
    }

    /*
    * Survey deprecated for clients:
    * Nubyx
    *  */
    public InferenceOutRequest conversationToInferenceSurveyOutRequestNewConversation(
            Conversation conversation){
        return InferenceOutRequest.builder().apiKey(conversation.getChannel().equals(Constants.WIDGET_CHANNEL_TYPE) ?
                        apiKeyInferenceBotWebWidget : apiKeyInferenceBotMain)
                .taskKey(getTaskKeyByChannel(conversation))
                .sessionId(Optional.ofNullable(conversation.getSessionId()).orElse(Constants.EMPTY_STRING))
                .text(conversation.getMessageText())
                .userKey(Constants.EMPTY_STRING)
                .messageJson(
                        MessageJson.builder().userId(Constants.EMPTY_STRING)
                                .idRecipient(conversation.getClientId())
                                .channel(Constants.WHATSAPP_CHANNEL)
                                .clientName(conversation.getClientName())
                                .agent(conversation.getAgentSurvey()).build()
                )
                .build();
    }

    public Conversation five9MessageAddedRequestToConversation(ConversationMessageEventRequest conversationMessageEventRequest
            , Sessions incomingSession) {
        return Conversation.builder().conversationId(incomingSession.getConversationId())
                .sessionId(incomingSession.getSessionId())
                .status(EnumConversationStatus.TRANSFER_AGENT)
                .messageText(conversationMessageEventRequest.getText())
                .clientId(incomingSession.getIdentifier().split("-")[0])
                .imSubject(incomingSession.getIdentifier().split("-")[1])
                .subjectId(incomingSession.getSubjectId())
                .channel(getChannel(incomingSession))
                .cascadeId(whatsappSubjectProperties.getCascade().get(incomingSession.getSubjectId()))
                .build();
    }

    private String getChannel(Sessions incomingSession){
        if(incomingSession.getIdentifier().contains(Constants.FACEBOOK_CHANNEL)){
            return Constants.FACEBOOK_CHANNEL;
        } else if (incomingSession.getWidgetId() != null && incomingSession.getWidgetId().isEmpty()){
            return Constants.WIDGET_SUBJECT_DEFAULT;
        } else {
            return Constants.WHATSAPP_CHANNEL;
        }
    }

    public Conversation genericConversationResponse(Sessions incomingSession, String message) {
        return Conversation.builder().conversationId(incomingSession.getConversationId())
                .sessionId(incomingSession.getSessionId())
                .status(EnumConversationStatus.IN_PROGRESS)
                .messageResponse(message)
                .clientId(incomingSession.getIdentifier().split("-")[0])
                .imSubject(incomingSession.getIdentifier().split("-")[1])
                .subjectId(incomingSession.getSubjectId())
                .channel(incomingSession.getWidgetId() != null && incomingSession.getWidgetId().isEmpty()?
                        Constants.WHATSAPP_CHANNEL : Constants.WIDGET_CHANNEL_TYPE)
                .cascadeId(whatsappSubjectProperties.getCascade().get(incomingSession.getSubjectId()))
                .build();
    }

    public SendConversationMessageRequest conversationToFive9SendConversationMessage(Conversation conversation){
        return SendConversationMessageRequest.builder().messageType(Constants.WHATSAPP_CONTENT_TYPE_TEXT)
                .message(conversation.getMessageText())
                .externalId(conversation.getConversationId())
                .build();
    }

    public Five9AcknowledgeMessageRequest messageRequestIdToAcknowledgeMessage(String requestId){
        return Five9AcknowledgeMessageRequest.builder().messages(
                List.of(
                        AcknowledgeMessage.builder().messageId(requestId)
                        .type(EnumAcknowledgeType.DELIVERED).build()
                )
        ).build();
    }

    public Conversation conversationAddTransferAgentInformation(Conversation conversation
            , InferenceOutResponse inferenceOutResponse){
        conversation.setTransferAgentInformation(
                TransferAgentInformation.builder()
                        .agentSkill(inferenceOutResponse.getMessageJson().getAgentSkill())
                        .email(inferenceOutResponse.getMessageJson().getEmail())
                        .campaign(inferenceOutResponse.getMessageJson().getCampaign())
                        .clientRequest(inferenceOutResponse.getMessageJson().getClientRequest())
                        .requirementProgramming(inferenceOutResponse.getMessageJson().getClientRequest())
                        .channel(inferenceOutResponse.getMessageJson().getChannel())
                        .chatSessionId(inferenceOutResponse.getSessionId())
                        .clientScoreService(inferenceOutResponse.getMessageJson().getServOption())
                        .clientDniRuc(inferenceOutResponse.getMessageJson().getClientDNI())
                        .clientBranch(inferenceOutResponse.getMessageJson().getClientFilial())
                        .clientFirstName(inferenceOutResponse.getMessageJson().getClientName())
                        .clientLastName(inferenceOutResponse.getMessageJson().getClientLastName())
                        .clientEmail(inferenceOutResponse.getMessageJson().getClientMail())
                        .clientSubscriberId(inferenceOutResponse.getMessageJson().getClientIdSubscriber())
                        .clientServiceClass(inferenceOutResponse.getMessageJson().getClientServiceClass())
                        .clientCategories(inferenceOutResponse.getMessageJson().getClientCategory())
                        .clientServiceId(inferenceOutResponse.getMessageJson().getClientIdService())
                        .clientDepartment(inferenceOutResponse.getMessageJson().getClientDepartment())
                        .clientAddress(inferenceOutResponse.getMessageJson().getClientAddress())
                        .clientContractedPlan(inferenceOutResponse.getMessageJson().getClientContractedPlan())
                        .clientProvince(inferenceOutResponse.getMessageJson().getClientProvince())
                        .clientDistrict(inferenceOutResponse.getMessageJson().getClientDistrict())
                        .clientPlan(inferenceOutResponse.getMessageJson().getClientPlan())
                        .clientServiceStatus(inferenceOutResponse.getMessageJson().getClientServiceStatus())
                        .clientInstallationDate(inferenceOutResponse.getMessageJson().getClientInstallationDate())
                        .clientWholeBalance(inferenceOutResponse.getMessageJson().getClientWholeBalance())
                        .clientDecimalBalance(inferenceOutResponse.getMessageJson().getClientDecimalBalance())
                        .clientDueDate(inferenceOutResponse.getMessageJson().getClientDueDate())
                        .clientPaymentCode(inferenceOutResponse.getMessageJson().getClientPaymentCode())
                        .clientBirthYear(inferenceOutResponse.getMessageJson().getClientBirthYear())
                        .clientEnteredPhone(inferenceOutResponse.getMessageJson().getClientPhone())
                        .clientRate(inferenceOutResponse.getMessageJson().getClientRate())
                        .build()
        );
        return conversation;
    }

    public WhatsappDailyConversationHistory conversationMessageEventToWhatsappHistory(
            ConversationMessageEventRequest conversationMessageEventRequest, Sessions incomingSession){
        return WhatsappDailyConversationHistory.builder().conversationId(incomingSession.getConversationId())
                .sender(Constants.FIVE9_RECEIVER)
                .receiver(Constants.CLIENT_DEFAULT_IDENTIFIER)
                .type(Constants.OUTBOUND_TYPE)
                .ani(incomingSession.getDigitalChannelType() != null &&
                        incomingSession.getDigitalChannelType().equals(Constants.WHATSAPP_CHANNEL) ?
                        incomingSession.getAni() : Constants.EMPTY_STRING)
                .channel(incomingSession.getDigitalChannelType() != null &&
                        !incomingSession.getDigitalChannelType().isEmpty() ?
                        incomingSession.getDigitalChannelType() : Constants.EMPTY_STRING)
                .content(conversationMessageEventRequest.getText())
                .build();
    }

    public ConversationCountSummary createNewBotConversationCountSummary(Conversation conversation){
        return ConversationCountSummary.builder().conversationId(conversation.getConversationId())
                .idClient(conversation.getClientId())
                .agent(Constants.INFERENCE_BOT_RECEIVER)
                .channel(conversation.getChannel())
                .countClient(Constants.DEFAULT_SUMMARY_COUNT_INITIALIZER)
                .countAgent(Constants.DEFAULT_SUMMARY_COUNT_INITIALIZER)
                .countTotal(Constants.DEFAULT_SUMMARY_TOTAL_COUNT_INITIALIZER)
                .status(EnumSummaryStatus.IN_PROGRESS).build();
    }

    public ConversationCountSummary createNewFive9ConversationCountSummary(Conversation conversation){
        return ConversationCountSummary.builder().conversationId(conversation.getConversationId())
                .idClient(conversation.getClientId())
                .agent(Constants.FIVE9_RECEIVER)
                .channel(conversation.getChannel())
                .countClient(Constants.DEFAULT_FIVE9_SUMMARY_COUNT_INITIALIZER)
                .countAgent(Constants.DEFAULT_FIVE9_SUMMARY_COUNT_INITIALIZER)
                .countTotal(Constants.DEFAULT_FIVE9_SUMMARY_COUNT_INITIALIZER)
                .status(EnumSummaryStatus.IN_PROGRESS).build();
    }

}
