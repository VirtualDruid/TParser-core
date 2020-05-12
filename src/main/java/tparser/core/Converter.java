package tparser.core;

import org.jsoup.nodes.Element;

/**
 * functional interface converts text or element to value with specified type
 * the implementation should be :
 * <p>
 * 1.only functional without any mutable state
 * 2.return the type json library supports
 * 3.never modify the element input
 *
 * @param <O> the output type converted from input
 * @see JsonDelegate
 */
@SuppressWarnings("unused")
public interface Converter<O> {
    /**
     * transform a text or element to the specific type
     *
     * WARNING:
     * the element is NOT a clone for performance reason
     * thus it should not be modified or it's possible that unexpected results will occur
     *
     * @param text extracted text
     * @param context the owner element of the text extraction
     * @return a value of type O
     */
    O convert(String text, Element context);

    /**
     * check the input validity before calling convert()
     * output a null if it's invalid
     *
     * @see TypeProcessor
     */
    boolean shouldConvert(String text, Element context);
}
