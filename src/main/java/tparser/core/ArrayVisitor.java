package tparser.core;

import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.jsoup.select.Evaluator;
import org.jsoup.select.QueryParser;

import java.util.ArrayList;
import java.util.List;

/**
 * the step which will 'select all' matching elements from roots and manage extraction results as json array
 */
public class ArrayVisitor extends StructPlaceHolderVisitor {

    private static final Evaluator NON_STRUCTURE_CHILDREN = QueryParser.parse(":not(json-array):not(json-object)");
    static final         String    TAG                    = "json-array";

//    private static Elements selectNonStructChildren(Element root) {
//        return SelectHelper.selectInChildren(root, NON_STRUCTURE_CHILDREN);
//    }

    private static int countNonStructChildren(Element root) {
        int count = 0;
        for (Node node : root.childNodes()) {
            if (node instanceof Element) {
                Element element = (Element) node;
                if (NON_STRUCTURE_CHILDREN.matches(root, element)) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * source independent constructor
     *
     * @param container                  attribute set
     * @param nonStructureDirectChildren number of direct children that are not a structure node for choosing classify strategy
     */
    public ArrayVisitor(AttributeContainer container, int nonStructureDirectChildren) {
        super(TAG, container);
//        int depthLimit = SearchMethodHelper.arrayDepthLimit(container);
        classifier = (nonStructureDirectChildren == 1) ?
                new Classifier.SingleTypeArray(container) :
                new Classifier.MultiTypeArray(container);

    }

    ArrayVisitor(Element template) {
        this(CollectionHelper.asAttributeContainer(template), countNonStructChildren(template));
    }

    @Override
    public void onAddProperty(String property, boolean dupe) {
        if (dupe) {
            //duplicate identifier
            throw new TemplateSyntaxError(String.format("duplicate property in array : %s", property));
        }
    }

    @Override
    public void onAddEvaluator(Evaluator evaluator, boolean dupe) {
        //preserved
        //maybe check possible duplicate evaluator
    }


    @Override
    public <JO, JA> void onVisit(ParseResult<JO, JA> state) {
        //init root
        if (state.shouldInitRoot()) {
            state.resultArray = state.delegate.createArrayNode();
        }
        Elements      parents       = state.selectionStack.peek();
        ElementGroups elementGroups = classifier.classify(parents);
        ArrayList<JO> jsonList      = new ArrayList<>();
        for (int i = 0, groupsFound = elementGroups.getGroupsFound(); i < groupsFound; i++) {
            jsonList.add(state.delegate.createObjectNode());
        }
        state.pendingItemStack.push(jsonList);
        state.elementGroupsStack.push(elementGroups);
    }

    @Override
    public <JO, JA> void onExit(ParseResult<JO, JA> state) {
        resolveStack(state);
    }

    @Override
    public void onBuilderVisiting() {

    }

    @Override
    public void onBuilderExiting() {
        classifier.finish();
    }

    @Override
    public <JO, JA> void onExtract(
            ParseResult<JO, JA> state,
            String property,
            Object result,
            int index) {
        List<JO> items = state.pendingItemStack.peek();
        if (result == null) {
            state.delegate.putNull(items.get(index), property);
        } else {
            state.delegate.putValue(items.get(index), property, result);
        }
    }

    @Override
    public <JO, JA> void resolveStack(ParseResult<JO, JA> state) {
        JsonDelegate<JO, JA> delegate      = state.delegate;
        ElementGroups        elementGroups = state.elementGroupsStack.pop();
        List<JO>             items         = state.pendingItemStack.pop();
        List<Integer>        sizes         = elementGroups.getSizes();

        List<JO> parentItems = state.pendingItemStack.peek();
        //spilt items by sizes and put into parent structure
        if (parentItems != null) {
            int processedItemSize = 0;
            for (int i = 0, arraySizesLength = sizes.size(); i < arraySizesLength; i++) {
                int size = sizes.get(i);

                //sub array starting index
                final int startIndex = processedItemSize;
                //ending (exclusive)
                final int end         = processedItemSize + size;
                final JO  parentItem  = parentItems.get(i);
                final JA  destination = delegate.createArrayNode();
                delegate.putArrayNode(parentItem, this.name, destination);
                for (int j = startIndex; j < end; j++) {
                    delegate.add(destination, items.get(j));
                }
                processedItemSize += size;
            }

        } else {
            //empty stack, resolving root
            if (state.resultArray != null) {
                //root is array
                for (JO jsonObject : items) {
                    delegate.add(state.resultArray, jsonObject);
                }
            } else {
                //root is object
                JA jsonArray = delegate.createArrayNode();
                for (JO jsonObject : items) {
                    delegate.add(jsonArray, jsonObject);
                }
                delegate.putArrayNode(state.resultObject, this.name, jsonArray);
            }

        }

    }

    @Override
    public String toString() {
        return String.format("ARRAY NAME:%s GROUP:%s", name, StringUtil.join(classifier.classifications.iterator(), " / "));
    }
}
