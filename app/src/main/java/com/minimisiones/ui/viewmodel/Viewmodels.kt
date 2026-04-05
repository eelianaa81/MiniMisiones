package com.minimisiones.ui.viewmodel

import androidx.lifecycle.ViewModel
import  androidx.lifecycle.viewModelScope
import  com.minimisiones.data.repository.*
import  com.minimisiones.domain.model.*
import  kotlinx.coroutines.flow.*
import  kotlinx.coroutines.launch

// ═══════════════════════════════════════════════════════════════════
// VIEWMODELS — Estado de la UI
// ═══════════════════════════════════════════════════════════════════
// Cada ViewModel gestiona el estado de una o varias pantallas.
// Expone StateFlow<UiState> para que los Composables observen
// los cambios y se actualicen automáticamente.
//
// Las funciones suspendidas se lanzan en viewModelScope para que
// se cancelen automáticamente cuando el ViewModel se destruye.
//
// La UI nunca llama directamente al Repository — siempre pasa
// por el ViewModel. Esto mantiene la separación de responsabilidades.
// ═══════════════════════════════════════════════════════════════════

// ── Resultado genérico de operaciones ────────────────────────────

/**
 * Representa el resultado de cualquier operación asíncrona.
 * Se usa en todos los ViewsModels para gestionar el estado de la UI.
 */
sealed class OperacionResult {
    /** No hay ninguna operación en curso. Estado inicial. */
    object Idle: OperacionResult()

    /** La operación está en curso. Se muestra un indicador de carga. */
    object Cargando : OperacionResult()

    /** La operación terminó con éxito. */
    data class Exito(val mensaje: String = "") : OperacionResult()

    /** La operación falló. Se muestra el mensaje de error al usuario. */
    data class Error(val mensaje: String) : OperacionResult()
}

// ── FamiliaViewModel ──────────────────────────────────────────────

/**
 * Estado de la pantalla de selección y creación de familia.
 */
data class FamiliaUiState(
    val familias: List<Familia> = emptyList(),
    val operacion: OperacionResult = OperacionResult.Idle
)

/**
 * ViewModel para la pantalla de selección de familia.
 * Observa los cambios en la BD y actualiza la UI automáticamente.
 */
class FamiliaViewModel(
    private val familiaRepository: FamiliaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FamiliaUiState())
    val uiState: StateFlow<FamiliaUiState> = _uiState.asStateFlow()

    init {
        // Empieza a observar las gfamilias nada más crearse el ViewModel
        viewModelScope.launch {
            familiaRepository.obtenerFamilias().collect { familias ->
                _uiState.update { it.copy(familias = familias) }
            }
        }
    }

    /**
     * Crea una nueva familia con el nombre indicado.
     * El código de invitación se genera automáticamente en el Repository.
     */
    fun crearFamilia(nombre: String) {
        if (nombre.isBlank()) {
            _uiState.update {
                it.copy(operacion = OperacionResult.Error("El nombre no puede estar vacío"))
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(operacion = OperacionResult.Cargando) }
            runCatching { familiaRepository.crearFamilia(nombre) }
                .onSuccess {
                    _uiState.update { it.copy(operacion = OperacionResult.Exito()) }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            operacion = OperacionResult.Error(
                                e.message ?: "Error al crear la familia"
                            )
                        )
                    }
                }
        }
    }

    fun resetOperacion() = _uiState.update { it.copy(operacion = OperacionResult.Idle) }
}

// ── UsuarioViewModel ──────────────────────────────────────────────

/**
 * Estado de la pantalla de selección de perfil y alta de miembros.
 */
data class UsuarioUiState(
    val miembros: List<Usuario> = emptyList(),
    val ninos: List<Usuario> = emptyList(),
    val usuarioActual: Usuario? = null,
    val operacion: OperacionResult = OperacionResult.Idle
)

