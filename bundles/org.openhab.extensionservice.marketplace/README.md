# Eclipse marketplace Extension service

This is an `ExtensionService`, which accesses the Eclipse IoT Marketplace and makes its content available as
extensions.

The internally used http address is "https://marketplace.eclipse.org/taxonomy/term/4988%2C4396/api/p?client=org.eclipse.smarthome".

The default refresh value is 1 hour.
If the eclipse server is not responding, another attempt happens 60 seconds later.

## Marketplace format

Structure
```
|-repository.json
|-Example 1
|       \__addon.json
|        |_logo.png
|-Binding 2
|       \__addon.json
|        |_logo.png
...
```

`respository.json`

```json
{
  "name": "openHAB Marketplace Add-ons",
  "url": "https://openhab.community/addons",
  "maintainer": "John Do"
}
```

`addon.json`

```json
{
  "name" : "Example",
  "description" : "This is an example binding.",
  "url" : "http://mydomain",  // optional
  "license" : "EPL v2.0 ",
  "maintainer" : "My Name",
  "package_type" :
  "package_format": "kar",
  "packages" [
    { 
      "description" : "",
      "version" : "1.0",
      "version_compatibility" : {
        "from" : "2.5.0",
        "to" : "2.5.*"
       }
      "package_url" : "https://mydomain/mydownloads/my.cool.binding-1.0.kar",
      "readme_url" : "https://mydomain/mycoolbinding/1.0/readme.html",
    },
    { 
      "name" : "",
      "description" : "",
      "version" : "2.0",
      "version_compatibility" : {
        "from" : "3.0",
       }
      "package_url" : "https://mydomain/mydownloads/my.cool.binding-2.0.kar",
      "readme_url" : "https://mydomain/mycoolbinding/2.0/readme.html",
    }
  ]
}
  
    
  
  
  "version": "3.4.0",
  "slug": "example",
  "description": "Example add-on by Community Hass.io Add-ons",
  "url": "https://github.com/hassio-addons/addon-example",
  "startup": "application",
  "init": false,
  "arch": [
    "aarch64",
    "amd64",
    "armhf",
    "armv7",
    "i386"
  ],
  "boot": "auto",
  "hassio_api": true,
  "hassio_role": "default",
  "options": {
    "log_level": "info",
    "seconds_between_quotes": 5
  },
  "schema": {
    "log_level": "list(trace|debug|info|notice|warning|error|fatal)",
    "seconds_between_quotes": "int(1,120)"
  },
  "image": "hassioaddons/example-{arch}"
}```



The received extensions description is XML. The schema is like in the following example:

```xml
<marketplace>
  <category id="4988" marketid="4396" name="Eclipse SmartHome" url="https://marketplace.eclipse.org/category/markets/iot">
    <node id="3305842" name="Energy Meter" url="https://marketplace.eclipse.org/content/energy-meter">
        <type>iot_package</type>
        <categories>
            <category id="4988" name="Eclipse SmartHome" url="https://marketplace.eclipse.org/category/categories/eclipse-smarthome"/>
        </categories>
        <owner>Kai Kreuzer</owner>
        <favorited>0</favorited>
        <installstotal>0</installstotal>
        <installsrecent>0</installsrecent>
        <shortdescription>
            Desc
        </shortdescription>
        <body>
            Example body
        </body>
        <created>1487690446</created>
        <changed>1489494314</changed>
        <foundationmember>1</foundationmember>
        <homepageurl>https://www.openhabfoundation.org</homepageurl>
        <image>
            https://marketplace.eclipse.org/sites/default/files/styles/ds_medium/public/iot-package/logo/heating.png?itok=qMbbIXEU
        </image>
        <license>EPL</license>
        <companyname>
            openHAB Foundation
        </companyname>
        <status>Alpha</status>
        <supporturl></supporturl>
        <version/>
        <updateurl>
            https://raw.githubusercontent.com/kaikreuzer/esh-templates/master/energymeter.json
        </updateurl>
        <packagetypes>rule_template</packagetypes>
        <sourceurl/>
        <versioncompatibility>
        <from/>
        <to/>
        </versioncompatibility>
        <packageformat>json</packageformat>
    </node>
  </category>
</marketplace>
```
