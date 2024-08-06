package com.inference.whatsappintegration.application.service.imp;

import com.inference.whatsappintegration.application.dto.facebook.facebookinresponse.FacebookInResponse;
import com.inference.whatsappintegration.application.dto.facebook.facebookoutresponse.FacebookOutResponse;
import com.inference.whatsappintegration.application.dto.five9.conversationsdto.five9createconversationrequest.Five9CreateConversationRequest;
import com.inference.whatsappintegration.application.dto.five9.five9tokenrequest.Five9TokenRequest;
import com.inference.whatsappintegration.application.dto.five9.five9tokenresponse.Five9TokenResponse;
import com.inference.whatsappintegration.application.dto.inference.inferenceoutrequest.InferenceOutRequest;
import com.inference.whatsappintegration.application.dto.inference.inferenceoutresponse.InferenceOutResponse;
import com.inference.whatsappintegration.application.dto.webwidget.webwidgetoutresponse.WebWidgetIncomingMessageResponseDTO;
import com.inference.whatsappintegration.application.dto.whatsapp.whatappoutrequest.WhatsappContent;
import com.inference.whatsappintegration.application.dto.whatsapp.whatappoutrequest.WhatsappMessage;
import com.inference.whatsappintegration.application.mapper.ConversationMapper;
import com.inference.whatsappintegration.application.mapper.FacebookMapper;
import com.inference.whatsappintegration.application.mapper.WhatsappMapper;
import com.inference.whatsappintegration.application.mapper.WidgetMapper;
import com.inference.whatsappintegration.domain.model.Conversation;
import com.inference.whatsappintegration.domain.service.*;
import com.inference.whatsappintegration.infrastructure.persistence.entity.Five9Session;
import com.inference.whatsappintegration.infrastructure.persistence.entity.Sessions;
import com.inference.whatsappintegration.infrastructure.persistence.repository.Five9SessionRepository;
import com.inference.whatsappintegration.util.Constants;
import com.inference.whatsappintegration.util.Utils;
import com.inference.whatsappintegration.util.enums.EnumConversationStatus;
import com.inference.whatsappintegration.util.enums.EnumWhatsappContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConversationServiceImp implements ConversationService {

    private static final Logger LOGGER  = LoggerFactory.getLogger(ConversationServiceImp.class);

    private ConversationMapper conversationMapper;
    private WhatsappMapper whatsappMapper;
    private InferenceBotApi inferenceBotApi;
    private WhatsappMessageAPI whatsappMessageAPI;
    private Five9Service five9Service;
    private Five9SessionRepository five9SessionRepository;
    private RedisService redisService;
    private WidgetMapper widgetMapper;
    private WidgetServiceAPI widgetServiceAPI;
    private FacebookMapper facebookMapper;
    private FacebookMessageApi facebookMessageApi;


    public ConversationServiceImp (ConversationMapper conversationMapper, InferenceBotApi inferenceBotApi,
                                   WhatsappMessageAPI whatsappMessageAPI, WhatsappMapper whatsappMapper,
                                   Five9Service five9Service, Five9SessionRepository five9SessionRepository,
                                   RedisService redisService, WidgetMapper widgetMapper,
                                   WidgetServiceAPI widgetServiceAPI, FacebookMapper facebookMapper,
                                   FacebookMessageApi facebookMessageApi){
        this.conversationMapper = conversationMapper;
        this.whatsappMapper = whatsappMapper;
        this.inferenceBotApi = inferenceBotApi;
        this.whatsappMessageAPI = whatsappMessageAPI;
        this.five9Service = five9Service;
        this.five9SessionRepository = five9SessionRepository;
        this.redisService = redisService;
        this.widgetMapper = widgetMapper;
        this.widgetServiceAPI = widgetServiceAPI;
        this.facebookMapper = facebookMapper;
        this.facebookMessageApi = facebookMessageApi;
    }

    public Conversation sendConversationToInferenceBot(Conversation conversation) {
        LOGGER.info("Starting send conversation to inference bot");
        InferenceOutRequest inferenceOutRequest = conversationMapper
                .conversationToInferenceOutRequestNewConversation(conversation);
        InferenceOutResponse inferenceOutResponse = inferenceBotApi.sendMessageBot(inferenceOutRequest);
        if (inferenceOutResponse != null && Constants.SUCCESS_STATUS_CODES.contains(inferenceOutResponse.getStatus())) {
            if (conversation.getSessionId().isEmpty()) {
                conversation.setSessionId(inferenceOutResponse.getSessionId());
            }
            if (inferenceOutResponse.getMessageJson() != null){
                switch (inferenceOutResponse.getMessageJson().getBotStatus()) {
                    case TRANSFER:
                        conversation.setStatus(EnumConversationStatus.TRANSFER_AGENT);
                        conversation.setMessageText(inferenceOutResponse.getMessageResponse());
                        conversation = conversationMapper.conversationAddTransferAgentInformation(conversation, inferenceOutResponse);
                        break;
                    case CLOSE:
                        conversation.setStatus(EnumConversationStatus.TERMINATED);
                        conversation.setMessageResponse(inferenceOutResponse.getMessageResponse());
                        break;
                    case OPEN:
                        conversation.setMessageResponse(inferenceOutResponse.getMessageResponse());
                        break;

                    default: throw new IllegalArgumentException("Unexpected bot status: " +
                            inferenceOutResponse.getMessageJson().getBotStatus());
                }
                if(inferenceOutResponse.getMessageJson().getNodeCode() != null){
                    LOGGER.info("Actual Node Code is: " + inferenceOutResponse.getMessageJson().getNodeCode());
                    conversation.setNodeCode(inferenceOutResponse.getMessageJson().getNodeCode());
                } else {
                    LOGGER.info("No Node Code returned");
                }
            } else {
                conversation.setMessageResponse(inferenceOutResponse.getMessageResponse());
            }
        } else if (inferenceOutResponse != null && inferenceOutResponse.getStatus() == Constants.CREDIT_EXPIRED_STATUS_CODE) {
            conversation.setStatus(EnumConversationStatus.TRANSFER_AGENT);
        }
        return conversation;
    }

    public Conversation sendConversationToInferenceSurveyBot(Conversation conversation){
        LOGGER.info("Starting send conversation to inference survey bot");
        try {
            InferenceOutRequest inferenceOutRequest = conversationMapper
                    .conversationToInferenceSurveyOutRequestNewConversation(conversation);
            InferenceOutResponse inferenceOutResponse = inferenceBotApi.sendMessageBot(inferenceOutRequest);
            if (inferenceOutResponse != null && Constants.SUCCESS_STATUS_CODES.contains(inferenceOutResponse.getStatus())) {
                if (conversation.getSessionId().isEmpty()) {
                    conversation.setSessionId(inferenceOutResponse.getSessionId());
                }
                switch (inferenceOutResponse.getMessageJson().getBotStatus()) {
                    case CLOSE:
                        conversation.setStatus(EnumConversationStatus.TERMINATED);
                        conversation.setMessageResponse(inferenceOutResponse.getMessageResponse());
                        break;
                    case OPEN:
                        conversation.setMessageResponse(inferenceOutResponse.getMessageResponse());
                        break;
                    default:
                        throw new IllegalArgumentException("Unexpected bot status: " +
                            inferenceOutResponse.getMessageJson().getBotStatus());
                }
            } else if (inferenceOutResponse != null && inferenceOutResponse.getStatus() == Constants.CREDIT_EXPIRED_STATUS_CODE) {
                LOGGER.info("Credit expired while sending to inference bot survey");
                conversation.setStatus(EnumConversationStatus.TERMINATED);
            }
        } catch (Exception ex) {
            LOGGER.error("Exception while processing send conversation to inference bot survey: {}", ex.getMessage());
        }  finally {
            LOGGER.info("Finish process send conversation to inference bot survey");
        }
        return conversation;
    }

    public void sendConversationWhatsappResponse(Conversation conversation) {
        LOGGER.info("Starting send conversation response to whatsapp");
        try {
            List<WhatsappMessage> listWhatsappMessage = whatsappMapper.conversationToWhatsappTextMessage(conversation);
            for (WhatsappMessage whatsappMessage: listWhatsappMessage) {
                WhatsappContent messageContent = whatsappMessage.getContent().getWhatsappContent();
                if (!messageContent.getContentType().equals(EnumWhatsappContentType.TEXT.getContentType())){
                    whatsappMessageAPI.sendMessageResponse(whatsappMessage);
                    if (messageContent.getContentType().equals(EnumWhatsappContentType.VIDEO.getContentType())){
                        Thread.sleep(500);
                    }
                }else if (Utils.isNotEmptyStringValidation(messageContent.getText())) {
                    whatsappMessageAPI.sendMessageResponse(whatsappMessage);
                } else {
                    LOGGER.info("Mensaje vació");
                }
            }
        } catch (Exception ex) {
            LOGGER.error("Exception while processing send conversation response to whatsapp: {}", ex.getMessage());
        }  finally {
            LOGGER.info("Finish process send conversation response to whatsapp");
        }

    }

    public void sendConversationFacebookResponse(Conversation conversation) {
        LOGGER.info("Starting send conversation response to facebook");
        try {
            List<FacebookInResponse> facebookInResponses = facebookMapper.sendFacebookMessagesResponse(conversation);
            for (FacebookInResponse facebookInResponse: facebookInResponses) {
                if (facebookInResponse.getMessage().getText() == null){
                    facebookMessageApi.sendFacebookMessage(facebookInResponse);
                    if (facebookInResponse.getMessage().getAttachment().getType().equals("video")){
                        Thread.sleep(500);
                    }
                }else if (Utils.isNotEmptyStringValidation(facebookInResponse.getMessage().getText())) {
                    facebookMessageApi.sendFacebookMessage(facebookInResponse);
                } else {
                    LOGGER.info("Empty message");
                }
            }
        } catch (Exception ex) {
            LOGGER.error("Exception while processing send conversation response to facebook: {}", ex.getMessage());
        }  finally {
            LOGGER.info("Finish process send conversation response to facebook");
        }
    }

    public void sendConversationToFive9Agent(Conversation conversation, Sessions incomingSession){
        LOGGER.info("Sending interaction to five9 agent");
        try {
            Five9TokenRequest five9TokenRequest = whatsappMapper.createFive9TokenRequest();
            Five9TokenResponse five9TokenResponse = five9Service.getToken(five9TokenRequest);
            Five9Session five9Session = whatsappMapper.five9TokenResponseToFive9Session(five9TokenResponse, incomingSession);
            incomingSession.setSessionId(five9Session.getTokenId());
            conversation.setSessionId(five9Session.getTokenId());
            updateSessionExpirationTimeNull(incomingSession);
            five9SessionRepository.save(five9Session);
            Five9CreateConversationRequest five9CreateConversationRequest = whatsappMapper
                    .five9TokenResponseToCreateConversationRequest(conversation, five9TokenResponse);
            five9Service.createConversation(five9CreateConversationRequest, five9Session);
        } catch (Exception ex) {
            LOGGER.error("Exception while sending interaction to five9 agent: {}", ex.getMessage());
        }  finally {
            LOGGER.info("Finish process sending interaction to five9 agent");
        }
    }

    public void sendConversationToWidget(Conversation conversation, Sessions incomingSession){
        LOGGER.info("Sending interaction to widget");
        try {
            WebWidgetIncomingMessageResponseDTO webWidgetIncomingMessageResponseDTO = widgetMapper
                    .webWidgetIncomingMessageResponseDTO(conversation, incomingSession);
            widgetServiceAPI.sendMessageToWidgetAPI(webWidgetIncomingMessageResponseDTO);
        } catch (Exception ex) {
            LOGGER.error("Exception while sending interaction to widget: {}", ex.getMessage());
        }  finally {
            LOGGER.info("Finish process sending to widget");
        }
    }

    private void updateSessionExpirationTimeNull(Sessions incomingSession){
        redisService.saveWithAudit(incomingSession, null);
    }

}
