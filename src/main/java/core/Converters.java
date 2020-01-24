package core;

import com.google.code.regexp.Pattern;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * utility class for TextConverter
 */
@SuppressWarnings("unused")
public class Converters {
    private Converters() {
    }

    //<br>
    private static final Pattern PATTERN_HTML_LINE_BREAK_TAG  = Pattern.compile("<br>");
    //<br\/> -> <br/>
    private static final Pattern PATTERN_XHTML_LINE_BREAK_TAG = Pattern.compile("<br\\/>");
    private static final String  LINE_BREAK                   = "\n";

    /*-- built-in --*/
    public static final TextConverter<String>  toString  = (text) -> text;
    public static final TextConverter<Integer> toInteger = Integer::valueOf;
    public static final TextConverter<Long>    toLong    = Long::valueOf;
    public static final TextConverter<Short>   toShort   = Short::valueOf;
    public static final TextConverter<Byte>    toByte    = Byte::valueOf;
    public static final TextConverter<Float>   toFloat   = Float::valueOf;
    public static final TextConverter<Double>  toDouble  = Double::valueOf;

    public static final TextConverter<Boolean> toBoolean = Boolean::valueOf;
    /*-- built-in --*/

    /*-- additional --*/
    public static final TextConverter<String>     htmlReplaceLinebreak  = (text) -> PATTERN_HTML_LINE_BREAK_TAG.matcher(text).replaceAll(LINE_BREAK);
    public static final TextConverter<String>     xhtmlReplaceLinebreak = (text) -> PATTERN_XHTML_LINE_BREAK_TAG.matcher(text).replaceAll(LINE_BREAK);
    public static final TextConverter<String>     discardEmpty          = (text) -> text.isEmpty() ? null : text;
    public static final TextConverter<BigInteger> toBigInteger          = BigInteger::new;
    public static final TextConverter<BigDecimal> toBigDecimal          = BigDecimal::new;
    public static final TextConverter<byte[]>     decodeBase64          = Base64.getDecoder()::decode;
    /*-- additional --*/

    /**
     * varargs version
     * <p>
     * convenient method to create a converter with series of steps and a specified type output
     *
     * @param out                    list of converters where the text will be processed
     * @param firstInput             require at least one arg to ensure method call signature
     * @param textProcessingPipeItem converter that process the final output
     * @param <T>                    the final result type
     * @return converter outputs T type
     */
    @SafeVarargs
    //all converters into array are TextConverter<String> ensured by compiler
    @SuppressWarnings("unchecked")
    public static <T> TextConverter<T> pipe(TextConverter<T> out, TextConverter<String> firstInput, TextConverter<String>... textProcessingPipeItem) {
        TextConverter[] converters = new TextConverter[textProcessingPipeItem.length + 1];
        System.arraycopy(textProcessingPipeItem, 0, converters, 1, textProcessingPipeItem.length);
        converters[0] = firstInput;
        return pipe(Arrays.asList(converters), out);
    }

