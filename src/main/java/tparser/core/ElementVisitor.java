package tparser.core;

import com.google.code.regexp.Matcher;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.select.Evaluator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

// TODO: 2020/2/24 support xml event based construction
public class ElementVisitor extends StepNode {

    private static final String ATTR_CLASS = "class";

    private StructPlaceHolderVisitor       parentStructure;
    private Evaluator                      selfEval;
    //    private DOMSearchMethod.First          searchMethod;
    private DOMSearchMethod.FirstSelector  firstSelector;
    private DOMSearchMethod.ExistenceCheck checker;
    private List<ExtractionProcessor>      processors = new ArrayList<>();
    private boolean                        isDirectChildOfStructure;
    private boolean                        hasExtractions;

    private PendingState state;


    /**
     * pre construction state
     * call to onBuilderExiting to finish initialization
     */
    private class PendingState {
        String                                                  tagName;
        String                                                  ownText;
        boolean                                                 isDirectChildOfStructure;
        StructPlaceHolderVisitor                                parentPlaceholder;
        AttributeContainer<? extends Map.Entry<String, String>> attrs;
        TextConverterFactory                                    factory;

        void ensureNotNull() {
            ownText = ownText == null ? null : "";
            attrs = attrs == null ? null : CollectionHelper.emptyContainer();
        }
    }

    /**
     * create a pre initialization instance for event based xml parser support
     * call to onBuilderExiting to finish init
     */
    @SuppressWarnings("unused")
    public ElementVisitor(
            String tagName,
            boolean isDirectChildOfStructure,
            StructPlaceHolderVisitor parentPlaceholder,
            TextConverterFactory factory
    ) {
        super(tagName);
        this.state = new PendingState();
        state.tagName = tagName;
        state.isDirectChildOfStructure = isDirectChildOfStructure;
        state.parentPlaceholder = parentPlaceholder;
        state.factory = factory;
    }

    @SuppressWarnings("unused")
    public void setOwnText(String ownText) {
        state.ownText = ownText;
    }

    @SuppressWarnings("unused")
    public void setAttrs(AttributeContainer<? extends Map.Entry<String, String>> container) {
        state.attrs = container;
    }


    @Override
    public void onBuilderVisiting() {
    }

    @Override
    public void onBuilderExiting() {
        if (state != null) {
            state.ensureNotNull();
            initialize(
                    state.tagName,
                    state.ownText,
                    state.isDirectChildOfStructure,
                    state.parentPlaceholder,
                    state.attrs,
                    state.factory
            );
            state = null;
        }
        processors = Collections.unmodifiableList(processors);
    }

    /**
     * a purified, source independent constructor
     * create and init a ready instance
     * <p>
     * could be used for alternative template construction
     * <p>
     * e.g. build from org.xml.DOM
     *
     * @param tagName                  this element's tag
     * @param ownText                  this element's text
     * @param isDirectChildOfStructure if this is directly under a structure placeholder
     * @param parentPlaceholder        the structure this step belongs to
     * @param attrs                    equivalent to Element#attributes
     * @param factory                  factory to create converters
     */
    @SuppressWarnings("unused")
    public ElementVisitor(
            String tagName,
            String ownText,
            boolean isDirectChildOfStructure,
            StructPlaceHolderVisitor parentPlaceholder,
            AttributeContainer<? extends Map.Entry<String, String>> attrs,
            TextConverterFactory factory) {

        super(tagName);
        initialize(tagName, ownText, isDirectChildOfStructure, parentPlaceholder, attrs, factory);
    }

