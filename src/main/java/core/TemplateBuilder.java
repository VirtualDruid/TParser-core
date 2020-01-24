package core;

import org.jsoup.nodes.Document;

@SuppressWarnings("unused")
public class TemplateBuilder {
    private StepTreeBuilder                    stepTreeBuilder;
    private Converters.ConverterFactoryBuilder factoryBuilder = Converters.factoryBuilder();

    public TemplateBuilder(StepTreeBuilder templateSource) {
        this.stepTreeBuilder = templateSource;
    }

    public TemplateBuilder(Document templateSource) {
        this.stepTreeBuilder = new DefaultBuilder(templateSource);
    }

    /**
     * set client implemented factory
     *
     * @param factory external converter factory
     * @return this for chaining
     */
    public TemplateBuilder setExternalConverterFactory(TextConverterFactory factory) {
        factoryBuilder.registerFactory(factory);
        return this;
    }

    /**
     * register a shared converter instance with type name
     *
     * the converter should be:
     * 1.with only immutable state, functional instance which simply converts text to specified type
     * 2.without any runtime-changing state
     * 3.with output type the Json library can handle
     *
     * @see JsonDelegate
     *
     * thus the template can parse documents thread safely and consistently
     *
     * @param typeName  the name as type annotation in template file
     * @param converter the shared functional instance converts text into value with type
     * @return this for chaining
     */
    public TemplateBuilder registerConverter(String typeName, TextConverter converter) {
        factoryBuilder.register(typeName, converter);
        return this;
    }

    public Template build() {
        return new Template(stepTreeBuilder, factoryBuilder.build());
    }
}
