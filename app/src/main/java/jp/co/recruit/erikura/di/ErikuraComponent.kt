package jp.co.recruit.erikura.di

import dagger.Component
import javax.inject.Singleton


@Singleton
@Component(modules = [ApiModule::class])
interface ErikuraComponent {
//    fun erikuraApiService(): IErikuraApiService
}
