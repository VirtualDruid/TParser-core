package tparser.core;

import org.jsoup.nodes.Element;

class TypeProcessor {
    private final StructPlaceHolderVisitor structure;
    final         String                   property;
    private final String                   typeName;
    private final Converter                converter;

    TypeProcessor(StructPlaceHolderVisitor structure, String property, Converter converter, String typeName) {
        this.property = property;
        this.structure = structure;
        this.converter = converter;
        this.typeName = typeName;
    }

    <JO, JA> void process(ParseResult<JO, JA> state, String result, Element context, int index) {
        Object value = converter.shouldConvert(result, context) ?
                converter.convert(result, context) :
                null;
        structure.onExtract(state, property, value, index);
    }

    @Override
    public String toString() {
        return String.format("%s@%s", property, typeName);
    }

}
