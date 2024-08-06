package com.inference.whatsappintegration.application.service.imp;

import com.inference.whatsappintegration.application.dto.five9.conversationsdto.five9conversationsendmessagerequest.SendConversationMessageRequest;
import com.inference.whatsappintegration.application.dto.webwidget.webwidgetinrequest.WebWidgetIncomingMessageRequestDTO;
import com.inference.whatsappintegration.application.mapper.ConversationMapper;
import com.inference.whatsappintegration.application.mapper.WidgetMapper;
import com.inference.whatsappintegration.domain.model.Conversation;
import com.inference.whatsappintegration.domain.service.*;
import com.inference.whatsappintegration.infrastructure.config.mdc.MdcAwareExecutor;
import com.inference.whatsappintegration.infrastructure.exception.ErrorMessages;
import com.inference.whatsappintegration.infrastructure.exception.GenericException;
import com.inference.whatsappintegration.infrastructure.persistence.entity.Five9Session;
import com.inference.whatsappintegration.infrastructure.persistence.entity.Sessions;
import com.inference.whatsappintegration.infrastructure.persistence.repository.Five9SessionRepository;
import com.inference.whatsappintegration.infrastructure.persistence.repository.SessionRepository;
import com.inference.whatsappintegration.util.Constants;
import com.inference.whatsappintegration.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WebWidgetServiceImp implements WebWidgetService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebWidgetServiceImp.class);

    private WidgetMapper widgetMapper;
    private RedisService redisService;
    private ConversationService conversationService;
    private ConversationMapper conversationMapper;
    private MdcAwareExecutor mdcAwareExecutor;
    private DatabaseOperationsService databaseOperationsService;
    private Five9SessionRepository five9SessionRepository;
    private Five9ConversationService five9ConversationService;
    private SessionRepository sessionRepository;

    @Value("${property.inference.bot.disable}")
    private int botFlag;


    public WebWidgetServiceImp(WidgetMapper widgetMapper, RedisService redisService,
                               ConversationService conversationService, ConversationMapper conversationMapper,
                               MdcAwareExecutor mdcAwareExecutor, DatabaseOperationsService databaseOperationsService,
                               Five9SessionRepository five9SessionRepository, Five9ConversationService five9ConversationService,
                               SessionRepository sessionRepository){
        this.widgetMapper = widgetMapper;
        this.redisService = redisService;
        this.conversationService = conversationService;
        this.conversationMapper = conversationMapper;
        this.mdcAwareExecutor = mdcAwareExecutor;
        this.databaseOperationsService = databaseOperationsService;
        this.five9SessionRepository = five9SessionRepository;
        this.five9ConversationService = five9ConversationService;
        this.sessionRepository = sessionRepository;
    }

    @Override
    public void processWebWidgetInteraction(WebWidgetIncomingMessageRequestDTO webWidgetIncomingMessageRequestDTO) {
        LOGGER.info("Starting process receive web widget interaction");
        try {
            Utils.logAsJson(LOGGER, webWidgetIncomingMessageRequestDTO, "WebWidgetIncomingMessageRequestDTO");
            Sessions incomingSessionTmp = widgetMapper.widgetReceiveSession(webWidgetIncomingMessageRequestDTO);
            Sessions incomingSession = redisService.getSessionFromRequest(incomingSessionTmp);
            Conversation conversation = conversationMapper.widgetIncomingRequestToConversation(incomingSession
                    , webWidgetIncomingMessageRequestDTO);
            if (!validateFirstInteraction(conversation, incomingSession)){
                return;
            }
            mdcAwareExecutor.execute(() -> databaseOperationsService.insertClientInteraction(conversation, incomingSession));

            if (botFlag == Constants.BOT_FLAG_DEACTIVATED) {
                if (conversation.getMessageText().equals(Constants.WIDGET_FIRST_INTERACTION)) {
                    return;
                }
                if (incomingSession.getSessionId() == null) {
                    handleBotDeactivatedAndSessionIsNotPresent(conversation, incomingSession);
                } else {
                    handleBotDeactivatedAndSessionIsPresent(conversation, incomingSession);
                }
            } else {
                switch (incomingSession.getChannelType()){
                    case Constants.CHANNEL_TYPE_INFERENCE_BOT:
                        processBotInteraction(conversation, incomingSession);
                        break;
                    case Constants.CHANNEL_TYPE_FIVE9_AGENT:
                        processAgentInteraction(conversation, incomingSession);
                        break;
                    default:
                        throw new GenericException("No channel type session matching");
                }
            }

        } catch (Exception ex) {
            LOGGER.error("Exception while processing receive web widget interaction: {}", ex.getMessage());
        }  finally {
            LOGGER.info("Finish process receive web widget interaction");
            Utils.clearMDCParameters();
        }
    }

    private void processBotInteraction(Conversation conversation, Sessions incomingSession) {
        LOGGER.info("Starting process bot interaction");
        try {
            conversation = conversationService.sendConversationToInferenceBot(conversation);
            Conversation tempConversation = conversation;
            mdcAwareExecutor.execute(() -> databaseOperationsService.insertBotMetricsInteraction(tempConversation));
            switch (conversation.getStatus()) {
                case IN_PROGRESS:
                    if (incomingSession.getSessionId() == null) {
                        incomingSession.setSessionId(conversation.getSessionId());
                    }
                    redisService.saveWithAudit(incomingSession, Constants.EXPIRATION_TIME_DEFAULT);
                    conversationService.sendConversationToWidget(conversation, incomingSession);
                    break;
                case TRANSFER_AGENT:
                    incomingSession.setChannelType(Constants.CHANNEL_TYPE_FIVE9_AGENT);
                    incomingSession.setSessionId(null);
                    redisService.saveWithAudit(incomingSession, null);
                    if (conversation.getTransferAgentInformation() != null) {
                        conversationService.sendConversationToWidget(conversation, incomingSession);
                    }
                    conversationService.sendConversationToFive9Agent(conversation, incomingSession);
                    break;
                case TERMINATED:
                    sessionRepository.delete(incomingSession);
                    conversationService.sendConversationToWidget(conversation, incomingSession);
                    break;
                default:
                    throw new GenericException("No conversation status matching");
            }
        } catch (Exception ex) {
            LOGGER.error("Exception while processing bot interaction: {}", ex.getMessage());
        }  finally {
            LOGGER.info("Finish process bot interaction");
        }
    }

    private void handleBotDeactivatedAndSessionIsNotPresent(Conversation conversation, Sessions incomingSession){
        LOGGER.info("Bot flow is deactivated and sessionId is not present, sending interaction to five 9 agent");
        try {
            if (incomingSession.getChannelType() == Constants.CHANNEL_TYPE_INFERENCE_BOT){
                incomingSession.setChannelType(Constants.CHANNEL_TYPE_FIVE9_AGENT);
                redisService.saveWithAudit(incomingSession, null);
            }
            conversationService.sendConversationToFive9Agent(conversation, incomingSession);
        } catch (Exception ex) {
            LOGGER.error("Exception while processing bot deactivated and sessionId not present: {}", ex.getMessage());
        } finally {
            LOGGER.info("Finish process bot deactivated and sessionId not present");
        }
    }

    private void handleBotDeactivatedAndSessionIsPresent(Conversation conversation, Sessions incomingSession){
        LOGGER.info("Bot flow is deactivated and sessionId is present");
        try {
            if (five9SessionRepository.findById(incomingSession.getSessionId()).isEmpty()){
                conversationService.sendConversationToFive9Agent(conversation, incomingSession);
            } else {
                processAgentInteraction(conversation, incomingSession);
            }
        } catch (Exception ex) {
            LOGGER.error("Exception while processing bot deactivated and sessionId present: {}", ex.getMessage());
        } finally {
            LOGGER.info("Finish process bot deactivated and sessionId present");
        }
    }

    private void processAgentInteraction(Conversation conversation, Sessions incomingSession){
        LOGGER.info("Starting process agent interaction");
        try {
            Five9Session five9Session = five9SessionRepository.findById(incomingSession.getSessionId()).orElseThrow(
                    () -> new GenericException(ErrorMessages.FIVE9_SESSION_NOT_PRESENT));
            mdcAwareExecutor.execute(() -> databaseOperationsService.processClientSummaryInteraction(conversation));
            SendConversationMessageRequest sendConversationMessageRequest = conversationMapper.conversationToFive9SendConversationMessage(conversation);
            five9ConversationService.sendFive9MessageConversation(five9Session, sendConversationMessageRequest);
        } catch (Exception ex) {
            LOGGER.error("Exception while processing agent interaction: {}", ex.getMessage());
        } finally {
            LOGGER.info("Finish process agent interaction");
        }
    }

    private boolean validateFirstInteraction(Conversation conversation, Sessions incomingSession){
        LOGGER.info("Validate first interaction");
        boolean firstInteraction = true;
        try {
            if(conversation.getMessageText().equals(Constants.WIDGET_FIRST_INTERACTION) && incomingSession.getSessionId() != null){
                firstInteraction = false;
            }
        } catch (Exception ex) {
            LOGGER.error("Exception while processing validate first interaction: {}", ex.getMessage());
            firstInteraction = false;
        }  finally {
            LOGGER.info("Finish process validate first interaction");
        }
        return firstInteraction;
    }
}
