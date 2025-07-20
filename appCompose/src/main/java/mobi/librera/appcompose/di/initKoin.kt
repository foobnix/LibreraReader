package mobi.librera.appcompose.di

import androidx.room.Room
import mobi.librera.appcompose.model.DataModel
import mobi.librera.appcompose.room.AppDatabase
import mobi.librera.appcompose.room.BookDao
import mobi.librera.appcompose.room.BookRepository
import org.koin.android.ext.koin.androidApplication
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val appModule = module {
    // Provide the Room database instance as a singleton
    single<AppDatabase> {
        Room.databaseBuilder(
            androidApplication(), // Koin provides Application context
            AppDatabase::class.java,
            "book_database"
        ).build()
    }

    single<BookDao> {
        get<AppDatabase>().bookDao()
    }

    single<BookRepository> {
        BookRepository(get())
    }

    viewModelOf(::DataModel)
}

fun initKoin(config: KoinAppDeclaration) {
    startKoin {
        config.invoke(this)
        modules(
            appModule
        )

    }
}

