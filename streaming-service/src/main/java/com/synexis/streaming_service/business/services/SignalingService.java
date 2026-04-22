package com.synexis.streaming_service.business.services;

import com.synexis.streaming_service.presentation.dto.request.SignalRequest;

public interface SignalingService {
    void relaySignal(SignalRequest request);
}
