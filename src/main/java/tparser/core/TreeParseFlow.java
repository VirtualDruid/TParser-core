package tparser.core;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeFilter;

class TreeParseFlow {
    private StepNode stepRoot;

    TreeParseFlow(StepNode stepRoot) {
        this.stepRoot = stepRoot;
    }

    public <JO, JA> ParseResult<JO, JA> parse(Element rootInput, JsonDelegate<JO, JA> delegate) throws HtmlParseException {
        ParseResult<JO, JA> state = new ParseResult<>(rootInput, delegate);
        state.start();
        StopOnErrorTraversal<JO, JA> traversal = new StopOnErrorTraversal<>(state);
        stepRoot.filter(traversal);
        if (traversal.parseException != null) {
            state.end();
            throw traversal.parseException;
        }
        state.end();
        return state;
    }

    private static class StopOnErrorTraversal<JO, JA> implements NodeFilter {

        private ParseResult<JO, JA> state;
        private HtmlParseException  parseException;

        StopOnErrorTraversal(ParseResult<JO, JA> state) {
            this.state = state;
        }


        @Override
        public FilterResult head(Node node, int depth) {
            //element #root need to be ignored because steps cannot select itself
            if (depth != 0) {
                StepNode stepNode = (StepNode) node;
                try {
                    stepNode.onVisit(state);
                } catch (HtmlParseException e) {
                    parseException = e;
                    return FilterResult.STOP;
                } catch (RuntimeException re) {
                    //wrap up and re-throw
                    throw new RuntimeException(
                            String.format("STEP NODE: %s onVisit: %s", stepNode.toString(), state.toString()),
                            re
                    );
                }
            }
            return FilterResult.CONTINUE;
        }

        @Override
        public FilterResult tail(Node node, int depth) {
            if (depth != 0) {
                StepNode stepNode = (StepNode) node;
                try{
                    stepNode.onExit(state);
                }catch (RuntimeException re) {
                    //wrap up and re-throw
                    throw new RuntimeException(
                            String.format("STEP NODE: %s onExit: %s", stepNode.toString(), state.toString()),
                            re
                    );
                }
            }
            return FilterResult.CONTINUE;
        }
    }
}
