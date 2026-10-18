package dev.omatheusmesmo.demo.common;

public record ErrorResponse(int status, String error, String message) {
}
