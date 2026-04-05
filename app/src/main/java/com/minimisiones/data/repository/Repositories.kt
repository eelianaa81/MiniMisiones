package com.minimisiones.data.repository

import com.minimisiones.data.local.dao.*
import com.minimisiones.data.local.entities.*
import com.minimisiones.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter


// ═══════════════════════════════════════════════════════════════════
// REPOSITORIOS — Lógica de negocio
// ═══════════════════════════════════════════════════════════════════
// Los repositorios median entre los DAOs y los ViewModels.
// Aquí viven las reglas de MiniMisiones:
// - Las monedas solo se suman al aprobar
// - No se puede entregar la misma misión dos veces en un día
// - El canje falla si no hay saldo suficiente
// - El bonus de racha se aplica cada 5 días aprobados
//
// También traducen las entidades de Room (FamiliaEntity) a los
// modelos de dominio limpios (Familia) que usa la UI.
// ═══════════════════════════════════════════════════════════════════

// Formato de fecha español para toda la app
private val FORMATO_FECHA: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy")

// ── FamiliaRepository ─────────────────────────────────────────────

/**
 * Gestiona la creación y consulta de familias.
 * El código de invitación se genera aquí, no en la UI.
 */

class FamiliaRepository(private val familiaDao: FamiliaDao) {

    /**
     * Crea una nueva familia generando automáticamente su código de invitación.
     *
     * @param nombre Nombre de la familia.
     * @return ID de la familia recién creada.
     */
    suspend fun crearFamilia(nombre: String): Long {
        require(nombre.isNotBlank()) {"El nombre de la familia no puede estar vacío"}
        val codigo = generarCodigo()
        val entidad = FamiliaEntity(nombre= nombre.trim(), codigoInvite = codigo)
        return  familiaDao.insertar(entidad)
    }

    /** Devuelve todas las familias como Flow reactivo. */
    fun obtenerFamilias(): Flow<List<Familia>> =
        familiaDao.obtenerTodas().map { lista -> lista.map { it.toDomain() } }

    suspend fun buscarPorCodigo(codigo: String): Familia? =
        familiaDao.buscarPorCodigo(codigo)?.toDomain()

    //Genera un código de 6 caracteres sin caracteres coonfusos (0, O, I, 1)
    private fun generarCodigo(): String =
        (1..6).map { "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".random() } .joinToString("")

    //Convierte FamiliaEntity -> Familia (modelo de dominio)
    private fun FamiliaEntity.toDomain() = Familia(id, nombre, codigoInvite)
}

// ── UsuarioRepository ─────────────────────────────────────────────

/**
 * Gestiona el alta y consulta de los miembros de la familia.
 */
class UsuarioRepository(private val usuarioDao: UsuarioDao) {

    /**
     * Registra un nuevo miembro en la familia.
     *
     * @param nombre    Nombre del miembro.
     * @param rol       PADRE, MADRE, NINO, NINA.
     * @param avatar    Nombre del recurso drawable del avatar.
     * @param familiaID ID de la familia a la que pertenece.
     * @return ID del usuario creado.
     */
    suspend fun crearUsuario(
        nombre: String,
        rol: Rol,
        avatar: String,
        familiaId: Long
    ): Long {
        require(nombre.isNotBlank()) {"El nombre del usuario no puede estar vacío" }
        val entidad = UsuarioEntity(
            nombre = nombre.trim(),
            rol = rol.name,
            avatar = avatar,
            monedas = 0,
            familiaId = familiaId
        )
        return usuarioDao.insertar(entidad)
    }

    fun obtenerMiembrosFamilia(familiaId: Long): Flow<List<Usuario>> =
        usuarioDao.obtenerPorFamilia(familiaId).map { lista -> lista.map { it.toDomain() } }

    fun obtenerNinos(familiaId: Long): Flow<List<Usuario>> =
        usuarioDao.obtenerNinos(familiaId).map { lista -> lista.map { it.toDomain() } }

    suspend fun obtenerPorId(id: Long): Usuario? =
        usuarioDao.obtenerPorId(id)?.toDomain()

    // Convierte UsuarioEntity -> Usuario (modelo de dominio)
    private fun UsuarioEntity.toDomain() = Usuario(
        id = id,
        nombre = nombre,
        rol = Rol.valueOf(rol),
        avatar = avatar,
        monedas = monedas,
        familiaId = familiaId
    )
}

