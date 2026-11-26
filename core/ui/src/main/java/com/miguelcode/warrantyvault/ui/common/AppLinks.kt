package com.miguelcode.warrantyvault.ui.common

/** Enlaces externos que usan varias pantallas (paywall y ajustes). */
object AppLinks {

    /**
     * Página de suscripciones de Google Play filtrada a esta app. Es la vía que Play
     * pide ofrecer para cambiar de plan o cancelar sin tener que buscarlo a mano.
     */
    fun manageSubscriptions(packageName: String): String =
        "https://play.google.com/store/account/subscriptions?package=$packageName"
}
