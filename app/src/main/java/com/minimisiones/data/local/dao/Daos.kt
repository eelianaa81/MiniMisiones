package com.minimisiones.data.local.dao

import androidx.room.*
import com.minimisiones.data.local.entities.*
import kotlinx.coroutines.flow.Flow

// ═══════════════════════════════════════════════════════════════════
// DAOs — Data Access Objects
// ═══════════════════════════════════════════════════════════════════
// Cada DAO define las consultas a la base de datos para una entidad.
// Room genera el código SQL automáticamente a partir de estas
// interfaces anotadas.
//
// Se usa Flow<T> para que la UI se actualice sola cuando cambian
// los datos — sin necesidad de volver a pedir los datos manualmente.
// ═══════════════════════════════════════════════════════════════════

/**
 * Consultas sobre la tabla FAMILIA.
 */
@Dao
interface FamiliaDao {

    /** Inserta una nueva familia y devuelva su id generado. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(familia: FamiliaEntity): Long

    /** Devuelve todas las familias. Se usa en la pantalla de selección de familia. */
    @Query("SELECT * FROM familia")
    fun obtenerTodas(): Flow<List<FamiliaEntity>>

    /** Busca una familia por su código de invitación. */
    @Query("SELECT * FROM familia WHERE codigoInvite = :codigo LIMIT 1")
    suspend fun buscarPorCodigo(codigo: String): FamiliaEntity?

    @Query( "SELECT * FROM familia WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: Long): FamiliaEntity?
}

/**
 * Consultas sobre la tabla USUARIO.
 */
@Dao
interface UsuarioDao {

    /** Inserta un nuevo miembro y devuelve su id generado. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(usuario: UsuarioEntity): Long

    /** Devuelve todos los miembros de una familia como Flow reactivo. */
    @Query("SELECT * FROM usuario WHERE familiaId = :familiaId")
    fun obtenerPorFamilia(familiaId: Long): Flow<List<UsuarioEntity>>

    /**
     * Devuelve solo los niños/niñas de una familia.
     * Se usa para asignar misiones y ver saldos.
     */
    @Query(
        "SELECT * FROM usuario WHERE familiaId = :familiaId AND (rol= 'NINO' " +
                "OR rol = 'NINA')"
    )
    fun obtenerNinos(familiaId: Long): Flow<List<UsuarioEntity>>

    @Query("SELECT * FROM usuario WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: Long): UsuarioEntity?

    /**
     * Actualiza el saldo de monedas de un usuario.
     * Se llama con un valor positivo al aprobar una misión y con valor negativo al confirmar
     * un canje.
     *
     * @param delta Cantidad a sumar (positivo) o restar (negativo).
     */
    @Query("UPDATE usuario SET monedas = monedas + :delta WHERE id = :usuarioId")
    suspend fun actualizarMonedas(usuarioId: Long, delta: Int)

    @Update
    suspend fun actualizar(usuario: UsuarioEntity)

    @Delete
    suspend fun eliminar(usuario: UsuarioEntity)
}
/**
 * Consultas sobre la tabla MISION
 */
@Dao
interface MisionDao {

    /** Inserta una nueva misión y devuelve su id generado. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(mision: MisionEntity): Long

    /** Devuelve todas las misiones asignadas a un niño/niña concreto. */
    @Query("SELECT * FROM mision WHERE asignadoA = :ninoId")
    fun obtenerPorNino(ninoId: Long): Flow<List<MisionEntity>>

    /**Devuelve todas las misiones de los niños de una familia.
     * Se usa en la vista del administrador para ver todas las misiones.
     */
    @Query("""
        SELECT m.* FROM mision m
        INNER JOIN usuario u ON m.asignadoA = u.id
        WHERE u.familiaId = :familiaId
        """)
    fun obtenerPorFamilia(familiaId: Long): Flow<List<MisionEntity>>

