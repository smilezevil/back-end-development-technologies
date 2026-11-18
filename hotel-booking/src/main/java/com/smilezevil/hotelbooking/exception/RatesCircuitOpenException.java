package com.smilezevil.hotelbooking.exception;

public class RatesCircuitOpenException extends RuntimeException {

    public RatesCircuitOpenException(Throwable cause) {
        super("Сервіс курсів валют тимчасово відключено: після серії збоїв спрацював circuit breaker, "
                + "і запити до нього зараз не надсилаються. Спробуйте трохи пізніше", cause);
    }
}