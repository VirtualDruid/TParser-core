package tparser.core;

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

    private AttributeContainer container;

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


//    public ArrayVisitor(AttributeContainer container, int nonStructureDirectChildren) {
//        super(TAG, container);
//        this.container = container;
////        int depthLimit = SearchMethodHelper.arrayDepthLimit(container);
//        classifier = (nonStructureDirectChildren == 1) ?
//                new Classifier.SingleTypeArray(container) :
//                new Classifier.MultiTypeArray(container);
//
//    }

    /**
     * source independent constructor
     * should call onBuilderExiting to init (actual) classifier
     *
     * @param container options attribute set
     */
    @SuppressWarnings("WeakerAccess")
    public ArrayVisitor(AttributeContainer container) {
        super(TAG, container);
        this.container = container;
        this.classifier = new ClassifierTemp();
    }

    ArrayVisitor(Element template) {
        this(CollectionHelper.asAttributeContainer(template));
        classifier = (countNonStructChildren(template) == 1) ?
                new Classifier.SingleTypeArray(container) :
                new Classifier.MultiTypeArray(container);
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
        JsonDelegate<JO, JA> delegate      = state.delegate;
        List<JO>             items         = state.pendingItemStack.pop();
        List<Integer>        arraySizeList = state.elementGroupsStack.pop().getSubArraySizes();

        List<JO> parentItems = state.pendingItemStack.peek();
        //spilt items by sizes and put into parent structure
        if (parentItems != null) {
            int processedItems = 0;
//            for (int i = 0, arraySizesLength = arraySizeList.size(); i < arraySizesLength; i++) {
//                int size = arraySizeList.get(i);
//
//                //sub array starting index
//                final int startIndex = processedItemSize;
//                //ending (exclusive)
//                final int end         = processedItemSize + size;
//                final JO  parentItem  = parentItems.get(i);
//                final JA  destination = delegate.createArrayNode();
//                delegate.putArrayNode(parentItem, this.name, destination);
//                for (int j = startIndex; j < end; j++) {
//                    delegate.add(destination, items.get(j));
//                }
//                processedItemSize += size;
//            }

            //now based on items instead of sizes
            //the sub arrays out of bound from the parent item's index will be ignore
            final int maxIndexOfSizeList = arraySizeList.size() - 1;
            for (int i = 0, size = parentItems.size(); i < size; i++) {
                int subArraySize = i <= maxIndexOfSizeList ?
                        arraySizeList.get(i) :
                        0;

                //sub array starting index
                final int startIndex = processedItems;
                //ending (exclusive)
                final int end = processedItems + subArraySize;

                final JO parentItem  = parentItems.get(i);
                final JA destination = delegate.createArrayNode();

                delegate.putArrayNode(parentItem, this.name, destination);
                for (int j = startIndex; j < end; j++) {
                    delegate.add(destination, items.get(j));
                }
                processedItems += subArraySize;

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
    public void onBuilderVisiting() {

    }

    @Override
    public void onBuilderExiting() {
        if (classifier instanceof ClassifierTemp) {
            classifier = ((ClassifierTemp) classifier).toArrayClassifier(container);
        }
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

//    @Override
//    public <JO, JA> void resolveStack(ParseResult<JO, JA> state) {
//        JsonDelegate<JO, JA> delegate      = state.delegate;
//        ElementGroups        elementGroups = state.elementGroupsStack.pop();
//        List<JO>             items         = state.pendingItemStack.pop();
//        List<Integer>        sizes         = elementGroups.getSizes();
//
//        List<JO> parentItems = state.pendingItemStack.peek();
//        //spilt items by sizes and put into parent structure
//        if (parentItems != null) {
//            int processedItemSize = 0;
//            for (int i = 0, arraySizesLength = sizes.size(); i < arraySizesLength; i++) {
//                int size = sizes.get(i);
//
//                //sub array starting index
//                final int startIndex = processedItemSize;
//                //ending (exclusive)
//                final int end         = processedItemSize + size;
//                final JO  parentItem  = parentItems.get(i);
//                final JA  destination = delegate.createArrayNode();
//                delegate.putArrayNode(parentItem, this.name, destination);
//                for (int j = startIndex; j < end; j++) {
//                    delegate.add(destination, items.get(j));
//                }
//                processedItemSize += size;
//            }
//
//        } else {
//            //empty stack, resolving root
//            if (state.resultArray != null) {
//                //root is array
//                for (JO jsonObject : items) {
//                    delegate.add(state.resultArray, jsonObject);
//                }
//            } else {
//                //root is object
//                JA jsonArray = delegate.createArrayNode();
//                for (JO jsonObject : items) {
//                    delegate.add(jsonArray, jsonObject);
//                }
//                delegate.putArrayNode(state.resultObject, this.name, jsonArray);
//            }
//
//        }
//
//    }

    @Override
    public String toString() {
        return String.format("ARRAY NAME:%s GROUP:%s", name, classifier.toString());
    }

    //array classifier's lazy-construction state
    private static final class ClassifierTemp extends Classifier {
        private List<DOMSearchMethod.Scope> scopes              = new ArrayList<>();
        private int                         classificationCount = 0;

        @Override
        final void addClassification(Evaluator evaluator, DOMSearchMethod.Scope scope) {
            classifications.add(evaluator);
            scopes.add(scope);
            classificationCount++;
        }

        @Override
        final protected Finder createFinder(ElementGroups groups, Elements parents) {
            //no usage
            return null;
        }

        @Override
        protected void onNullParent(ElementGroups groups, Element parent) {
            //no usage
        }

        final Classifier toArrayClassifier(AttributeContainer container) {
            Classifier classifier = (classificationCount == 1) ?
                    new Classifier.SingleTypeArray(container) :
                    new Classifier.MultiTypeArray(container);
            for (int i = 0, size = classifications.size(); i < size; i++) {
                classifier.addClassification(classifications.get(i), scopes.get(i));
            }
            return classifier;
        }
    }
}
