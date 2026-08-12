package com.miguelcode.warrantyvault.domain.usecase

/**
 * Se alcanzó el tope de garantías del plan gratuito al intentar insertar.
 *
 * Tipada para que la pantalla pueda mostrar el mismo mensaje localizable que ya usa el
 * guard de la UI, en vez del texto crudo de una excepción genérica.
 */
class PremiumLimitReachedException : Exception("Free warranty limit reached")
