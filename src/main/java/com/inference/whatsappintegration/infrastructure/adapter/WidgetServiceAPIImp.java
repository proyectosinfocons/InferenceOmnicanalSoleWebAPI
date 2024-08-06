package com.inference.whatsappintegration.infrastructure.adapter;

import com.inference.whatsappintegration.application.dto.webwidget.webwidgetoutresponse.WebWidgetIncomingMessageResponseDTO;
import com.inference.whatsappintegration.domain.service.WidgetServiceAPI;
import com.inference.whatsappintegration.util.Constants;
import com.inference.whatsappintegration.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class WidgetServiceAPIImp implements WidgetServiceAPI {

    private static final Logger LOGGER = LoggerFactory.getLogger(WidgetServiceAPIImp.class);

    @Value("${property.widget.url}")
    private String webWidgetServerUrl;

    @Override
    public void sendMessageToWidgetAPI(WebWidgetIncomingMessageResponseDTO webWidgetIncomingMessageResponseDTO) {
        LOGGER.info("Starting processing send message to web widget");
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();

            headers.setContentType(MediaType.APPLICATION_JSON);

            Utils.logAsJson(LOGGER, webWidgetIncomingMessageResponseDTO, "WebWidgetIncomingMessageResponseDTO");


            HttpEntity<WebWidgetIncomingMessageResponseDTO> entity = new HttpEntity<>(webWidgetIncomingMessageResponseDTO, headers);
            ResponseEntity<Void> widgetResponseEntity = restTemplate.postForEntity(webWidgetServerUrl, entity, Void.class);

            if (Constants.SUCCESS_STATUS_CODES.contains(widgetResponseEntity.getStatusCode().value())){
                LOGGER.info("Success to send message to widget");
            } else {
                LOGGER.error("Error while sending message to widget");
            }
        } catch(Exception ex){
            LOGGER.warn("Exception while sending message to widget: {}", ex.getMessage());
        } finally{
            LOGGER.info("Finish sending message to widget");
        }
    }
}
