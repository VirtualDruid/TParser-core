package core;

import org.jsoup.nodes.Element;
import org.jsoup.select.Evaluator;
import org.jsoup.select.QueryParser;
import org.jsoup.select.Selector;

import static core.Classifier.DepthStrategy;

final class SearchMethodHelper {
    private SearchMethodHelper() {
    }

//    /**
//     * get array searching depth strategy based on array node's depth-limit attr
//     *
//     * @return RUNTIME_AUTO_DEPTH(- 2) / NO_LIMIT (-1) / depth(positive int)
//     */

    //    static int arrayDepthLimit(AttributeContainer container) {
//        if (!container.hasAttr(DEPTH_LIMIT)) {
//            return RUNTIME_AUTO_DEPTH;
//        }
//        try {
//            int depth = Integer.parseInt(container.getAttr(DEPTH_LIMIT));
//
//            final int noLimitAttrValue = -1;
//            if (depth == noLimitAttrValue) {
//                return NO_LIMIT;
//            }
//            if (depth < 0) {
//                throw new TemplateSyntaxError("depth-limit must be >=0 or -1(no limit)", new IllegalArgumentException());
//            }
//            return depth;
//        } catch (NumberFormatException ne) {
//            throw new TemplateSyntaxError("depth-limit must be an Integer", ne);
//        }
//
//    }
    private static final String DEPTH_LIMIT = "depth-limit";

    static int getDepthLimitAttr(AttributeContainer container) {
        return Integer.parseInt(container.getAttr(DEPTH_LIMIT));
    }

    /**
     * get array searching depth strategy based on array node's depth-limit attr
     */
    static DepthStrategy arrayDepthStrategy(AttributeContainer container) {
        if (!container.hasAttr(DEPTH_LIMIT)) {
            return DepthStrategy.RUNTIME;
        }
        try {
            int depth = getDepthLimitAttr(container);

            final int noLimitAttrValue = -1;
            if (depth == noLimitAttrValue) {
                return DepthStrategy.NO_LIMIT;
            }
            if (depth < noLimitAttrValue) {
                throw new TemplateSyntaxError("depth-limit must be >=0 or -1(no limit)", new IllegalArgumentException());
            }
            return DepthStrategy.STATIC_LIMIT;
        } catch (NumberFormatException ne) {
            throw new TemplateSyntaxError("depth-limit must be an Integer", ne);
        }
    }


    /**
     * required: if the element is missing, indicate that's an error,
     * fail if found: if the element exists, indicate that's an error, such as "page not found"
     * <p>
     * attr value is a customized msg for
     *
     * @see HtmlParseException.Existence
     */

    /*---existence check-----*/
    private static final String X_REQUIRED      = "x-required";
    private static final String X_FAIL_IF_FOUND = "x-fail-if-found";
    /*-----------------------*/

    /**
     * and:       combine selector with the default one as AND selection
     * or:        combine selector with the default one as OR selection
     * overwrite: use selector instead of default
     * <p>
     * default: selector automatically generate by template element
     * selector: the css selector from the attr's value
     * <p>
     * 1. attr value must be in a valid css selector syntax,
     * 2. only one selector combination is allowed at once
     * else will throw
     *
     * @see TemplateSyntaxError
     */

    /*----selector combination----------*/
    private static final String X_AND_SELECTOR               = "x-and-selector";
    private static final String X_OR_SELECTOR                = "x-or-selector";
    private static final String X_OVERWRITE_DEFAULT_SELECTOR = "x-overwrite-default-selector";
    /*-----------------------------------*/

    /**
     * select in subtree: should search the element in subtree instead of only in direct children
     * <p>
     * attr value should be true, else will fallback
     * default: false
     * fallback: false
     */
    /*------------------------------------*/
    private static final String X_SELECT_IN_SUBTREE = "x-select-in-subtree";

    /**
     * should include root from selection or not
     * default: false
     */
//    private static final String X_INCLUDE_ROOT = "x-include-root";
    /*------------------------------------*/

    private static final String TRUE = Boolean.TRUE.toString();

    /**
     * check should an attr is an extension and cannot be a part of evaluator
     */
    static boolean isExtensionAttr(String attrKey) {
        switch (attrKey) {
            case X_REQUIRED:
                return true;
            case X_FAIL_IF_FOUND:
                return true;
            case X_AND_SELECTOR:
                return true;
            case X_OR_SELECTOR:
                return true;
            case X_OVERWRITE_DEFAULT_SELECTOR:
                return true;
            case X_SELECT_IN_SUBTREE:
                return true;
//            case X_INCLUDE_ROOT:
//                return true;
            default:
                if (attrKey.startsWith("x")) {
                    throw new TemplateSyntaxError("unknown extension attr");
                }
                return false;
        }
    }

    /**
     * create checker based on x-required or x-fail-if-found
     */
    static DOMSearchMethod.ExistenceCheck createChecker(AttributeContainer attributes) {
        boolean required    = attributes.hasAttr(X_REQUIRED);
        boolean failIfFound = attributes.hasAttr(X_FAIL_IF_FOUND);
        if (required && failIfFound) {
            throw new TemplateSyntaxError("cannot have both required and fail-if-found");
        }
        if (required) {
            return new DOMSearchMethod.ExistenceCheck.Required(attributes.getAttr(X_REQUIRED));
        }
        if (failIfFound) {
            return new DOMSearchMethod.ExistenceCheck.FailIfFound(attributes.getAttr(X_FAIL_IF_FOUND));
        }
        return DOMSearchMethod.ExistenceCheck.noCheck;
    }

