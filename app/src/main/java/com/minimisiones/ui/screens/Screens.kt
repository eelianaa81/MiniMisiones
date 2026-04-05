package com.minimisiones.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimisiones.domain.model.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.minimisiones.R

// ═══════════════════════════════════════════════════════════════════
// PANTALLAS — Jetpack Compose
// ═══════════════════════════════════════════════════════════════════
// Cada función @Composable es una pantalla de la app.
// Las pantallas reciben datos y callbacks — nunca llaman
// directamente al ViewModel ni al Repository.
// Siguen el patrón UDF (Unidirectional Data Flow):
//   Estado baja → pantalla
//   Eventos suben → ViewModel
// ═══════════════════════════════════════════════════════════════════

// ── Colores de la app ─────────────────────────────────────────────

private val ColorPrimario = Color(0xFF5C35D4)
private val ColorSecundario = Color(0xFFFFB703)
private val ColorExito = Color(0xFF2E7D32)
private val ColorError = Color(0xFFB00020)

private val ColoresAvatar = listOf(
    Color(0xFFE91E63),
    Color(0xFF2196F3),
    Color(0xFF4CAF50),
    Color(0xFFFF9800),
    Color(0xFF9C27B0),
    Color(0xFF00BCD4)
)

// ── Componentes reutilizables ─────────────────────────────────────

/**
 * Barra superior estándar de MiniMisiones.
 * Aparece en todas las pantallas con el título y botón de retroceso opcional.
 */
@OptIn(ExperimentalMaterial3Api ::class)
@Composable
fun MiniMisionesTopBar(
    titulo: String,
    onVolver: (() -> Unit)? = null
) {
    TopAppBar(
        title = {
            Text(titulo, fontWeight = FontWeight.SemiBold)
        },
        navigationIcon = {
            if (onVolver != null){
                IconButton(onClick = onVolver) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = ColorPrimario,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White
        )
    )
}

/**
 * Tarjeta de monedas que muestra el saldo del niño/niña.
 * Se usa en el dashboard del niño y en la tienda de premios.
 */
@Composable
fun TarjetaMonedas(monedas: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = ColorSecundario),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("🪙", fontSize = 24.sp)
            Text(
                text = "$monedas monedas",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A)
            )
        }
    }
}

/**
 * Avatar circular con la inicial del nombre del usuario.
 * El color se asigna automáticamente según el ID del usuario.
 */
@Composable
fun AvatarUsuario(nombre: String, id: Long, tamanio: Int = 48){
    val color = ColoresAvatar[(id % ColoresAvatar.size).toInt()]
    Box(
        modifier = Modifier
            .size(tamanio.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = nombre.first().uppercase(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (tamanio / 2.5).sp
        )
    }
}

// ── PANTALLA: Selección de familia ───────────────────────────────

/**
 * Pantalla de bienvenida. El usuario selecciona su familia
 * o crea una nueva. Es el punto de entrada de la app.
 *
 * @param familias              Lista de familias guardadas en el dispositivo.
 * @param onFamiliaSeleccionada Callback con el ID de la familia elegida.
 * @param onCrearFamilia        Callback con el nombre de la nueva familia.
 */
@Composable
fun SeleccionFamiliaScreen(
    familias: List<Familia>,
    onFamiliaSeleccionada: (Long) -> Unit,
    onCrearFamilia: (String) -> Unit
) {
    var mostrarDialogo by remember { mutableStateOf(false) }
    var nombreFamilia by remember { mutableStateOf("") }

    Scaffold(
      topBar = { MiniMisionesTopBar("MiniMisiones")},
        floatingActionButton = {
            FloatingActionButton(
                onClick = {mostrarDialogo = true },
                containerColor = ColorPrimario
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nueva familia",
                        tint = Color.White) }
        }
        ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (familias.isEmpty()) {
                //Estado vacío - primera vez que se abre la app
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("👨‍👩‍👧‍👦", fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Crea tu primera familia para comenzar",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                Text(
                    "Selecciona tu familia",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(familias) { familia ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onFamiliaSeleccionada(familia.id) },
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            ListItem(
                                headlineContent = {
                                    Text(familia.nombre, fontWeight = FontWeight.Medium)
                                },
                                supportingContent = {
                                    Text(
                                        "Código: ${familia.codigoInvite}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                },
                                leadingContent = {
                                    Icon(
                                        Icons.Default.Home,
                                        contentDescription = null,
                                        tint = ColorPrimario
                                    )
                                }
                            )
                        }

                    }
                }
            }
        }

        // Diálogo para crear nueva familia
        if (mostrarDialogo) {
            AlertDialog(
                onDismissRequest = {
                    mostrarDialogo = false
                    nombreFamilia = ""
                },
                title = { Text("Nueva familia") },
                text = {
                    OutlinedTextField(
                        value = nombreFamilia,
                        onValueChange = { nombreFamilia = it },
                        label = { Text("Nombre de la familia") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onCrearFamilia(nombreFamilia)
                            mostrarDialogo = false
                            nombreFamilia = ""
                        },
                        enabled = nombreFamilia.isNotBlank()
                    ) { Text("Crear") }
                },
                dismissButton = {
                    TextButton(onClick = { mostrarDialogo = false }) { Text("Cancelar") }
                }
            )
        }
    }
}

