package tparser.core;

import org.jsoup.nodes.Element;

@SuppressWarnings("unused")
public class TemplateBuilder {
    private StepTreeBuilder                    stepTreeBuilder;
    private Converters.ConverterFactoryBuilder factoryBuilder = Converters.factoryBuilder();

    public TemplateBuilder(StepTreeBuilder templateSource) {
        this.stepTreeBuilder = templateSource;
    }

    public TemplateBuilder(Element templateSource) {
        this.stepTreeBuilder = new DefaultBuilder(templateSource);
    }

    /**
     * set client implemented factory
     *
     * @param factory external converter factory
     * @return this for chaining
     */
    public TemplateBuilder setExternalConverterFactory(ConverterFactory factory) {
        factoryBuilder.registerFactory(factory);
        return this;
    }

    /**
     * register a shared converter instance with type name
     * <p>
     * the converter should be:
     * 1.with only immutable state, functional instance which simply converts text to specified type
     * 2.without any runtime-changing state
     * 3.with output type the Json library can handle
     *
     * @param typeName  the name as type annotation in template file
     * @param converter the shared functional instance converts text into value with type
     * @return this for chaining
     * @see JsonDelegate
     * <p>
     * thus the template can parse documents thread safely and consistently
     */
    public TemplateBuilder registerConverter(String typeName, Converter converter) {
        factoryBuilder.register(typeName, converter);
        return this;
    }

    public Template build() {
        return new Template(stepTreeBuilder, factoryBuilder.build());
    }
}
