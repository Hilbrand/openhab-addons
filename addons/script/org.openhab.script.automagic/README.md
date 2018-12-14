# <bindingName> Binding


```(js)
scriptExtension.importPreset("Automagic");
```

```(js)
var logger = NamedLogger('my.custom.rule');

logger.trace('trace message');
logger.debug('debug message');
logger.info('info message');
logger.warn('warning message');
logger.error('error message');
```


```
var helloWorkRule =
    RuleBuilder.create("helloWorld")
        .withName("Hello World")
        .withTriggers(CronTrigger("0 * * * * ?"))
        .withActions(SimpleAction({
            execute: function(module, input) {
                logger.info("This is a 'hello world!' from a Javascript rule.");
            }))
        .build();

automationManager.addRule(helloWorkRule);
```