// ── PANTALLA: Dashboard del administrador ────────────────────────

/**
 * Panel de control del padre/madre.
 * Muestra accessos a las funciones principales con el contador
 * de entregas pendientes como badge.
 *
 * @param pendientesCount Número de entregas esperando aprobación.
 */
@Composable
fun DashboardAdminScreen(
    nombreAdmin: String,
    pendientesCount: Int,
    onGestionMisiones: () -> Unit,
    onAprobaciones: () -> Unit,
    onGestionPremios: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    Scaffold(
        topBar = {MiniMisionesTopBar("Hola, $nombreAdmin") }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "¿Qué quieres hacer?",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            BotonAccionAdmin(
                emoji = "📋",
                titulo = "Gestionar misiones",
                descripcion = "Crear, editar y asignar misiones",
                onClick = onGestionMisiones
            )

            BotonAccionAdmin(
                emoji = "✅",
                titulo = "Aprobar misiones",
                descripcion = if (pendientesCount > 0)
                    "$pendientesCount pendientes de revisión"
                else "Sin misiones pendientes",
                badgeCount = pendientesCount,
                onClick = onAprobaciones
            )

            BotonAccionAdmin(
                emoji = "🎁",
                titulo = "Premios",
                descripcion = "Gestionar el catálogo de premios",
                onClick = onGestionPremios
            )


            Spacer(modifier = Modifier.weight(1f))

            TextButton(
                onClick = onCerrarSesion,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cambiar de perfil")
            }
        }
    }
}
@Composable
private fun BotonAccionAdmin(
    emoji: String,
    titulo: String,
    descripcion: String,
    badgeCount: Int = 0,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(3.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(emoji, fontSize = 32.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
                Text(
                    descripcion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            if (badgeCount > 0) {
                Badge(containerColor = ColorError) {
                    Text("$badgeCount", color = Color.White, fontSize = 12.sp)
                }
            }
            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = ColorPrimario)
        }
    }
}

// ── PANTALLA: Aprobaciones (administrador) ───────────────────────

/**
 * Pantalla de aprobación de misiones pendientes.
 * Es la pantalla más importante del proyecto - implementa el flujo
 * de validación adulto-menor que da valor al sistema de monedas.
 *
 * @param pendientes    Lista de entregas pendientes de aprobación.
 * @param onAprobar     Callback con (entregaId, misionId, ninoId).
 * @param onRechazar    Callback con entregaId.
 */