// ── MisionRepository ──────────────────────────────────────────────

/**
 * Gestiona la creación y consulta de misiones.
 */
class MisionRepository(private val misionDao: MisionDao) {

    /**
     * Crea una nueva misión y la asigna a un niño/niña.
     *
     * @param nombre        Descripción de la misión.
     * @param monedas       Monedas que vale al ser aprobada (debe ser >0).
     * @param frecuencia    DIARIA o SEMANAL.
     * @param ninoId        ID del niño/niña al que se asigna.
     * @return ID de la misión creada.
     */
    suspend fun crearMision(
        nombre: String,
        monedas: Int,
        frecuencia: Frecuencia,
        ninoId: Long
    ): Long {
        require(nombre.isNotBlank()) { "El nombre de la misión no puede estar vacío"}
        require(monedas > 0) {"Las monedas deben ser un valor positivo"}
        val entidad = MisionEntity(
            nombre = nombre.trim(),
            monedas = monedas,
            frecuencia = frecuencia.name,
            asignadoA = ninoId
        )
        return misionDao.insertar(entidad)
    }
    fun obtenerMisionesPorNino(ninoId: Long): Flow<List<Mision>> =
        misionDao.obtenerPorNino(ninoId).map { lista -> lista.map { it.toDomain() } }

    fun obtenerMisionesPorFamilia(familiaId: Long): Flow<List<Mision>> =
        misionDao.obtenerPorFamilia(familiaId).map { lista -> lista.map { it.toDomain() } }

    suspend fun eliminarMision(misionId: Long) {
        val entidad = misionDao.obtenerPorId(misionId) ?: return
        misionDao.eliminar(entidad)
    }
    //Convierte MisionEntity -> Mision (modelo de dominio)
    private fun MisionEntity.toDomain() = Mision(
        id = id,
        nombre = nombre,
        monedas = monedas,
        frecuencia = Frecuencia.valueOf(frecuencia),
        asignadoA = asignadoA
    )
}

// ── EntregaMisionRepository ───────────────────────────────────────

/**
 * Gestiona el ciclo de vida completo de una entrega:
 *
 * 1. El niño/niña pulsa "¡Conseguido!" -> se crea entrega en PENDIENTE
 * 2. El administrador aprueba -> APROBADA + monedas sumadas (+bonus si hay racha)
 * 3. El administrador rechaza -> RECHAZADA (el niño/niña puede reintentar)
 *
 * Este repositorio es el núcleo del proyecto.
 */
