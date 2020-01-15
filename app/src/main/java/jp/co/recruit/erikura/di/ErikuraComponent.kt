package jp.co.recruit.erikura.di

import dagger.Component
import jp.co.recruit.erikura.data.network.IErikuraApiService
import jp.co.recruit.erikura.data.storage.AssetsManager
import javax.inject.Singleton


@Singleton
@Component(modules = [ApiModule::class])
interface ErikuraComponent {
    fun erikuraApiService(): IErikuraApiService
    fun assetsManager(): AssetsManager
}
