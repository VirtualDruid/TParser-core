package tparser.core;


@SuppressWarnings("unused")
public class TemplateSyntaxError extends Error {

    TemplateSyntaxError(String message) {
        super(message);
    }

    TemplateSyntaxError(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getMessage() {
        return super.getMessage();
    }
}
