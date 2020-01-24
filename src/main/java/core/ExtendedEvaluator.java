package core;

import org.jsoup.nodes.Element;
import org.jsoup.select.Evaluator;

abstract class ExtendedEvaluator extends Evaluator {

    public static final class HasAttrWithValue extends ExtendedEvaluator {
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
