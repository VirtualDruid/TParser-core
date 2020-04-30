package tparser.core;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.jsoup.select.Evaluator;
import org.jsoup.select.NodeFilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * functional class
 * find and group up elements
 *
 * @see ElementGroups
 */
abstract class Classifier {
    private static final int CHILDREN_DEPTH = 1;
    private static final int SKIP_SEARCH    = 0;
    List<Evaluator> classifications = new ArrayList<>();

    /*---instance method---*/
    abstract void addClassification(Evaluator evaluator, DOMSearchMethod.Scope scope);

    final ElementGroups classify(Elements parents) {
        ElementGroups elementGroups = new ElementGroups(classifications);

        Finder finder = createFinder(elementGroups, parents);
        for (Element parent : parents) {
//            elementGroups.onStartOfArray();
            finder.before();
            if (!NullWrapper.isNullRepresent(parent)) {
                onNonNullParent(elementGroups, parent, finder);
            } else {
                onNullParent(elementGroups, parent);
            }
//            elementGroups.onEndOfArray();
            finder.after();
        }
        elementGroups.onParentsAllVisited(parents);
        return elementGroups;
    }

    protected abstract Finder createFinder(ElementGroups groups, Elements parents);

    protected void onNonNullParent(ElementGroups groups, Element parent, Finder finder) {
        finder.find(parent);
    }

    protected abstract void onNullParent(ElementGroups groups, Element parent);

    void finish() {
        //lock to unmodifiable
        classifications = Collections.unmodifiableList(classifications);
    }
    /*---instance method---*/

    /*---inner classes---*/
    interface Finder {
        void before();

        void find(Element parent);

        void after();
    }

    enum DepthStrategy {
        RUNTIME, NO_LIMIT, STATIC_LIMIT
    }

    protected static class DepthFinder implements NodeFilter {
        private static final int NOT_FOUND = -1;

        private int       foundDepth = NOT_FOUND;
        private Element   root;
        private Evaluator evaluator;

        DepthFinder(Evaluator evaluator) {
            this.evaluator = evaluator;
        }

        @Override
        public FilterResult head(Node node, int depth) {
            if (node instanceof Element && node != root) {
                Element element = (Element) node;
                if (evaluator.matches(root, element)) {
                    foundDepth = depth;
                    return FilterResult.STOP;
                }
            }
            return FilterResult.CONTINUE;
        }

        @Override
        public FilterResult tail(Node node, int depth) {
            return FilterResult.CONTINUE;
        }

        boolean hasFound() {
            return foundDepth != NOT_FOUND;
        }

        int find(Element root) {
            //reset
            foundDepth = NOT_FOUND;
            this.root = root;
            root.filter(this);
            return foundDepth;
        }
    }

    static class Object extends Classifier {
        private List<DOMSearchMethod.FirstSelector> selectors = new ArrayList<>();

        @Override
        void addClassification(Evaluator evaluator, DOMSearchMethod.Scope scope) {
            classifications.add(evaluator);
            selectors.add(SearchOptionAttributeHelper.singleSelector(scope));
        }

        @Override
        protected Finder createFinder(ElementGroups groups, Elements parents) {
            //no usage
            //see onNonNullParent
            return EmptyFinder.instance;
        }

        @Override
        protected void onNonNullParent(ElementGroups groups, Element nonNull, Finder finder) {
            for (int i = 0, evaluatorSize = classifications.size(); i < evaluatorSize; i++) {
                Evaluator                     evaluator = classifications.get(i);
                DOMSearchMethod.FirstSelector selector  = selectors.get(i);
                Element                       element   = selector.select(nonNull, evaluator);
                if (i == 0) {
                    groups.onShouldNewGroup(nonNull, element, evaluator);
                } else {
                    groups.onFound(element, evaluator);
                }
            }
        }

        @Override
        protected void onNullParent(ElementGroups groups, Element parent) {
            groups.addNullGroup(parent);
        }


        private static final class EmptyFinder implements Finder {

            private static final Finder instance = new EmptyFinder();

            private EmptyFinder() {
            }

            @Override
            public void before() {

            }

            @Override
            public void find(Element parent) {

            }

            @Override
            public void after() {

            }
        }
    }

    /**
     * find groups with only 1 type of element
     */
    static class SingleTypeArray extends Classifier {
        private DepthStrategy         depthStrategy;
        private int                   depthLimit;
        private Evaluator             evaluator;
        private DOMSearchMethod.Scope scope;
        private Delimiter.Factory     delimiterFactory;

//        private Evaluator startDelimiter;
//        private Evaluator endDelimiter;

