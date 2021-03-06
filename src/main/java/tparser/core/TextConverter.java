package tparser.core;

import org.jsoup.nodes.Element;

public interface TextConverter<O> extends Converter<O> {
    @Override
    default boolean shouldConvert(String text, Element context) {
        return text != null;
    }
}
