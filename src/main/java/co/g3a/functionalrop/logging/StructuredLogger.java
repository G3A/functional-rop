package co.g3a.functionalrop.logging;

import java.util.Map;

/**
 * Contrato para logging estructurado.
 * <p>
 * Permite registrar eventos con contexto estructurado (clave-valor).
 */
public interface StructuredLogger {

    /**
     * Registra un evento con metadatos estructurados.
     *
     * @param eventName nombre del evento (ej: "user_created", "payment_processed")
     * @param context   mapa con detalles adicionales (duration_ms, status, error, etc.)
     */
    void log(String eventName, Map<String, Object> context);
}