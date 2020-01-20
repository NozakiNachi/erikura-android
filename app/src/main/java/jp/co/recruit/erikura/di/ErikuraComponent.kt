package jp.co.recruit.erikura.di

import dagger.Component
import jp.co.recruit.erikura.data.network.IErikuraApiService
import jp.co.recruit.erikura.data.storage.AssetsManager
import jp.co.recruit.erikura.presenters.util.LocationManager
import javax.inject.Singleton


@Singleton
@Component(modules = [ErikuraModule::class])
interface ErikuraComponent {
    fun erikuraApiService(): IErikuraApiService
    fun assetsManager(): AssetsManager
    fun locationManger(): LocationManager

}
