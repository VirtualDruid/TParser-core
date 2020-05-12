package tparser.core;

import com.google.code.regexp.Pattern;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * utility class for TextConverter
 */
@SuppressWarnings({"unused", "WeakerAccess"})


public class Converters {
    private Converters() {
    }

    /**
     * the 'Redundant Escape' must not be removed
     * the java Regex is platform sensitive since it may use different Regex Engine
     * <p>
     * on the Android platform
     * the missing escape will result in throwing PatterSyntaxException by ICU Engine
     */
    //<br>
    private static final Pattern PATTERN_HTML_LINE_BREAK_TAG  = Pattern.compile("<br>");
    //<br\/> -> <br/>
    private static final Pattern PATTERN_XHTML_LINE_BREAK_TAG = Pattern.compile("<br\\/>");
    //<br/?>
    private static final Pattern PATTERN_ANY_LINE_BREAK_TAG   = Pattern.compile("<br\\/?>");
    private static final String  LINE_BREAK                   = "\n";

    /*--  built-in  --*/
    /**
     * these are a part of public api to construct advanced converter or aliasing defaults
     */
    public static final TextConverter<String>  toString  = (text, _element) -> text;
    public static final TextConverter<Integer> toInteger = (text, _element) -> Integer.valueOf(text);
    public static final TextConverter<Long>    toLong    = (text, _element) -> Long.valueOf(text);
    public static final TextConverter<Short>   toShort   = (text, _element) -> Short.valueOf(text);
    public static final TextConverter<Byte>    toByte    = (text, _element) -> Byte.valueOf(text);
    public static final TextConverter<Float>   toFloat   = (text, _element) -> Float.valueOf(text);
    public static final TextConverter<Double>  toDouble  = (text, _element) -> Double.valueOf(text);
    public static final TextConverter<Boolean> toBoolean = (text, _element) -> Boolean.valueOf(text);
    /*--  built-in  --*/

    /*-- additional --*/
    public static final TextConverter<String> replaceAnyLineBreak   = (text, _element) -> PATTERN_ANY_LINE_BREAK_TAG.matcher(text).replaceAll(LINE_BREAK);
    public static final TextConverter<String> htmlReplaceLinebreak  = (text, _element) -> PATTERN_HTML_LINE_BREAK_TAG.matcher(text).replaceAll(LINE_BREAK);
    public static final TextConverter<String> xhtmlReplaceLinebreak = (text, _element) -> PATTERN_XHTML_LINE_BREAK_TAG.matcher(text).replaceAll(LINE_BREAK);

    public static final TextConverter<String>     emptyAsNull = (text, _element) -> text.isEmpty() ? null : text;
    public static final NullableConverter<String> nullAsEmpty = (text, _element) -> text == null ? "" : text;

    public static final TextConverter<BigInteger> toBigInteger = (text, _element) -> new BigInteger(text);
    public static final TextConverter<BigDecimal> toBigDecimal = (text, _element) -> new BigDecimal(text);

    public static final TextConverter<byte[]> decodeBase64 = (text, _element) -> Base64.getDecoder().decode(text);

    public static final NullableConverter<Boolean> textNotNull           = (text, _element) -> text != null;
    public static final NullableConverter<Boolean> elementExists         = (_text, context) -> context != null;
    /*-- additional --*/

//    /**
//     * varargs version
//     * <p>
//     * convenient method to create a converter with series of steps and a specified type output
//     *
//     * @param out                    list of converters where the text will be processed
//     * @param firstInput             require at least one arg to ensure method call signature
//     * @param textProcessingPipeItem converter that process the final output
//     * @param <T>                    the final result type
//     * @return converter outputs T type
//     */
//    @SafeVarargs
//    //all converters into array are TextConverter<String> ensured by compiler
//    @SuppressWarnings("unchecked")
//    public static <T> TextConverter<T> pipe(TextConverter<T> out, TextConverter<String> firstInput, TextConverter<String>... textProcessingPipeItem) {
//        TextConverter[] converters = new TextConverter[textProcessingPipeItem.length + 1];
//        System.arraycopy(textProcessingPipeItem, 0, converters, 1, textProcessingPipeItem.length);
//        converters[0] = firstInput;
//        return pipe(Arrays.asList(converters), out);
//    }

//    /**
//     * convenient method to create a converter with series of steps and a specified type output
//     *
//     * @param textProcessingPipe list of converters where the text will be processed
//     * @param out                converter that process the final output
//     * @param <T>                the final result type
//     * @return converter outputs T type
//     */
//    @SuppressWarnings("WeakerAccess")
//    public static <T> TextConverter<T> pipe(List<TextConverter<String>> textProcessingPipe, TextConverter<T> out) {
//        return new Concat<>(new Pipe(textProcessingPipe), out);
//    }
//
//    @SafeVarargs
//    @SuppressWarnings("unchecked")
//    public static TextConverter<String> pipe(TextConverter<String> firstInput, TextConverter<String>... textProcessingPipeItem) {
//        TextConverter[] converters = new TextConverter[textProcessingPipeItem.length + 1];
//        System.arraycopy(textProcessingPipeItem, 0, converters, 1, textProcessingPipeItem.length);
//        converters[0] = firstInput;
//        return pipe(Arrays.asList(converters));
//    }

//    /**
//     * convenient method to create a converter with series of steps which still outputs text
//     *
//     * @param textProcessingPipe list of converters where the text will be processed
//     * @return converter outputs String type
//     */
//    @SuppressWarnings("WeakerAccess")
//    public static TextConverter<String> pipe(List<TextConverter<String>> textProcessingPipe) {
//        return new Pipe(textProcessingPipe);
//    }

