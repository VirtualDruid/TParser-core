package tparser.core;

import org.jsoup.nodes.Element;
import org.jsoup.select.Evaluator;

class DOMSearchMethod {
    private DOMSearchMethod(){}
//
//    void select(Element root, ResultCollector observer);

    interface FirstSelector {
        Element select(Element root, Evaluator evaluator);
//        FirstSelector includeRoot     = SelectHelper::selectFirst;
        FirstSelector excludeRoot     = SelectHelper::selectFirstExcludeRoot;
        FirstSelector firstInChildren = SelectHelper::selectFirstInChildren;
    }

    enum Scope {
        CHILDREN, SUBTREE_EXCLUDE_ROOT
    }

//    interface ResultCollector {
//        void onResult(Element result, Evaluator evaluator);
//
//    }

    interface ExistenceCheck {
        void check(Element subjectToCheck, Evaluator evaluator) throws HtmlParseException.Existence;

        //null safe no check
        @SuppressWarnings("unused")
        static void noCheck(Element subjectToCheck, Evaluator evaluator) {

        }

        static void failIfFound(Element subjectToCheck, Evaluator evaluator, String msg) throws HtmlParseException.FailIfFound {
            if (!NullWrapper.isNullRepresent(subjectToCheck)) {
                // null also return false
                throw new HtmlParseException.FailIfFound(evaluator, msg);
            }
        }

        static void required(Element subjectToCheck, Evaluator evaluator, String msg) throws HtmlParseException.RequiredNotFound {
            if (subjectToCheck == null || NullWrapper.isNullRepresent(subjectToCheck)) {
                throw new HtmlParseException.RequiredNotFound(evaluator, msg);
            }
        }

        ExistenceCheck noCheck = ExistenceCheck::noCheck;

        /**
         * present attr: x-required
         * indicates that's a error when the element is missing
         */
        class Required implements ExistenceCheck {
            String customMsg;

            Required(String customMsg) {
                this.customMsg = customMsg;
            }

            @Override
            public void check(Element subjectToCheck, Evaluator evaluator) throws HtmlParseException.RequiredNotFound {
                required(subjectToCheck, evaluator, customMsg);
            }
        }

        /**
         * present attr: x-fail-if-found
         * useful when dealing with error pages with incorrect HTTP status
         * (e.g. 404 page with http 200)
         */
        class FailIfFound implements ExistenceCheck {
            String customMsg;

            FailIfFound(String customMsg) {
                this.customMsg = customMsg;
            }

            @Override
            public void check(Element subjectToCheck, Evaluator evaluator) throws HtmlParseException.FailIfFound {
                failIfFound(subjectToCheck, evaluator, customMsg);
            }
        }
    }


}
