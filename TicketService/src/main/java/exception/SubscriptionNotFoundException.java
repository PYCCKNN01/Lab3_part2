package exception;

import lombok.experimental.StandardException;

@StandardException
public class SubscriptionNotFoundException extends Exception {
}



/*
package exception;

public class SubscriptionNotFoundException extends Exception { //Проверяемое
    public SubscriptionNotFoundException(String message) {
        super(message);
    }
}

//При попытке найти или оплатить несуществующий билет в базе данных

 */