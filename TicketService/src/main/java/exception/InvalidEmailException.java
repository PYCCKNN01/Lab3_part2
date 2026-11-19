package exception;

import lombok.experimental.StandardException;

@StandardException
public class InvalidEmailException extends RuntimeException {
}



/*
package exception;

public class InvalidEmailException extends RuntimeException {
    public InvalidEmailException(String message) {
        super(message);
    }
}

//Email не содержит @ или точки.

 */