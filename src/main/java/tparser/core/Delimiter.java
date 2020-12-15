package tparser.core;

import org.jsoup.nodes.Element;
import org.jsoup.select.Evaluator;

abstract class Delimiter {
    static final Factory noDelimiter = new NoDelimiterFactory();
    ElementGroups elementGroups;

    Delimiter(ElementGroups elementGroups) {
        this.elementGroups = elementGroups;
    }

    abstract boolean shouldCollect();

    /**
     * check if the element is a delimiter
     * if it is, spilt the array
     *
     * @param root    the element's root
     * @param element to check
     * @return if this element is a delimiter
     */
    abstract boolean checkShouldSplit(Element root, Element element);


    abstract void onStartOfParent();

    abstract void onEndOfParent();


    interface Factory {
        Delimiter create(ElementGroups elementGroups);
    }

    private static class NoDelimiter extends Delimiter {

        private NoDelimiter(ElementGroups elementGroups) {
            super(elementGroups);
        }

        @Override
        boolean shouldCollect() {
            return true;
        }

        @Override
        boolean checkShouldSplit(Element root, Element element) {
            return false;
        }

        @Override
        void onStartOfParent() {
            elementGroups.onStartOfSubArray();
        }

        @Override
        void onEndOfParent() {
            elementGroups.onEndOfSubArray();
        }
    }

    private static class NoDelimiterFactory implements Factory {
        private NoDelimiterFactory() {
        }

        @Override
        public Delimiter create(ElementGroups elementGroups) {
            return new NoDelimiter(elementGroups);
        }


        @Override
        public String toString() {
            return "NO-DELIMITER";
        }
    }

    static class Between extends Delimiter {
        private Evaluator betweenDelimiter;

        Between(ElementGroups elementGroups, Evaluator betweenDelimiter) {
            super(elementGroups);
            this.betweenDelimiter = betweenDelimiter;
        }

        @Override
        boolean shouldCollect() {
            return true;
        }

        @Override
        boolean checkShouldSplit(Element root, Element element) {
            boolean isDelimiter = betweenDelimiter.matches(root, element);
            if (isDelimiter) {
                elementGroups.onEndOfSubArray();
                elementGroups.onStartOfSubArray();
            }
            return isDelimiter;
        }

        @Override
        void onStartOfParent() {
            elementGroups.onStartOfSubArray();
        }

        @Override
        void onEndOfParent() {
            elementGroups.onEndOfSubArray();
        }
    }

    static class BetweenFactory implements Factory {
        private Evaluator betweenDelimiter;

        BetweenFactory(Evaluator betweenDelimiter) {
            this.betweenDelimiter = betweenDelimiter;
        }

        @Override
        public Delimiter create(ElementGroups elementGroups) {
            return new Between(elementGroups, betweenDelimiter);
        }

        @Override
        public String toString() {
            return String.format("BETWEEN-DELIMITER(%s)", betweenDelimiter);
        }
    }

    static class Start extends Delimiter {

        private boolean   shouldCollect = false;
        private Evaluator startDelimiter;

        Start(ElementGroups elementGroups, Evaluator startDelimiter) {
            super(elementGroups);
            this.startDelimiter = startDelimiter;
        }

        @Override
        boolean shouldCollect() {
            return shouldCollect;
        }

        @Override
        boolean checkShouldSplit(Element root, Element element) {

            boolean isDelimiter = startDelimiter.matches(root, element);
            if (startDelimiter.matches(root, element)) {
                if (shouldCollect) {
                    elementGroups.onEndOfSubArray();
                }
                shouldCollect = true;
                elementGroups.onStartOfSubArray();
            }
            return isDelimiter;
        }

        @Override
        void onStartOfParent() {
            //reset
            shouldCollect = false;
        }

        @Override
        void onEndOfParent() {
            elementGroups.onEndOfSubArray();
        }

    }

    static class StartFactory implements Factory {

        private Evaluator startDelimiter;

        StartFactory(Evaluator startDelimiter) {
            this.startDelimiter = startDelimiter;
        }

        @Override
        public Delimiter create(ElementGroups elementGroups) {
            return new Start(elementGroups, startDelimiter);
        }

        @Override
        public String toString() {
            return String.format("START-DELIMITER(%s)", startDelimiter);
        }
    }

    static class End extends Delimiter {
        private Evaluator endDelimiter;
        private int       subArrayCount = 0;

        End(ElementGroups elementGroups, Evaluator endDelimiter) {
            super(elementGroups);
            this.endDelimiter = endDelimiter;
        }

        @Override
        boolean shouldCollect() {
            return true;
        }

        @Override
        boolean checkShouldSplit(Element root, Element element) {
            boolean isDelimiter = endDelimiter.matches(root, element);
            if (isDelimiter) {
                elementGroups.onEndOfSubArray();
                elementGroups.onStartOfSubArray();
                subArrayCount++;
            }
            return isDelimiter;
        }

        @Override
        void onStartOfParent() {
            elementGroups.onStartOfSubArray();
        }

        @Override
        void onEndOfParent() {
//            elementGroups.onEndOfSubArray();
            if (elementGroups.getSubArraySizes().size() > subArrayCount) {
                elementGroups.removeLastArray();
            }
        }


    }

    static class EndFactory implements Factory {
        EndFactory(Evaluator endDelimiter) {
            this.endDelimiter = endDelimiter;
        }

        private Evaluator endDelimiter;

        @Override
        public Delimiter create(ElementGroups elementGroups) {
            return new End(elementGroups, endDelimiter);
        }

        @Override
        public String toString() {
            return String.format("END-DELIMITER(%s)", endDelimiter);
        }
    }


    static class StartEnd extends Delimiter {
        private boolean   shouldCollect = false;
        private Evaluator startDelimiter;
        private Evaluator endDelimiter;

        StartEnd(ElementGroups elementGroups, Evaluator startDelimiter, Evaluator endDelimiter) {
            super(elementGroups);
            this.startDelimiter = startDelimiter;
            this.endDelimiter = endDelimiter;
        }


        @Override
        boolean shouldCollect() {
            return shouldCollect;
        }

        @Override
        boolean checkShouldSplit(Element root, Element element) {
            boolean isStartDelimiter = startDelimiter.matches(root, element);
            boolean isEndDelimiter   = endDelimiter.matches(root, element);
            if (isStartDelimiter) {
                // prevent misplace by 2 start before the end
                // s aa s aaa e  should be:  aaaaa
                if (!shouldCollect) {
                    elementGroups.onStartOfSubArray();
                }
                shouldCollect = true;
            }
            if (isEndDelimiter) {
                if (shouldCollect) {
                    elementGroups.onEndOfSubArray();
                }
                shouldCollect = false;
            }
            return isStartDelimiter || isEndDelimiter;
        }

        @Override
        void onStartOfParent() {
            //reset
            shouldCollect = false;
        }

        @Override
        void onEndOfParent() {

        }

    }

    static class StartEndFactory implements Factory {
        private Evaluator startDelimiter;
        private Evaluator endDelimiter;

        StartEndFactory(Evaluator startDelimiter, Evaluator endDelimiter) {
            this.startDelimiter = startDelimiter;
            this.endDelimiter = endDelimiter;
        }

        @Override
        public Delimiter create(ElementGroups elementGroups) {
            return new StartEnd(elementGroups, startDelimiter, endDelimiter);
        }

        @Override
        public String toString() {
            return String.format("START-END-DELIMITER(%s/%s)", startDelimiter, endDelimiter);
        }
    }
}
