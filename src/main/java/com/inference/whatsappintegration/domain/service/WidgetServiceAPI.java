package com.inference.whatsappintegration.domain.service;

import com.inference.whatsappintegration.application.dto.webwidget.webwidgetoutresponse.WebWidgetIncomingMessageResponseDTO;

public interface WidgetServiceAPI {
    void sendMessageToWidgetAPI(WebWidgetIncomingMessageResponseDTO webWidgetIncomingMessageResponseDTO);
}
