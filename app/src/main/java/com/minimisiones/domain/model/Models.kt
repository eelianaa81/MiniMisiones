package com.minimisiones.domain.model

// ═══════════════════════════════════════════════════════════════════
// MODELOS DE DOMINIO — MiniMisiones
// ═══════════════════════════════════════════════════════════════════
// Estas clases representan los datos de la app de forma limpia,
// sin depender de Room ni de Android. Son el "lenguaje común"
// que usan todas las capas del proyecto.
// ═══════════════════════════════════════════════════════════════════

// ── Enumeraciones ────────────────────────────────────────────────

/**
 * Roles disponibles en la app.
 * PADRE/MADRE: crea tareas,aprueba, gestiona recompensas.
 * NINO/NINA: ve sus tareas, las completa, canjea recompensas.
 */

enum class Rol { PADRE, MADRE, NINO, NINA }

/**
 * Estados por los que pasa una misión cuando el niño/niña la entrega.
 * PENDIENTE -> el niño/niña dijo "¡Conseguido!", espera que el padre lo revise.
 * APROBADA  -> el padre/madre confirma. Las monedas se suman.
 * RECHAZADA -> el padre/madre no lo acepta. El niño/niña lo puede intentar.
 */

enum class EstadoMision {PENDIENTE, APROBADA, RECHAZADA}

/**
 * Frecuencia que se repite la misión.
 * DIARIA: entrega una vez al día.
 * SEMANAL: entrega una vez a la semana.
 */

enum class Frecuencia {DIARIA, SEMANAL}

// ── Modelos de datos ─────────────────────────────────────────────

/**
 * Representa la unidad familiar.
 * Cada familia tiene un código único para unir a otros miembros.
 */
data class Familia(
    val id: Long = 0,
    val nombre: String,
    val codigoInvite: String
)

/**
 * Representa a un miembre de la familia.
 *
 * @property rol PADRE/MADRE o NINO/NINA - determina las pantallas que ven.
 * @property monedas Saldo actual. Aumenta cuando el PADRE/MADRE aprueba una
 *                     misión. Baja cuando el PADRE/MADRE confirma un canje.
 * @property avatar Nombre de la imagen para el avatar del usuario.
 */
data class Usuario(
    val id: Long = 0,
    val nombre: String,
    val rol: Rol,
    val avatar: String,
    val monedas: Int = 0,
    val familiaId: Long
)

/**
 * Misión asignada al niño/niña.
 * El administrador la crea, el niño/niña la ve en su lista y la marca como conseguida.
 *
 * @property monedas Cantidad de monedas que gana si el administrador lo aprueba.
 * @property frecuencia DIARIA o SEMANAL.
 * @property asignadoA ID del niño/niña al que pertenece esta misión.
 */
data class Mision(
    val id: Long = 0,
    val nombre: String,
    val monedas: Int,
    val frecuencia: Frecuencia,
    val asignadoA: Long
)

/**
 * Registro de que un niño/niña entregó una misión.
 * Se crea cuando pulsa "¡Conseguido!" y empieza en PENDIENTE.
 * Se da por APROBADA cuando el administrador lo confirma.
 *
 * @property fecha Fecha en formato español, ej: "19/03/2025".
 */
data class EntregaMision(
    val id: Long = 0,
    val misionId: Long,
    val fecha: String,
    val estado: EstadoMision
)

/**
 * Premio que el administrador define y el niño/niña puede canjear con sus monedas.
 *
 * @property coste Monedas necesarias para canjear por premios.
 */
data class Premio (
    val id: Long= 0,
    val nombre: String,
    val coste: Int,
    val familiaId: Long
)

/**
 * Registro canjeo premio por el niño/niña.
 * Se crea solo si el administrador confirma el canje.
 */
data class Canje(
    val id: Long = 0,
    val premioId: Long,
    val ninoId: Long,
    val fecha: String
)

// ── Constantes de gamificación ───────────────────────────────────

/** Días seguidos de aprobaciones necesarios para activar el bonus de racha. */
const val DIAS_PARA_RACHA = 5

/** Las monedas se multiplican por este valor cuando hay racha activa. */
const val BONUS_RACHA = 2























