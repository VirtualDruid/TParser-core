package tparser.core;

import org.jsoup.select.Evaluator;

public class HtmlParseException extends Exception {

    public HtmlParseException(String msg) {
        super(msg);
    }

    static abstract class Existence extends HtmlParseException {
        Existence(String msg) {
            super(msg);
        }
    }

    public static class RequiredNotFound extends Existence {
        RequiredNotFound(Evaluator evaluator, String customMsg) {
            super(String.format("required %s not found: %s", evaluator.toString(), customMsg));
        }
    }

    public static class FailIfFound extends Existence {
        FailIfFound(Evaluator evaluator, String customMsg) {
            super(String.format("fail if fount element %s is found: %s", evaluator.toString(), customMsg));
        }
    }

    public static class RegexNotMatch extends HtmlParseException {
        RegexNotMatch(String regex) {
            super(String.format("%s not match", regex));
        }
    }


}
