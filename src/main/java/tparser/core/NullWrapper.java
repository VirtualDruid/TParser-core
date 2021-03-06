package tparser.core;

import org.jsoup.nodes.Element;
import org.jsoup.select.Evaluator;

import java.util.Objects;

/**
 * internally use for represent the not-found element to preserve the hierarchy of parsing process
 */
final class NullWrapper extends Element {
    private static final String TAG = "null";

    private NullWrapper(Element parent) {
        super(TAG);
        this.setParentNode(parent);
    }

    @Override
    public boolean is(String cssQuery) {
        return false;
    }

    @Override
    public boolean is(Evaluator evaluator) {
        return false;
    }

    static Element wrapNullElement(Element selection, Element parent) {
        Objects.requireNonNull(parent);
        return selection == null ?
                new NullWrapper(parent) :
                selection;
    }

    static boolean isNullRepresent(Element test) {
        return test instanceof NullWrapper || test == null;
    }

    static Element nullRepresent(Element parent) {
        return new NullWrapper(parent);
    }

//    static Element unwarp(Element element) {
//        return isNullRepresent(element) ? null : element;
//    }
}
