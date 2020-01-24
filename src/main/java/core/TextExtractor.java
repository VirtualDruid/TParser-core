package core;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;

interface TextExtractor {

    String extract(Element subject);

    TextExtractor ownText   = Element::ownText;
    TextExtractor fullText  = Element::text;
    TextExtractor innerHtml = Element::html;
    TextExtractor outerHtml = Element::outerHtml;


    //debug usage
    static String getTypeTag(TextExtractor extractor) {
        if (extractor == ownText) {
            return "$own-text";
        }
        if (extractor == fullText) {
            return "$'#-full-text";
        }
        if (extractor == innerHtml) {
            return "$#-inner-html";
        }
        if (extractor == outerHtml) {
            return "*$*#-outer-html";
        }
        if (extractor instanceof AttrValueTarget) {
            AttrValueTarget attrValueTarget = (AttrValueTarget) extractor;
            return String.format("attr-value[%s=$]", attrValueTarget.attrKey);
        }
        if (extractor instanceof AttrKeyTarget) {
            AttrKeyTarget attrValueTarget = (AttrKeyTarget) extractor;
            return String.format("attr-key[$=%s]", attrValueTarget.attrValue);
        }
        throw new IllegalArgumentException("unknown type extractor");
    }

    class AttrValueTarget implements TextExtractor {
        private String attrKey;

        AttrValueTarget(String attrKey) {
            this.attrKey = attrKey;
        }

        @Override
        public String extract(Element subject) {
            return subject.hasAttr(attrKey) ? subject.attr(attrKey) : null;
        }

        @Override
        public String toString() {
            return String.format("attr-value[%s=$]", attrKey);
        }
    }

    class AttrKeyTarget implements TextExtractor {
        private String attrValue;

        public AttrKeyTarget(String attrValue) {
            this.attrValue = attrValue;
        }

        @Override
        public String extract(Element subject) {
            String target = null;
            for (Attribute attribute : subject.attributes()) {
                if (attribute.getValue().equals(attrValue)) {
                    target = attribute.getKey();
                    break;
                }
            }
            return target;
        }

        @Override
        public String toString() {
            return String.format("attr-key[$=%s]", attrValue);
        }
    }

}