    private void initialize(
            String tagName,
            String ownText,
            boolean isDirectChildOfStructure,
            StructPlaceHolderVisitor parentPlaceholder,
            AttributeContainer<? extends Map.Entry<String, String>> attrs,
            TextConverterFactory factory) {

        this.isDirectChildOfStructure = isDirectChildOfStructure;
        this.parentStructure = parentPlaceholder;

        Matcher matchText = IdentifierHelper.matchTextIdentifier(ownText);
        if (matchText.matches()) {
            checkLevel(ownText);
            processors.add(IdentifierHelper
                    .createElementTextProcessor(matchText, parentStructure, factory));
        }
        ArrayList<Evaluator> combines = new ArrayList<>();
        combines.add(new Evaluator.Tag(tagName));
        for (Map.Entry<String, String> attr : attrs) {

            String key   = attr.getKey();
            String value = attr.getValue();

            if (SearchMethodHelper.isExtensionAttr(key)) {
                //ignore x-attr
                continue;
            }
            Matcher matchValue = IdentifierHelper.matchAttrIdentifier(value);
            Matcher matchKey   = IdentifierHelper.matchAttrIdentifier(key);

            boolean isValueIdentifier = matchValue.matches();
            boolean isKeyIdentifier   = matchKey.matches();

            if (isValueIdentifier && isKeyIdentifier) {
                throw new TemplateSyntaxError(
                        String.format("attribute identifiers cannot be in both key and value: %s-%s", key, value)
                );
            } else if (isValueIdentifier) {
                checkLevel(value);
                boolean isAttrValueSelection = IdentifierHelper.hasSelectionAnnotation(matchValue);
                if (isAttrValueSelection) {
                    //is one of selection info
                    combines.add(new Evaluator.Attribute(key));
                }
                processors.add(IdentifierHelper.createAttrProcessor(matchValue, false, attr, parentStructure, factory));
            } else if (isKeyIdentifier) {
                checkLevel(key);
                boolean isAttrKeySelection = IdentifierHelper.hasSelectionAnnotation(matchKey);
                if (isAttrKeySelection) {
                    combines.add(new ExtendedEvaluator.HasAttrWithValue(value));
                }
                processors.add(IdentifierHelper.createAttrProcessor(matchValue, true, attr, parentStructure, factory));
            } else {
                if (!value.isEmpty()) {
                    if (key.equals(ATTR_CLASS)) {
                        //class can be multiple inheritance
                        for (String className : attrs.getClasses()) {
                            combines.add(new Evaluator.Class(className));
                        }
                    } else {
                        combines.add(new Evaluator.AttributeWithValue(key, value));
                    }
                } else {
                    //empty attr values
                    //should only eval with attr key
                    combines.add(new Evaluator.Attribute(key));
                }
            }
        }
        this.hasExtractions = (processors.size() != 0);
        this.checker = SearchMethodHelper.createChecker(attrs);
//        this.searchMethod = SearchMethodHelper.searchFirstMethod(attrs);

        DOMSearchMethod.Scope scope = SearchMethodHelper.scope(attrs);
        this.firstSelector = SearchMethodHelper.singleSelector(scope);
        Evaluator defaultEval = new CombiningEvaluator.And(combines);

        selfEval = SearchMethodHelper.composeEvaluator(attrs, defaultEval);
        if (isDirectChildOfStructure) {
            parentPlaceholder.addClassification(selfEval, scope);
        }

    }

    ElementVisitor(Element selfTemplate, StructPlaceHolderVisitor parentPlaceholder, TextConverterFactory factory) {
        super(selfTemplate.tagName());
        initialize(
                selfTemplate.tagName(),
                selfTemplate.ownText(),
                StructPlaceHolderVisitor.isDirectChildOfStructure(selfTemplate),
                parentPlaceholder,
                CollectionHelper.asAttributeContainer(selfTemplate),
                factory
        );

    }

