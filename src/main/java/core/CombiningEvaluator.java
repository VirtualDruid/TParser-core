package core;

import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Element;
import org.jsoup.select.Evaluator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/**
 * borrowed from jsoup.select
 */
public abstract class CombiningEvaluator
        extends Evaluator
        implements Iterable<Evaluator> {

    final ArrayList<Evaluator> evaluators;
    int num = 0;

    CombiningEvaluator() {
        super();
        evaluators = new ArrayList<>();
    }

    public CombiningEvaluator(Collection<Evaluator> evaluators) {
        this();
        this.evaluators.addAll(evaluators);
        updateNumEvaluators();
    }

    Evaluator rightMostEvaluator() {
        return num > 0 ? evaluators.get(num - 1) : null;
    }

    void replaceRightMostEvaluator(Evaluator replacement) {
        evaluators.set(num - 1, replacement);
    }

    void updateNumEvaluators() {
        num = evaluators.size();
    }

    @Override
    public Iterator<Evaluator> iterator() {
        return evaluators.iterator();
    }

    public static final class And extends CombiningEvaluator {
        public And(Collection<Evaluator> evaluators) {
            super(evaluators);
        }

        public And(Evaluator... evaluators) {
            this(Arrays.asList(evaluators));
        }

        @Override
        public boolean matches(Element root, Element node) {
            for (int i = 0; i < num; i++) {
                Evaluator s = evaluators.get(i);
                if (!s.matches(root, node))
                    return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return StringUtil.join(evaluators, " ");
        }
    }

    public static final class Or extends CombiningEvaluator {
        /**
         * Create a new Or evaluator. The initial evaluators are ANDed together and used as the first clause of the OR.
         *
         * @param evaluators initial OR clause (these are wrapped into an AND evaluator).
         */
        public Or(Collection<Evaluator> evaluators) {
            super();
            if (num > 1)
                this.evaluators.add(new CombiningEvaluator.And(evaluators));
            else // 0 or 1
                this.evaluators.addAll(evaluators);
            updateNumEvaluators();
        }

        public Or(Evaluator... evaluators) {
            this(Arrays.asList(evaluators));
        }

        Or() {
            super();
        }

        public void add(Evaluator e) {
            evaluators.add(e);
            updateNumEvaluators();
        }

        @Override
        public boolean matches(Element root, Element node) {
            for (int i = 0; i < num; i++) {
                Evaluator s = evaluators.get(i);
                if (s.matches(root, node))
                    return true;
            }
            return false;
        }
        @Override
        public String toString() {
            return StringUtil.join(evaluators, ",");
        }
    }
}
