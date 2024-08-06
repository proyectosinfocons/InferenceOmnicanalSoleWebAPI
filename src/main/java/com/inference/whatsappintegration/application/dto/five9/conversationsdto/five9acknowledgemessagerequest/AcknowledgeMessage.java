package com.inference.whatsappintegration.application.dto.five9.conversationsdto.five9acknowledgemessagerequest;

import com.inference.whatsappintegration.util.enums.EnumAcknowledgeType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AcknowledgeMessage {

        private EnumAcknowledgeType type;
        private String messageId;

}
