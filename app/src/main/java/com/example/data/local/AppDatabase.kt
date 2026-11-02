package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Script
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Script::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scriptDao(): ScriptDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "teleprompter_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialScripts(database.scriptDao())
                    }
                }
            }

            suspend fun populateInitialScripts(dao: ScriptDao) {
                val starterScripts = listOf(
                    Script(
                        title = "TikTok / Reels: Hook Viral (30s)",
                        category = "TikTok/Reels",
                        fontSize = 38f,
                        scrollSpeed = 65f,
                        mirrorHorizontal = false,
                        showCueGuide = true,
                        content = """¡Alto ahí! Si todavía estás haciendo esto al grabar tus videos, estás perdiendo el 80% de tu audiencia en los primeros 3 segundos.

Hoy te revelo la estrategia que utilizan los mejores creadores para mantener la retención al máximo:

1. Empieza siempre con una pregunta provocativa o una afirmación que rompa el patrón visual.
2. Ve directo al grano sin saludos largos ni rodeos.
3. Aporta un tip práctico aplicable de inmediato.

Guarda este reel para tu próxima grabación y déjame en los comentarios qué tema quieres ver mañana."""
                    ),
                    Script(
                        title = "YouTube: Intro y Llamada a la Acción",
                        category = "YouTube",
                        fontSize = 36f,
                        scrollSpeed = 55f,
                        mirrorHorizontal = false,
                        showCueGuide = true,
                        content = """¡Hola a todos y bienvenidos a un nuevo video!

Hoy vamos a analizar a fondo las claves definitivas para que tus videos se vean con calidad de estudio profesional sin gastar una fortuna.

Si eres nuevo por aquí, recuerda suscribirte y activar la campanita para no perderte ningún tutorial como este.

Punto número 1: La iluminación. Una luz suave a 45 grados cambia por completo la presencia en cámara.
Punto número 2: El audio. Un buen micrófono de solapa o condensador es más importante que la propia cámara.
Punto número 3: La mirada. Usa este teleprompter manteniendo los ojos fijos en la línea guía para conectar directamente con tu espectador.

Quédate hasta el final porque te daré una plantilla gratuita. ¡Vamos allá!"""
                    ),
                    Script(
                        title = "Podcast: Bienvenida e Invitado Especial",
                        category = "Podcast",
                        fontSize = 34f,
                        scrollSpeed = 50f,
                        mirrorHorizontal = false,
                        showCueGuide = true,
                        content = """Muy buenas a todos y bienvenidos a un nuevo episodio de nuestro podcast.

Hoy tenemos el honor de recibir a un invitado excepcional, referente en la industria del contenido digital y estrategia de marca personal.

En esta charla hablaremos sobre los errores más comunes al iniciar un canal, cómo monetizar audiencias sin vender humo y qué esperar de las nuevas tendencias tecnológicas.

Prepara tus auriculares, ponte cómodo y disfruta de esta enriquecedora conversación. ¡Empezamos!"""
                    )
                )

                starterScripts.forEach { script ->
                    dao.insertScript(script)
                }
            }
        }
    }
}
