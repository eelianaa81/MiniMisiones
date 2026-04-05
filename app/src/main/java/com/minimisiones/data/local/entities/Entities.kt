package com.minimisiones.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// ═══════════════════════════════════════════════════════════════════
// ENTIDADES DE BASE DE DATOS — Room
// ═══════════════════════════════════════════════════════════════════
// Cada @Entity es una tabla en la base de datos local del dispositivo.
// Room lee estas anotaciones y genera el código SQL automáticamente.
// La app funciona 100% offline — los datos nunca salen del móvil.
// ═══════════════════════════════════════════════════════════════════

/**
 * Tabla FAMILIA.
 * Es la raíz de todo. Los demás datos pertenecen a una familia.
 *
 * @property codigoInvite Código de 6 caracteres para invitar a nuevos miembros
 *                        que se unan a esta familia.
 */
@Entity(
    tableName = "familia")
data class FamiliaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val codigoInvite: String
)

/**
 * Tabla USUARIO.
 * Cada miembro de la familia: padre, madre, niño o niña.
 *
 * @property rol "PADRE", "MADRE", "NINO" o "NINA".
 *                Los dos primeros son administradores.
 * @property monedas Saldo actual. Empiezan con 0 y solo cambia
 *                   si un administrador aprueda o confirma canje.
 * @property familiaId Clave foránea familia a la qué pertenece el usuario.
 *
 * La anotación @ForeignKey garantiza que si se borra una familia, todos los usuarios
 * se borran también (CASCADE)
 */
@Entity(
    tableName = "usuario",
    foreignKeys = [
        ForeignKey(
            entity = FamiliaEntity::class,
            parentColumns = ["id"],
            childColumns = ["familiaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("familiaId")]
)
data class UsuarioEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long =0,
    val nombre: String,

    // Se guarda como texto: "PADRE", "MADRE", "NINO", "NINA"
    val rol: String,
    val avatar: String,
    val monedas: Int = 0,
    val familiaId: Long
)

/**
 * Tabla MISION
 * Tabla en la que el administrador asigna a un niño/niña una tarea.
 *
 * @property monedas Monedas que gana al aprobarla.
 * @property frecuencia "DIARIA" o "SEMANAL".
 * @property asignadoA ID del niño/niña al que pertenece.
 */
@Entity(
    tableName = "mision",
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["asignadoA"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("asignadoA")]
)
data class MisionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val monedas: Int,

    //Se guarda como texto: "DIARIA" o "SEMANAL"
    val frecuencia: String,
    val asignadoA: Long
)

/**
 * Tabla ENTREGA_MISION
 * Registro que genera cada vez que el niño/niña pulsa "¡Conseguido!".
 *
 * El estado empieza en "PENDIENTE".
 * Solo puede cambiarlo a "APROBADA" o "RECHAZADA" el administrador.
 * Las monedas se suman SÓLO al pasar a "APROBADA".
 *
 *  @property fecha Fecha en formato español "dd/MM/yyyy".
 *  @property estado "PENDIENTE", "APROBADA" o "RECHAZADA".
 */
@Entity(
    tableName = "entrega_mision",
    foreignKeys = [
        ForeignKey(
            entity = MisionEntity::class,
            parentColumns = ["id"],
            childColumns = ["misionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("misionId")]
)
data class EntregaMisionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val misionId: Long,
    val fecha: String,
    val estado: String
)
/** Tabla PREMIO.
 * El administrador define una recompensa para la familia.
 * El niño/niña lo ve en su cuenta y lo solicita.
 *
 * @property coste Monedas necesarias para canjearlo.
 * @property familiaId Sólo lo puede visualizar esta familia.
 */
@Entity(
    tableName = "premio",
    foreignKeys = [
        ForeignKey(
            entity = FamiliaEntity::class,
            parentColumns = ["id"],
            childColumns = ["familiaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("familiaId")]
)
data class PremioEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val coste: Int,
    val familiaId: Long
)

/**
 * Tabla CANJE.
 * Registro cada vez que un niño/niña canjea un premio.
 * SOLO se crea si el administrador lo confirma.
 * Las monedas se descuentan del saldo en ese momento.
 *
 * @property ninoId ID del niño/niña que realizó el canje.
 * @property fecha Fecha en formato español "dd/MM/yyyy".
 */
@Entity(
    tableName = "canje",
    foreignKeys = [
        ForeignKey(
            entity = PremioEntity::class,
            parentColumns = ["id"],
            childColumns = ["premioId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["ninoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("premioId"), Index("ninoId")]
)
data class CanjeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val premioId: Long,
    val ninoId: Long,
    val fecha: String
)












