package tparser.core;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeVisitor;

import java.util.Objects;

/**
 * the facade class
 */
@SuppressWarnings("unused")
public class Template {
    private StepNode      root;
    private TreeParseFlow parser;

    /**
     * time of building the template object , in nanoseconds
     */
    public final long buildTimeConsumed;

    /**
     * default convenient constructor to simply create from a DOM
     * @param template
     */
    public Template(Element template) {
        this(new DefaultBuilder(template), Converters.defaultFactory());
    }

    /**
     * create a template with custom deserialization builder and default type conversion
     * use template builder if requiring both custom builder and custom typing
     * @param builder tree builder implementation
     * @see TemplateBuilder
     */
    public Template(StepTreeBuilder builder) {
        this(builder, Converters.defaultFactory());
    }

    /**
     * constructor used by template builder to ensure basic typing
     * @param builder tree builder implementation
     * @param factory produces text converter by given type name
     */
    Template(StepTreeBuilder builder, ConverterFactory factory) {
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

    /**
     * parse a DOM into json, with a given json type from delegate
     * @param rootInput root of DOM
     * @param delegate delegation to the json library
     * @param <JO> json object's type given by delegate
     * @param <JA> json array's type given by delegate
     * @return parse result
     * @throws HtmlParseException if the given DOM has a required but missing, or a found-and-fail element
     */
    public <JO, JA> ParseResult<JO, JA> parse(Element rootInput, JsonDelegate<JO, JA> delegate) throws HtmlParseException {
        Objects.requireNonNull(rootInput, "input element cannot be null");
        Objects.requireNonNull(delegate, "json delegate must be provided");
        return parser.parse(rootInput, delegate);
    }
}
