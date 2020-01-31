package tparser.core;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeVisitor;

@SuppressWarnings("unused")
/**
 * the facade parser
 */
public class Template {
    private      StepNode      root;
    private      TreeParseFlow parser;
    public final long          buildTimeConsumed;

    public Template(Element template) {
        this(new DefaultBuilder(template), Converters.defaultFactory());
    }

    public Template(StepTreeBuilder builder) {
        this(builder, Converters.defaultFactory());
    }

    Template(StepTreeBuilder builder, TextConverterFactory factory) {
        long start = System.nanoTime();
        root = builder.build(factory);
        parser = new TreeParseFlow(root);
        buildTimeConsumed = System.nanoTime() - start;
    }


    /**
     * debug info usage
     * format in a readable text representing the parser step tree
     *
     * @see ElementVisitor
     * @see StructPlaceHolderVisitor
     * @see ObjectVisitor
     * @see ArrayVisitor
     */
    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        final char    indent        = '\t';
        final char    linebreak     = '\n';
        root.traverse(new NodeVisitor() {
            @Override
            public void head(Node node, int depth) {
                StepNode stepNode = (StepNode) node;
                for (int i = 0; i <= depth; i++) {
                    //indent
                    stringBuilder.append(indent);
                }
                stringBuilder.append(stepNode.toString());
                stringBuilder.append(linebreak);
            }

            @Override
            public void tail(Node node, int depth) {

            }
        });
        return stringBuilder.toString();
    }

    public <JO, JA> ParseResult<JO, JA> parse(Element rootInput, JsonDelegate<JO, JA> delegate) throws HtmlParseException {
        return parser.parse(rootInput, delegate);
    }
}
