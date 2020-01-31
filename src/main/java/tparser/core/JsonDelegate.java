package tparser.core;

/**
 * reusable Json operation delegation for supporting various Json libraries
 *
 * @param <JO> Json object type provided by Json library
 * @param <JA> Json array type provided by Json library
 */
public interface JsonDelegate<JO, JA> {
    /**
     * create a new json object
     *
     * @return json object
     */
    JO createObjectNode();

    /**
     * create a new json array
     *
     * @return json array
     */
    JA createArrayNode();

    /**
     * put value into a json object, the value is guaranteed non null
     * <p>
     * the value could be one of the types based on template file's type annotation along with TextConverters registered in a template
     *
     * @param objectNode json object to contain the value
     * @param key        json key
     * @param value      a pure value of type defined by template
     * @see TextConverter
     * @see TemplateBuilder
     * <p>
     * <p>
     * built-in types:
     * <p>
     * String (default)
     * Integer
     * Long
     * Short
     * Byte
     * Float
     * Double
     * Boolean
     */
    void putValue(JO objectNode, String key, Object value);

    /**
     * put a null value into json object
     * null can be passed by any converter registered in template
     *
     * builtin null situation:
     * 1.element not found
     * 2.non failing unmatched regex
     *
     * @param objectNode json object to contain the null
     * @param key        json key
     *
     * @see TextConverter
     * @see TemplateBuilder
     */
    void putNull(JO objectNode, String key);

    /**
     * put a json object into parent object
     */
    void putObjectNode(JO objectNode, String key, JO value);

    /**
     * put a json array into parent object
     */
    void putArrayNode(JO objectNode, String key, JA value);

    /**
     * add a json object item to json array
     */
    void add(JA arrayNode, JO itemToAdd);

}
