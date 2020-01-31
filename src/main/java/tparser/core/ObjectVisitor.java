package tparser.core;

import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Element;
import org.jsoup.select.Evaluator;

import java.util.ArrayList;
import java.util.List;

public class ObjectVisitor extends StructPlaceHolderVisitor {

    static final String TAG = "json-object";

    public ObjectVisitor(AttributeContainer container) {
        super(TAG, container);
        classifier = new Classifier.Object();
    }

    ObjectVisitor(Element template) {
        this(CollectionHelper.asAttributeContainer(template));
    }


    @Override
    public void onAddProperty(String property, boolean dupe) {
        if (dupe) {
            throw new TemplateSyntaxError(String.format("duplicate object property : %s", property));
        }
    }

    @Override
    public void onAddEvaluator(Evaluator evaluator, boolean dupe) {
        //preserved
    }

    @Override
    public <JO, JA> void onVisit(ParseResult<JO, JA> state) {
        JsonDelegate<JO, JA> delegate = state.delegate;
        if (state.resultObject == null && state.resultArray == null) {
            //root
            state.resultObject = delegate.createObjectNode();
        }
        ElementGroups elementGroups = classifier.classify(state.selectionStack.peek());
        state.elementGroupsStack.push(elementGroups);
        List<JO> parentStructure = state.pendingItemStack.peek();
        List<JO> pendingObjects  = new ArrayList<>();
        if (parentStructure != null) {
            for (int i = 0, size = parentStructure.size(); i < size; i++) {
                pendingObjects.add(delegate.createObjectNode());
            }
        } else {
            //empty stack
            //root is object
            pendingObjects.add(state.resultObject);
        }
        state.pendingItemStack.push(pendingObjects);
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
        List<JO> currentItems = state.pendingItemStack.pop();
        List<JO> parentItems  = state.pendingItemStack.peek();
        if (parentItems != null) {
            for (int i = 0, size = currentItems.size(); i < size; i++) {
                state.delegate.putObjectNode(parentItems.get(i), this.name, currentItems.get(i));
            }
        }
    }

    @Override
    public String toString() {
        return String.format("OBJECT NAME:%s GROUP:%s", name, StringUtil.join(classifier.classifications.iterator(), " / "));
    }
}
