package tparser.core;

/**
 * functional interface converts text to value with specified type
 * the implementation generally should be :
 * <p>
 * 1.only functional without any mutable state
 * 2.return the type json library supports
 *
 * @param <O> the output type converted from string input
 * @see JsonDelegate
 */
public interface TextConverter<O> {
    O valueOf(String text);
}
