package jp.co.recruit.erikura.di

import dagger.Component
import jp.co.recruit.erikura.data.network.IErikuraApiService
import jp.co.recruit.erikura.data.network.IGoogleMapApiService
import jp.co.recruit.erikura.data.storage.AssetsManager
import jp.co.recruit.erikura.presenters.util.GoogleFitApiManager
import jp.co.recruit.erikura.presenters.util.LocationManager
import jp.co.recruit.erikura.presenters.util.PedometerManager
import javax.inject.Singleton


@Singleton
@Component(modules = [ErikuraModule::class])
interface ErikuraComponent {
    fun erikuraApiService(): IErikuraApiService
    fun googleMapApiService(): IGoogleMapApiService
    fun assetsManager(): AssetsManager
    fun locationManger(): LocationManager
    fun googleFitApiManager(): GoogleFitApiManager
    fun pedometerManager(): PedometerManager
}
