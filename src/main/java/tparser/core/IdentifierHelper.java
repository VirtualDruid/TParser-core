package tparser.core;

import com.google.code.regexp.Matcher;
import com.google.code.regexp.Pattern;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


class IdentifierHelper {
    private static final String GROUP_IDENTIFIER      = "identifier";
    private static final String ANNOTATION_INNER_HTML = "#";
    private static final String ANNOTATION_OUTER_HTML = "*#";
    private static final String ANNOTATION_FULL_TEXT  = "'#";

    // ^\s*\[.*\]\{.*\}\s*$
    private static final Pattern PATTERN_BASIC_TEXT_IDENTIFIER = Pattern.compile("^\\s*(?<types>\\[.*\\])?\\s*\\{(?<identifier>.*)\\}\\s*$");

    private static final String GROUP_SELECTION_ANNOTATION = "selectionAnnotation";

    // ^\s*\$?\{.*\}\s*$
    private static final Pattern PATTERN_BASIC_ATTR_IDENTIFIER = Pattern.compile("^\\s*(?<selectionAnnotation>\\$?)\\s*(?<types>\\[.*\\])?\\s*\\{(?<identifier>.*)\\}\\s*$");


    private static final String GROUP_SHOULD_FAIL_NOT_MATCH = "shouldFailNotMatch";
    private static final String GROUP_ACTUAL_REGEX          = "actualRegex";

    // ^!?\.*\$
    private static final Pattern PATTERN_REGEX_ANNOTATION = Pattern.compile("^(?<shouldFailNotMatch>!?)/(?<actualRegex>.*)/$");
    private static final String  GROUP_PROPERTY           = "property";
    private static final String  GROUP_TYPES              = "types";

    // ^(#|\*#|'#)?(.*)$
    private static final Pattern PATTERN_ACTUAL_IDENTIFIER = Pattern.compile("^((#)|(\\*#)|('#))?(?<property>.*)$");

    // ^\s\[(.*)\]$
    private static final Pattern PATTERN_ARRAY_LITERAL = Pattern.compile("^\\s*\\[(?<items>.*)\\]\\s*$");
    private static final String  GROUP_ITEMS           = "items";

    static Matcher matchTextIdentifier(String text) {
        return PATTERN_BASIC_TEXT_IDENTIFIER.matcher(text);
    }

    static Matcher matchAttrIdentifier(String text) {
        return PATTERN_BASIC_ATTR_IDENTIFIER.matcher(text);
    }


    static boolean hasSelectionAnnotation(Matcher matchAttrIdentifier) {
        return groupExists(matchAttrIdentifier, GROUP_SELECTION_ANNOTATION);
    }

    private static String getActualIdentifier(Matcher matchIdentifier) {
        return matchIdentifier.group(GROUP_IDENTIFIER).trim();
    }

    private static Matcher matchRegexAnnotation(String identifier) {
        return PATTERN_REGEX_ANNOTATION.matcher(identifier);
    }

    private static String getPropertyName(String simpleIdentifier) {
        Matcher matcher = PATTERN_ACTUAL_IDENTIFIER.matcher(simpleIdentifier);
        matcher.matches();
        return matcher.group(GROUP_PROPERTY);
    }

    private static String getActualRegex(Matcher matchRegex) {
        return matchRegex.group(GROUP_ACTUAL_REGEX);
    }

    private static boolean shouldFailNotMatch(Matcher matchRegex) {
        return groupExists(matchRegex, GROUP_SHOULD_FAIL_NOT_MATCH);
    }

    private static boolean groupExists(Matcher matcher, String groupName) {
        String group = matcher.group(groupName);
        return group != null && !group.isEmpty();
    }

    private static boolean isInnerHtml(String actualIdentifier) {
        return actualIdentifier.startsWith(ANNOTATION_INNER_HTML);
    }

    private static boolean isOuterHtml(String actualIdentifier) {
        return actualIdentifier.startsWith(ANNOTATION_OUTER_HTML);
    }

