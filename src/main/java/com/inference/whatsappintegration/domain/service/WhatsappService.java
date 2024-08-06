package com.inference.whatsappintegration.domain.service;

import com.inference.whatsappintegration.application.dto.whatsapp.whatsappinrequest.WhatsappIncomingMessageRequestDTO;
import com.inference.whatsappintegration.application.dto.whatsapp.whatsappoldinrequest.WhatsappOldIncomingMessageRequestDTO;

public interface WhatsappService {

    void processReceiveInteraction(WhatsappIncomingMessageRequestDTO whatsappIncomingMessageRequestDTO);
    void processReceiveInteractionOld(WhatsappOldIncomingMessageRequestDTO whatsappOldIncomingMessageRequestDTO);

}