class UsuarioViewModel(
    private val usuarioRepository: UsuarioRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UsuarioUiState())
    val uiState: StateFlow<UsuarioUiState> = _uiState.asStateFlow()

    /** Carga los miembros de una familia. Llamar al seleccionar familia. */
    fun cargarMiembros(familiaId: Long) {
        viewModelScope.launch {
            usuarioRepository.obtenerMiembrosFamilia(familiaId).collect { miembros ->
                _uiState.update { it.copy(miembros = miembros) }
            }
        }
        viewModelScope.launch {
            usuarioRepository.obtenerNinos(familiaId).collect { ninos ->
                _uiState.update { it.copy(ninos = ninos) }
            }
        }
    }

    /**
     * Registra un nuevo miembro en la familia.
     *
     * @param nombre    Nombre del miembro.
     * @param rol       PADRE, MADRE, NINO o NINA.
     * @param avatar    Recurso del avatar seleccionado.
     * @param familiaId Familia a la que pertenece.
     */
    fun crearUsuario(nombre: String, rol: Rol, avatar: String, familiaId: Long) {
        if (nombre.isBlank()) {
            _uiState.update {
                it.copy(operacion = OperacionResult.Error("El nombre es obligatorio"))
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(operacion = OperacionResult.Cargando) }
            runCatching { usuarioRepository.crearUsuario(nombre, rol, avatar, familiaId) }
                .onSuccess {
                    _uiState.update { it.copy(operacion = OperacionResult.Exito()) }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(operacion = OperacionResult.Error(e.message ?: "Error al crear el usuario"))
                    }
                }
        }
    }

    /** Establece el usuario activo en sesión. */
    fun seleccionarUsuario(usuario: Usuario) {
        _uiState.update { it.copy(usuarioActual = usuario) }
    }

    fun resetOperacion() = _uiState.update { it.copy(operacion = OperacionResult.Idle) }
}
// ── MisionViewModel ───────────────────────────────────────────────

    /**
     * Estado de la pantalla de gestión de misiones.
     */
    data class MisionUiState(
        val misiones: List<Mision> = emptyList(),
        val operacion: OperacionResult = OperacionResult.Idle
    )

    class MisionViewModel(
        private val misionRepository: MisionRepository
    ) : ViewModel() {

        private val _uiState = MutableStateFlow(MisionUiState())
        val uiState: StateFlow<MisionUiState> = _uiState.asStateFlow()

        /**Carga las misiones asignadas a un niño/niña. */
        fun cargarMisionesPorNino(ninoId: Long) {
            viewModelScope.launch {
                misionRepository.obtenerMisionesPorNino(ninoId).collect { misiones ->
                    _uiState.update { it.copy(misiones = misiones) }
                }
            }
        }

        /** Carga todas las misiones de la familia (vista del administrador). */
        fun cargarMisionesFamilia(familiaId: Long) {
            viewModelScope.launch {
                misionRepository.obtenerMisionesPorFamilia(familiaId).collect { misiones ->
                    _uiState.update { it.copy(misiones = misiones) }
                }
            }
        }

        /**
         * Crea una nueva misión asignada a un niño/niña.
         *
         * @param nombre        Descripción de la misión.
         * @param monedas       Monedas que vale (debe ser >0).
         * @param frecuencia    DIARIA o SEMANAL.
         * @param ninoId        ID del niño/niña asignado.
         */
        fun crearMision(nombre: String, monedas: Int, frecuencia: Frecuencia, ninoId: Long) {
            when {
                nombre.isBlank() ->
                    _uiState.update { it.copy(operacion = OperacionResult.Error(
                        "El nombre es obligatorio")) }
                monedas <= 0 ->
                    _uiState.update { it.copy(operacion = OperacionResult.Error(
                        "Las monedas deben ser mayor que 0")) }
                else -> viewModelScope.launch {
                    _uiState.update { it.copy(operacion = OperacionResult.Cargando) }
                    runCatching { misionRepository.crearMision(nombre, monedas, frecuencia, ninoId) }
                        .onSuccess {
                            _uiState.update { it.copy(operacion = OperacionResult.Exito(
                                "Misión creada")) }
                        }
                        .onFailure { e ->
                            _uiState.update {
                                it.copy(operacion = OperacionResult.Error(e.message ?:
                                "Error al crear la misión"))
                            }
                        }
                }
            }
        }

        fun eliminarMision(misionId: Long) {
            viewModelScope.launch {
                runCatching { misionRepository.eliminarMision(misionId) }
                    .onSuccess {
                        _uiState.update {
                            it.copy(
                                operacion = OperacionResult.Exito
                                    ("Misión eliminada")
                            )
                        }
                    }
                    .onFailure { e ->
                        _uiState.update {
                            it.copy(
                                operacion = OperacionResult.Error(
                                    e.message ?: "Error al eliminar"
                                )
                            )
                        }
                    }
            }
        }

        fun resetOperacion() = _uiState.update { it.copy(operacion = OperacionResult.Idle) }
    }