    private static boolean isFullText(String actualIdentifier) {
        return actualIdentifier.startsWith(ANNOTATION_FULL_TEXT);
    }

    private static String getTypeArrayLiteral(Matcher matcher) {
        return matcher.group(GROUP_TYPES);
    }

    private static boolean hasTypes(Matcher matcher) {
        return groupExists(matcher, GROUP_TYPES);
    }

    private static String[] parseArray(String arrayLiteral) {
        Matcher matcher = PATTERN_ARRAY_LITERAL.matcher(arrayLiteral);
        matcher.matches();
        String[] items = matcher.group(GROUP_ITEMS).split(",");
        for (int i = 0, itemsLength = items.length; i < itemsLength; i++) {
            //ignore whitespace
            items[i] = items[i].trim();
        }
        return items;
    }

    //should check matches() first
    static ExtractionProcessor createElementTextProcessor(
            Matcher matchTextIdentifier,
            StructPlaceHolderVisitor structure,
            TextConverterFactory factory) {

        String        identifier = getActualIdentifier(matchTextIdentifier);
        TextExtractor extractor;
        if (isInnerHtml(identifier)) {
            extractor = TextExtractor.innerHtml;
        } else if (isOuterHtml(identifier)) {
            extractor = TextExtractor.outerHtml;
        } else if (isFullText(identifier)) {
            extractor = TextExtractor.fullText;
        } else {
            extractor = TextExtractor.ownText;
        }
        return createExtractionProcessor(matchTextIdentifier, structure, extractor, factory);
    }

    static ExtractionProcessor createAttrProcessor(
            Matcher matchAttrIdentifier,
            boolean isKeyTarget,
            Map.Entry<String, String> attribute,
            StructPlaceHolderVisitor structure,
            TextConverterFactory factory) {

        TextExtractor extractor = isKeyTarget ?
                new TextExtractor.AttrKeyTarget(attribute.getValue()) :
                new TextExtractor.AttrValueTarget(attribute.getKey());
        return createExtractionProcessor(matchAttrIdentifier, structure, extractor, factory);
    }

    private static void checkTypePropertiesRange(int types, int expectLessThan) {
        if (types > expectLessThan) {
            throw new TemplateSyntaxError("types is more than properties");
        }
    }

    private static ExtractionProcessor createExtractionProcessor(
            Matcher matchIdentifier,
            StructPlaceHolderVisitor structure,
            TextExtractor extractor,
            TextConverterFactory factory) {

        String   identifier = getActualIdentifier(matchIdentifier);
        Matcher  matchRegex = matchRegexAnnotation(identifier);
        boolean  isRegex    = matchRegex.matches();
        String[] types      = hasTypes(matchIdentifier) ? parseArray(getTypeArrayLiteral(matchIdentifier)) : new String[0];
        if (isRegex) {
            Pattern regex = Pattern.compile(getActualRegex(matchRegex));

            List<String> groupNames = regex.groupNames();
            checkTypePropertiesRange(types.length, groupNames.size());

            List<TypeProcessor> processors = new ArrayList<>(groupNames.size());
            for (int i = 0, groupNamesSize = groupNames.size(); i < groupNamesSize; i++) {
                String name = groupNames.get(i);
                structure.addProperty(name);
                // add as default type(String) until fulfill all group names
                // ex: [Integer,Integer] {(?<a>)(?<b>)(?<c>)} -> a: Integer, b: Integer, c: String
                String type = i < types.length ? types[i] : Converters.defaultConverterType();

                TextConverter converter = factory.create(type);
                processors.add(new TypeProcessor(structure, name, converter, type));
            }
            return new ExtractionProcessor.RegexProcessor(extractor, regex, shouldFailNotMatch(matchRegex), processors);
        } else {
            checkTypePropertiesRange(types.length, 1);
            String type = types.length == 0 ? Converters.defaultConverterType() : types[0];
            structure.addProperty(identifier);
            return new ExtractionProcessor.SimpleProcessor(extractor, new TypeProcessor(structure, getPropertyName(identifier), factory.create(type), type));
        }

    }
}