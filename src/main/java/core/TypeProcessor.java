package core;

class TypeProcessor {
    private final StructPlaceHolderVisitor structure;
    private final String                   property;
    private final String                   typeName;
    private final TextConverter            converter;

    TypeProcessor(StructPlaceHolderVisitor structure, String property, TextConverter converter, String typeName) {
        this.property = property;
        this.structure = structure;
        this.converter = converter;
        this.typeName = typeName;
    }

    <JO, JA> void process(ParseResult<JO, JA> state, String result, int index) {
        structure.onExtract(state, property, result == null ? null : converter.valueOf(result), index);
    }

    @Override
    public String toString() {
        return String.format("%s@%s", property, typeName);
    }

    String getProperty() {
        return property;
    }
}
