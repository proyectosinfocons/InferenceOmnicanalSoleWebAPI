package com.inference.whatsappintegration.domain.service;

import com.inference.whatsappintegration.application.dto.webwidget.webwidgetinrequest.WebWidgetIncomingMessageRequestDTO;

public interface WebWidgetService {

    void processWebWidgetInteraction(WebWidgetIncomingMessageRequestDTO webWidgetIncomingMessageRequestDTO);

}
