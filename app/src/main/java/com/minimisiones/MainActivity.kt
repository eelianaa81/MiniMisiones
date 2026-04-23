package com.minimisiones

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.minimisiones.data.local.Minimisionesdatabase
import com.minimisiones.data.repository.*
import com.minimisiones.domain.model.*
import com.minimisiones.ui.screens.*
import com.minimisiones.ui.viewmodel.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()

        val db = Minimisionesdatabase.getInstance(applicationContext)

        val familiaRepository = FamiliaRepository(db.familiaDao())
        val usuarioRepository = UsuarioRepository(db.usuarioDao())
        val misionRepository = MisionRepository(db.misionDao())
        val entregaMisionRepository = EntregaMisionRepository(
            db.entregaMisionDao(),
            db.usuarioDao(),
            db.misionDao()
        )
        val premioRepository = PremioRepository(
            db.premioDao(),
            db.canjeDao(),
            db.usuarioDao()
        )

        setContent {
            MiniMisionesApp(
                familiaRepository = familiaRepository,
                usuarioRepository = usuarioRepository,
                misionRepository = misionRepository,
                entregaRepository = entregaMisionRepository,
                premioRepository = premioRepository
            )
        }
    }
}

sealed class Pantalla {
    object Splash : Pantalla()
    object SeleccionFamilia : Pantalla()
    data class SeleccionPerfil(val familiaId: Long) : Pantalla()
    data class DashboardAdmin(val familiaId: Long, val adminId: Long) : Pantalla()
    data class GestionMisiones(val familiaId: Long) : Pantalla()
    data class DashboardNino(val familiaId: Long, val ninoId: Long) : Pantalla()
    data class Aprobaciones(val familiaId: Long) : Pantalla()

    data class GestionPremios(val familiaId: Long) : Pantalla()
    data class TiendaPremios(val familiaId: Long, val ninoId: Long) : Pantalla()
}

