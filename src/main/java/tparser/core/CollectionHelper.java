package tparser.core;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

class CollectionHelper {
    private CollectionHelper() {
    }

    /**
     * Gueva/jdk8 measure
     * <p>
     * calculate a large enough hash collection size for constructor with least table resizing and collision
     * only works for java's classic hash collections
     * <p>
     * <p>
     * e.g.
     *
     * @see java.util.HashMap
     * @see java.util.HashSet
     */
    static int enoughHashTableCapacity(int expectedEntries) {
        if (expectedEntries < 3) {
            return expectedEntries + 1;
        }
        return (int) ((float) expectedEntries / 0.75F + 1.0F);
    }


    /**
     * wrap element as attributes
     *
     * @param template element contains the information of ElementVisitor
     * @return container
     */
    static AttributeContainer<Attribute> asAttributeContainer(Element template) {
        return new TemplateAttributeContainer(template);
    }

    static AttributeContainer<Map.Entry<String, String>> emptyContainer() {
        return EmptyContainer.INSTANCE;
    }

    private static class EmptyContainer implements AttributeContainer<Map.Entry<String, String>> {

        private static final EmptyContainer INSTANCE = new EmptyContainer();

        private Map<String, String> emptyMap = Collections.emptyMap();

        private EmptyContainer() {
        }

        @Override
        public boolean hasAttr(String key) {
            return false;
        }

        @Override
        public String getAttr(String key) {
            return null;
        }

        @Override
        public Iterable<String> getClasses() {
            return Collections.emptyList();
        }

        @Override
        public Iterator<Map.Entry<String, String>> iterator() {
            return emptyMap.entrySet().iterator();
        }
    }

    private static class TemplateAttributeContainer implements AttributeContainer<Attribute> {
        private Element template;

        TemplateAttributeContainer(Element template) {
            this.template = template;
        }

        @Override
        public boolean hasAttr(String key) {
            return template.hasAttr(key);
        }

        @Override
        public String getAttr(String key) {
            return template.attr(key);
        }

        @Override
        public Iterable<String> getClasses() {
            return template.classNames();
        }

        @Override
        public Iterator<Attribute> iterator() {
            return template.attributes().iterator();
        }
    }
}
