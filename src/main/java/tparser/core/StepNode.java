package tparser.core;

import org.jsoup.nodes.Element;

/**
 * abstract functional node
 *
 */
public abstract class StepNode extends Element {

    public StepNode(String tagName) {
        super(tagName);
    }

    /**
     * when parsing process first visit the node
     */
    abstract <JO, JA> void onVisit(ParseResult<JO, JA> state) throws HtmlParseException;

    /**
     * when parsing process exit the node(subtree under this node is all visited)
     */
    abstract <JO, JA> void onExit(ParseResult<JO, JA> state);

    /**
     * only called when DFT visiting the node (subtree not yet visited)
     * this is currently preserved and has no usage
     */
    public abstract void onBuilderVisiting();

    /**
     * only called when DFT exiting the node (subtree are all visited)
     */
    public abstract void onBuilderExiting();

    /**
     * type sensitive tree building
     *
     * @param node step node instance
     */
    public void addChild(StepNode node) {
        appendChild(node);
    }

    @Override
    public String toString() {
        throw new IllegalStateException("not override");
    }
}
