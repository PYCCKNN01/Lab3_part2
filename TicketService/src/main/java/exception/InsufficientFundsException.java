package exception;

import lombok.experimental.StandardException;

@StandardException
public class InsufficientFundsException extends Exception {
}




/*
package exception;

public class InsufficientFundsException extends Exception {
    public InsufficientFundsException(String message) {
        super(message);
    }
}

//Баланс пользователя меньше, чем стоимость билета

 */