class EntregaMisionRepository(
    private val entregaDao: EntregaMisionDao,
    private val usuarioDao: UsuarioDao,
    private val misionDao: MisionDao
){
    /** Registra que un niño/niña ha completado una misión.
     *
     * Valida que no exista ya una entrega para la misma misión hoy.
     * para evitar que se marque varias veces el mismo día.
     *
     * @param misionId ID de la misión completada.
     * @return ID de la nueva entrega, o null si ya existe una para hoy.
     */
    suspend fun marcarComoConseguida(misionId: Long): Long? {
        val hoy = LocalDate.now().format(FORMATO_FECHA)

        //Regla de negocio: una misión diaria solo se puede entregar una vez por día
        val yaEntregadaHoy = entregaDao.contarEntregasHoy(misionId, hoy) > 0
        if (yaEntregadaHoy) return null

        val entidad = EntregaMisionEntity(
            misionId = misionId,
            fecha = hoy,
            estado = EstadoMision.PENDIENTE.name
        )
        return entregaDao.insertar(entidad)
    }

    /**
     * El administrador aprueba una entrega pendiente.
     *
     * Pasos internos:
     * 1. Cambia el estado a APROBADA
     * 2. Cuenta los días aprobados para detectar racha
     * 3. Calcula las monedas (base x BONUS_RACHA si hay racha)
     * 4. Suma las monedas al saldo del niño/niña
     *
     * @param entregaId     ID de la entrega a aprobar.
     * @param misionId      ID de la misión asociada.
     * @param ninoId        ID del niño/niña que entregó la misión.
     * @return Monedas efectivamente otorgadas (con o sin bonus de racha).
     */
    suspend fun aprobar(entregaId: Long, misionId: Long, ninoId: Long): Int {
        //Paso 1: Actualizar estado en BD
        entregaDao.actualizarEstado(entregaId, EstadoMision.APROBADA.name)

        //Paso 2: Obtener las monedas base de la misión
        val mision = misionDao.obtenerPorId(misionId) ?: return 0
        val monedasBase = mision.monedas

        //Paso 3: Calcular si hay racha aciva
        val diasAprobados = entregaDao.contarDiasAprobados(misionId)
        val hayRacha = diasAprobados > 0 && diasAprobados % DIAS_PARA_RACHA ==0

        //Paso 4: Calcular monedas totales
        val monedasTotales = if (hayRacha) monedasBase * BONUS_RACHA else monedasBase

        //Paso 5: Sumar monedas al saldo del niño/niña
        usuarioDao.actualizarMonedas(ninoId, monedasTotales)

        return monedasTotales
    }

    /**
     * El administrador rechaza una entrega.
     * El niño/niña puede volver a entregar la misión.
     */
    suspend fun rechazar(entregaId: Long) {
        entregaDao.actualizarEstado(entregaId, EstadoMision.RECHAZADA.name)
    }
    /** Flow de entregas pendientes para la pantalla de aprobaciones. */
    fun obtenerPendientesPorFamilia(familiaId: Long): Flow<List<EntregaMision>> =
        entregaDao.obtenerPendientesPorFamilia(familiaId)
            .map { lista -> lista.map {it.toDomain()} }

    /** Flow del historial de entrega de un niño/niña. */
    fun obtenerHistorialNino(ninoId: Long): Flow<List<EntregaMision>> =
        entregaDao.obtenerHistorialNino(ninoId)
            .map { lista -> lista.map {it.toDomain () } }

    //Convierte EntregaMisionEntity -> EntregaMision (modelo de dominio)
    private fun EntregaMisionEntity.toDomain() = EntregaMision(
        id = id,
        misionId = misionId,
        fecha = fecha,
        estado = EstadoMision.valueOf(estado)
    )
}

// ── PremioRepository ───────────────────────────────────────

/**
 * Gestiona el catálogo de premios y los canjes.
 */
class PremioRepository(
    private val premioDao: PremioDao,
    private val canjeDao: CanjeDao,
    private val usuarioDao: UsuarioDao
) {
    suspend fun crearPremio(nombre: String, coste: Int, familiaId: Long): Long {
        require(nombre.isNotBlank()) {"El nombre del premio no puede estar vacío"}
        require(coste > 0) {"El coste debe ser valor positivo"}
        val entidad = PremioEntity(nombre = nombre.trim(), coste = coste, familiaId = familiaId)
        return premioDao.insertar(entidad)
    }

    fun obtenerCatalogo(familiaId: Long): Flow<List<Premio>> =
        premioDao.obtenerPorFamilia(familiaId).map { lista -> lista.map { it.toDomain() } }

    suspend fun eliminarPremio(premioId: Long) {
        val entidad = premioDao.obtenerPorId(premioId) ?: return
        premioDao.eliminar(entidad)
    }

    /**
     * Procesa el canje de un premio por un niño/niña.
     *
     * Reglas de negocio:
     * - El niño/niña debe tener saldo suficiente.
     * - El administrador debe haber confirmado el canje.
     * - Las moneda se descuentan en el mismo momento que se registra el canje.
     *
     * @param premioId  ID del premio a canjear.
     * @param ninoId    ID del niño/niña que realiza el canje.
     * @return true si el canje fue exitoso, false si el saldo es insuficiente.
     */
    suspend fun canjear(premioId: Long, ninoId: Long): Boolean {
        val premio = premioDao.obtenerPorId(premioId) ?: return false
        val nino = usuarioDao.obtenerPorId(ninoId) ?: return false

        // Validación de saldo suficiente
        if (nino.monedas < premio.coste) return false

        //Registrar el canje
        val hoy = LocalDate.now().format(FORMATO_FECHA)
        canjeDao.insertar(CanjeEntity(premioId = premioId, ninoId = ninoId, fecha = hoy))

        //Descontar monedas (delta negativo)
        usuarioDao.actualizarMonedas(ninoId, -premio.coste)

        return true
    }

    //Convierte PremioEntity -> Premio (modelo de dominio)
    private fun PremioEntity.toDomain() = Premio(id, nombre, coste, familiaId)
}



