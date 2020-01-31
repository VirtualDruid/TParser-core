package tparser.core;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;

import java.util.Iterator;

class CollectionHelper {
    private CollectionHelper() {
    }

    /**
     * Gueva/jdk8 measure
     * <p>
     * calculate a large enough hash collection size for constructor with least table resizing and collision
     * only works for java's classic hash collections
     * <p>
     *
     * e.g.
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
     * @param template element contains the information of ElementVisitor
     * @return container
     */
    static AttributeContainer<Attribute> asAttributeContainer(Element template) {
        return new TemplateAttributeContainer(template);
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
