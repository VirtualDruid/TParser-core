# TParser-core

TParser is a simple spider/crawler middleware built on top of **Jsoup** that **extracts and structures data (as JSON)** from DOM tree by simply define a template file without writing boilerplate parser code.

## Features

1. Extract text as object property/field via **Named Regex Groups**.

2. Structure extracted data as any level of nested objects, arrays.

3. Convert text/element to any type, with extendable converters.

4. Output data as any format by implementing **JSONDelegate**.

5. Automatic Null-Proof whenever target element is missing, 
and you may use **x-required** or **x-fail-if-found** to quick fail parsing unexpected htmls' content.


**Download: via Jitpack**
```groovy
allprojects {
    repositories {
        //configure jitpack
        maven { url 'https://jitpack.io' }
    }
}
```
```groovy

dependencies {
    //latest version
    implementation 'com.github.VirtualDruid:TParser-core:master-SNAPSHOT'
    implementation 'com.github.VirtualDruid:TParser-delegate-jackson:master-SNAPSHOT'
    
    //dependency of jackson delegate
    implementation group: 'com.fasterxml.jackson.core', name: 'jackson-databind', version: '2.10.0'
    implementation group: 'com.fasterxml.jackson.core', name: 'jackson-core', version: '2.10.0'
    implementation group: 'com.fasterxml.jackson.core', name: 'jackson-annotations', version: '2.10.0'
}
```

## How to use

1. write a template:
```html
<!--example of parsing packages info on https://mvnrepository.com/open-source-->

<!--by default, it only select a element in children of its parent-->
<!--use x-select-in-subtree="true" extension attr to make it select in subtree-->
<div id="page" x-select-in-subtree="true">
    
    <div id="maincontent">
        <!--use json-object or json-array for structuring result-->
        <json-array>
            
            <!-- write a element the template should selected, it will create a default css-selector that match it-->
            <!-- use 'x-' extension attr to combine default css-selector with another-->
            
            <!-- there are 3 combination-->
            
            <!-- 1. 'x-and-selector'                 : select elements match both default and another-->
            <!-- 2. 'x-or-selector'                  : select elements match default or another -->
            <!-- 3. 'x-overwrite-default-selector'   : select elements match only another(ignore default)-->
            
            <div x-and-selector=":not([class])">
                <h4 style="border-bottom: 1px solid gray; padding-bottom: 2px">
                    
                    <!-- use {<extractionPrefix>propertyName} on an element attr or text to extract it into result-->
                    
                    <!-- by default, attr with extraction is ignored and not considered a part of css-selector-->
                    <!-- ex: <a href="{href}" rel="blank"> -> a[rel=blank] -->
                    
                    <!-- use '$' on attr extraction to make it a part of css-selector -->
                    <!-- ex: <a href="${href}" rel="blank"> -> a[href][rel=blank] -->
                    
                    <!-- (own text) extraction prefixes: -->
                    
                    <!-- 1. {#propertyName} : extract text from this element as inner html  (subtree without self) -->
                    <!-- 2. {'#propertyName} : extract text from element as full text  (with all text blocks but without html tags) -->
                    <!-- 3. {*#propertyName} : extract text from element as outer html (subtree with self) -->
                    <!-- 4. {!#proertyName} :  extract nothing  (for implementing custom text extraction)-->
                    
                    <!-- if a property is outside a structuring tag (json-array/json-object), a TemplateSyntaxError will be thrown -->
                    
                    <a href="${categoryUrl}">{categoryName}</a>
                    
                    <!--use regex to get named groups as json properties and convert it to specific type-->
                    
                    <!-- ['typeA','typeB'..] {/'regex with named groups'/} -->
                    
                    <!--use CDATA without html escape is possible-->
                    <!CDATA[[
                    
                    [int]{/\((?<artifactCount>\d+)\)/}
                    
                    ]]>
                    <!-- <b>[int]{/\((?&lt;artifactCount&gt;\d+)\)/}</b> -->
                </h4>
                <table width="100%">
                    <tbody>
                        <tr x-and-selector=":last-child">
                            <td x-and-selector=":has(b)">
                                <b>
                                    <a href="{moreArtifactsUrl}"></a>
                                </b>
                            </td>
                        </tr>
                        <json-array name="artifacts">
                            <tr x-and-selector=":not(:last-child)">
                                <td style="width: 1.5em">
                                    <img class="im-logo" x-select-in-subtree="true"
                                         src="{logoImageUrl}"/>
                                </td>
                                <a href="${artifactUrl}" x-select-in-subtree="true">{artifactName}</a>
                                <td style="text-align: right; padding-right: 12px">
                                    <json-array name="licenses">
                                        <span class="b lic im-lic">{license}</span>
                                    </json-array>
                                </td>
                                <td style="width: 7em;">
                                
                                    <!--using custom converter to convert integer expression that has separator-->
                                    <a href="${usageListUrl}">[CommaSeparatorInt]{usageCount}</a>
                                    
                                </td>
                            </tr>
                        </json-array>
                    </tbody>
                </table>
            </div>
        </json-array>
    </div>
</div>

```

