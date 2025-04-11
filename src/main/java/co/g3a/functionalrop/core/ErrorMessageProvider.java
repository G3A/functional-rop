package co.g3a.functionalrop.errors;

import java.util.ResourceBundle;

/**
 * Proveedor de mensajes de error internacionalizados.
 * <p>
 * Carga mensajes desde un properties según el locale indicado.
 */
public class ErrorMessageProvider {

    private final ResourceBundle bundle;

    /**
     * Crea un proveedor para el locale indicado.
     *
     * @param locale código de locale (es, en, fr, etc.)
     */
    public ErrorMessageProvider(String locale) {
        this.bundle = ResourceBundle.getBundle("errors", new java.util.Locale(locale));
    }

    /**
     * Obtiene el mensaje asociado a la clave.
     * <p>
     * Si no existe, retorna la misma clave.
     *
     * @param key identificador del mensaje
     * @return mensaje localizado o la clave si no se encuentra
     */
    public String get(String key) {
        return bundle.containsKey(key) ? bundle.getString(key) : key;
    }
}