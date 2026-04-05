package com.minimisiones.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.minimisiones.data.local.dao.*
import com.minimisiones.data.local.entities.*


// ═══════════════════════════════════════════════════════════════════
// BASE DE DATOS — Room
// ═══════════════════════════════════════════════════════════════════
// Este archivo es el punto de entrada único a la base de datos.
// Une todas las entidades (tablas) y los DAOs (consultas) en
// un solo contenedor.
//
// Se implementa como Singleton — solo existe una instancia en toda
// la app para evitar abrir la BD varias veces a la vez.
// ═══════════════════════════════════════════════════════════════════

/**
 * Base de datos principal de MiniMisiones.
 *
 * Incluye las 6 tablas del proyecto:
 * Familia, Usuario, Mision, EntregaMision, Premio, Canje.
 *
 * @param version Número de versión de la BD. Si cambia el esquema
 *                 (añadir tabla, cambiar de campo) hay que incrementarlo.
 *
 */

@Database(
    entities = [
        FamiliaEntity::class,
        UsuarioEntity::class,
        MisionEntity::class,
        EntregaMisionEntity::class,
        PremioEntity::class,
        CanjeEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class Minimisionesdatabase : RoomDatabase() {

    // Cada función abstracta expone un DAO.
    // Room genera la implementación automáticamente.
    abstract fun familiaDao(): FamiliaDao
    abstract fun usuarioDao(): UsuarioDao
    abstract fun misionDao(): MisionDao
    abstract fun entregaMisionDao(): EntregaMisionDao
    abstract fun premioDao(): PremioDao
    abstract fun canjeDao(): CanjeDao

    companion object {

        //@Volatile garantiza que el valor sea visible para todos los hilos
        // al mismo tiempo - evita que dos hilos creen dos instancias distintas.
        @Volatile
        private var INSTANCE: Minimisionesdatabase? = null

        /**
         * Devuelve la única instancia de la base de datos.
         * Si no existe todavía, la crea.
         *
         * @param context Se usa el applicationContext para evitar
         *                memory leaks con Activities o Fragments.
         */
        fun getInstance(context: Context): Minimisionesdatabase {
            return INSTANCE ?: synchronized(this){
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    Minimisionesdatabase::class.java,
                    "minimisiones.db"
                )

                    //Si se cambia el esquema sin migración, borra y recrea la BD.
                    //En producción real se usarían migraciones explícitas.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}