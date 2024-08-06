package com.inference.whatsappintegration.application.dto.facebook.facebookinrequest;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Message {
    private String mid;
    private String text;
    private List<Attachment> attachments;
}