// ── EntregaMisionViewModel ────────────────────────────────────────

    /**
     * Estado de las pantallas de entregas y aprobaciones.
     *
     * @property ultimasMonedasOtorgadas Monedas de la última aprobación.
     *                                   Se muestra en el mensaje de confirmación.
     */
    data class EntregasUiState(
        val pendientes: List<EntregaMision> = emptyList(),
        val historial: List<EntregaMision> = emptyList(),
        val ultimasMonedasOtorgadas: Int = 0,
        val operacion: OperacionResult = OperacionResult.Idle
    )

    class EntregaMisionViewModel(
        private val entregaRepository: EntregaMisionRepository
    ) : ViewModel() {

        private val _uiState = MutableStateFlow(EntregasUiState())
        val uiState: StateFlow<EntregasUiState> = _uiState.asStateFlow()

        /** Carga las entregas pendientes de aprobación (vista del administrador). */
        fun cargarPendientes(familiaId: Long) {
            viewModelScope.launch {
                entregaRepository.obtenerPendientesPorFamilia(familiaId).collect { pendientes ->
                    _uiState.update { it.copy(pendientes = pendientes) }
                }
            }
        }


        /** Carga el historial de entregas de un niño/niña. */
        fun cargarHistorial(ninoId: Long) {
            viewModelScope.launch {
                entregaRepository.obtenerHistorialNino(ninoId).collect { historial ->
                    _uiState.update { it.copy(historial = historial) }
                }
            }
        }

        /**
         * El niño marca una misión como conseguida.
         * Si ya existe una entrega para hoy, muestra un error informativo.
         *
         * @param misionId Id de la misión a completar.
         */
        fun marcarComoConseguida(misionId: Long) {
            viewModelScope.launch {
                _uiState.update { it.copy(operacion = OperacionResult.Cargando) }
                val id = entregaRepository.marcarComoConseguida(misionId)
                if (id != null) {
                    _uiState.update {
                        it.copy(
                            operacion = OperacionResult.Exito(
                                "¡Misión enviada!" +
                                        " Espera la aprobación"
                            )
                        )
                    }
                } else {

                    _uiState.update {
                        it.copy(
                            operacion = OperacionResult.Error(
                                "Ya enviaste esta misión hoy"
                            )
                        )
                    }
                }
            }
        }

        /**
         * El administrador aprueba una entrega pendiente.
         * Las monedas otorgadas (con posible bonus de racha) se muestran en la UI.
         *
         * @param entregaId     ID de la entrega a aprobar.
         * @param misionId      ID de la misión asociada.
         * @param ninoId        ID del niño/niña que completó la misión.
         */
        fun aprobar(entregaId: Long, misionId: Long, ninoId: Long) {
            viewModelScope.launch {
                _uiState.update { it.copy(operacion = OperacionResult.Cargando) }
                runCatching { entregaRepository.aprobar(entregaId, misionId, ninoId) }
                    .onSuccess { monedas ->
                        _uiState.update {
                            it.copy(
                                ultimasMonedasOtorgadas = monedas,
                                operacion = OperacionResult.Exito("+$monedas monedas otorgadas")
                            )
                        }
                    }
                    .onFailure { e ->
                        _uiState.update {
                            it.copy(
                                operacion = OperacionResult.Error(
                                    e.message ?: "Error al aprobar"
                                )
                            )
                        }
                    }
            }
        }

        /** El administrador rechaza una entrega. */
        fun rechazar(entregaId: Long) {
            viewModelScope.launch {
                runCatching { entregaRepository.rechazar(entregaId) }
                    .onSuccess {
                        _uiState.update {
                            it.copy(
                                operacion = OperacionResult.Exito(
                                    "Entrega rechazada"
                                )
                            )
                        }
                    }
                    .onFailure { e ->
                        _uiState.update {
                            it.copy(
                                operacion = OperacionResult.Error(
                                    e.message ?: "Error al rechazar"
                                )
                            )
                        }
                    }
            }
        }

        fun resetOperacion() = _uiState.update { it.copy(operacion = OperacionResult.Idle) }
    }

