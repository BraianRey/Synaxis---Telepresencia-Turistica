package com.synexis.management_service.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IceServersResponse {

    private List<IceServerInfo> iceServers;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IceServerInfo {
        private List<String> urls;
        private String username;
        private String credential;
    }
}
