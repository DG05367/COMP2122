package exception;

public class WrongNumberOfArguments extends Exception {
    public WrongNumberOfArguments (int index) {
        super(String.valueOf(index));
    }
}
