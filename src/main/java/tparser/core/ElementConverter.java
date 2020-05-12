package tparser.core;

import org.jsoup.nodes.Element;

@SuppressWarnings("unused")
public interface ElementConverter<O> extends Converter<O> {
    @Override
    default boolean shouldConvert(String text, Element context) {
        return context != null;
    }
}