    /**
     * get searching scope, default is direct children
     */
    static DOMSearchMethod.Scope scope(Element template) {
        return TRUE.equals(template.attr(X_SELECT_IN_SUBTREE)) ?
                DOMSearchMethod.Scope.SUBTREE_EXCLUDE_ROOT :
                DOMSearchMethod.Scope.CHILDREN;
    }

    static DOMSearchMethod.Scope scope(AttributeContainer attributes) {
        return TRUE.equals(attributes.getAttr(X_SELECT_IN_SUBTREE)) ?
                DOMSearchMethod.Scope.SUBTREE_EXCLUDE_ROOT :
                DOMSearchMethod.Scope.CHILDREN;
    }

//    private static boolean includeRoot(AttributeContainer attributes) {
//        return TRUE.equals(attributes.getAttr(X_INCLUDE_ROOT));
//    }

    static DOMSearchMethod.FirstSelector singleSelector(DOMSearchMethod.Scope scope) {
        switch (scope) {
            case SUBTREE_EXCLUDE_ROOT:
                return DOMSearchMethod.FirstSelector.excludeRoot;
            default:
                return DOMSearchMethod.FirstSelector.firstInChildren;
        }
    }


//    static DOMSearchMethod.First searchFirstMethod(DOMSearchMethod.Scope scope) {
//        DOMSearchMethod.SingleSelect selector = (scope == DOMSearchMethod.Scope.SUBTREE) ?
//                DOMSearchMethod.SingleSelect.firstInSubtree :
//                DOMSearchMethod.SingleSelect.firstInChildren;
//        return new DOMSearchMethod.First(selector);
//    }

//    static Element searchFirst(Element element, Evaluator evaluator, DOMSearchMethod.First searchMethod) {
//        DOMSearchMethod.SingleResult singleResult = new DOMSearchMethod.SingleResult();
//        searchMethod.select(element, evaluator, singleResult);
//        return singleResult.get();
//    }


//
//    static Evaluator composeEvaluator(Element template, Evaluator evalFromTemplate) {
//        boolean and       = template.hasAttr(X_AND_SELECTOR);
//        boolean or        = template.hasAttr(X_OR_SELECTOR);
//        boolean overwrite = template.hasAttr(X_OVERWRITE_DEFAULT_SELECTOR);
//        if (and && or || and && overwrite || or && overwrite) {
//            throw new TemplateSyntaxError("only one AND/OR/OVERWRITE selector is allowed at once", template);
//        }
//        try {
//            if (and) {
//                return new CombiningEvaluator.And(evalFromTemplate, QueryParser.parse(template.attr(X_AND_SELECTOR)));
//            }
//            if (or) {
//                return new CombiningEvaluator.Or(evalFromTemplate, QueryParser.parse(template.attr(X_OR_SELECTOR)));
//            }
//            if (overwrite) {
//                return QueryParser.parse(template.attr(X_OVERWRITE_DEFAULT_SELECTOR));
//            }
//        } catch (Selector.SelectorParseException parseException) {
//            throw new TemplateSyntaxError(String.format("css selector syntax error: %s", parseException.getMessage()), parseException, template);
//        }
//        return evalFromTemplate;
//    }

    /**
     * combines evaluator from template itself and x-selector attr's css selector into one evaluator
     *
     * @param attributes       attr set that contains selection info
     * @param evalFromTemplate the evaluator generated from template
     * @return composed evaluator base on template's evaluator and x-selector attr's css selector
     * @throws TemplateSyntaxError if invalid syntax of css selector, or found multiple x-selector in once
     */
    static Evaluator composeEvaluator(AttributeContainer attributes, Evaluator evalFromTemplate) {
        boolean and       = attributes.hasAttr(X_AND_SELECTOR);
        boolean or        = attributes.hasAttr(X_OR_SELECTOR);
        boolean overwrite = attributes.hasAttr(X_OVERWRITE_DEFAULT_SELECTOR);
        if ((and && or) || (and && overwrite) || (or && overwrite)) {
            throw new TemplateSyntaxError("only one AND/OR/OVERWRITE selector is allowed at once");
        }
        try {
            if (and) {
                return new CombiningEvaluator.And(evalFromTemplate, QueryParser.parse(attributes.getAttr(X_AND_SELECTOR)));
            }
            if (or) {
                return new CombiningEvaluator.Or(evalFromTemplate, QueryParser.parse(attributes.getAttr(X_OR_SELECTOR)));
            }
            if (overwrite) {
                return QueryParser.parse(attributes.getAttr(X_OVERWRITE_DEFAULT_SELECTOR));
            }
        } catch (Selector.SelectorParseException parseException) {
            throw new TemplateSyntaxError(String.format("css selector syntax error: %s", parseException.getMessage()), parseException);
        }
        return evalFromTemplate;
    }
}
