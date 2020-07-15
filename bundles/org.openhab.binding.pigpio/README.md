# PiGPIO Binding


## Supported Things


## Discovery


## Binding Configuration

## Thing Configuration

## Channels


| channel  | type   | description                  |
|----------|--------|------------------------------|
| control  | Switch | This is the control channel  |

## Full Example


```
Thing pigpio:mcp23017:chipA  "MCP23017 chip A" [address=20,bus=1] {
    Channels:
        Type input_pin : input#A0 [pull_up=false]
        Type input_pin : input#A1 [pull_up=false, active_low=true]

        Type output_pin : output#A6 [default_state="HIGH"]
        Type output_pin : output#A7 [default_state="LOW"]
}

```

### Items example

```
Switch living_room_led_switch_1 "Living room switch 1"  {channel="mcp23017:mcp23017:chipA:input#A0"}
Switch living_room_led_switch_2 "Living room switch 2"  {channel="mcp23017:mcp23017:chipA:input#A1"}
Switch living_room_led_1 "Living room LED 1"  {channel="mcp23017:mcp23017:chipA:output#A6"}
Switch living_room_led_2 "Living room LED 2"  {channel="mcp23017:mcp23017:chipA:output#A7"}
```

```
rule "blink"
when switch_A6 changed to ON
then
    val pigpioActions = getActions("pigpio", "pigpio:raspi:raspi")

    pigpioActions.blink("out#6", 1000, 5000)
end

```