@Composable
fun MiniMisionesApp(
    familiaRepository: FamiliaRepository,
    usuarioRepository: UsuarioRepository,
    misionRepository: MisionRepository,
    entregaRepository: EntregaMisionRepository,
    premioRepository: PremioRepository
) {
    var pantallaActual by remember { mutableStateOf<Pantalla>(Pantalla.Splash) }

    val familiaViewModel = remember { FamiliaViewModel(familiaRepository) }
    val usuarioViewModel = remember { UsuarioViewModel(usuarioRepository) }
    val misionViewModel = remember { MisionViewModel(misionRepository) }
    val entregaViewModel = remember { EntregaMisionViewModel(entregaRepository) }
    val premioViewModel = remember { PremioViewModel(premioRepository) }

    val familiaUiState by familiaViewModel.uiState.collectAsState()
    val usuarioUiState by usuarioViewModel.uiState.collectAsState()
    val misionUiState by misionViewModel.uiState.collectAsState()
    val entregaUiState by entregaViewModel.uiState.collectAsState()
    val premioUiState by premioViewModel.uiState.collectAsState()

    LaunchedEffect(pantallaActual) {
        if (pantallaActual is Pantalla.DashboardAdmin) {
            val p = pantallaActual as Pantalla.DashboardAdmin
            usuarioViewModel.cargarMiembros(p.familiaId)
        }
    }

    when (val pantalla = pantallaActual) {

        is Pantalla.Splash -> {
            SplashScreen(onNextScreen = { pantallaActual = Pantalla.SeleccionFamilia })
        }

         is Pantalla.SeleccionFamilia -> {
            SeleccionFamiliaScreen(
                familias = familiaUiState.familias,
                onFamiliaSeleccionada = { familiaId ->
                    usuarioViewModel.cargarMiembros(familiaId)
                    pantallaActual = Pantalla.SeleccionPerfil(familiaId)
                },
                onCrearFamilia = { nombre ->
                    familiaViewModel.crearFamilia(nombre)
                }
            )
        }

        is Pantalla.SeleccionPerfil -> {
            SeleccionPerfilScreen(
                miembros = usuarioUiState.miembros,
                familiaId = pantalla.familiaId,
                onPerfilSeleccionado = { usuario ->
                    usuarioViewModel.seleccionarUsuario(usuario)
                    val esAdmin = usuario.rol == Rol.PADRE || usuario.rol == Rol.MADRE
                    if (esAdmin) {
                        misionViewModel.cargarMisionesFamilia(pantalla.familiaId)
                        entregaViewModel.cargarPendientes(pantalla.familiaId)
                        premioViewModel.cargarCatalogo(pantalla.familiaId)
                        pantallaActual = Pantalla.DashboardAdmin(pantalla.familiaId, usuario.id)

                    } else {
                        misionViewModel.cargarMisionesPorNino(usuario.id)
                        premioViewModel.cargarCatalogo(pantalla.familiaId)
                        pantallaActual = Pantalla.DashboardNino(pantalla.familiaId, usuario.id)
                    }
                },
                onCrearMiembro = { nombre, rol, avatar ->
                    usuarioViewModel.crearUsuario(nombre, rol, avatar, pantalla.familiaId)
                },
                onVolver = { pantallaActual = Pantalla.SeleccionFamilia }
            )
        }

        is Pantalla.DashboardAdmin -> {
        val admin = usuarioUiState.usuarioActual ?: usuarioUiState.miembros.find { it.id == pantalla.adminId }
            DashboardAdminScreen(
                nombreAdmin = admin?.nombre ?: "Admin",
                pendientesCount = entregaUiState.pendientes.size,
                onGestionMisiones = { pantallaActual = Pantalla.GestionMisiones(pantalla.familiaId) },
                onAprobaciones = { pantallaActual = Pantalla.Aprobaciones(pantalla.familiaId) },
                onGestionPremios = { pantallaActual = Pantalla.GestionPremios(pantalla.familiaId) },
                onCerrarSesion = {
                    pantallaActual = Pantalla.SeleccionPerfil(pantalla.familiaId)
                }
            )
        }

        is Pantalla.Aprobaciones -> {
            AprobacionesScreen(
                pendientes = entregaUiState.pendientes,
                misiones = misionUiState.misiones,
                usuarios = usuarioUiState.ninos,
                onAprobar = { entregaId, misionId, ninoId ->
                    entregaViewModel.aprobar(entregaId, misionId, ninoId)
                },
                onRechazar = { entregaId ->
                    entregaViewModel.rechazar(entregaId)
                },
                onVolver = {
                    pantallaActual = Pantalla.DashboardAdmin(
                        pantalla.familiaId,
                        usuarioUiState.usuarioActual?.id ?: 0L
                    )
                }
            )
        }
        is Pantalla.GestionMisiones -> {
           GestionMisionesScreen(
                misiones = misionUiState.misiones,
                ninos = usuarioUiState.ninos,
                onCrearMision = { nombre, monedas, frecuencia, ninoId ->
                    misionViewModel.crearMision(nombre, monedas, frecuencia, ninoId)
                },
                onEliminarMision = { misionId ->
                    misionViewModel.eliminarMision(misionId)
                },
                onVolver = {
                    pantallaActual = Pantalla.DashboardAdmin(
                        pantalla.familiaId,
                        usuarioUiState.usuarioActual?.id ?: 0L
                    )
                }
            )
        }

        is Pantalla.DashboardNino -> {
            val nino = usuarioUiState.miembros.find { it.id == pantalla.ninoId }
            if (nino != null) {
                DashboardNinoScreen(
                    nino = nino,
                    misiones = misionUiState.misiones,
                    onConseguida = { misionId ->
                        entregaViewModel.marcarComoConseguida(misionId)
                    },
                    onIrTienda = {
                        pantallaActual = Pantalla.TiendaPremios(pantalla.familiaId, pantalla.ninoId)
                    },
                    onCerrarSesion = {
                        pantallaActual = Pantalla.SeleccionPerfil(pantalla.familiaId)
                    }
                )
            }
        }

        is Pantalla.GestionPremios -> {
            GestionPremiosScreen(
                premios = premioUiState.catalogo,
                familiaId = pantalla.familiaId,
                onCrearPremio = { nombre, coste ->
                    premioViewModel.crearPremio(nombre, coste, pantalla.familiaId)
                },
                onEliminarPremio = { premioId ->
                    premioViewModel.eliminarPremio(premioId)
                },
                onVolver = {
                    pantallaActual = Pantalla.DashboardAdmin(
                        pantalla.familiaId,
                        usuarioUiState.usuarioActual?.id ?: 0L
                    )
                }
            )
        }

        is Pantalla.TiendaPremios -> {
            val nino = usuarioUiState.miembros.find { it.id == pantalla.ninoId }
            TiendaPremiosScreen(
                monedas = nino?.monedas ?: 0,
                catalogo = premioUiState.catalogo,
                onSolicitarCanje = { premioId ->
                    premioViewModel.canjear(premioId, pantalla.ninoId)
                },
                onVolver = {
                    pantallaActual = Pantalla.DashboardNino(pantalla.familiaId, pantalla.ninoId)
                }
            )
        }
    }
}