in code:
```java
//prepare json library support
//a JsonDelegate is used to wrap libraries to operate json nodes 

ObjectMapper objectMapper = new ObjectMapper();
JsonDelegate<ObjectNode,ArrayNode> delegate = new JacksonDelegate(objectMapper);

//load template file

Element templateSource = Jsoup.parse(templateFile, utf8, "", Parser.xmlParser());

//templates are reusable and immutable, and can be concurrently shared
//register custom converter with type name on template with a TemplateBuilder

Template tparserTemplate = new TemplateBuilder(templateSource)
            .registerConverter("CommaSeparatorInt", new Converter<Integer>() {
                @Override
                public Integer convert(String text, Element contextElement){
                  return Integer.valueOf(text.replace(",",""))
                }
            })
            .build();

//extract html data as json
Document input = ... ;
ArrayNode jsonResult = tparserTemplate.parse(input,delegate).getResultArray();

print(objectMapper.writeValueAsString(jsonResult))

```

test output result:
```json

[
  {
    "categoryName": "Testing Frameworks",
    "categoryUrl": "/open-source/testing-frameworks",
    "artifactCount": 51,
    "moreArtifactsUrl": "/open-source/testing-frameworks",
    "artifacts": [
      {
        "logoImageUrl": "/img/7cb2d4617d97415f562bd5711c429a95",
        "artifactName": "JUnit",
        "artifactUrl": "/artifact/junit/junit",
        "licenses": [
          {
            "license": "EPL"
          }
        ],
        "usageCount": 100417,
        "usageListUrl": "/artifact/junit/junit/usages"
      },
      {
        "logoImageUrl": "/img/7a661545feb3d061428b106572b5eba0",
        "artifactName": "ScalaTest",
        "artifactUrl": "/artifact/org.scalatest/scalatest",
        "licenses": [
          {
            "license": "Apache"
          }
        ],
        "usageCount": 13089,
        "usageListUrl": "/artifact/org.scalatest/scalatest/usages"
      }, ......
      ]
    }, ......
]
```

a hand-written bolierplate parser code in comparison (without null-proof):

https://gist.github.com/VirtualDruid/4e8c5c91f1cd8352174ba656bae1dc8f

## Extending Template Functionality

### Custom Data Converters
Implement **Converter** and register it to **TemplateBuilder** to parse specific text/element format

### Custom output format
Implement **JSONDelegate** to support other JSON lib or any other format (like XML, CSV)

## Limitation

1. **\<json-array\>** tag does not support JSON primitive array (string array, number array), all items are JSON objects.
To use primitive in your template, use **!#** with custom **Converter** that converts DOM element to primitive JSON array.

2. All **\<json-array\>** or **\<json-object\>** tags must have at least one **Selectable element** or a **TemplateSyntaxException** is thrown due to how TParser internally works

**Example:**

```html
<!--not allowed-->

<json-object>

<!--no element to be selected-->

    <json-object name="nested">
        <a/>
    </json-object>

</json-object>

```