    /**
     * convenient method to create a converter with series of steps and a specified type output
     *
     * @param textProcessingPipe list of converters where the text will be processed
     * @param out                converter that process the final output
     * @param <T>                the final result type
     * @return converter outputs T type
     */
    public static <T> TextConverter<T> pipe(List<TextConverter<String>> textProcessingPipe, TextConverter<T> out) {
        return new Concat<>(new Pipe(textProcessingPipe), out);
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static TextConverter<String> pipe(TextConverter<String> firstInput, TextConverter<String>... textProcessingPipeItem) {
        TextConverter[] converters = new TextConverter[textProcessingPipeItem.length + 1];
        System.arraycopy(textProcessingPipeItem, 0, converters, 1, textProcessingPipeItem.length);
        converters[0] = firstInput;
        return pipe(Arrays.asList(converters));
    }

    /**
     * convenient method to create a converter with series of steps which still outputs text
     *
     * @param textProcessingPipe list of converters where the text will be processed
     * @return converter outputs String type
     */
    public static TextConverter<String> pipe(List<TextConverter<String>> textProcessingPipe) {
        return new Pipe(textProcessingPipe);
    }

    static ConverterFactoryBuilder factoryBuilder() {
        return new ConverterFactoryBuilder();
    }

    static TextConverterFactory defaultFactory() {
        return BuiltinFactory.INSTANCE;
    }

    static String defaultConverterType() {
        return BuiltinFactory.STRING;
    }

    private static class Concat<O> implements TextConverter<O> {
        private TextConverter<String> in;
        private TextConverter<O>      out;

        Concat(TextConverter<String> in, TextConverter<O> out) {
            this.in = in;
            this.out = out;
        }

        @Override
        public O valueOf(String text) {
            return out.valueOf(in.valueOf(text));
        }
    }

    private static class Pipe implements TextConverter<String> {
        List<TextConverter<String>> textProcessingPipe;

        Pipe(List<TextConverter<String>> textProcessingPipe) {
            this.textProcessingPipe = Collections.unmodifiableList(new ArrayList<>(textProcessingPipe));
        }

        @Override
        public String valueOf(String text) {
            String result = text;
            for (TextConverter<String> converter : textProcessingPipe) {
                result = converter.valueOf(result);
            }
            return result;
        }
    }

    /**
     * basic built-in primitives converters that cannot be overwrite
     */
    private static class BuiltinFactory implements TextConverterFactory {
        static final BuiltinFactory INSTANCE;

        private static final Map<String, TextConverter> DEFAULT;

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
            Map<String, TextConverter> map = new LinkedHashMap<>(16);
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
        public TextConverter create(String type) {
            //case insensitive
            return DEFAULT.get(type.toLowerCase());
        }

        //check for builtin overwrite
        static boolean isDefault(String type) {
            //case insensitive
            return DEFAULT.containsKey(type.toLowerCase());
        }
    }

    private static class EmptyFactory implements TextConverterFactory {
        static final TextConverterFactory INSTANCE = new EmptyFactory();

        private EmptyFactory() {
        }

        @Override
        public TextConverter create(String type) {
            return null;
        }
    }

    private static class InternalFactory implements TextConverterFactory {
        private TextConverterFactory       defaultFactory = BuiltinFactory.INSTANCE;
        private Map<String, TextConverter> additional;
        private TextConverterFactory       additionalFactory;


        InternalFactory(Map<String, TextConverter> additional, TextConverterFactory additionalFactory) {
            this.additional = Collections.unmodifiableMap(additional);
            this.additionalFactory = additionalFactory;
        }

        @Override
        public TextConverter create(String type) {
            TextConverter converter;
            if ((converter = defaultFactory.create(type)) != null) {
                return converter;
            }
            if ((converter = additional.get(type)) != null) {
                return converter;
            }
            if ((converter = additionalFactory.create(type)) != null) {
                return converter;
            }
            throw new TemplateSyntaxError(String.format("unknown converter type: %s", type));
        }

    }

    static final class ConverterFactoryBuilder {
        private TextConverterFactory additionalFactory = EmptyFactory.INSTANCE;

        private Map<String, TextConverter> map = new HashMap<>();

        void registerFactory(TextConverterFactory factory) {
            additionalFactory = factory;
        }

        void register(String typeName, TextConverter converter) {
            if (BuiltinFactory.isDefault(typeName)) {
                throw new TemplateSyntaxError(String.format("cannot overwrite builtin type: %s", typeName.toLowerCase()));
            }
            map.put(typeName, converter);
        }

        TextConverterFactory build() {
            if (additionalFactory == EmptyFactory.INSTANCE && map.size() == 0) {
                return BuiltinFactory.INSTANCE;
            }
            Map<String, TextConverter> hold = new HashMap<>(CollectionHelper.enoughHashTableCapacity(map.size()));
            hold.putAll(map);
            return new InternalFactory(hold, additionalFactory);
        }

    }
}
