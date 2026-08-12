package com.miguelcode.warrantyvault.domain.storage

/**
 * Borra fotos del filesystem dado su path absoluto. Se invoca desde los use
 * cases de delete/update para evitar que queden fotos huérfanas ocupando
 * espacio cuando la warranty desaparece o se cambia la foto.
 */
interface PhotoFileCleaner {
    fun delete(absolutePath: String?)
}