@Composable
fun SeleccionPerfilScreen(
    miembros: List<Usuario>,
    familiaId: Long,
    onPerfilSeleccionado: (Usuario) -> Unit,
    onCrearMiembro: (nombre: String, rol: Rol, avatar: String) -> Unit,
    onVolver: () -> Unit
) {
    var mostrarDialogo by remember { mutableStateOf(false) }
    var nombreMiembro by remember { mutableStateOf("") }
    var rolSeleccionado by remember { mutableStateOf(Rol.NINO) }

    Scaffold(
        topBar = { MiniMisionesTopBar("¿Quién eres?", onVolver = onVolver) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { mostrarDialogo = true },
                containerColor = Color(0xFF5C35D4)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Añadir miembro",
                    tint = Color.White
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            if (miembros.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Añade el primer miembro\nde tu familia",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            items(miembros) { usuario ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPerfilSeleccionado(usuario) },
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AvatarUsuario(nombre = usuario.nombre, id = usuario.id)
                        Column {
                            Text(usuario.nombre, fontWeight = FontWeight.Medium)
                            Text(
                                when (usuario.rol) {
                                    Rol.PADRE -> "Papá"
                                    Rol.MADRE -> "Mamá"
                                    Rol.NINO -> "Niño"
                                    Rol.NINA -> "Niña"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }

        if (mostrarDialogo) {
            AlertDialog(
                onDismissRequest = { mostrarDialogo = false },
                title = { Text("Nuevo miembro") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = nombreMiembro,
                            onValueChange = { nombreMiembro = it },
                            label = { Text("Nombre") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Done
                            )
                        )
                        Text("Rol:", style = MaterialTheme.typography.labelLarge)
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                        ) {
                            listOf(Rol.PADRE, Rol.MADRE).forEach { rol ->
                                FilterChip(
                                    selected = rolSeleccionado == rol,
                                    onClick = { rolSeleccionado = rol },
                                    label = {
                                        Text(
                                            when (rol) {
                                                Rol.PADRE -> "Papá"
                                                Rol.MADRE -> "Mamá"
                                                else -> ""
                                            }
                                        )
                                    }
                                )
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                        ) {
                            listOf(Rol.NINO, Rol.NINA).forEach { rol ->
                                FilterChip(
                                    selected = rolSeleccionado == rol,
                                    onClick = { rolSeleccionado = rol },
                                    label = {
                                        Text(
                                            when (rol) {
                                                Rol.NINO -> "Niño"
                                                Rol.NINA -> "Niña"
                                                else -> ""
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onCrearMiembro(nombreMiembro, rolSeleccionado, "avatar_default")
                            mostrarDialogo = false
                            nombreMiembro = ""
                        },
                        enabled = nombreMiembro.isNotBlank()
                    ) { Text("Añadir") }
                },
                dismissButton = {
                    TextButton(onClick = { mostrarDialogo = false }) { Text("Cancelar") }
                }
            )
        }
    }
}




