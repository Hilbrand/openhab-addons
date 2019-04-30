package org.openhab.binding.exec.internal.handler;

import java.util.Calendar;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.lang.text.StrLookup;
import org.eclipse.smarthome.core.items.ItemNotFoundException;

class ExecStrLookup extends StrLookup {

    private String execInput;

    public void setInput(String input) {
        this.execInput = input;
    }

    @Override
    public String lookup(String key) {
        String transform = null;
        String format = null;

        String parts[] = key.split(":");
        String subkey = parts[0];

        if (parts.length == 2) {
            format = parts[1];
        } else if (parts.length == 3) {
            transform = parts[1];
            format = parts[2];
        }

        try {
            if ("exec-time".equals(subkey)) {
                // Transform is not relevant here, as our source is a Date
                if (format != null) {
                    return String.format(format, Calendar.getInstance().getTime());
                } else {
                    return null;
                }
            } else if ("exec-input".equals(subkey)) {
                if (execInput != null) {
                    String transformedInput = execInput;
                    if (transform != null) {
                        transformedInput = transformString(transformedInput, transform);
                    }
                    if (format != null) {
                        return String.format(format, transformedInput);
                    } else {
                        return execInput;
                    }
                } else {
                    return null;
                }
            } else {
                String transformedInput = itemRegistry.getItem(subkey).getState().toString();
                if (transform != null) {
                    transformedInput = transformString(transformedInput, transform);
                }
                if (format != null) {
                    return String.format(format, transformedInput);
                } else {
                    return transformedInput;
                }
            }
        } catch (PatternSyntaxException e) {
            logger.warn("Invalid substitution key '{}'", key);
            return null;
        } catch (ItemNotFoundException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("The Item '{}' could not be found in the Registry : '{}'", subkey, e.getMessage(), e);
            }
            return null;
        }
    }

}