package tools.vitruv.cli.exceptions;

/** Exception thrown when a required model is missing. */
public class MissingModelException extends Exception {

  /**
   * Exception thrown when a required model is missing.
   *
   * @param message
   */
  public MissingModelException(String message) {
    super(message);
  }

  /**
   * Exception thrown when a required model is missing.
   *
   * @param message
   * @param cause
   */
  public MissingModelException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Exception thrown when a required model is missing.
   *
   * @param cause
   */
  public MissingModelException(Throwable cause) {
    super(cause);
  }
}