        SingleTypeArray(AttributeContainer container) {
            depthStrategy = SearchOptionAttributeHelper.arrayDepthStrategy(container);
            switch (depthStrategy) {
                case RUNTIME:
                    break;
                case NO_LIMIT:
                    depthLimit = Integer.MAX_VALUE;
                    break;
                case STATIC_LIMIT:
                    depthLimit = SearchOptionAttributeHelper.getDepthLimitAttr(container);
                    break;
            }

            delimiterFactory = SearchOptionAttributeHelper.delimiterFactory(container);
//            startDelimiter = SearchMethodHelper.getStartDelimiter(container);
//            endDelimiter = SearchMethodHelper.getEndDelimiter(container);
        }

        private int measureDeepest(Elements parents) {
            if (scope == DOMSearchMethod.Scope.CHILDREN) {
                return CHILDREN_DEPTH;
            }
            DepthFinder depthFinder = new DepthFinder(evaluator);
            for (Element element : parents) {
                int depth = depthFinder.find(element);
                if (depthFinder.hasFound()) {
                    return depth;
                }
            }
            //not found
            return 0;
        }

        @Override
        protected Finder createFinder(ElementGroups groups, Elements parents) {
            int limit = (depthStrategy == DepthStrategy.RUNTIME) ?
                    measureDeepest(parents) :
                    depthLimit;
            return new LimitDepthSingleTypeFinder(evaluator, groups, limit, delimiterFactory.create(groups));
        }

        @Override
        protected void onNullParent(ElementGroups groups, Element parent) {
            //empty array
        }

        private static class LimitDepthSingleTypeFinder implements NodeFilter, Finder {
            int depthLimit;
            protected Element   root;
            protected Evaluator evaluator;
            ElementGroups elementGroups;
            Delimiter     delimiter;

            LimitDepthSingleTypeFinder(
                    Evaluator evaluator,
                    ElementGroups elementGroups,
                    int depthLimit,
                    Delimiter delimiter
            ) {
                this.evaluator = evaluator;
                this.elementGroups = elementGroups;
                this.depthLimit = depthLimit;
                this.delimiter = delimiter;
            }

            @Override
            public FilterResult head(Node node, int depth) {
                if (node instanceof Element && node != root) {
                    Element element     = (Element) node;
                    boolean isDelimiter = delimiter.checkShouldSplit(root, element);
                    //exclude delimiter
                    if (!isDelimiter && delimiter.shouldCollect()) {
                        if (evaluator.matches(root, element)) {
                            elementGroups.onShouldNewGroup(root, element, evaluator);
                        }
                    }

                }
                if (depth == depthLimit) {
                    return FilterResult.SKIP_CHILDREN;
                }
                return FilterResult.CONTINUE;
            }

            @Override
            public FilterResult tail(Node node, int depth) {
                return FilterResult.CONTINUE;
            }

            @Override
            public void before() {
                delimiter.onStartOfParent();
            }

            @Override
            public void find(Element root) {
                this.root = root;
                this.root.filter(this);
            }

            @Override
            public void after() {
                delimiter.onEndOfParent();
            }
        }

        @Override
        void addClassification(Evaluator evaluator, DOMSearchMethod.Scope scope) {
            classifications.add(evaluator);
            this.evaluator = evaluator;
            this.scope = scope;
        }


    }

    /**
     * find groups with multiple type of elements in a group
     */
    static class MultiTypeArray extends Classifier {
        private DepthStrategy     depthStrategy;
        private int               depthLimit;
        private List<Evaluator>   subtreeEvaluators = new ArrayList<>();
        private Delimiter.Factory delimiterFactory;


        MultiTypeArray(AttributeContainer container) {
            depthStrategy = SearchOptionAttributeHelper.arrayDepthStrategy(container);
            switch (depthStrategy) {
                case RUNTIME:
                    break;
                case NO_LIMIT:
                    depthLimit = Integer.MAX_VALUE;
                    break;
                case STATIC_LIMIT:
                    depthLimit = SearchOptionAttributeHelper.getDepthLimitAttr(container);
                    break;
            }
            delimiterFactory = SearchOptionAttributeHelper.delimiterFactory(container);

        }