    private void checkLevel(String identifier) {
        if (parentStructure == null) {
            throw new TemplateSyntaxError(
                    String.format(
                            "identifier declaration: %s outside %s/%s placeholder",
                            identifier,
                            ObjectVisitor.TAG,
                            ArrayVisitor.TAG)
            );
        }
    }

//    public ElementVisitor(Element selfTemplate, StructPlaceHolderVisitor parentPlaceholder, TextConverterFactory factory) {
//        super(selfTemplate.tagName());
//        this.parentStructure = parentPlaceholder;
//        Element parent    = selfTemplate.parent();
//        String  parentTag = parent == null ? null : parent.tagName();
//        isDirectChildOfStructure = StructPlaceHolderVisitor.isStructure(parentTag);
//        Matcher matchText = IdentifierHelper.matchTextIdentifier(selfTemplate.ownText());
//        if (matchText.matches()) {
//            checkLevel();
//            processors.add(IdentifierHelper
//                    .createElementTextProcessor(matchText, parentStructure, factory));
//        }
//        ArrayList<Evaluator> combines = new ArrayList<>();
//        combines.add(new Evaluator.Tag(selfTemplate.tagName()));
//        for (Attribute attr : selfTemplate.attributes()) {
//            String key   = attr.getKey();
//            String value = attr.getValue();
//            if (SearchMethodHelper.isExtensionAttr(key)) {
//                //ignore x-attr
//                continue;
//            }
//            Matcher matchValue        = IdentifierHelper.matchAttrIdentifier(value);
//            Matcher matchKey          = IdentifierHelper.matchAttrIdentifier(key);
//            boolean isValueIdentifier = matchValue.matches();
//            boolean isKeyIdentifier   = matchKey.matches();
//            if (isValueIdentifier && isKeyIdentifier) {
//                throw new TemplateSyntaxError(
//                        "attribute identifiers cannot be in both key and value",
//                        this
//                );
//            } else if (isValueIdentifier) {
//                checkLevel();
//                boolean isAttrValueSelection = IdentifierHelper.hasSelectionAnnotation(matchValue);
//                if (isAttrValueSelection) {
//                    //is one of selection info
//                    combines.add(new Evaluator.Attribute(key));
//                }
//                processors.add(IdentifierHelper.createAttrProcessor(matchValue, false, attr, parentStructure, factory));
//            } else if (isKeyIdentifier) {
//                checkLevel();
//                boolean isAttrKeySelection = IdentifierHelper.hasSelectionAnnotation(matchKey);
//                if (isAttrKeySelection) {
//                    combines.add(new ExtendedEvaluator.HasAttrWithValue(value));
//                }
//                processors.add(IdentifierHelper.createAttrProcessor(matchValue, true, attr, parentStructure, factory));
//            } else {
//                if (!value.isEmpty()) {
//                    if (key.equals("class")) {
//                        //class can be multiple inheritance
//                        for (String className : selfTemplate.classNames()) {
//                            combines.add(new Evaluator.Class(className));
//                        }
//                    } else {
//                        combines.add(new Evaluator.AttributeWithValue(key, value));
//                    }
//                } else {
//                    //empty attr values
//                    //should only eval with attr key
//                    combines.add(new Evaluator.Attribute(key));
//                }
//            }
//        }//attr loop
//        hasExtractions = (processors.size() != 0);
//        this.checker = SearchMethodHelper.createChecker(selfTemplate);
//        searchMethod = SearchMethodHelper.searchFirstMethod(selfTemplate);
//        DOMSearchMethod.Scope scope       = SearchMethodHelper.scope(selfTemplate);
//        Evaluator             defaultEval = new CombiningEvaluator.And(combines);
//        selfEval = SearchMethodHelper.composeEvaluator(selfTemplate, defaultEval);
//        if (isDirectChildOfStructure) {
//            parentPlaceholder.addClassification(selfEval, scope);
//        }
//
//    }

//    public ElementVisitor(Element selfTemplate, StructPlaceHolderVisitor parentPlaceholder) {
//        this(selfTemplate, parentPlaceholder, Converters.defaultFactory());
//    }

    private <JO, JA> void extract(Element element, ParseResult<JO, JA> state, int index) throws HtmlParseException {
        if (hasExtractions) {
            for (ExtractionProcessor processor : processors) {
                processor.process(state, element, index);
            }
        }
    }


    @Override
    public <JO, JA> void onVisit(ParseResult<JO, JA> state) throws HtmlParseException {
        Elements selection = new Elements(32);
        if (isDirectChildOfStructure) {
            ArrayList<Element> classifiedElements = state.elementGroupsStack.peek().getClassifiedElements(selfEval);
            for (int index = 0, size = classifiedElements.size(); index < size; index++) {
                Element element = classifiedElements.get(index);
                checker.check(element, selfEval);
                extract(element, state, index);
                selection.add(element);
            }
        } else {
            Elements peek = state.selectionStack.peek();
            for (int index = 0, peekSize = peek.size(); index < peekSize; index++) {
                Element parent = peek.get(index);
                Element target = NullWrapper.isNullRepresent(parent) ?
                        null : //can't be selected
                        firstSelector.select(parent, selfEval);
//                        SearchMethodHelper.searchFirst(parent, selfEval, searchMethod);
                checker.check(target, selfEval);
                target = NullWrapper.wrapNullElement(target, parent);
                extract(target, state, index);
                selection.add(target);
            }
        }
        state.selectionStack.push(selection);
    }

    @Override
    public <JO, JA> void onExit(ParseResult<JO, JA> state) {
        state.selectionStack.pop();
    }


    /**
     * format this step in a readable text
     * should only be used for debug purpose
     *
     * @return String in format: "SELECT: <css selectors> EXTRACT: <extractors>"
     * <p>
     * selectors represent how to search this element in DOM
     * extractor represent the element's properties to extract
     */
    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("EXTRACT").append(": ");
        if (hasExtractions) {
            for (ExtractionProcessor processor : processors) {
                stringBuilder.append(" ");
                stringBuilder.append(processor.toString());
            }
        } else {
            stringBuilder.append("no extractor");
        }
        return String.format("SELECT: %s %s", selfEval.toString(), stringBuilder.toString());
    }

}
