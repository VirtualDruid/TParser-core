package tparser.core;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * model class represent the result parsed by a template
 * this also simply stores internal parsing state
 *
 * @param <JO> Json object type provide by Json library
 * @param <JA> Json array type provide by Json library
 */
@SuppressWarnings("unused")
public class ParseResult<JO, JA> {
    JsonDelegate<JO, JA> delegate;
    JO                   resultObject = null;
    JA                   resultArray  = null;
    private Element rootInput;

    //using ArrayDeque because no need for thread safe
    //work only as stack
    Deque<ElementGroups> elementGroupsStack = new ArrayDeque<>();
    Deque<Elements>      selectionStack     = new ArrayDeque<>();
    Deque<List<JO>>      pendingItemStack   = new ArrayDeque<>();

    private long start;

    private long processTimeNanos;

    ParseResult(Element input, JsonDelegate<JO, JA> delegate) {
        this.rootInput = input;
        this.delegate = delegate;
        Elements root = new Elements(1);
        root.add(input);
        selectionStack.push(root);
    }

    boolean shouldInitRoot() {
        return resultObject == null && resultArray == null;
    }

    /**
     * time measurement
     */
    void start() {
        start = System.nanoTime();
    }

    void end() {
        processTimeNanos = System.nanoTime() - start;
        //clean up
        elementGroupsStack = null;
        selectionStack = null;
        pendingItemStack = null;
        delegate = null;
    }

    public Element getRootInput() {
        return rootInput;
    }

    /**
     * get the result as json object which is defined by the template
     * if root is array, should be using
     *
     * @return the root entry of Json
     * <p>
     * return null if it's actually an array
     * @see #getResultArray()
     * <p>
     * the concern why there is 2 method not only 1,like getJsonRoot()
     * is because some Json library does not have polymorphic node model
     * <p>
     * for instance: org.JSON's JOSNObject and JSONArray don't have same inheritance
     */
    public JO getResultObject() {
        return resultObject;
    }

    /**
     * @return the root entry of Json
     * <p>
     * return null if it's actually an object
     */
    public JA getResultArray() {
        return resultArray;
    }

    /**
     * @return parsing time consumed, in nanos
     */
    public long processTimeNanos() {
        return processTimeNanos;
    }

    /**
     * @return parsing time consumed, in millis
     */
    public float processTimeMillis() {
        final int nanosInMillis = 1000000;
        return (float) processTimeNanos / nanosInMillis;
    }

    @Override
    public String toString() {
        return "ParseResult{" +
                "delegate=" + delegate +
                ", resultObject=" + resultObject +
                ", resultArray=" + resultArray +
                ", rootInput=" + rootInput +
                ", elementGroupsStack=" + elementGroupsStack +
                ", selectionStack=" + selectionStack +
                ", pendingItemStack=" + pendingItemStack +
                ", start=" + start +
                ", processTimeNanos=" + processTimeNanos +
                '}';
    }
}
