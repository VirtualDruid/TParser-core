package tparser.core;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.*;

import java.util.ArrayList;

public final class SelectHelper {
    private SelectHelper() {
    }

    /**
     * select first element in given elements matches the query in only fist level
     *
     * @param elements
     * @param cssQuery
     * @return null if not found
     */
    public static Element selectFirstIn(Elements elements, String cssQuery) {
        return selectFirstIn(elements, QueryParser.parse(cssQuery));
    }

    /**
     * select first element in given elements matches the evaluator in only fist level
     *
     * @param elements
     * @param eval
     * @return null if not found
     */
    public static Element selectFirstIn(Elements elements, Evaluator eval) {
        for (Element element : elements) {
            if (element.is(eval)) {
                return element;
            }
        }
        return null;
    }

    public static Elements selectIn(Elements elements, Evaluator evaluator) {
        ArrayList<Element> results = new ArrayList<>();
        for (Element e : elements) {
            if (e.is(evaluator)) {
                results.add(e);
            }
        }
        return new Elements(results);
    }

    public static Elements selectIn(Elements elements, String cssQuery) {
        return selectIn(elements, QueryParser.parse(cssQuery));
    }

    public static Elements selectInChildren(Element root, Evaluator eval) {
        return selectIn(root.children(), eval);
    }

    public static Elements selectInChildren(Element root, String cssQuery) {
        return selectIn(root.children(), cssQuery);
    }

    /**
     * select first direct child matches the query in only first level
     *
     * @param root
     * @param cssQuery
     * @return null if not found
     */
    public static Element selectFirstInChildren(Element root, String cssQuery) {
        return selectFirstIn(root.children(), cssQuery);
    }

    /**
     * select first direct child matches the evaluator in only first level
     *
     * @param root
     * @param eval
     * @return null if not found
     */
    public static Element selectFirstInChildren(Element root, Evaluator eval) {
        return selectFirstIn(root.children(), eval);
    }

    public static Elements select(Element root, Evaluator eval) {
        return Selector.select(eval, root);
    }

    public static Element selectFirst(Element root, Evaluator evaluator) {
        return Collector.findFirst(evaluator, root);
    }

    public static Element selectFirstExcludeRoot(Element root, Evaluator evaluator) {
        ExcludeRoot filter = new ExcludeRoot(root, evaluator);
        root.filter(filter);
        return filter.found;
    }

    private static class ExcludeRoot implements NodeFilter {
        Element root;
        Element found;
        private Evaluator evaluator;

        public ExcludeRoot(Element root, Evaluator evaluator) {
            this.root = root;
            this.evaluator = evaluator;
        }

        @Override
        public FilterResult head(Node node, int depth) {
            if (node instanceof Element && node != root) {
                Element element = (Element) node;
                if (evaluator.matches(root, element)) {
                    found = element;
                    return FilterResult.STOP;
                }
            }
            return FilterResult.CONTINUE;
        }

        @Override
        public FilterResult tail(Node node, int depth) {
            return FilterResult.CONTINUE;
        }
    }

}
