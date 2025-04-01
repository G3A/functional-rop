package co.g3a.functionalrop;

import java.util.ResourceBundle;

public class ErrorMessageProvider {
    private final ResourceBundle bundle;

    public ErrorMessageProvider(String locale) {
        this.bundle = ResourceBundle.getBundle("errors", new java.util.Locale(locale));
    }

    public String get(String key) {
        return bundle.containsKey(key) ? bundle.getString(key) : key;
    }
}