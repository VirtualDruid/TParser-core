package tparser.core;

import org.jsoup.nodes.Element;

/**
 * the converter that do not check null and null result will be passed to convert()
 *
 * @see Converter
 */
@SuppressWarnings("unused")
public interface NullableConverter<O> extends Converter<O> {

    @Override
    default boolean shouldConvert(String text, Element context) {
        return true;
    }
}
