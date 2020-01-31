package tparser.core;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

import java.util.ArrayDeque;

class DefaultBuilder implements StepTreeBuilder, NodeVisitor {
    private Element                              templateRoot;
    private TextConverterFactory                 factory;
    private StepNode                             root;
    private ArrayDeque<StepNode>                 parentStack = new ArrayDeque<>();
    private ArrayDeque<StructPlaceHolderVisitor> structStack = new ArrayDeque<>();

    public DefaultBuilder(Element templateRoot) {
        this.templateRoot = templateRoot;
    }

    @Override
    public StepNode build(TextConverterFactory converterFactory) {
        this.factory = converterFactory;
        NodeTraversor.traverse(this, templateRoot);
        return root;
    }

    @Override
    public void head(Node node, int depth) {
        if (node instanceof Element) {
            //ignore comment
            Element  templateElement = (Element) node;
            StepNode currentStep     = createStepNode(templateElement, structStack.peek(), factory);
            if (currentStep instanceof StructPlaceHolderVisitor) {
                structStack.push((StructPlaceHolderVisitor) currentStep);
            }
            if (parentStack.size() != 0) {
                parentStack.peek().addChild(currentStep);
            }
            currentStep.onBuilderVisiting();
            parentStack.push(currentStep);
        }
    }

    @Override
    public void tail(Node node, int depth) {
        if (node instanceof Element) {
            Element e = (Element) node;
            if (StructPlaceHolderVisitor.isStructure(e.tagName())) {
                //exiting object or array
                structStack.pop();
            }
            if (depth == 0) {
                root = parentStack.peek();
            }
            parentStack.pop().onBuilderExiting();
        }
    }

    private static StepNode createStepNode(Element template, StructPlaceHolderVisitor parent, TextConverterFactory factory) {
        if (ArrayVisitor.TAG.equals(template.tagName())) {
            return new ArrayVisitor(template);
        }
        if (ObjectVisitor.TAG.equals(template.tagName())) {
            return new ObjectVisitor(template);
        }
        return new ElementVisitor(template, parent, factory);
    }
}
