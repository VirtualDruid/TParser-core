package tparser.core;

import com.google.code.regexp.Matcher;
import com.google.code.regexp.Pattern;
import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Element;

import java.util.Collections;
import java.util.List;
import java.util.regex.PatternSyntaxException;

abstract class ExtractionProcessor {
    TextExtractor preExtractor;

    ExtractionProcessor(TextExtractor preExtractor) {
        this.preExtractor = preExtractor;
    }

    abstract <JO, JA> void process(ParseResult<JO, JA> state, Element element, int index)
            throws HtmlParseException;


    static class RegexProcessor extends ExtractionProcessor {
        //need access to named group names
        private Pattern             pattern;
        private List<TypeProcessor> typeProcessors;
        private boolean             shouldFailNotMatch;

        RegexProcessor(
                TextExtractor preExtractor,
                Pattern pattern,
                boolean shouldFailNotMatch,
                List<TypeProcessor> processors

        ) throws PatternSyntaxException {

            super(preExtractor);
            this.pattern = pattern;
//            pattern = Pattern.compile(regex);
//            ArrayList<TypeProcessor> processors = new ArrayList<>();
//            List<String> groupNames = pattern.groupNames();
//            checkTypePropertiesRange(types.size(), groupNames.size());
//            for (String groupName : groupNames) {
//                structure.addProperty(groupName);
//            }
//            for (int i = 0, typesSize = types.size(); i < typesSize; i++) {
//                processors.add(new TypeProcessor(structure, groupNames.get(i), types.get(i)));
//            }
//            for (int i = types.size(), end = groupNames.size(); i < end; i++) {
//                //add as default type(String) until fulfill all group names

//                processors.add(new DefaultProcessor(structure, groupNames.get(i)));
//            }
//            assert processors.size() == groupNames.size();
            this.typeProcessors = Collections.unmodifiableList(processors);
            this.shouldFailNotMatch = shouldFailNotMatch;
        }

        @Override
        final <JO, JA> void process(ParseResult<JO, JA> state, Element element, int index)
                throws HtmlParseException.RegexNotMatch {

//            String  extraction = preExtractor.extract(element);
//            Matcher matcher    = pattern.matcher(extraction);
//            boolean found      = matcher.find();
//            if (shouldFailNotMatch && !found) {
//                throw new HtmlParseException.RegexNotMatch(String.format("pattern: %s not match", pattern.toString()));
//            }
            final boolean isElementNull = NullWrapper.isNullRepresent(element);
            String        extraction    = null;
            boolean       found         = false;
            Matcher       matcher       = null;
            if (!isElementNull) {
                extraction = preExtractor.extract(element);
                matcher = pattern.matcher(extraction);
                found = matcher.find();
                if (shouldFailNotMatch && !found) {
                    throw new HtmlParseException.RegexNotMatch(String.format("pattern: %s not match", pattern.toString()));
                }
            }
            boolean shouldNull = extraction == null || !found;
            for (TypeProcessor processor : typeProcessors) {
                String result = shouldNull ? null : matcher.group(processor.property);
                processor.process(state, result, isElementNull ? null : element, index);
            }
        }


        @Override
        public String toString() {
            return String.format("%s %s AS: %s", TextExtractor.getTypeTag(preExtractor), pattern.toString(), StringUtil.join(typeProcessors, ","));
        }
    }

    static class SimpleProcessor extends ExtractionProcessor {

        TypeProcessor processor;

        SimpleProcessor(TextExtractor preExtractor, TypeProcessor processor) {
            super(preExtractor);
            this.processor = processor;
        }


        @Override
        <JO, JA> void process(ParseResult<JO, JA> state, Element element, int index) {
            boolean isNull = NullWrapper.isNullRepresent(element);
            String  result = isNull ? null : preExtractor.extract(element);
            processor.process(state, result, isNull ? null : element, index);
        }

        @Override
        public String toString() {
            return String.format("%s AS: %s", TextExtractor.getTypeTag(preExtractor), processor.toString());
        }
    }

}
