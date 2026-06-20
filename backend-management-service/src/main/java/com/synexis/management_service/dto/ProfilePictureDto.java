package com.synexis.management_service.dto;

/** Simple DTO to carry profile picture bytes and content type. */
public record ProfilePictureDto(byte[] data, String contentType) {}
