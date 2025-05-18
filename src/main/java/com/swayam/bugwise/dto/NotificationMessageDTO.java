package com.swayam.bugwise.dto;

import com.swayam.bugwise.enums.NotificationChannel;
import com.swayam.bugwise.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessageDTO {
    private NotificationType type;
    private String title;
    private String content;
    private Map<String, Object> metadata;
    private List<String> recipients;
    private InAppDetails inAppDetails;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InAppDetails {
        private String deepLink;
        private String iconUrl;
    }

}