        @Override
        void addClassification(Evaluator evaluator, DOMSearchMethod.Scope scope) {
            classifications.add(evaluator);
            if (scope == DOMSearchMethod.Scope.SUBTREE_EXCLUDE_ROOT) {
                subtreeEvaluators.add(evaluator);
            }
        }

        //pre-measure
        private int measureDeepest(Elements parents) {
            if (subtreeEvaluators.size() == 0) {
                return CHILDREN_DEPTH;
            }
            int deepest = -1;

            final int notFound = -1;
            for (Evaluator evaluator : subtreeEvaluators) {
                DepthFinder finder = new DepthFinder(evaluator);

                int depthOfClassification = -1;

                for (Element parent : parents) {
                    int depth = finder.find(parent);
                    if (finder.hasFound()) {
                        depthOfClassification = depth;
                        break;
                    }
                }
                if (depthOfClassification > deepest) {
                    deepest = depthOfClassification;
                }
            }
            if (deepest == notFound) {
                //nothing found
                //search children or skip
                deepest = (subtreeEvaluators.size() == classifications.size()) ?
                        SKIP_SEARCH : //all are subtree, nothing to search
                        CHILDREN_DEPTH; //some are children
            }
            return deepest;
        }

        @Override
        protected Finder createFinder(ElementGroups groups, Elements parents) {
            int limit = (depthStrategy == DepthStrategy.RUNTIME) ?
                    measureDeepest(parents) :
                    depthLimit;
            return new LimitDepthMultiTypeFinder(limit, new MultiTypeCollector(groups), classifications, delimiterFactory.create(groups));
        }

        @Override
        protected void onNullParent(ElementGroups groups, Element parent) {
            //should be an empty array
        }

        private static class MultiTypeCollector {
            private static final java.lang.Object PRESENT = new java.lang.Object();

            private ElementGroups elementGroups;
            //map as set
            IdentityHashMap<Evaluator, java.lang.Object> currentFoundGroup = new IdentityHashMap<>();

            MultiTypeCollector(ElementGroups elementGroups) {
                this.elementGroups = elementGroups;
            }

            void onResult(Element parent, Element result, Evaluator evaluator) {
                //do not use containsKey to check should new group or not
                //the first should also be a new group and it does not have any key yet
                if (currentFoundGroup.containsKey(evaluator)) {
                    currentFoundGroup.clear();
                }
                //the empty map could be just the first or a new group
                if (currentFoundGroup.isEmpty()) {
                    elementGroups.onShouldNewGroup(parent, result, evaluator);
                } else {
                    elementGroups.onFound(result, evaluator);

                }
                currentFoundGroup.put(evaluator, PRESENT);
            }
        }


        private static class LimitDepthMultiTypeFinder implements NodeFilter, Finder {
            private int                depthLimit;
            private Element            root;
            private MultiTypeCollector collector;
            private List<Evaluator>    types;
            private Delimiter          delimiter;

            LimitDepthMultiTypeFinder(int depthLimit, MultiTypeCollector collector, List<Evaluator> types, Delimiter delimiter) {
                this.depthLimit = depthLimit;
                this.collector = collector;
                this.types = types;
                this.delimiter = delimiter;
            }

            //test an element if it's any type of element from the evaluators
            private void evaluateWithEachType(Element subject) {
                //using indexed for-loop so there won't be an iterator created each time
                for (int i = 0, evaluatorsSize = types.size(); i < evaluatorsSize; i++) {
                    Evaluator type = types.get(i);
                    if (type.matches(root, subject)) {
                        collector.onResult(root, subject, type);
                        break;
                        //break because we don't want to have an element evaluated twice
                    }
                }

            }

            @Override
            public FilterResult head(Node node, int depth) {
                //exclude root
                if (node instanceof Element && node != root) {
                    Element element     = (Element) node;
                    boolean isDelimiter = delimiter.checkShouldSplit(root, element);
                    //exclude delimiter
                    if (!isDelimiter && delimiter.shouldCollect()) {
                        evaluateWithEachType(element);
                    }
                }
                if (depth == depthLimit) {
                    return FilterResult.SKIP_CHILDREN;
                }
                return FilterResult.CONTINUE;
            }

            @Override
            public FilterResult tail(Node node, int depth) {
                return FilterResult.CONTINUE;
            }

            @Override
            public void before() {
                delimiter.onStartOfParent();
            }

            @Override
            public void find(Element parent) {
                //set new root
                this.root = parent;
                this.root.filter(this);
            }

            @Override
            public void after() {
                delimiter.onEndOfParent();
            }
        }
    }

}
