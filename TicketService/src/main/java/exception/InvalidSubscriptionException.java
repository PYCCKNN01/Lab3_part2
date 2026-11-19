package exception;

import lombok.experimental.StandardException;

@StandardException
public class InvalidSubscriptionException extends RuntimeException {
}


/*
package exception;

public class InvalidSubscriptionException extends RuntimeException { //Непроверяемое
    public InvalidSubscriptionException(String message) {
        super(message);
    }
}

 */

//Базовая стоимость билета меньше или равна нулю, или имя владельца пустое