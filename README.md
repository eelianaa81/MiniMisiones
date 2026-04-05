# 🎯 MiniMisiones

> Aplicación Android nativa para la gamificación de tareas del hogar en familia.

---

## 📱 ¿Qué es MiniMisiones?

MiniMisiones convierte las tareas del hogar en **misiones gamificadas** para los hijos e hijas de una familia. Los adultos crean y asignan misiones, aprueban las entregas de los menores y gestionan el catálogo de premios. Los menores acumulan monedas virtuales y las canjean por premios.

Funciona **completamente offline**, sin necesidad de Internet ni cuenta de usuario.

---

## ✨ Funcionalidades

- 👨‍👩‍👧‍👦 **Gestión de familias** con código de invitación único
- 👤 **Perfiles por rol**: Papá, Mamá, Niño, Niña
- 📋 **Creación de misiones** con frecuencia diaria o semanal
- ✅ **Flujo de aprobación**: el menor marca la misión, el adulto aprueba o rechaza
- 🪙 **Sistema de monedas** con bonus de racha cada 5 días consecutivos aprobados (×2)
- 🎁 **Tienda de premios** canjeables definida por los administradores
- 📵 **100% offline** — sin servidor externo ni registro

---

## 🏗️ Arquitectura

La aplicación sigue el patrón **MVVM** (Model-View-ViewModel) con las siguientes capas:

```
com.minimisiones/
├── data/
│   ├── local/
│   │   ├── dao/          # DAOs de Room
│   │   ├── entities/     # Entidades de la base de datos
│   │   └── MiniMisionesDatabase.kt
│   └── repository/       # Repositorios con lógica de negocio
├── domain/
│   └── model/            # Modelos de dominio y enumeraciones
├── ui/
│   ├── screens/          # Pantallas Composable
│   └── viewmodel/        # ViewModels con StateFlow
└── MainActivity.kt       # Navegación y punto de entrada
```

---

## 🗄️ Modelo de datos

La base de datos local (Room/SQLite) consta de 6 tablas:

| Tabla | Descripción |
|-------|-------------|
| `familia` | Familias con código de invitación |
| `usuario` | Miembros con rol (PADRE, MADRE, NINO, NINA) y saldo de monedas |
| `mision` | Misiones asignadas a un menor con frecuencia y monedas |
| `entrega_mision` | Entregas con estado PENDIENTE / APROBADA / RECHAZADA |
| `premio` | Catálogo de premios canjeables por familia |
| `canje` | Historial de canjes realizados |

---

## 🛠️ Tecnologías utilizadas

| Tecnología | Versión | Uso |
|-----------|---------|-----|
| Kotlin | 2.0.21 | Lenguaje principal |
| Jetpack Compose BOM | 2024.10.01 | Interfaz de usuario |
| Room | 2.6.1 | Persistencia local |
| KSP | 2.0.21-1.0.27 | Procesamiento de anotaciones |
| Corrutinas Kotlin | 1.8.1 | Programación asíncrona |
| StateFlow / Flow | — | Estado reactivo de la UI |
| Android Gradle Plugin | 8.7.0 | Sistema de construcción |
| Core KTX | 1.15.0 | Extensiones Kotlin para Android |
| Lifecycle Runtime KTX | 2.8.7 | Ciclo de vida y corrutinas |
| Activity Compose | 1.9.3 | Integración Compose con Activity |

---

## 📲 Pantallas

| Pantalla | Rol | Descripción |
|---------|-----|-------------|
| Selección de familia | Todos | Lista de familias guardadas en el dispositivo |
| Selección de perfil | Todos | Lista de miembros con opción de añadir nuevos |
| Dashboard Admin | Admin | Acceso a misiones, aprobaciones y premios |
| Gestionar misiones | Admin | Crear y eliminar misiones asignadas a menores |
| Aprobar misiones | Admin | Aprobar o rechazar entregas pendientes |
| Dashboard Niño | Menor | Lista de misiones con botón ¡Conseguido! |
| Tienda de premios | Menor | Catálogo de premios canjeables con saldo actual |

---

## 🚀 Cómo ejecutar el proyecto

### Requisitos
- Android Studio Hedgehog o superior
- JDK 11
- Android SDK API 26+
- Emulador o dispositivo físico con Android 8.0+

### Pasos

1. Clona el repositorio:
```bash
git clone https://github.com/eelianaa81/MiniMisiones.git
```

2. Abre el proyecto en Android Studio.

3. Compila e instala con Gradle:
```bash
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat clean installDebug
```

4. O usa el botón **▶ Run** de Android Studio.

---

## 📌 Estado del proyecto

- [x] Arquitectura MVVM completa
- [x] Base de datos Room con 6 tablas
- [x] Flujo de aprobación de misiones
- [x] Sistema de monedas y racha
- [x] Tienda de premios
- [x] Gestión de misiones (crear / eliminar)
- [ ] Gestión de premios desde el admin
- [ ] Splash screen con logo animado
- [ ] Sincronización en la nube
- [ ] Notificaciones push
- [ ] Estadísticas y gráficas

---

## 👩‍💻 Autora

**Elena Elia** — TFG Ingeniería Informática / DAM 2024-2025

---

## 📄 Licencia

Proyecto académico desarrollado como Trabajo Fin de Grado.
