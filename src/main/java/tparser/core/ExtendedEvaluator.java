package tparser.core;

import org.jsoup.nodes.Element;
import org.jsoup.select.Evaluator;

/**
 * extended Evaluator for internal usage
 * it cannot be created by any css selector syntax
 */
abstract class ExtendedEvaluator extends Evaluator {

//    static final Evaluator neverMatches = new NeverMatches();
//
//    /**
//     * a evaluator that never matches anything
//     */
//    private static final class NeverMatches extends Evaluator {
//        @Override
//        public boolean matches(Element root, Element element) {
//            return false;
//        }
//    }

    /**
     * evaluates if the element has a attr value
     */
    static final class HasAttrWithValue extends ExtendedEvaluator {
        private String value;

        HasAttrWithValue(String value) {
            this.value = value;
        }

        @Override
        public boolean matches(Element root, Element element) {
            for (org.jsoup.nodes.Attribute attribute : element.attributes()) {
                if (attribute.getValue().equals(value)) {
                    return true;
                }
            }
            return false;
        }
    }
}
