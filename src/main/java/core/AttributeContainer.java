package core;

import java.util.Map;

/**
 * delegation for different attribute set implementation
 *
 * @param <E> the key-value type of attribute
 */
public interface AttributeContainer<E extends Map.Entry<String, String>>
        extends Iterable<E> {
    /**
     * @param key attr key
     * @return if has the attr
     */
    boolean hasAttr(String key);

    /**
     * @param key attr key
     * @return the value of attr
     */
    String getAttr(String key);

    /**
     * get element's class attribute
     *
     * @return classes
     */
    Iterable<String> getClasses();
}
