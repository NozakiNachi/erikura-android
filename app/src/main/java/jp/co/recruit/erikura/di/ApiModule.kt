package jp.co.recruit.erikura.di

import dagger.Module
import dagger.Provides
import jp.co.recruit.erikura.data.network.ErikuraApiServiceBuilder
import jp.co.recruit.erikura.data.network.IErikuraApiService
import javax.inject.Singleton

@Module
class ApiModule {
    @Singleton
    @Provides fun providesIErikuraApiService(): IErikuraApiService {
        return ErikuraApiServiceBuilder().create()
    }
}