    static ConverterFactoryBuilder factoryBuilder() {
        return new ConverterFactoryBuilder();
    }

    static ConverterFactory defaultFactory() {
        return BuiltinFactory.INSTANCE;
    }

    static String defaultConverterType() {
        return BuiltinFactory.STRING;
    }

//    private static class Concat<O> implements TextConverter<O> {
//        private TextConverter<String> in;
//        private TextConverter<O>      out;
//
//        Concat(TextConverter<String> in, TextConverter<O> out) {
//            this.in = in;
//            this.out = out;
//        }
//
//        @Override
//        public O valueOf(String text) {
//            return out.valueOf(in.valueOf(text));
//        }
//    }
//
//    private static class Pipe implements TextConverter<String> {
//        List<TextConverter<String>> textProcessingPipe;
//
//        Pipe(List<TextConverter<String>> textProcessingPipe) {
//            this.textProcessingPipe = Collections.unmodifiableList(new ArrayList<>(textProcessingPipe));
//        }
//
//        @Override
//        public String valueOf(String text) {
//            String result = text;
//            for (TextConverter<String> converter : textProcessingPipe) {
//                result = converter.valueOf(result);
//            }
//            return result;
//        }
//    }

    /**
     * basic built-in primitives converters that cannot be overwrite
     */
    private static class BuiltinFactory implements ConverterFactory {
        static final BuiltinFactory INSTANCE;

        private static final Map<String, Converter> DEFAULT;

        /*---- primitives ----*/
        private static String STRING  = lowerCaseType(String.class);
        private static String INTEGER = lowerCaseType(Integer.class);
        //alternative
        private static String INT     = int.class.getSimpleName();

        private static String LONG   = lowerCaseType(Long.class);
        private static String SHORT  = lowerCaseType(Short.class);
        private static String BYTE   = lowerCaseType(Byte.class);
        private static String FLOAT  = lowerCaseType(Float.class);
        private static String DOUBLE = lowerCaseType(Double.class);

        private static String BOOLEAN = lowerCaseType(Boolean.class);

        /*---- primitives ----*/
        private static String lowerCaseType(Class type) {
            return type.getSimpleName().toLowerCase();
        }

        static {
            Map<String, Converter> map = new LinkedHashMap<>(16);
            map.put(STRING, toString);

            map.put(INTEGER, toInteger);
            map.put(INT, toInteger);

            map.put(LONG, toLong);
            map.put(SHORT, toShort);
            map.put(BYTE, toByte);
            map.put(FLOAT, toFloat);
            map.put(DOUBLE, toDouble);

            map.put(BOOLEAN, toBoolean);
            DEFAULT = Collections.unmodifiableMap(map);
            INSTANCE = new BuiltinFactory();
        }


        @Override
        public Converter create(String type) {
            //case insensitive
            return DEFAULT.get(type.toLowerCase());
        }

        //check for builtin overwrite
        static boolean isDefault(String type) {
            //case insensitive
            return DEFAULT.containsKey(type.toLowerCase());
        }
    }

    private static class EmptyFactory implements ConverterFactory {
        static final ConverterFactory INSTANCE = new EmptyFactory();

        private EmptyFactory() {
        }

        @Override
        public Converter create(String type) {
            return null;
        }
    }

    private static class InternalFactory implements ConverterFactory {
        private ConverterFactory       defaultFactory = BuiltinFactory.INSTANCE;
        private Map<String, Converter> additional;
        private ConverterFactory       additionalFactory;


        InternalFactory(Map<String, Converter> additional, ConverterFactory additionalFactory) {
            this.additional = Collections.unmodifiableMap(additional);
            this.additionalFactory = additionalFactory;
        }

        @Override
        public Converter create(String type) {
            Converter converter;
            if ((converter = defaultFactory.create(type)) != null) {
                return converter;
            }
            if ((converter = additional.get(type)) != null) {
                return converter;
            }
            if ((converter = additionalFactory.create(type)) != null) {
                return converter;
            }
            return null;
        }

    }

    static final class ConverterFactoryBuilder {
        private ConverterFactory additionalFactory = EmptyFactory.INSTANCE;

        private Map<String, Converter> map = new HashMap<>();

        void registerFactory(ConverterFactory factory) {
            additionalFactory = factory;
        }

        void register(String typeName, Converter converter) {
            if (BuiltinFactory.isDefault(typeName)) {
                throw new TemplateSyntaxError(String.format("cannot overwrite builtin type: %s", typeName.toLowerCase()));
            }
            map.put(typeName, converter);
        }

        ConverterFactory build() {
            if (additionalFactory == EmptyFactory.INSTANCE && map.size() == 0) {
                return BuiltinFactory.INSTANCE;
            }
            Map<String, Converter> hold = new HashMap<>(CollectionHelper.enoughHashTableCapacity(map.size()));
            hold.putAll(map);
            return new InternalFactory(hold, additionalFactory);
        }

    }
}
