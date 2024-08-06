package com.inference.whatsappintegration.api.controller;

import com.inference.whatsappintegration.application.dto.webwidget.webwidgetinrequest.WebWidgetIncomingMessageRequestDTO;
import com.inference.whatsappintegration.domain.service.WebWidgetService;
import com.inference.whatsappintegration.infrastructure.config.mdc.MdcAwareExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/widget")
public class WebWidgetController {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebWidgetController.class);

    private MdcAwareExecutor mdcAwareExecutor;

    private WebWidgetService webWidgetService;

    public WebWidgetController(MdcAwareExecutor mdcAwareExecutor, WebWidgetService webWidgetService){
        this.mdcAwareExecutor = mdcAwareExecutor;
        this.webWidgetService = webWidgetService;
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> getCallback(){
        LOGGER.info("Get callback call");
        return ResponseEntity.ok().build();
    }

    @PostMapping("/message")
    public ResponseEntity<Void> processMessage(@RequestBody WebWidgetIncomingMessageRequestDTO webWidgetIncomingMessageRequestDTO){
        LOGGER.info("Receive WebWidgetMessage");
        mdcAwareExecutor.execute(() -> webWidgetService.processWebWidgetInteraction(webWidgetIncomingMessageRequestDTO));
        return ResponseEntity.ok().build();
    }

}
