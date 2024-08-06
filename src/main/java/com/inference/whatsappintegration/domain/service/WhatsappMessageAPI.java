package com.inference.whatsappintegration.domain.service;

import com.inference.whatsappintegration.application.dto.whatsapp.whatappoutrequest.WhatsappMessage;

public interface WhatsappMessageAPI {

    void sendMessageResponse(WhatsappMessage whatsappMessage);

}
