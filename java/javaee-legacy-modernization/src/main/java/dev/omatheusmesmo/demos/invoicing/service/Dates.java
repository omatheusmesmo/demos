package dev.omatheusmesmo.demos.invoicing.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class Dates {

    private static final String PATTERN = "yyyy-MM-dd";

    private Dates() {
    }

    public static Date parse(String value) {
        try {
            SimpleDateFormat format = new SimpleDateFormat(PATTERN);
            format.setLenient(false);
            return format.parse(value);
        } catch (ParseException | NullPointerException e) {
            throw new IllegalArgumentException("issueDate must be yyyy-MM-dd, got " + value);
        }
    }

    public static String format(Date date) {
        return new SimpleDateFormat(PATTERN).format(date);
    }
}