    @Query("SELECT * FROM mision WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: Long): MisionEntity?

    @Update
    suspend fun actualizar(mision: MisionEntity)

    @Delete
    suspend fun eliminar(mision: MisionEntity)
}

/**
 * Consultas sobre la tabla ENTREGA_MISION.
 * Este DAO contiene las consultas más importantes del proyecto
 * porque gestiona el flujo PENDIENTE -> APROBADA / RECHAZADA.
 */
@Dao
interface EntregaMisionDao {

    /** Inserta un nuevo registro de entrega. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(entrega: EntregaMisionEntity): Long

    /**
     * Devuelve las entregas pendientes de aprobación de una familia.
     * Es la consulta principal de la pantalla de aprobaciones del administrador.
     */
    @Query("""
        SELECT e.* FROM entrega_mision e
        INNER JOIN mision m ON e.misionId = m.id
        INNER JOIN usuario u ON m.asignadoA = u.id
        WHERE u.familiaId = :familiaId AND e.estado = 'PENDIENTE'
        ORDER BY e.fecha DESC      
    """)
    fun obtenerPendientesPorFamilia(familiaId: Long): Flow<List<EntregaMisionEntity>>

    /** Historial completo de entregas de un niño/niña. */
    @Query("""
        SELECT e.* FROM entrega_mision e
        INNER JOIN mision m ON e.misionId = m.id
        WHERE m.asignadoA = :ninoId
        ORDER BY e.fecha DESC     
    """)
    fun obtenerHistorialNino(ninoId: Long): Flow<List<EntregaMisionEntity>>

    /**
     * Comprueba si el niño/niña ya entregá esta misión hoy.
     * Evita que se pueda marcar la misma misión varias veces en un día.
     *
     * @param misionId ID de la misión.
     * @param fecha Fecha actual en formato "dd/MM/yyyy".
     */
    @Query("""
        SELECT COUNT(*) FROM entrega_mision
        WHERE misionId = :misionId AND fecha = :fecha AND estado != 'RECHAZADA'   
    """)
    suspend fun contarEntregasHoy(misionId: Long, fecha: String): Int

    /**
     * Actualiza el estado de una entrega.
     * Solo debe llamarse con "APROBADA" o "RECHAZADA".
     */
    @Query("UPDATE entrega_mision SET estado = :nuevoEstado WHERE id = :entregaId")
    suspend fun actualizarEstado(entregaId: Long, nuevoEstado: String)

    /**
     * Cuenta los días distintos en que una misión fue aprobada.
     * Se usa para calcular rachas y otorgar el bonus de monedas.
     */
    @Query("""
        SELECT COUNT(DISTINCT fecha) FROM entrega_mision
        WHERE misionId = :misionId AND estado = 'APROBADA'    
    """)
    suspend fun contarDiasAprobados(misionId: Long): Int
}

/**
 * Consultas sobre la tabla PREMIO.
 */
@Dao
interface PremioDao {

    /** Inserta un nuevo premio y devuelve su id generado. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(premio: PremioEntity): Long

    /**
    * Catálogo de premios de una familia, ordenados por coste ascendente.
    * Los más baratos aparecen primero para motivar al niño/niña.
    */
    @Query("SELECT * FROM premio WHERE familiaId = :familiaId ORDER BY coste ASC")
    fun obtenerPorFamilia(familiaId: Long): Flow<List<PremioEntity>>

    @Query("SELECT * FROM premio WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: Long): PremioEntity?

    @Update
    suspend fun actualizar(premio: PremioEntity)

    @Delete
    suspend fun eliminar(premio: PremioEntity)
}

/**
*Consultas sobre la tabla CANJE.
*/
@Dao
interface CanjeDao {

    /** Inserta un nuevo registro de canje. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(canje: CanjeEntity): Long

    /** Historial de canjes de un niño/niña, más reciente primero. */
    @Query("SELECT * FROM canje WHERE ninoId = :ninoId ORDER BY fecha DESC")
    fun obtenerPorNino(ninoId: Long): Flow<List<CanjeEntity>>
}