// ── PremioViewModel ───────────────────────────────────────────────

    /**
     * Estado de las pantallas de premios y canjes.
     */

    data class PremioUiState(
        val catalogo: List<Premio> = emptyList(),
        val operacion: OperacionResult = OperacionResult.Idle
    )

    class PremioViewModel(
        private val premioRepository: PremioRepository
    ) : ViewModel() {

        private val _uiState = MutableStateFlow(PremioUiState())
        val uiState: StateFlow<PremioUiState> = _uiState.asStateFlow()

        fun cargarCatalogo(familiaId: Long) {
            viewModelScope.launch {
                premioRepository.obtenerCatalogo(familiaId).collect { catalogo ->
                    _uiState.update { it.copy(catalogo = catalogo) }
                }
            }
        }

        fun crearPremio(nombre: String, coste: Int, familiaId: Long) {
            when {
                nombre.isBlank() ->
                    _uiState.update {
                        it.copy(
                            operacion = OperacionResult.Error(
                                "El nombre es obligatorio"
                            )
                        )
                    }

                coste <= 0 ->
                    _uiState.update {
                        it.copy(
                            operacion = OperacionResult.Error(
                                "El coste debe ser mayor que 0"
                            )
                        )
                    }

                else -> viewModelScope.launch {
                    runCatching { premioRepository.crearPremio(nombre, coste, familiaId) }
                        .onSuccess {
                            _uiState.update {
                                it.copy(
                                    operacion = OperacionResult.Exito(
                                        "Premio añadido"
                                    )
                                )
                            }
                        }
                        .onFailure { e ->
                            _uiState.update {
                                it.copy(
                                    operacion = OperacionResult.Error(
                                        e.message ?: "Error al crear premio"
                                    )
                                )
                            }
                        }
                }
            }
        }

        /**
         * Procesa el canje de un premio.
         * Solo llamar cuando el administrador haya confirmado el canje.
         *
         * @param premioId  Premio a canjear.
         * @param nninoId   Niño/niña que realiza el canje.
         */
        fun canjear(premioId: Long, ninoId: Long) {
            viewModelScope.launch {
                _uiState.update { it.copy(operacion = OperacionResult.Cargando) }
                runCatching { premioRepository.canjear(premioId, ninoId) }
                    .onSuccess { exito ->
                        if (exito) {
                            _uiState.update {
                                it.copy(
                                    operacion = OperacionResult.Exito(
                                        "¡Premio canjeado"
                                    )
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    operacion = OperacionResult.Error(
                                        "Saldo insuficiente"
                                    )
                                )
                            }
                        }
                    }
                    .onFailure { e ->
                        _uiState.update {
                            it.copy(
                                operacion = OperacionResult.Error(
                                    e.message ?: "Error al canjear"
                                )
                            )
                        }
                    }
            }
        }

        fun eliminarPremio(premioId: Long) {
            viewModelScope.launch {
                runCatching { premioRepository.eliminarPremio((premioId)) }
                    .onSuccess {
                        _uiState.update {
                            it.copy(
                                operacion = OperacionResult.Exito(
                                    "Premio eliminado"
                                )
                            )
                        }
                    }
                    .onFailure { e ->
                        _uiState.update {
                            it.copy(
                                operacion = OperacionResult.Error(
                                    e.message ?: "Error al eliminar"
                                )
                            )
                        }
                    }
            }
        }

        fun resetOperacion() = _uiState.update { it.copy(operacion = OperacionResult.Idle) }
    }













