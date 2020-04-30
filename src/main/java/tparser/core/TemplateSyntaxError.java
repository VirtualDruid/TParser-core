package tparser.core;


import org.jsoup.select.Selector;

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

    static TemplateSyntaxError selectorSyntaxError(String query, Selector.SelectorParseException parseException) {
        return new TemplateSyntaxError(String.format("css selector syntax error: %s - %s", query, parseException.getMessage()));
    }
}
