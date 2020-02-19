package tparser.core;

import org.jsoup.nodes.Element;
import org.jsoup.select.Evaluator;

import java.util.LinkedHashSet;

public abstract class StructPlaceHolderVisitor extends StepNode {
    private static final String                ATTR_NAME  = "name";
    final                String                name;
    protected            Classifier            classifier;
    protected            LinkedHashSet<String> properties = new LinkedHashSet<>();


    public StructPlaceHolderVisitor(String tagName, AttributeContainer container) {
        super(tagName);
        this.name = container.hasAttr(ATTR_NAME) ?
                container.getAttr(ATTR_NAME) :
                null;
    }

    abstract void onAddProperty(String property, boolean dupe);

    abstract void onAddEvaluator(Evaluator evaluator, boolean dupe);

    abstract <JO, JA> void onExtract(
            ParseResult<JO, JA> state,
            String property,
            Object result,
            int index);

    abstract <JO, JA> void resolveStack(ParseResult<JO, JA> state);

    void addClassification(Evaluator evaluator, DOMSearchMethod.Scope scope) {
        onAddEvaluator(evaluator, false);
        classifier.addClassification(evaluator, scope);
    }

    void addProperty(String property) {
        boolean dupe = !(properties.add(property));
        onAddProperty(property, dupe);
    }

    public static boolean isStructure(String tag) {
        return ObjectVisitor.TAG.equals(tag) || ArrayVisitor.TAG.equals(tag);
    }

    public static boolean isDirectChildOfStructure(Element element) {
        return isStructure(element.parent() == null ? null : element.parent().tagName());
    }

}
