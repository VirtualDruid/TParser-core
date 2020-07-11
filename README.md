# TParser-core


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
    implementation 'com.github.VirtualDruid:TParser-core:933eefee24'
    implementation 'com.github.VirtualDruid:TParser-delegate-jackson:master-SNAPSHOT'
    
    //dependency of jackson delegate
    implementation group: 'com.fasterxml.jackson.core', name: 'jackson-databind', version: '2.10.0'
    implementation group: 'com.fasterxml.jackson.core', name: 'jackson-core', version: '2.10.0'
    implementation group: 'com.fasterxml.jackson.core', name: 'jackson-annotations', version: '2.10.0'
}
```

## How to use

template file:
```html
<!--parse packages info on https://mvnrepository.com/open-source-->
<div id="page" x-select-in-subtree="true">
    <div id="maincontent">
        <json-array>

            <div x-and-selector=":not([class])">
                <h4 style="border-bottom: 1px solid gray; padding-bottom: 2px">
                    <a href="${categoryUrl}">{categoryName}</a>

                    <b>[int]{/\((?&lt;artifactCount&gt;\d+)\)/}</b>
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
                                
                                    <!--use custom converter to convert integer expression that has separator-->
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
ObjectMapper objectMapper = new ObjectMapper();
JsonDelegate<ObjectNode,ArrayNode> delegate = new JacksonDelegate(objectMapper);

//prepare template
Element templateSource = Jsoup.parse('template file', utf8, "", Parser.xmlParser());

//register custom converter with type name on template
Template tparserTemplate = new TemplateBuilder(templateSource)
            .registerConverter("CommaSeparatorInt", new TextConverter<Integer>() {
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

output result:
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