@Composable
fun AprobacionesScreen(
    pendientes: List<EntregaMision>,
    misiones: List<Mision>,
    usuarios: List<Usuario>,
    onAprobar: (entregaId: Long, misionId: Long, ninoId: Long) -> Unit,
    onRechazar: (Long) -> Unit,
    onVolver: () -> Unit
){
    Scaffold(
        topBar = { MiniMisionesTopBar("Aprobar misiones", onVolver = onVolver) }
    ) { padding ->
        if (pendientes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎉", fontSize = 64.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "¡Todo al día!\nNo hay misiones pendientes.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(pendientes, key = { it.id }) { entrega ->
                    val mision = misiones.find { it.id == entrega.misionId }
                    val nino = usuarios.find { mision != null && it.id == mision.asignadoA }

                    if (mision != null && nino != null) {
                        TarjetaAprobacion(
                            entrega = entrega,
                            mision = mision,
                            nino = nino,
                            onAprobar = { onAprobar(entrega.id, mision.id, nino.id) },
                            onRechazar = { onRechazar(entrega.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaAprobacion(
    entrega: EntregaMision,
    mision: Mision,
    nino: Usuario,
    onAprobar: () -> Unit,
    onRechazar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(16.dp)
    ){
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ){
                AvatarUsuario(nombre = nino.nombre, id = nino.id)
                Column {
                    Text(nino.nombre, fontWeight = FontWeight.SemiBold)
                    Text(
                        entrega.fecha,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            Text(mision.nombre, style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(onClick = {}, label = { Text("🪙 ${mision.monedas} monedas") })
                AssistChip(onClick = {}, label = { Text(mision.frecuencia.name)})
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onRechazar,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorError)
                ) {
                   Icon(Icons.Default.Close, contentDescription = null, modifier =
                       Modifier.size(16.dp))
                   Spacer(modifier = Modifier.width(4.dp))
                   Text("Rechazar")
                }
                Button(
                    onClick = onAprobar,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = ColorExito)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null,
                        modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Aprobar")
                }
            }
        }
    }
}

// ── PANTALLA: Dashboard del niño/niña ────────────────────────────

/**
 * Visita principal del niño/niña.
 * Muestra sus misiones asignadas, saldo de monedas y acceso a la tienda.
 *
 * @param nino          Usuario activo con rol NINO o NINA.
 * @param misiones      Misiones asignadas al niño/niña.
 * @param onConseguida  Callback con el ID de la misión a completar.
 * @param onIrTienda    Navega a la pantalla de premios.
 */
@Composable
fun DashboardNinoScreen(
    nino: Usuario,
    misiones: List<Mision>,
    onConseguida: (Long) -> Unit,
    onIrTienda: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    Scaffold(
        topBar = { MiniMisionesTopBar("¡Hola, ${nino.nombre}!", onVolver = onCerrarSesion) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    onClick = {},
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Misiones") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onIrTienda,
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) },
                    label = { Text("Tienda") }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                TarjetaMonedas(
                    monedas = nino.monedas,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text(
                    "Tus misiones",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (misiones.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "¡Sin misiones por ahora! 🎉",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                items(misiones, key = { it.id }) { mision ->
                    TarjetaMisionNino(
                        mision = mision,
                        onConseguida = { onConseguida(mision.id) }
                    )
                }
            }
        }
    }
}
@Composable
private fun TarjetaMisionNino(
    mision: Mision,
    onConseguida: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(3.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    mision.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        "🪙 ${mision.monedas}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF854F0B)
                    )
                    Text("·", style = MaterialTheme.typography.bodySmall)
                    Text(
                        mision.frecuencia.name.lowercase(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
            Button(
                onClick = onConseguida,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ColorPrimario)
            ) {
                Text("¡Conseguido!", fontSize = 13.sp)
            }
        }
    }
}

// ── PANTALLA: Tienda de premios (niño/niña) ──────────────────────

/**
 * Catálogo de premios disponibles para el niño/niña.
 * Los premios que no puede permitirse aparecen bloqueados.
 * Al pulsar "Canjear" aparece un diálogo de confirmación.
 *
 * @param monedas           Saldo actual del niño/niño.
 * @param catalogo          Lista de premios disponibles.
 * @param onSolicitarCanje  Callback con el ID del premio seleccionado.
 */
@Composable
fun TiendaPremiosScreen(
    monedas: Int,
    catalogo: List<Premio>,
    onSolicitarCanje: (Long) -> Unit,
    onVolver: () -> Unit
) {
    var premioSeleccionado by remember { mutableStateOf<Premio?>(null) }

    Scaffold(
        topBar = {MiniMisionesTopBar("Tienda de premios", onVolver = onVolver) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                TarjetaMonedas(monedas = monedas, modifier = Modifier.fillMaxWidth())
            }

            items( catalogo, key = { it.id }) {premio ->
                val puedeCanjear = monedas >= premio.coste
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled =  puedeCanjear) { premioSeleccionado = premio },
                    elevation = CardDefaults.cardElevation(if (puedeCanjear) 3.dp else 0.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (puedeCanjear)
                            MaterialTheme.colorScheme.surface
                        else
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🎁", fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                premio.nombre,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (puedeCanjear)
                                    MaterialTheme.colorScheme.onSurface
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                            Text(
                                "🪙 ${premio.coste} monedas",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (puedeCanjear) ColorSecundario
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }
                        if (puedeCanjear) {
                            Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = ColorPrimario
                            )
                        } else {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Bloqueado",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        }

        // Diálogo de confirmación de canje
        premioSeleccionado?.let { premio ->
            AlertDialog(
                onDismissRequest = { premioSeleccionado = null },
                title = { Text("Confirmar canje") },
                text = {
                    Column {
                        Text("¿Quieres canjear?")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            premio.nombre,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text("Coste: 🪙 ${premio.coste} monedas")
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFF3CD)
                            )
                        ) {
                            Text(
                                "⚠️ Pide a un adulto que confirme el canje.",
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        onSolicitarCanje(premio.id)
                        premioSeleccionado = null
                    }) { Text("Confirmar") }
                },
                dismissButton = {
                    TextButton(onClick = { premioSeleccionado = null }) { Text("Cancelar") }
                }
            )
        }
    }
}
// ── PANTALLA: Gestión de misiones (administrador) ─────────────────

/**
 * Pantalla para crear y eliminar misiones.
 * El administrador asigna cada misión a un niño/niña de la familia.
 *
 * @param misiones       Lista de misiones actuales de la familia.
 * @param ninos          Lista de niños/niñas disponibles para asignar.
 * @param onCrearMision  Callback con los datos de la nueva misión.
 * @param onEliminarMision Callback con el ID de la misión a eliminar.
 */
@Composable
fun GestionMisionesScreen(
    misiones: List<Mision>,
    ninos: List<Usuario>,
    onCrearMision: (nombre: String, monedas: Int, frecuencia: Frecuencia, ninoId: Long) -> Unit,
    onEliminarMision: (Long) -> Unit,
    onVolver: () -> Unit
) {
    var mostrarFormulario by remember { mutableStateOf(false) }
    var nombre by remember { mutableStateOf("") }
    var monedas by remember { mutableStateOf("10") }
    var frecuencia by remember { mutableStateOf(Frecuencia.DIARIA) }
    var ninoSeleccionado by remember { mutableStateOf<Usuario?>(null) }

    Scaffold(
        topBar = { MiniMisionesTopBar("Gestionar misiones", onVolver = onVolver) },
        floatingActionButton = {
            if (ninos.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { mostrarFormulario = true },
                    containerColor = ColorPrimario
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nueva misión", tint = Color.White)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            if (ninos.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Primero añade un niño/niña a la familia para crear misiones.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            if (misiones.isEmpty() && ninos.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No hay misiones todavía.\nPulsa + para crear la primera.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            items(misiones, key = { it.id }) { mision ->
                val ninoAsignado = ninos.find { it.id == mision.asignadoA }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (ninoAsignado != null) {
                            AvatarUsuario(nombre = ninoAsignado.nombre, id = ninoAsignado.id, tamanio = 40)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(mision.nombre, fontWeight = FontWeight.Medium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AssistChip(onClick = {}, label = { Text("🪙 ${mision.monedas}") })
                                AssistChip(onClick = {}, label = { Text(mision.frecuencia.name.lowercase()) })
                                ninoAsignado?.let {
                                    Text(
                                        it.nombre,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                        IconButton(onClick = { onEliminarMision(mision.id) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Eliminar",
                                tint = ColorError
                            )
                        }
                    }
                }
            }
        }

        // Diálogo para crear nueva misión
        if (mostrarFormulario) {
            if (ninoSeleccionado == null && ninos.isNotEmpty()) {
                ninoSeleccionado = ninos.first()
            }
            AlertDialog(
                onDismissRequest = { mostrarFormulario = false },
                title = { Text("Nueva misión") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = nombre,
                            onValueChange = { nombre = it },
                            label = { Text("Nombre de la misión") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = monedas,
                            onValueChange = { monedas = it.filter { c -> c.isDigit() } },
                            label = { Text("Monedas al aprobar") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Frecuencia:", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Frecuencia.entries.forEach { freq ->
                                FilterChip(
                                    selected = frecuencia == freq,
                                    onClick = { frecuencia = freq },
                                    label = { Text(freq.name.lowercase().replaceFirstChar { it.uppercase() }) }
                                )
                            }
                        }
                        if (ninos.size > 1) {
                            Text("Asignar a:", style = MaterialTheme.typography.labelLarge)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ninos.forEach { nino ->
                                    FilterChip(
                                        selected = ninoSeleccionado?.id == nino.id,
                                        onClick = { ninoSeleccionado = nino },
                                        label = { Text(nino.nombre) }
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            ninoSeleccionado?.let { nino ->
                                onCrearMision(
                                    nombre,
                                    monedas.toIntOrNull() ?: 10,
                                    frecuencia,
                                    nino.id
                                )
                            }
                            mostrarFormulario = false
                            nombre = ""
                            monedas = "10"
                        },
                        enabled = nombre.isNotBlank() && ninoSeleccionado != null
                    ) { Text("Crear") }
                },
                dismissButton = {
                    TextButton(onClick = { mostrarFormulario = false }) { Text("Cancelar") }
                }
            )
        }
    }
}
