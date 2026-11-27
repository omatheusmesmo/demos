package dev.omatheusmesmo.demos.invoicing.api;

public class ErrorDto {

    private String error;

    public ErrorDto(String error) {
        this.error = error;
    }

    public String getError() {
        return error;
    }
}
