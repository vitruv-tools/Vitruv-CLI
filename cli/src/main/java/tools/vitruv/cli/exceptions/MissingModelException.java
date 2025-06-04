package tools.vitruv.cli.exceptions;

public class MissingModelException extends Exception {

  public MissingModelException(String message) {
    super(message);
  }

  public MissingModelException(String message, Throwable cause) {
    super(message, cause);
  }

  public MissingModelException(Throwable cause) {
    super(cause);
  }
}
